package com.trains;

import com.trains.backend.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class OrderServiceTestManyUsers {

    private static BackendSession session;
    private static OrderService orderService;
    private static UserService userService;
    private static TrainService trainService;
    private static ReservationService reservationService;
    private static List<String> trains = new ArrayList<>();
    private static List<UUID> users = new ArrayList<>();

    @BeforeAll
    public static void setup() throws Exception {
        session = new BackendSession("127.0.0.1:9042,127.0.0.1:9043,127.0.0.1:9044", "Test", "QUORUM");
        orderService = session.getOrderService();
        userService = session.getUserService();
        trainService = session.getTrainService();
        reservationService = session.getReservationService();

        // Setup initial data
        UUID user1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID user2 = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID user3 = UUID.fromString("33333333-3333-3333-3333-333333333333");
        UUID user4 = UUID.fromString("44444444-4444-4444-4444-444444444444");
        if (userService.getUser(user1) == null) {
            userService.upsertUser(user1, "Jon Snow");
            userService.upsertUser(user2, "Jane Doe");
            userService.upsertUser(user3, "Alice Cooper");
            userService.upsertUser(user4, "Bob Marley");
            users.add(user1);
            users.add(user2);
            users.add(user3);
            users.add(user4);
        }
        trains.add(trainService.upsertTrain(4091, Timestamp.valueOf("2024-12-28 11:00:00"), 5, 50));
        trains.add(trainService.upsertTrain(4092, Timestamp.valueOf("2024-12-28 12:00:00"), 4, 60));
        trains.add(trainService.upsertTrain(4093, Timestamp.valueOf("2024-12-28 13:00:00"), 6, 40));
    }

    @Test
    public void benchmarkInsertOrdersWithMultipleThreads() {
        int numberOfThreads = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                assertDoesNotThrow(() -> {
                    String train = trains.get(new Random().nextInt(trains.size()));
                    addTicket(train, users.get(new Random().nextInt(users.size())), new Random().nextInt(10)+1 );
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

        // Add the select statement here
        String query = "SELECT train_id, trip_date, car, SUM(seats_amount) FROM orders GROUP BY train_id, trip_date, car";
        final int[] previousTrainId = {-1};
        session.getSession().execute(query).forEach(row -> {
            int currentTrainId = row.getInt("train_id");
            if (currentTrainId != previousTrainId[0] && previousTrainId[0] != -1) {
                System.out.println();
            }
            System.out.println(String.format("Train ID: %d, Trip Date: %s, Car: %d, Seats Amount: %d",
                    currentTrainId, row.getTimestamp("trip_date"), row.getInt("car"), row.getInt("system.sum(seats_amount)")));
            previousTrainId[0] = currentTrainId;
        });
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

        // Check availability in all cars first
        int totalCapacity = cars * carCapacity;
        int reservedSeats = orderService.getTakenSeats(trainId, departureTime) + reservationService.getReservedSeats(trainId, departureTime);
        int availableSeats = totalCapacity - reservedSeats;

        if (availableSeats < numberOfTickets) {
            System.out.println("Not enough seats available for the requested number of tickets.");
            return null;
        }
        int[] reservationsSeats = new int[cars];
        UUID resId = UUID.randomUUID();
        Random random = new Random();
        while (remainingTickets > 0) {
            reservedSeats = orderService.getTakenSeats(trainId, departureTime) + reservationService.getReservedSeats(trainId, departureTime);
            availableSeats = totalCapacity - reservedSeats;
            if (availableSeats < numberOfTickets) {
                System.out.println("Not enough seats available for the requested number of tickets.");
                for (int car = 1; car <= cars; car++) { 
                    if (reservationsSeats[car - 1] > 0) {
                        reservationService.deleteReservation(trainId, Timestamp.valueOf(departureTime), car, resId);
                    }
                }
                return null;
            }
            int car = random.nextInt(cars) + 1;
            availableSeats = carCapacity - orderService.getTakenSeatsByCar(trainId, departureTime, car) - reservationService.getSumReservedSeatsByCar(trainId, departureTime, car);
            if (availableSeats > 0) {
                int ticketsToReserve = Math.min(remainingTickets, availableSeats);
                int res = reservationService.reserveSeats(resId, trainId, Timestamp.valueOf(departureTime), userId, car, ticketsToReserve, carCapacity);
                if (res == 0 ){
                    System.out.println("Reservation failed. Please try again.");
                    continue;
                }
                ticketInfo.append(String.format("Reserved %d tickets in car %d\n", ticketsToReserve, car));
                remainingTickets -= ticketsToReserve;
                System.out.println(ticketInfo);
                reservationsSeats[car - 1] = ticketsToReserve;
            }
        }

        // Confirm reservation
        if (remainingTickets == 0) {
            for (int car = 1; car <= cars; car++) {
                reservedSeats = reservationsSeats[car - 1];
                if (reservedSeats > 0) {
                    int res = reservationService.confirmReservation(resId, orderId, trainId, Timestamp.valueOf(departureTime), userId, car, reservedSeats, orderService);
                    if (res == 0) {
                        System.out.println("Reservation failed. Please try again.");
                    }
                }
            }
        }

        return remainingTickets > 0 ? null : ticketInfo.toString();
    }




}
