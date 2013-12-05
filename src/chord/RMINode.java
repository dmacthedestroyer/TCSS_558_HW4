package chord;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implements a node in the Chord network.
 * 
 * @author Daniel McDonald
 */
public class RMINode implements RMINodeServer, RMINodeState {

	private static final int FIX_FINGER_INTERVAL = 1000;
	private final int networkRetries;
	private final int hashLength;
	private final long nodeKey;
	private FingerTable fingerTable;
	private final Map<Long, Serializable> nodeStorage = new ConcurrentHashMap<>();
	private RMINodeServer predecessor;
	private boolean hasNodeLeft;
	private final RingRange ringRange = new RingRange();
	private final Thread backgroundThread = new Thread() {
		{
			setDaemon(true);
		}

		public void run() {
			while (!isInterrupted()) {
				try {
					synchronized (this) {
						wait(FIX_FINGER_INTERVAL);
					}
				} catch (InterruptedException e) {
					break;
				}
				stabilize();
				fixFinger(fingerTable.getRandomFinger());
			}
		}
	};

	public RMINode(final int hashLength, final long nodeKey) {
		this.hashLength = hashLength;
		this.nodeKey = nodeKey;
		networkRetries = hashLength + 1;
		fingerTable = new FingerTable(this.hashLength, this.nodeKey);
		backgroundThread.start();
	}

