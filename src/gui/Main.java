package gui;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class Main extends JPanel {
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

	private ChordNetworkTable chordNetworkTable;

	public Main(String host, int port) {
		add((chordNetworkTable = new ChordNetworkTable()));

		pollRegistry();
	}

	private void pollRegistry() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (true)
					try {
						Thread.sleep(500);
						System.out.println("polling the stuff");
					} catch (Exception e) {
						e.printStackTrace();
						
						return;
					}
			}
		}).start();
	}
}
