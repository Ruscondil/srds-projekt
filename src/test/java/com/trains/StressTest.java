package com.trains;

import com.trains.backend.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class StressTest {

    private static BackendSession session;
    private static OrderService orderService;
    private static UserService userService;
    private static TrainService trainService;
    private static ReservationService reservationService;
    private static final int seatsPerCar = 100;
    private static final int cars = 1000;

    @BeforeAll
    public static void setup() throws Exception {
        session = new BackendSession("127.0.0.1:9042", "Pociagi", "QUORUM");
        orderService = session.getOrderService();
        userService = session.getUserService();
        trainService = session.getTrainService();
        reservationService = session.getReservationService();
    }

    @Test
    public void stressTestInsertOrders() {
        int numberOfThreads = 1000;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                assertDoesNotThrow(() -> {
                    UUID userId = UUID.randomUUID();
                    userService.upsertUser(userId, "User " + userId);
                    int trainId = ThreadLocalRandom.current().nextInt(1000, 20000);
                    trainService.upsertTrain(trainId, Timestamp.valueOf("2024-12-28 10:00:00"), cars, seatsPerCar);

                    while (true) {
                        synchronized (StressTest.class) {
                            if (isTrainFull(trainId, "2024-12-28 10:00:00")) {
                                trainId = ThreadLocalRandom.current().nextInt(1000, 20000);
                                trainService.upsertTrain(trainId, Timestamp.valueOf("2024-12-28 10:00:00"), cars, seatsPerCar);
                            }
                        }
                        orderService.upsertOrder(UUID.randomUUID(), trainId, Timestamp.valueOf("2024-12-28 10:00:00"), userId, new Random().nextInt(cars)+1, 1);
                    }
                });
            });
        }

        executorService.shutdown();
        try {
            executorService.awaitTermination(10, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean isTrainFull(int trainId, String tripDate) {
        int totalSeats = cars * seatsPerCar;
        int takenSeats = orderService.getTakenSeats(trainId, tripDate);
        return takenSeats >= totalSeats;
    }
}
