package test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import chord.ChordInterval;
import chord.RingRange;

public class ChordIntervalTest {
	private ChordInterval ci;

	@Before
	public void setup() {
		ci = new ChordInterval(8);
	}

	@Test
	public void testWithinClosedIntervalInsideBounds() {
		assertTrue(ci.withinClosedInterval(3, 1, 5)); // is 3 in (1,5]?
		assertTrue(ci.withinClosedInterval(3, 2, 3)); // is 3 in (2,5]?
		assertFalse(ci.withinClosedInterval(3, 3, 5)); // is 3 in (3,5]?
		assertTrue(ci.withinClosedInterval(3, 3, 3)); // is 3 in (3,3]?
		// for some reason, any value is within the interval if the interval has the
		// same left and right
		assertTrue(ci.withinClosedInterval(Long.MAX_VALUE, 3, 3));
	}

	@Test
	public void testWithinClosedIntervalCrossRing() {
		assertTrue(ci.withinClosedInterval(0, 6, 3));
		assertTrue(ci.withinClosedInterval(0, 7, 1));
		assertTrue(ci.withinClosedInterval(0, 7, 0));
		assertFalse(ci.withinClosedInterval(6, 7, 1));
		assertFalse(ci.withinClosedInterval(2, 7, 1));
	}

	@Test
	public void testWithinOpenIntervalInsideBounds() {
		assertTrue(ci.withinOpenInterval(3, 1, 5)); // is 3 in (1,5)?
		assertFalse(ci.withinOpenInterval(3, 2, 3)); // is 3 in (2,5)?
		assertFalse(ci.withinOpenInterval(3, 3, 5)); // is 3 in (3,5)?
		assertFalse(ci.withinOpenInterval(3, 3, 3)); // is 3 in (3,3)?
		assertTrue(ci.withinOpenInterval(Long.MAX_VALUE, 3, 3));
		assertTrue(ci.withinOpenInterval(Long.MIN_VALUE, 3, 7));
	}
}
