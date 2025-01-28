package com.trains;

import com.trains.backend.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class PerformanceTest {

    private static BackendSession session;
    private static OrderService orderService;
    private static UserService userService;
    private static TrainService trainService;
    private static final int numberOfThreads = 100;

    @BeforeAll
    public static void setup() throws Exception {
        session = new BackendSession("127.0.0.1:9042", "Pociagi", "QUORUM");
        orderService = session.getOrderService();
        userService = session.getUserService();
        trainService = session.getTrainService();


        UUID userId = UUID.randomUUID();
        userService.upsertUser(userId, "Performance Test User");
        trainService.upsertTrain(5000, Timestamp.valueOf("2024-12-28 10:00:00"), 10, 100);
    }

    @Test
    public void testOrderServicePerformance() {
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                assertDoesNotThrow(() -> {
                    UUID userId = UUID.randomUUID();
                    userService.upsertUser(userId, "User " + userId);
                    orderService.upsertOrder(UUID.randomUUID(), 5000, Timestamp.valueOf("2024-12-28 10:00:00"), userId, 1, 1);
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
        double operationsPerSecond = (double) numberOfThreads / (duration / 1000.0);

        System.out.println("Operations per second: " + operationsPerSecond);
    }
}
