package chord;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import util.Log;

/**
 * Implements a node in the Chord network.
 * 
 * @author Daniel McDonald
 */
public class RMINode implements RMINodeServer, RMINodeState {

	/**
	 * The key for the node.
	 */
	private long nodeKey;

	/**
	 * The length of the hash.
	 */
	private int hashLength;

	/**
	 * The finger table for this node.
	 */
	private FingerTable fingerTable;

	/**
	 * The storage structure for this node.
	 */
	private Map<Long, Serializable> nodeMap;

	/**
	 * The node's predecessor.
	 */
	private RMINodeServer predecessor;

	/**
	 * The file logger.
	 */
	private NodeFileLogger logger;

	/**
	 * A periodic task for updating the finger table.
	 */
	private final ScheduledExecutorService periodicTask = Executors.newScheduledThreadPool(1);

	/**
	 * Log the current state to the output file.
	 * 
	 * @param message
	 */
	private void logState(String message) {
		String s = getNodeKey() + ": p=";
		try {
			s += predecessor.getNodeKey();
		} catch (Throwable t) {
			s += "-";
		}
		logger.logOutput(s + " " + fingerTable.toString() + " --> " + message);
	}

	/**
	 * Constructs an RMINode.
	 * 
	 * @param hashLength
	 *          Length of the hash.
	 * @param key
	 *          The key to use.
	 * @throws RemoteException
	 */
	public RMINode(int hashLength, long key) throws RemoteException {
		double keySpace = Math.pow(2, hashLength);

		if (key > keySpace)
			throw new IllegalArgumentException("key (" + key + ") must be less than the keyspace provided by hashLength (" + keySpace + ")");

		this.hashLength = hashLength;
		this.nodeKey = key;
		fingerTable = new FingerTable(this);
		logger = new NodeFileLogger(key);
		nodeMap = new ConcurrentHashMap<>();
	}

