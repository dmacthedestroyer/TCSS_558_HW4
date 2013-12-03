package chord;

public class RingRange {
	public boolean isInRange(boolean leftInclusive, long left, long x, long right, boolean rightInclusive) {
		boolean xleft = (leftInclusive && x == left) || x > left;
		boolean xright = (rightInclusive && x == right) || x < right;

		if(left <= right)
			return xleft && xright;
		
		return xleft || xright;
	}
}
