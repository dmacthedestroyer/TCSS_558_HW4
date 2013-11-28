package gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Timer;

import chord.NodeState;
import chord.RMINodeState;

public class Main extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 967889003587846649L;

	/**
	 * Create the GUI and show it. For thread safety, this method should be
	 * invoked from the event-dispatching thread.
	 */
	private static void createAndShowGUI(String[] args) {
		String host = null;
		int port = 0;

		switch (args.length) {
		case 0:
			break;
		case 1:
			port = Integer.parseInt(args[0]);
			break;
		case 2:
			host = args[0];
			port = Integer.parseInt(args[1]);
			break;
		default:
			System.out.print("no more than two arguments allowed");
			return;
		}

		// Create and set up the window.
		JFrame frame = new JFrame("Chord node visualization");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// Create and set up the content pane.
		Main newContentPane = new Main(host, port);
		newContentPane.setOpaque(true); // content panes must be opaque
		frame.setContentPane(newContentPane);

		// Display the window.
		frame.pack();
		frame.setVisible(true);
	}

	public static void main(final String[] args) {
		// Schedule a job for the event-dispatching thread:
		// creating and showing this application's GUI.
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI(args);
			}
		});
	}

	private String host;
	private int port;
	private ChordNetworkTable chordNetworkTable = new ChordNetworkTable();

	private Timer poller = new Timer(1000, new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			chordNetworkTable.update(getNetworkState());
		}
	});

	public Main(String host, int port) {
		this.host = host;
		this.port = port;

		add(new JScrollPane(chordNetworkTable));

		poller.start();
	}

	private List<NodeState> getNetworkState() {
		Registry registry;
		try {
			System.out.print("locating registry...   ");
			registry = LocateRegistry.getRegistry(host, port);
			System.out.println("done.");
		} catch (RemoteException e) {
			e.printStackTrace();
			return new ArrayList<NodeState>();
		}

		try {
			ArrayList<NodeState> states = new ArrayList<>();
			System.out.print("acquiring node names...   ");
			String[] names = registry.list();
			System.out.println(names.length + " nodes found.");
			for (String name : names) {
				try {
					RMINodeState state = (RMINodeState) registry.lookup(name);
					states.add(state.getState());
				} catch (RemoteException re) {
					registry.unbind(name);

					System.out.println("bad registry entry found at id " + name);
					re.printStackTrace();
				}
			}

			java.util.Collections.sort(states, new Comparator<NodeState>() {
				@Override
				public int compare(NodeState o1, NodeState o2) {
					return (int) (o1.getKey() - o2.getKey());
				}
			});

			return states;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
