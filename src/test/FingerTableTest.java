package test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Before;
import org.junit.Test;

import chord.Finger;
import chord.FingerTable;
import chord.RMINode;
import chord.RMINodeServer;

public class FingerTableTest {

	FingerTable table;
	RMINodeServer nodeserver;
	
	@Before
	public void setUp() throws Exception {
		nodeserver = new RMINode(2, 0);
		table = new FingerTable(nodeserver);
	}

	@Test
	public void testGetSuccessor() {
		Finger successor = table.getSuccessor();
		assertNotEquals(successor, null);
	}

	@Test
	public void testGetRandomFinger() {
		Finger randomFinger = table.getRandomFinger();
		assertEquals(null, randomFinger.getNode());
	}

}
