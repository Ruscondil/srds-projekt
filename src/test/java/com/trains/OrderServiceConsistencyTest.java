package com.trains;

import com.trains.backend.BackendSession;
import com.trains.backend.OrderService;
import com.trains.backend.UserService;
import com.trains.backend.TrainService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class OrderServiceConsistencyTest {

    private static BackendSession session;
    private static OrderService orderService;
    private static UserService userService;
    private static TrainService trainService;

    @BeforeAll
    public static void setup() throws Exception {
        session = new BackendSession("127.0.0.1:9042,127.0.0.1:9043,127.0.0.1:9044", "Test", "QUORUM");
        orderService = session.getOrderService();
        userService = session.getUserService();
        trainService = session.getTrainService();

        // Setup initial data
        UUID userId = UUID.randomUUID();
        userService.upsertUser(userId, "Consistency Test User");
        trainService.upsertTrain(1001, Timestamp.valueOf("2024-12-28 11:00:00"), 5, 50);
    }

    @Test
    public void testInsertOrdersWithConsistencyCheck() {
        int numberOfThreads = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                UUID orderId = UUID.randomUUID();
                assertDoesNotThrow(() -> {
                    orderService.upsertOrder(orderId, 1001, Timestamp.valueOf("2024-12-28 11:00:00"), UUID.randomUUID(), 1, 1);
                });
            });
        }

        executorService.shutdown();
        try {
            executorService.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Check consistency
        int totalReservedSeats = orderService.getTakenSeats(1001, "2024-12-28 11:00:00");
        assertEquals(numberOfThreads, totalReservedSeats, "Total reserved seats should match the number of threads");
    }
}
