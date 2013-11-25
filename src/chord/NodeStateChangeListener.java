package chord;

/**
 * The listener interface for receiving state change events. The class that is
 * interested in receiving notifications of state change implements this
 * interface, and the object created with that class is registered with
 * {@link NodeStateChangeObservable}. When a state change occurs, that object's
 * {@code stateChanged} method is invoked.
 * 
 * @author dmac
 * 
 */
public interface NodeStateChangeListener {
	void stateChanged(NodeStateChangeEvent e);
}
