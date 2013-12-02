package gui;

import gui.util.JAutoSubscribeActionButton;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import chord.RMINode;
import chord.RMINodeServer;

public class ChordNetworkController extends JPanel {
	private String host;
	private int port;

	private JTextField txtStartNodeId = new JTextField(10);
	private JTextField txtStartNodeM = new JTextField(3);

	private JTextField txtAddRemoveNodeId = new JTextField(10);

	public ChordNetworkController(final String host, final int port) {
		super(new FlowLayout());
		this.host = host;
		this.port = port;

		add(new JLabel("node id:"));
		add(txtStartNodeId);
		add(new JLabel("m:"));
		add(txtStartNodeM);
		add(new JAutoSubscribeActionButton("seed network", new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					RMINode node = new RMINode(Integer.parseInt(txtStartNodeM.getText()), Long.parseLong(txtStartNodeId.getText()));
					LocateRegistry.createRegistry(port).bind(""+ node.getNodeKey(), UnicastRemoteObject.exportObject(node, 0));
					node.join(null);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}));

		add(new JSeparator(SwingConstants.VERTICAL));

		add(txtAddRemoveNodeId);
		add(new JAutoSubscribeActionButton("add node", new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub

			}
		}));
		add(new JAutoSubscribeActionButton("remove node", new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub

			}
		}));
	}
}
