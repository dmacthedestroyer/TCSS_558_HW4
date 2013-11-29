package chord;

import java.io.Serializable;
import java.util.List;

public class NodeState implements Serializable {

	private long key, predecessor, valueCount;
	private List<Long> fingers;

	public NodeState(long key, long predecessor, List<Long> fingers, long valueCount) {
		this.key = key;
		this.predecessor = predecessor;
		this.fingers = fingers;
		this.valueCount = valueCount;
	}

	public long getKey() {
		return key;
	}

	public long getPredecessor() {
		return predecessor;
	}

	public List<Long> getFingers() {
		return fingers;
	}

	public long getValueCount() {
		return valueCount;
	}
}