	/**
	 * Join the given network.
	 * 
	 * @param fromNetwork
	 *          The node to join.
	 * @throws RemoteException
	 */
	public void join(RMINodeServer fromNetwork) throws RemoteException {
		if (fromNetwork != null) {
			RMINodeServer successor = fromNetwork.findSuccessor(getNodeKey());
			if (successor.getNodeKey() == getNodeKey())
				throw new IllegalArgumentException("A node with this key already exists in the network");

			fingerTable.getSuccessor().setNode(successor);
			predecessor = successor.findPredecessor(successor.getNodeKey());
			successor.checkPredecessor(this);
			for (Finger f : fingerTable)
				f.setNode(fromNetwork.findSuccessor(f.getStart()));

			logState("node " + getNodeKey() + " added to network by " + fromNetwork.getNodeKey());
		} else {
			// this is currently the only node in the network, so set all fingers and
			// predecessor to self
			for (Finger f : fingerTable)
				f.setNode(this);
			predecessor = this;

			logState("new network created by " + getNodeKey());
		}

		periodicTask.scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				try {
					stabilize();
					if (fingerTable != null)
						fixFinger(fingerTable.getRandomFinger());
				} catch (Throwable t) {
					Log.err("error running periodic task: " + t.getClass());
					t.printStackTrace();
				}
			}
		}, 500, 500, TimeUnit.MILLISECONDS);
	}

	public void leave() throws RemoteException {
		periodicTask.shutdown();
		// notify everyone that should point to us that we're leaving
		if (predecessor != null)
			predecessor.nodeLeaving(this);

		for (int i = 0; i < getHashLength(); i++) {
			long key = (long) (getNodeKey() - Math.pow(2, i));
			if (key < 0)
				key = (long) (Math.pow(2, getHashLength()) - key);
			RMINodeServer node = findSuccessor(key);
			if (node != null)
				node.nodeLeaving(this);
		}

		RMINodeServer successor = fingerTable.getSuccessor().getNode();
		if (successor != null) {
			successor.checkPredecessor(predecessor);// tell our successor who their
																							// new predecessor is

			for (Long key : nodeMap.keySet())
				// transfer all our values to our successor
				successor.put(key, nodeMap.remove(key));
		}

		nodeMap = null;
		fingerTable = null;
	}

	/**
	 * Tests whether the key is within the given interval.
	 * 
	 * @param leftInclusive
	 *          <code>true</code> if this should be left inclusive.
	 * @param left
	 *          The left edge of the interval.
	 * @param x
	 *          The key to test.
	 * @param right
	 *          The right edge of the interval.
	 * @param rightInclusive
	 *          <code>true</code> if this should be right inclusive.
	 * @return <code>true</code> if the key is within the interval.
	 */
	private boolean isWithinInterval(boolean leftInclusive, long left, long x, long right, boolean rightInclusive) {
		// if the bounds have been violated, it's not within the interval
		if ((!leftInclusive && left == x) || (!rightInclusive && right == x))
			return false;

		// if left and right are the same, the entire key space is contained in the
		// set. And since we know the bounds are correct, we know that x is in the
		// interval
		if (left == right)
			return true;

		// if left > right, then we're in a situation where the interval spans the
		// end and beginning of the ring. The logic is a bit different there
		if (left > right)
			return left <= x || x <= right;

		return left <= x && x <= right;
	}

	/**
	 * Tests whether the key is in range.
	 * 
	 * @param key
	 *          The key to test.
	 * @return <code>true</code> if the key is in range
	 * @throws RemoteException
	 */
	private boolean isInRange(long key) throws RemoteException {
		if (predecessor == null)
			return false;

		try {
			// special case: if we're the only node in the network, then we're our own
			// predecessor. That means we own the whole god-damned thing
			if (predecessor.getNodeKey() == getNodeKey())
				return true;

			return isWithinInterval(false, predecessor.getNodeKey(), key, getNodeKey(), true);
		} catch (RemoteException e) {
			predecessor = null; // if we got a remote exception, then the predecessor
													// is no longer online
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getNodeKey() {
		return nodeKey;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getHashLength() {
		return hashLength;
	}

	public NodeState getState() throws RemoteException {
		if (fingerTable == null)
			throw new RemoteException();

		ArrayList<Long> fingers = new ArrayList<>();
		for (Finger f : fingerTable)
			if (f.getNode() != null)
				fingers.add(f.getNode().getNodeKey());
			else
				fingers.add(f.getStart() * -1);

		return new NodeState(getNodeKey(), predecessor != null ? predecessor.getNodeKey() : getNodeKey() - 1, fingers, 0);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Serializable get(String key) throws RemoteException {
		return get(new KeyHash<String>(key, getHashLength()).getHash());
	}

	@Override
	public Serializable get(long key) throws RemoteException {
		logger.logOutput("Requested object with key " + key);
		if (isInRange(key)) {
			Serializable value = nodeMap.get(key);
			logger.logOutput("Got object with key " + key);
			return value;
		}
		logger.logOutput("Object not found; passed request to successor with key " + key);
		return findSuccessor(key).get(key);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void put(String key, Serializable value) throws RemoteException {
		put(new KeyHash<String>(key, getHashLength()).getHash(), value);
	}

	@Override
	public void put(long key, Serializable value) throws RemoteException {
		logger.logOutput("Requested object insert with key " + key);
		if (isInRange(key)) {
			logger.logOutput("Inserted object into storage with key " + key);
			nodeMap.put(key, value);
		} else {
			logger.logOutput("Hashed to value at another node; passed insert request to successor");
			findSuccessor(key).put(key, value);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void delete(String key) throws RemoteException {
		delete(new KeyHash<String>(key, getHashLength()).getHash());
	}

	@Override
	public void delete(long key) throws RemoteException {
		logger.logOutput("Requested object delete with key " + key);
		if (isInRange(key)) {
			logger.logOutput("Deleted object with key " + key);
			nodeMap.remove(key);
		} else {
			logger.logOutput("Hashed to value at another node; passed delete request to successor");
			findSuccessor(key).delete(key);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public RMINodeServer findSuccessor(long key) throws RemoteException {
		// if the key belongs to our interval, then we're the successor
		if (isInRange(key))
			return this;

		RMINodeServer predecessor = findPredecessor(key);
		if (predecessor == null)
			return null;

		if (predecessor.getNodeKey() == getNodeKey())
			return fingerTable.getSuccessor().getNode();

		return predecessor.findSuccessor(key);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public RMINodeServer findPredecessor(long key) throws RemoteException {
		if (fingerTable == null)
			throw new RemoteException();

		// if the key belongs to our interval, then return our predecessor
		if (isInRange(key))
			return predecessor;

		try {
			// if the key belongs to the interval of our successor, then we're the
			// predecessor
			if (fingerTable.getSuccessor().getNode() != null && isWithinInterval(false, getNodeKey(), key, fingerTable.getSuccessor().getNode().getNodeKey(), true))
				return this;
		} catch (NullPointerException npe) {
		}

		for (Finger f : fingerTable.reverse())
			try {
				if (f.getNode() != null && isWithinInterval(false, getNodeKey(), f.getNode().getNodeKey(), key, false))
					return f.getNode().findPredecessor(key);
			} catch (RemoteException e) {
				f.setNode(null);
			}

		if (predecessor == null)
			return null;
		return predecessor.findPredecessor(key);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void checkPredecessor(RMINodeServer potentialPredecessor) throws RemoteException {
		long potentialPredecessorKey = potentialPredecessor.getNodeKey();
		if (predecessor == null || isWithinInterval(false, predecessor.getNodeKey(), potentialPredecessorKey, getNodeKey(), false)) {
			this.predecessor = potentialPredecessor;
			logState(getNodeKey() + ".predecessor = " + potentialPredecessorKey);
			for (Long key : nodeMap.keySet())
				// relocate all keys that are no longer within our range to the new
				// predecessor
				if (isWithinInterval(false, getNodeKey(), key, potentialPredecessorKey, true))
					potentialPredecessor.put(key, nodeMap.remove(key));
		}
	}

	@Override
	public void nodeLeaving(RMINodeServer leavingNode) throws RemoteException {
		long leavingNodeKey = leavingNode.getNodeKey();
		if (predecessor != null && predecessor.getNodeKey() == leavingNodeKey)
			predecessor = null;
		for (Finger f : fingerTable) {
			RMINodeServer node = f.getNode();
			if (node != null && node.getNodeKey() == leavingNodeKey)
				f.setNode(null);
		}
	}

	/**
	 * Fix the given finger in the finger table.
	 * 
	 * @param finger
	 *          The finger to fix.
	 */
	private void fixFinger(Finger finger) {
		try {
			RMINodeServer successor = findSuccessor(finger.getStart());
			if (successor == null)
				return;
			if (finger.getNode() != null && finger.getNode().getNodeKey() != successor.getNodeKey()) {
				finger.setNode(successor);
				logState(getNodeKey() + ".finger[" + finger.getStart() + "] = " + successor.getNodeKey());
			}
		} catch (RemoteException e) {
			finger.setNode(null);
		}
	}

	/**
	 * Stabilize the finger table.
	 */
	private void stabilize() {
		RMINodeServer successor = fingerTable.getSuccessor().getNode();
		if (successor != null) {
			try {
				RMINodeServer successor_predecessor = successor.findPredecessor(successor.getNodeKey());
				if (successor_predecessor != null) {
					if (isWithinInterval(false, getNodeKey(), successor_predecessor.getNodeKey(), successor.getNodeKey(), false) && successor_predecessor.getNodeKey() != fingerTable.getSuccessor().getNode().getNodeKey()) {
						fingerTable.getSuccessor().setNode(successor_predecessor);
						logState(getNodeKey() + ".successor = " + successor_predecessor.getNodeKey());
					}
					successor.checkPredecessor(this);
				}
			} catch (RemoteException e) {
			}
		}
	}
}
