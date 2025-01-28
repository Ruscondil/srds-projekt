package com.trains;

import com.trains.backend.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WriteConsistencyTestONE {
    private static BackendSession sessionOne;
    private static BackendSession sessionTwo;
    private static OrderService orderServiceOne;
    private static OrderService orderServiceTwo;
    private static UserService userServiceOne;
    private static TrainService trainServiceOne;

    @BeforeAll
    public static void setup() throws Exception {
        sessionOne = new BackendSession("127.0.0.1:9042", "Pociagi", "ONE");
        sessionTwo = new BackendSession("127.0.0.1:9042", "Pociagi", "ONE");

        orderServiceOne = sessionOne.getOrderService();
        userServiceOne = sessionOne.getUserService();
        trainServiceOne = sessionOne.getTrainService();

        orderServiceTwo = sessionTwo.getOrderService();
    }

    @Test
    public void testWriteConsistency() throws BackendException {
        UUID userId = UUID.randomUUID();
        int trainId = 4091;
        Timestamp tripDate = Timestamp.valueOf("2024-12-28 11:00:00");

        userServiceOne.upsertUser(userId, "Test User ONE");
        // Insert train with QUORUM consistency
        trainServiceOne.upsertTrain(trainId, tripDate, 5, 50);

        // Insert a test order
        orderServiceOne.upsertOrder(UUID.randomUUID(), trainId, tripDate, userId, 1, 2);

        int seats = orderServiceTwo.getTakenSeatsByCar(trainId, tripDate.toString(), 1);

        assertEquals(2, seats, "Inconsistency detected");

    }
}
