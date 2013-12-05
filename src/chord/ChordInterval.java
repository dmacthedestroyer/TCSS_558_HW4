package chord;

public class ChordInterval {
	private final long maxKeys;

	public ChordInterval(long maxKeys) {
		this.maxKeys = maxKeys;
	}

	public boolean withinOpenInterval(long x, long left, long right) {
		long offset = maxKeys - 1 - left;
		long aPrime = (x + offset) % maxKeys;
		long cPrime = (right + offset) % maxKeys;
		return aPrime < cPrime;
	}

	public boolean withinClosedInterval(long x, long left, long right) {
		return left == right || withinOpenInterval(x, left, right + 1);
	}
}