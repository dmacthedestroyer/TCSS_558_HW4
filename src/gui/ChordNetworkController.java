package gui;

import gui.util.JAutoSubscribeActionButton;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Random;

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

	private JTextField txtAddNodeId = new JTextField(10);
	private JTextField txtRemoveNodeId = new JTextField(10);

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
					LocateRegistry.createRegistry(port).bind("" + node.getNodeKey(), UnicastRemoteObject.exportObject(node, 0));
					node.join(null);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}));

		add(new JSeparator(SwingConstants.VERTICAL));

		add(txtAddNodeId);
		txtAddNodeId.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				addNode();

			}
		});
		add(new JAutoSubscribeActionButton("add node", new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				addNode();
			}
		}));

		add(new JSeparator());

		add(txtRemoveNodeId);
		txtRemoveNodeId.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				removeNode();
			}
		});
		add(new JAutoSubscribeActionButton("remove node", new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				removeNode();
			}
		}));
	}

	private RMINodeServer getRandomNode() {
		try {
			Registry registry = LocateRegistry.getRegistry(host, port);
			String[] nodeList = registry.list();
			return (RMINodeServer) registry.lookup("" + nodeList[new Random().nextInt(nodeList.length)]);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private void addNode() {
		try {
			RMINodeServer fromNetwork = getRandomNode();
			if (fromNetwork == null)
				return;

			RMINode node = new RMINode(fromNetwork.getHashLength(), Long.parseLong(txtAddNodeId.getText()));
			LocateRegistry.getRegistry(host, port).bind("" + node.getNodeKey(), UnicastRemoteObject.exportObject(node, 0));
			node.join(fromNetwork);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		txtAddNodeId.grabFocus();
		txtAddNodeId.selectAll();
	}

	private void removeNode() {
		try {
			((RMINodeServer)LocateRegistry.getRegistry(host, port).lookup(txtRemoveNodeId.getText())).leave();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		txtRemoveNodeId.grabFocus();
		txtRemoveNodeId.selectAll();
	}
}
