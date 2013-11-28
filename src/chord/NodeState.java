package chord;

public class NodeState {

	private long key, predecessor, valueCount;
	private long[] fingers;

	public NodeState(long key, long predecessor, long[] fingers, long valueCount) {
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

	public long[] getFingers() {
		return fingers;
	}

	public long getValueCount() {
		return valueCount;
	}
}