	/**
	 * Check if this node has left and throw an exception if it has.
	 * 
	 * @throws RemoteException
	 */
	private void checkHasNodeLeft() throws RemoteException {
		if (hasNodeLeft)
			throw new RemoteException();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getHashLength() throws RemoteException {
		checkHasNodeLeft();
		return hashLength;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getNodeKey() throws RemoteException {
		checkHasNodeLeft();
		return nodeKey;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public NodeState getState() throws RemoteException {
		checkHasNodeLeft();

		long predecessorNodeKey;
		try {
			predecessorNodeKey = predecessor.getNodeKey();
		} catch (NullPointerException | RemoteException e) {
			predecessor = null;
			predecessorNodeKey = nodeKey * -1;
		}

		List<Long> fingers = new ArrayList<>();
		for (Finger f : fingerTable)
			try {
				fingers.add(f.getNode().getNodeKey());
			} catch (NullPointerException | RemoteException e) {
				f.setNode(null);
				fingers.add(f.getStart() * -1);
			}

		return new NodeState(getNodeKey(), predecessorNodeKey, fingers, 0);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void join(RMINodeServer fromNetwork) throws RemoteException {
		checkHasNodeLeft();

		if (fromNetwork != null) {
			fingerTable.getSuccessor().setNode(fromNetwork.findSuccessor(nodeKey));
		} else {
			// the network is empty
			fingerTable.getSuccessor().setNode(this);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void leave() throws RemoteException {
		hasNodeLeft = true;
		forwardDataToSuccessor();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Serializable get(long key) throws RemoteException {
		for (int i = 0; i < networkRetries; i++) {
			checkHasNodeLeft();
			try {
				RMINodeServer server = findSuccessor(key);
				if (nodeKey == server.getNodeKey())
					return nodeStorage.get(key);
				else
					return server.get(key);
			} catch (NullPointerException | RemoteException e	) {
				// some node somewhere is dead... wait a while for our fingers to
				// correct then try again
				try {
					Thread.sleep(FIX_FINGER_INTERVAL);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		}
		// tried a bunch of times and failed, throw in the towel.
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Serializable get(String key) throws RemoteException {
		return get(new KeyHash<String>(key, hashLength).getHash());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void put(long key, Serializable value) throws RemoteException {
		for (int i = 0; i < networkRetries; i++) {
			checkHasNodeLeft();
			try {
				RMINodeServer server = findSuccessor(key);
				if (nodeKey == server.getNodeKey())
					nodeStorage.put(key, value);
				else
					server.put(key, value);
			} catch (NullPointerException | RemoteException e	) {
				// some node somewhere is dead... wait a while for our fingers to
				// correct then try again
				try {
					Thread.sleep(FIX_FINGER_INTERVAL);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		}
		// tried a bunch of times and failed, throw in the towel.
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void put(String key, Serializable value) throws RemoteException {
		put(new KeyHash<String>(key, hashLength).getHash(), value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void delete(long key) throws RemoteException {
		for (int i = 0; i < networkRetries; i++) {
			checkHasNodeLeft();
			try {
				RMINodeServer server = findSuccessor(key);
				if (nodeKey == server.getNodeKey())
					nodeStorage.remove(key);
				else
					server.delete(key);
			} catch (NullPointerException | RemoteException e	) {
				// some node somewhere is dead... wait a while for our fingers to
				// correct then try again
				try {
					Thread.sleep(FIX_FINGER_INTERVAL);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		}
		// tried a bunch of times and failed, throw in the towel.
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void delete(String key) throws RemoteException {
		delete(new KeyHash<String>(key, hashLength).getHash());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public RMINodeServer findSuccessor(long key) throws RemoteException {
		checkHasNodeLeft();

		long successorNodeKey;
		try {
			successorNodeKey = fingerTable.getSuccessor().getNode().getNodeKey();
		} catch (NullPointerException | RemoteException e) {
			fingerTable.getSuccessor().setNode(this);
			return findSuccessor(key);
		}

		if (ringRange.isInRange(false, nodeKey, key, successorNodeKey, true))
			return fingerTable.getSuccessor().getNode();

		for (Finger f : fingerTable.reverse()) {
			try {
				if (ringRange.isInRange(false, nodeKey, f.getNode().getNodeKey(), key, false))
					return f.getNode().findSuccessor(key);
			} catch (NullPointerException | RemoteException e) {
				f.setNode(f.getStart() == fingerTable.getSuccessor().getStart() ? this : null);
			}
		}

		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public RMINodeServer getPredecessor() throws RemoteException {
		checkHasNodeLeft();
		return predecessor;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void checkPredecessor(RMINodeServer potentialPredecessor) throws RemoteException {
		checkHasNodeLeft();

		long potentialPredecessorNodeKey;
		try {
			potentialPredecessorNodeKey = potentialPredecessor.getNodeKey();
		} catch (NullPointerException | RemoteException e) {
			return;
		}

		try {
			if (ringRange.isInRange(false, predecessor.getNodeKey(), potentialPredecessorNodeKey, nodeKey, false)) {
				predecessor = potentialPredecessor;
				forwardDataToPredecessor();
			}
		} catch (NullPointerException | RemoteException e) {
			predecessor = potentialPredecessor;
			forwardDataToPredecessor();
		}
	}

	/**
	 * When the predecessor changes, this function forwards values the new predecessor
	 * should manage now.
	 * 
	 * @param predecessor
	 * @throws RemoteException
	 */

	public void forwardDataToPredecessor() {
		try {
			long predecessorNodeKey = predecessor.getNodeKey();
			for (Long key : nodeStorage.keySet())
				if (ringRange.isInRange(false, nodeKey, key, predecessorNodeKey, true))
					predecessor.put(key, nodeStorage.remove(key));
		} catch (RemoteException r) {
			// node doesn't exist... This means the predecessor JUST crashed.
			// f*$k it, let's go bowling.
		}
	}

	/**
	 * When the node gracefully leaves, this puts all data to the successor, who
	 * now controls it.
	 * 
	 * @param successor
	 * @throws RemoteException
	 */

	public void forwardDataToSuccessor() {
		RMINodeServer successor = null;
		// find the first finger that's not us
		for (Finger f : fingerTable)
			try {
				if (f.getNode().getNodeKey() != nodeKey)
					successor = f.getNode();
				break;
			} catch (NullPointerException | RemoteException e) {
			}

		// no point in doing anything if we're our own successor
		if (successor == null)
			return;

		try {
			for (Long key : nodeStorage.keySet())
				successor.put(key, nodeStorage.remove(key));
		} catch (NullPointerException | RemoteException r) {
			// node doesn't exist, so do nothing...
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void nodeLeaving(long leavingNodeKey) throws RemoteException {
		checkHasNodeLeft();

		try {
			if (predecessor.getNodeKey() == leavingNodeKey)
				predecessor = null;
		} catch (NullPointerException | RemoteException e) {
			predecessor = null;
		}

		for (Finger f : fingerTable) {
			try {
				if (f.getNode().getNodeKey() == leavingNodeKey)
					f.setNode(f.getStart() == fingerTable.getSuccessor().getStart() ? this : null);
			} catch (NullPointerException | RemoteException e) {
				f.setNode(f.getStart() == fingerTable.getSuccessor().getStart() ? this : null);
			}
		}
	}

	private void fixFinger(Finger f) {
		try {
			f.setNode(findSuccessor(f.getStart()));
		} catch (RemoteException e) {
			f.setNode(null);
		}
	}

	private void stabilize() {
		long successorNodeKey;
		RMINodeServer successor;
		try {
			successor = fingerTable.getSuccessor().getNode();
			successorNodeKey = successor.getNodeKey();
		} catch (NullPointerException | RemoteException e) {
			successor = this;
			fingerTable.getSuccessor().setNode(this);
			successorNodeKey = nodeKey;
		}

		try {
			RMINodeServer successor_predecessor = successor.getPredecessor();
			if (successor_predecessor != null && ringRange.isInRange(false, nodeKey, successor_predecessor.getNodeKey(), successorNodeKey, false))
				fingerTable.getSuccessor().setNode(successor_predecessor);
		} catch (RemoteException e) {
		}

		try {
			fingerTable.getSuccessor().getNode().checkPredecessor(this);
		} catch (RemoteException e) {
			fingerTable.getSuccessor().setNode(this);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object other) {
		RMINode otherNode;
		if (other instanceof RMINode) {
			otherNode = (RMINode) other;
		} else {
			return false;
		}
		try {
			if (this.getNodeKey() == otherNode.getNodeKey()) {
				return true;
			} else {
				return false;
			}
		} catch (RemoteException e) {
			return false;
		}
	}
}
