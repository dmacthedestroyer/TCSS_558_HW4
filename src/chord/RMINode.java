package chord;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements a node in the Chord network.
 * 
 * @author Daniel McDonald
 */
public class RMINode implements RMINodeServer, RMINodeState {

	private final int hashLength;
	private final long nodeKey;
	private FingerTable fingerTable;
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
						wait(1000);
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
		fingerTable = new FingerTable(this.hashLength, this.nodeKey);

		backgroundThread.start();
	}

	private void checkHasNodeLeft() throws RemoteException {
		if (hasNodeLeft)
			throw new RemoteException();
	}

	@Override
	public int getHashLength() throws RemoteException {
		checkHasNodeLeft();
		return hashLength;
	}

	@Override
	public long getNodeKey() throws RemoteException {
		checkHasNodeLeft();
		return nodeKey;
	}

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

	@Override
	public void leave() throws RemoteException {
		hasNodeLeft = true;
	}

	@Override
	public Serializable get(long key) throws RemoteException {
		checkHasNodeLeft();

		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Serializable get(String key) throws RemoteException {
		checkHasNodeLeft();

		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void put(long key, Serializable value) throws RemoteException {

		// TODO Auto-generated method stub

	}

	@Override
	public void put(String key, Serializable value) throws RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public void delete(long key) throws RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public void delete(String key) throws RemoteException {
		// TODO Auto-generated method stub

	}

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

	@Override
	public RMINodeServer getPredecessor() throws RemoteException {
		checkHasNodeLeft();

		return predecessor;
	}

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
			if (ringRange.isInRange(false, predecessor.getNodeKey(), potentialPredecessorNodeKey, nodeKey, false))
				predecessor = potentialPredecessor;
				for (Finger f : fingerTable) {
					if(f.getNode().getNodeKey() <= predecessor.getNodeKey()){
						predecessor.put(f.getNode().getNodeKey(), this.get(f.getNode().getNodeKey()));
					}
				}
		} catch (NullPointerException | RemoteException e) {
			for (Finger f : fingerTable) {
				if(f.getNode().getNodeKey() <= predecessor.getNodeKey()){
					predecessor.put(f.getNode().getNodeKey(), this.get(f.getNode().getNodeKey()));
				}
			}
			predecessor = potentialPredecessor;
		}
	}

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
}
