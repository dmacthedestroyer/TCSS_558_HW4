package chord;

import java.util.Enumeration;

/**
 * An event that indicates that a state change has occurred for a node.
 * @author dmac
 *
 */
public interface NodeStateChangeEvent {
	/**
	 * Returns the key for the node whose state has changed
	 * @return
	 */
	int getNodeKey();
	
	/**
	 * Returns the hash length for the node whose state has changed
	 * @return
	 */
	int getHashLength();
	
	/**
	 * Returns the predecessor key for the node whose state has changed
	 * @return
	 */
	int getPredecessorKey();
	
	/**
	 * Returns a list of the keys for the actual nodes that each of the fingers point to
	 * @return
	 */
	Enumeration<Integer> getFingerKeys();
}