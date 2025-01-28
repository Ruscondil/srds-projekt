package com.trains;

import com.trains.backend.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WriteConsistencyTestONE {

    private static BackendSession sessionOne;
    private static OrderService orderServiceOne;
    private static UserService userServiceOne;
    private static TrainService trainServiceOne;

    @BeforeAll
    public static void setup() throws Exception {
        sessionOne = new BackendSession("127.0.0.1:9042,127.0.0.1:9043,127.0.0.1:9044", "Test", "ONE");

        orderServiceOne = sessionOne.getOrderService();
        userServiceOne = sessionOne.getUserService();
        trainServiceOne = sessionOne.getTrainService();
    }

    @Test
    public void testWriteConsistency() throws BackendException {
        UUID userId = UUID.randomUUID();
        int trainId = 4091;
        Timestamp tripDate = Timestamp.valueOf("2024-12-28 11:00:00");

        // Insert user with ONE consistency
        userServiceOne.upsertUser(userId, "Test User ONE");
        // Insert train with ONE consistency
        trainServiceOne.upsertTrain(trainId, tripDate, 5, 50);
        // Insert order with ONE consistency
        UUID orderIdOne = UUID.randomUUID();
        orderServiceOne.upsertOrder(orderIdOne, trainId, tripDate, userId, 1, 10);

        // Verify data with ONE consistency
        Client userOne = userServiceOne.getUser(userId);
        String trainOne = trainServiceOne.selectTrain(trainId, tripDate);
        int seatsOne = orderServiceOne.getTakenSeats(trainId, tripDate.toString());

        // Assertions
        assertEquals("Test User ONE", userOne.getName());
        assertEquals("Train ID: 4091, Departure: 2024-12-28 11:00:00.0, Cars: 5, Seats Per Car: 50", trainOne);
        assertEquals(10, seatsOne);
    }
}
