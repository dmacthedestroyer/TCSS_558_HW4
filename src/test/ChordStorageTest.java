package test;

import static org.junit.Assert.*;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import org.junit.BeforeClass;
import org.junit.Test;

import chord.RMINodeClient;
import util.GenerateMultiNodeNetwork;

/**
 * Tests the storage functions of the Chord network.
 * 
 * @author Jesse Carrigan
 *
 */
public class ChordStorageTest {

	RMINodeClient client;
	static Registry registry;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		String[] args = {"1337", "32", "4"};
		GenerateMultiNodeNetwork.main(args);
		registry = LocateRegistry.getRegistry(1337);
	}

	@Test
	public void testGetAndPut() throws AccessException, RemoteException, NotBoundException {
		RMINodeClient client = (RMINodeClient) registry.lookup("node0");
		String key = "testKey";
		String value = "testValue";
		client.put(key, value);
		String getValue = (String) client.get(key);
		assertEquals(value, getValue);
	}

	@Test
	public void testDelete() throws RemoteException, NotBoundException {
		RMINodeClient client = (RMINodeClient) registry.lookup("node0");
		String key = "testKey";
		String value = "testValue";
		client.put(key, value);
		client.delete(key);
		String getValue = (String) client.get(key);
		assertEquals(null, getValue);
	}

}
