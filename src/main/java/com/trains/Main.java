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


		session.upsertOrder(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"), 101, Timestamp.valueOf("2024-12-28 08:00:00"),
				UUID.fromString("850e8400-e29b-41d4-a716-446655440001"), 2, 4);

		session.upsertOrder(UUID.fromString("550e8400-e29b-41d4-a716-446655440002"), 102, Timestamp.valueOf("2024-12-28 09:30:00"),
				UUID.fromString("850e8400-e29b-41d4-a716-446655440003"), 1, 3);

		session.upsertOrder(UUID.fromString("550e8400-e29b-41d4-a716-446655440004"), 103, Timestamp.valueOf("2024-12-28 11:00:00"),
				UUID.fromString("850e8400-e29b-41d4-a716-446655440005"), 4, 2);

		session.upsertOrder(UUID.fromString("550e8400-e29b-41d4-a716-446655440006"), 104, Timestamp.valueOf("2024-12-28 12:30:00"),
				UUID.fromString("850e8400-e29b-41d4-a716-446655440007"), 5, 6);

		session.upsertOrder(UUID.fromString("550e8400-e29b-41d4-a716-446655440008"), 105, Timestamp.valueOf("2024-12-28 15:00:00"),
				UUID.fromString("850e8400-e29b-41d4-a716-446655440009"), 6, 8);



		String output = session.selectAllOrders();
		System.out.println("Orders: \n" + output);

		session.deleteAllOrders();

		System.exit(0);
	}
}
