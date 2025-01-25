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
import com.trains.ClientInputHandler;

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

        UUID user1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID user2 = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID user3 = UUID.fromString("33333333-3333-3333-3333-333333333333");
        UUID user4 = UUID.fromString("44444444-4444-4444-4444-444444444444");

        if (userService.getUser(user1) == null) {
            userService.upsertUser(user1, "Jon Snow");
            userService.upsertUser(user2, "Jane Doe");
            userService.upsertUser(user3, "Alice Cooper");
            userService.upsertUser(user4, "Bob Marley");

            trainService.upsertTrain(8022, Timestamp.valueOf("2024-12-28 08:00:00"), 4, 100);
            trainService.upsertTrain(1001, Timestamp.valueOf("2024-12-28 11:00:00"), 5, 50);
            trainService.upsertTrain(1212, Timestamp.valueOf("2024-12-28 12:30:00"), 3, 150);

            orderService.upsertOrder(UUID.randomUUID(), 8022, Timestamp.valueOf("2024-12-28 08:00:00"), user1, 2, 4);
            orderService.upsertOrder(UUID.randomUUID(), 8022, Timestamp.valueOf("2024-12-28 08:00:00"), user4, 1, 3);
            orderService.upsertOrder(UUID.randomUUID(), 1001, Timestamp.valueOf("2024-12-28 11:00:00"), user2, 4, 2);
            orderService.upsertOrder(UUID.randomUUID(), 1212, Timestamp.valueOf("2024-12-28 12:30:00"), user3, 5, 6);
            orderService.upsertOrder(UUID.randomUUID(), 1212, Timestamp.valueOf("2024-12-28 12:30:00"), user3, 6, 8);
            orderService.upsertOrder(UUID.randomUUID(), 1212, Timestamp.valueOf("2024-12-28 12:30:00"), user3, 6, 140);
            orderService.upsertOrder(UUID.randomUUID(), 1212, Timestamp.valueOf("2024-12-28 12:30:00"), user2, 6, 5);
        }

        ClientInputHandler clientInputHandler = new ClientInputHandler(userService, session);
        clientInputHandler.handleInput();

        orderService.deleteAllOrders();
        trainService.deleteAllTrains();
        userService.deleteAllUsers();

        System.exit(0);
    }
}
