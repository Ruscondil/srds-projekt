package com.trains;

import com.trains.backend.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WriteConsistencyTestQUORUM {

    private static BackendSession sessionQuorum;
    private static OrderService orderServiceQuorum;
    private static UserService userServiceQuorum;
    private static TrainService trainServiceQuorum;

    @BeforeAll
    public static void setup() throws Exception {
        sessionQuorum = new BackendSession("127.0.0.1:9042,127.0.0.1:9043,127.0.0.1:9044", "Test", "QUORUM");

        orderServiceQuorum = sessionQuorum.getOrderService();
        userServiceQuorum = sessionQuorum.getUserService();
        trainServiceQuorum = sessionQuorum.getTrainService();
    }

    @Test
    public void testWriteConsistency() throws BackendException {
        UUID userId = UUID.randomUUID();
        int trainId = 4091;
        Timestamp tripDate = Timestamp.valueOf("2024-12-28 11:00:00");

        // Insert user with QUORUM consistency
        userServiceQuorum.upsertUser(userId, "Test User QUORUM");
        // Insert train with QUORUM consistency
        trainServiceQuorum.upsertTrain(trainId, tripDate, 5, 50);
        // Insert order with QUORUM consistency
        UUID orderIdQuorum = UUID.randomUUID();
        orderServiceQuorum.upsertOrder(orderIdQuorum, trainId, tripDate, userId, 1, 20);

        // Verify data with QUORUM consistency
        Client userQuorum = userServiceQuorum.getUser(userId);
        String trainQuorum = trainServiceQuorum.selectTrain(trainId, tripDate);
        int seatsQuorum = orderServiceQuorum.getTakenSeats(trainId, tripDate.toString());

        // Assertions
        assertEquals("Test User QUORUM", userQuorum.getName());
        assertEquals("Train ID: 4091, Departure: 2024-12-28 11:00:00.0, Cars: 5, Seats Per Car: 50", trainQuorum);
        assertEquals(20, seatsQuorum);
    }
}
