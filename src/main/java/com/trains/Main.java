package com.trains;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Properties;
import java.util.UUID;

import com.trains.backend.BackendException;
import com.trains.backend.BackendSession;

public class Main {

	private static final String PROPERTIES_FILENAME = "config.properties";

	public static void main(String[] args) throws IOException, BackendException {
		String contactPoint = null;
		String keyspace = null;

		Properties properties = new Properties();
		try {
			properties.load(Main.class.getClassLoader().getResourceAsStream(PROPERTIES_FILENAME));

			contactPoint = properties.getProperty("contact_point");
			keyspace = properties.getProperty("keyspace");
		} catch (IOException ex) {
			ex.printStackTrace();
		}
			
		BackendSession session = new BackendSession(contactPoint, keyspace);

		UUID user1 = UUID.randomUUID();
		session.upsertUser(user1, "Jon Snow");
		UUID user2 = UUID.randomUUID();
		session.upsertUser(user2, "Jane Doe");
		UUID user3 = UUID.randomUUID();
		session.upsertUser(user3, "Alice Cooper");
		UUID user4 = UUID.randomUUID();
		session.upsertUser(user4, "Bob Marley");

		session.upsertTrain(8022, 4, 100);
		session.upsertTrain(1001, 5, 50);
		session.upsertTrain(1212, 3, 150);

		session.upsertOrder(UUID.randomUUID(), 8022, Timestamp.valueOf("2024-12-28 08:00:00"),
				user1, 2, 4);

		session.upsertOrder(UUID.randomUUID(), 8022, Timestamp.valueOf("2024-12-28 08:00:00"),
				user4, 1, 3);

		session.upsertOrder(UUID.randomUUID(), 1001, Timestamp.valueOf("2024-12-28 11:00:00"),
				user2, 4, 2);

		session.upsertOrder(UUID.randomUUID(), 1212, Timestamp.valueOf("2024-12-28 12:30:00"),
				user3, 5, 6);

		session.upsertOrder(UUID.randomUUID(), 1212, Timestamp.valueOf("2024-12-28 12:30:00"),
				user3, 6, 8);

		String output = session.selectAllUsers();
		System.out.println("Users: \n" + output);

		output = session.selectAllTrains();
		System.out.println("Trains: \n" + output);

		output = session.selectAllOrders();
		System.out.println("Orders: \n" + output);

		session.deleteAllOrders();
		session.deleteAllTrains();
		session.deleteAllUsers();

		System.exit(0);
	}
}
