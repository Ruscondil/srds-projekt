package com.trains;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Properties;
import java.util.UUID;

import com.trains.backend.BackendException;
import com.trains.backend.BackendSession;
import com.trains.backend.TrainService;
import com.trains.backend.UserService;
import com.trains.backend.OrderService;
import java.util.Scanner;
 // TODO: moze przeniesc kod klienta do osobnej klasy
public class Main {

	private static final String PROPERTIES_FILENAME = "config.properties";

	public static void main(String[] args) throws IOException, BackendException {
        String contactPoint = null;
        String keyspace = null;
		Client currentClient = null;

        Properties properties = new Properties();
        try {
            properties.load(Main.class.getClassLoader().getResourceAsStream(PROPERTIES_FILENAME));

            contactPoint = properties.getProperty("contact_point");
            keyspace = properties.getProperty("keyspace");
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        BackendSession session = new BackendSession(contactPoint, keyspace);
        UserService userService = session.getUserService();
        TrainService trainService = session.getTrainService();
        OrderService orderService = session.getOrderService();

        UUID user1 = UUID.randomUUID();
        userService.upsertUser(user1, "Jon Snow");
        UUID user2 = UUID.randomUUID();
        userService.upsertUser(user2, "Jane Doe");
        UUID user3 = UUID.randomUUID();
        userService.upsertUser(user3, "Alice Cooper");
        UUID user4 = UUID.randomUUID();
        userService.upsertUser(user4, "Bob Marley");

        trainService.upsertTrain(8022, 4, 100);
        trainService.upsertTrain(1001, 5, 50);
        trainService.upsertTrain(1212, 3, 150);

        orderService.upsertOrder(UUID.randomUUID(), 8022, Timestamp.valueOf("2024-12-28 08:00:00"),
                user1, 2, 4);

        orderService.upsertOrder(UUID.randomUUID(), 8022, Timestamp.valueOf("2024-12-28 08:00:00"),
                user4, 1, 3);

        orderService.upsertOrder(UUID.randomUUID(), 1001, Timestamp.valueOf("2024-12-28 11:00:00"),
                user2, 4, 2);

        orderService.upsertOrder(UUID.randomUUID(), 1212, Timestamp.valueOf("2024-12-28 12:30:00"),
                user3, 5, 6);

        orderService.upsertOrder(UUID.randomUUID(), 1212, Timestamp.valueOf("2024-12-28 12:30:00"),
                user3, 6, 8);

        Scanner scanner = new Scanner(System.in);
        System.out.println("Actions: ");
        System.out.println("1 - Quit ");
        System.out.println("2 - Create account ");
		System.out.println("3 - Print tables");
        int choice = 0;
        while (choice != 1) {
			System.out.print("Choose: ");
            choice = Integer.parseInt(scanner.nextLine());
            switch (choice) {
				case 1: break;
                case 2: {
					currentClient = userService.createNewUser();
					break;
				}
				case 3: {
					session.printAllTables();
					break;
				}
				default: System.out.println("Wrong action");
            }
        }




        orderService.deleteAllOrders();
        trainService.deleteAllTrains();
        userService.deleteAllUsers();

        System.exit(0);
    }
}
