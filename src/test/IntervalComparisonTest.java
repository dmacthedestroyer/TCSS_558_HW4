package test;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import chord.ChordInterval;
import chord.RingRange;

public class IntervalComparisonTest {
	private ChordInterval ci;
	private RingRange rr;

	@Before
	public void setup() {
		ci = new ChordInterval(8);
		rr = new RingRange();
	}

	@Test
	public void testClosedIntervalsEqual() {
		assertEquals(ci.withinClosedInterval(1, 0, 2), rr.isInRange(false, 0, 1, 2, true));
		assertEquals(ci.withinClosedInterval(2, 0, 2), rr.isInRange(false, 0, 2, 2, true));
		assertEquals(ci.withinClosedInterval(0, 0, 2), rr.isInRange(false, 0, 0, 2, true));
		assertEquals(ci.withinClosedInterval(3, 0, 2), rr.isInRange(false, 0, 3, 2, true));

		assertEquals(ci.withinClosedInterval(0, 7, 1), rr.isInRange(false, 7, 0, 1, true));
		assertEquals(ci.withinClosedInterval(1, 7, 0), rr.isInRange(false, 7, 1, 0, true));

		assertEquals(ci.withinClosedInterval(0, 7, 7), rr.isInRange(false, 7, 0, 7, true));
		assertEquals(ci.withinClosedInterval(0, 0, 0), rr.isInRange(false, 0, 0, 0, true));
	}

	@Test
	public void testOpenIntervalsEqual() {
		assertEquals(ci.withinOpenInterval(1, 0, 2), rr.isInRange(false, 0, 1, 2, false));
		assertEquals(ci.withinOpenInterval(2, 0, 2), rr.isInRange(false, 0, 2, 2, false));
		assertEquals(ci.withinOpenInterval(0, 0, 2), rr.isInRange(false, 0, 0, 2, false));
		assertEquals(ci.withinOpenInterval(3, 0, 2), rr.isInRange(false, 0, 3, 2, false));

		assertEquals(ci.withinOpenInterval(0, 7, 1), rr.isInRange(false, 7, 0, 1, false));
		assertEquals(ci.withinOpenInterval(1, 7, 0), rr.isInRange(false, 7, 1, 0, false));

		assertEquals(ci.withinOpenInterval(0, 7, 7), rr.isInRange(false, 7, 0, 7, false));
		assertEquals(ci.withinOpenInterval(0, 0, 0), rr.isInRange(false, 0, 0, 0, false));
	}
}
