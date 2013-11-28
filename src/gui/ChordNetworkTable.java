package gui;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import chord.NodeState;

public class ChordNetworkTable extends JPanel {

	public void update(List<NodeState> networkState) {
		this.removeAll();

		if (networkState.size() == 0)
			return;

		String[] columns = initializeColumns(networkState.get(0).getFingers().length);
		Object[][] data = new Object[networkState.size()][columns.length];
		for (int i = 0; i < networkState.size(); i++) {
			NodeState node = networkState.get(i);
			Object[] row = new Object[columns.length];
			row[0] = node.getKey();
			row[1] = node.getPredecessor();
			for (int k = 0; k < columns.length - 3; k++)
				row[k + 2] = node.getFingers()[k];
			row[columns.length - 1] = node.getValueCount();
		}

		JTable table = new JTable(data, columns);
		table.setPreferredScrollableViewportSize(new Dimension(500, 70));
		table.setFillsViewportHeight(true);

		this.add(new JScrollPane(table));
	}

	public ChordNetworkTable() {
		super(new GridLayout(1, 0));

		update(new ArrayList<NodeState>() {
			{
				add(new NodeState(0, 0, new long[3], 0));
			}
		});
	}

	private String[] initializeColumns(int numFingers) {
		String[] columns = new String[3 + numFingers];
		columns[0] = "key";
		columns[1] = "predecessor";
		for (int i = 0; i < numFingers; i++)
			columns[i + 2] = "" + i;
		columns[numFingers + 2] = "values";

		return columns;
	}
}
