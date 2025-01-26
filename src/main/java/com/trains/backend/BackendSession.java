package com.trains.backend;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.trains.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.ArrayList;

public class BackendSession {
    private static final Logger logger = LoggerFactory.getLogger(BackendSession.class);

    private Session session;
    private TrainService trainService;
    private UserService userService;
    private OrderService orderService;
    private UserOrderService userOrderService;
    private ReservationService reservationService;

    public BackendSession(String contactPoints, String keyspace) throws BackendException {
        Cluster.Builder clusterBuilder = Cluster.builder();
        for (String contactPoint : splitContactPoints(contactPoints)) {
            String[] parts = contactPoint.split(":");
            clusterBuilder.addContactPoint(parts[0].trim());
            if (parts.length > 1) {
                clusterBuilder.withPort(Integer.parseInt(parts[1].trim()));
            }
        }
        Cluster cluster = clusterBuilder.build();
        try {
            session = cluster.connect(keyspace);
            System.out.println("Connected to keyspace: " + keyspace);
        } catch (Exception e) {
            throw new BackendException("Could not connect to the cluster. " + e.getMessage() + ".", e);
        }
        trainService = new TrainService(session);
        userService = new UserService(session);
        orderService = new OrderService(session);
        userOrderService = new UserOrderService(session);
        reservationService = new ReservationService(session);
    }

    private String[] splitContactPoints(String contactPoints) {
        return contactPoints.split(",");
    }

    protected void finalize() {
        try {
            if (session != null) {
                session.getCluster().close();
            }
        } catch (Exception e) {
            logger.error("Could not close existing cluster", e);
        }
    }

    public TrainService getTrainService() {
        return trainService;
    }

    public UserService getUserService() {
        return userService;
    }

    public OrderService getOrderService() {
        return orderService;
    }

    public UserOrderService getUserOrderService() {
        return userOrderService;
    }

    public ReservationService getReservationService() {
        return reservationService;
    }

    public void printAllTables() throws BackendException {
        String output = userService.selectAllUsers();
        System.out.println("Users: \n" + output);

        output = trainService.selectAllTrains();
        System.out.println("Trains: \n" + output);

        output = orderService.selectAllOrders();
        System.out.println("Orders: \n" + output);

        output = userOrderService.selectAllUsersOrders();
        System.out.println("All User Orders: \n" + output);
    }
}