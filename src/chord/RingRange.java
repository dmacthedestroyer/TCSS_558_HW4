package chord;

public class RingRange {
	public boolean isInRange(boolean leftInclusive, long left, long x, long right, boolean rightInclusive) {
		//if left and right are the same, then the whole ring is within the interval (except for the bound, if both sides are exclusive)
		if(left == right && (leftInclusive || rightInclusive || x != left))
			return true;
		
		boolean xleft = (leftInclusive && x == left) || x > left;
		boolean xright = (rightInclusive && x == right) || x < right;

		if(left <= right)
			return xleft && xright;
		
		return xleft || xright;
	}
}
