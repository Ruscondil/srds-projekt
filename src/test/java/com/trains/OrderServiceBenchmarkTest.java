package com.trains;

import com.trains.backend.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class OrderServiceBenchmarkTest {

    private static BackendSession session;
    private static OrderService orderService;
    private static UserService userService;
    private static TrainService trainService;
    private static List<String> trains = new ArrayList<>();

    @BeforeAll
    public static void setup() throws Exception {
        session = new BackendSession("127.0.0.1:9042,127.0.0.1:9043,127.0.0.1:9044", "Test");
        orderService = session.getOrderService();
        userService = session.getUserService();
        trainService = session.getTrainService();

        // Setup initial data
        UUID userId = UUID.randomUUID();
        userService.upsertUser(userId, "Benchmark User");
        trains.add(trainService.upsertTrain(1081, Timestamp.valueOf("2024-12-28 11:00:00"), 5, 50));
    }

    @Test
    public void benchmarkInsertOrdersWithMultipleThreads() {
        int numberOfThreads = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                UUID orderId = UUID.randomUUID();
                assertDoesNotThrow(() -> {
                    addTicket(trains.get(0), UUID.randomUUID(), 1);
                    //orderService.upsertOrder(orderId, 1001, Timestamp.valueOf("2024-12-28 11:00:00"), UUID.randomUUID(), 1, 1);
                });
            });
        }

        executorService.shutdown();
        try {
            executorService.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("Benchmark completed in: " + duration + " ms");
    }

    private void addTicket(String train, UUID userId, int numberOfTickets) throws BackendException {
		UUID orderId = UUID.randomUUID();

		String ticketInfo = reserveTickets(orderId, train, userId, numberOfTickets);

		if (ticketInfo == null) {
			System.out.println("Not enough seats available for the requested number of tickets.");
		} else {
			System.out.println("Tickets reserved successfully:");
			System.out.println(ticketInfo);
		}
	}
    private String reserveTickets(UUID orderId, String train, UUID userId, int numberOfTickets) {
		String[] trainDetails = train.split(", ");
		int trainId = Integer.parseInt(trainDetails[0].split(": ")[1]);
		int cars = Integer.parseInt(trainDetails[2].split(": ")[1]);
		int carCapacity = Integer.parseInt(trainDetails[3].split(": ")[1]);
        String departureTime = trainDetails[1].split(": ")[1];
		int remainingTickets = numberOfTickets;
		StringBuilder ticketInfo = new StringBuilder();
		for (int car = 1; car <= cars && remainingTickets > 0; car++) {
			int availableSeats = carCapacity - orderService.getReservedSeats(trainId, departureTime, car);
            if (availableSeats > 0) {
				int ticketsToReserve = Math.min(remainingTickets, availableSeats);
				orderService.upsertOrder(orderId, trainId, Timestamp.valueOf(departureTime), userId, car, ticketsToReserve);
				ticketInfo.append(String.format("Reserved %d tickets in car %d\n", ticketsToReserve, car));
				remainingTickets -= ticketsToReserve;
                System.out.println(ticketInfo);
			}
		}

		return remainingTickets > 0 ? null : ticketInfo.toString();
	}

}
