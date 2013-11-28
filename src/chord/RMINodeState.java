package chord;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMINodeState extends Remote {
	NodeState getState() throws RemoteException;
}
