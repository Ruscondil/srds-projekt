package com.trains;

import com.trains.backend.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class CleanupDatabaseTest {

    private static BackendSession session;
    private static OrderService orderService;
    private static UserService userService;
    private static TrainService trainService;
    private static ReservationService reservationService;

    @BeforeAll
    public static void setup() throws Exception {
        session = new BackendSession("127.0.0.1:9042,127.0.0.1:9043,127.0.0.1:9044", "Test", "QUORUM");
        orderService = session.getOrderService();
        userService = session.getUserService();
        trainService = session.getTrainService();
        reservationService = session.getReservationService();
    }

    @Test
    public void cleanupDatabase() {
        orderService.deleteAllOrders();
        trainService.deleteAllTrains();
        userService.deleteAllUsers();
        reservationService.deleteAllReservations();
        System.out.println("All data deleted from the database.");
    }
}
