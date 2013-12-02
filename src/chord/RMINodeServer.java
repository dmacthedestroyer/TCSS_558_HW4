package chord;
import java.io.Serializable;
import java.rmi.RemoteException;

/** 
 * The RMINodeServer exposes methods that a node can use to interact with other nodes via RMI.
 */
public interface RMINodeServer extends RMINodeClient {
	/**
	 * returns the log(base2) of the total number of nodes allowed in the network
	 * @return
	 * @throws RemoteException
	 */
	public int getHashLength() throws RemoteException;
	
	/**
	 * returns the id of this node
	 * @return
	 * @throws RemoteException
	 */
	public long getNodeKey() throws RemoteException;

	/**
	 * returns the value stored at the given key in the network
	 * @param key
	 * @return
	 * @throws RemoteException
	 */
	public Serializable get(long key) throws RemoteException;
	
	/**
	 * sets the given value at the given key in the network
	 * @param key
	 * @param value
	 * @throws RemoteException
	 */
	public void put(long key, Serializable value) throws RemoteException;
	
	/**
	 * removes the value at the given key in the network
	 * @param key
	 * @throws RemoteException
	 */
	public void delete(long key) throws RemoteException;
	
	/**
	 * Finds the successor node for the given key
	 * @param key
	 * @return
	 * @throws RemoteException
	 */
	public RMINodeServer findSuccessor(long key) throws RemoteException;
	
	/**
	 * Finds the predecessor node for the given key
	 * @param key
	 * @return
	 * @throws RemoteException
	 */
	public RMINodeServer findPredecessor(long key) throws RemoteException;
	
	/**
	 * Check whether the specified node should be your predecessor
	 * @param potentialPredecessor
	 * @return
	 * @throws RemoteException
	 */
	public void checkPredecessor(RMINodeServer potentialPredecessor) throws RemoteException;
}
