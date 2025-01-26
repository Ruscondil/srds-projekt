package com.trains;

import com.trains.backend.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
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
    private static ReservationService reservationService;
    private static List<String> trains = new ArrayList<>();

    @BeforeAll
    public static void setup() throws Exception {
        session = new BackendSession("127.0.0.1:9042,127.0.0.1:9043,127.0.0.1:9044", "Test");
        orderService = session.getOrderService();
        userService = session.getUserService();
        trainService = session.getTrainService();
        reservationService = session.getReservationService();

        // Setup initial data
        UUID userId = UUID.randomUUID();
        userService.upsertUser(userId, "Benchmark User");
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
                UUID orderId = UUID.randomUUID();
                assertDoesNotThrow(() -> {
                    String train = trains.get(new Random().nextInt(trains.size()));
                    addTicket(train, UUID.randomUUID(), 1);
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

        // Resolve conflicts after the benchmark
        resolveConflictsForAllCars();

        // Verify data consistency
        //verifyDataConsistency();
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
        int reservedSeats = orderService.getTakenSeats(trainId, departureTime);
        int availableSeats = totalCapacity - reservedSeats;

        if (availableSeats < numberOfTickets) {
            System.out.println("Not enough seats available for the requested number of tickets.");
            return null;
        }
        int[] reservationsSeats = new int[cars];
        // Reserve tickets in available cars
        UUID resId = UUID.randomUUID();
        Random random = new Random();
        while (remainingTickets > 0) {
            int car = random.nextInt(cars) + 1;
            availableSeats = carCapacity - orderService.getTakenSeatsByCar(trainId, departureTime, car) - reservationService.getSumReservedSeatsByCar(trainId, departureTime, car);
            if (availableSeats > 0) {
                int ticketsToReserve = Math.min(remainingTickets, availableSeats);
                reservationService.reserveSeats(resId, trainId, Timestamp.valueOf(departureTime), userId, car, ticketsToReserve);
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
                    reservationService.confirmReservation(resId, orderId, trainId, Timestamp.valueOf(departureTime), userId, car, reservedSeats, orderService);
                    reservationService.resolveConflictsForAllCars(trainId, Timestamp.valueOf(departureTime), orderService); // Ensure conflicts are resolved after confirmation
                }
            }
        }

        return remainingTickets > 0 ? null : ticketInfo.toString();
    }

    private void resolveConflictsForAllCars() {
        for (String train : trains) {
            String[] trainDetails = train.split(", ");
            int trainId = Integer.parseInt(trainDetails[0].split(": ")[1]);
            String departureTime = trainDetails[1].split(": ")[1];
            Timestamp tripDate = Timestamp.valueOf(departureTime);
            int cars = Integer.parseInt(trainDetails[2].split(": ")[1]);

            for (int car = 1; car <= cars; car++) {
                reservationService.resolveConflicts(trainId, tripDate, car, orderService);
            }
        }
    }

    private void verifyDataConsistency() {
        // Query the database to check for data consistency
        String allOrders = orderService.selectAllOrders();
        String allUserOrders = orderService.getUserOrderService().selectAllUsersOrders();

        // Print the results for manual verification
        System.out.println("All Orders: \n" + allOrders);
        System.out.println("All User Orders: \n" + allUserOrders);

        // Add additional checks if needed to programmatically verify consistency
        // For example, you can parse the results and compare counts or specific values
    }

}
