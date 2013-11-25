package chord;

/**
 * An interface for registering event listeners for state changes in a given node.
 * @author dmac
 *
 */
public interface NodeStateChangeObservable {
	void addNodeStateChangeListener(NodeStateChangeListener listener);
}
