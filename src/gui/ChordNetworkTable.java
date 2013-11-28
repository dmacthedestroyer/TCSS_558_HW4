package gui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import chord.NodeState;

public class ChordNetworkTable extends JTable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -8601345570257078826L;

	public void update(List<NodeState> networkState) {
		if (networkState.size() == 0)
			return;

		DefaultTableModel tableModel = ((DefaultTableModel) getModel());
		String[] columns = initializeColumns(networkState.get(0).getFingers().size());
		tableModel.setColumnIdentifiers(columns);
		tableModel.setRowCount(0);

		for (int i = 0; i < networkState.size(); i++) {
			NodeState node = networkState.get(i);
			Object[] row = new Object[columns.length];
			row[0] = node.getKey();
			row[1] = node.getPredecessor();
			for (int k = 0; k < columns.length - 3; k++)
				row[k + 2] = node.getFingers().get(k);
			row[columns.length - 1] = node.getValueCount();

			tableModel.addRow(row);
		}
	}

	public ChordNetworkTable() {
		super();

		update(new ArrayList<NodeState>() {
			/**
			 * 
			 */
			private static final long serialVersionUID = 6368136066303131409L;

			{
				add(new NodeState(0, 0, new ArrayList<Long>(), 0));
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
