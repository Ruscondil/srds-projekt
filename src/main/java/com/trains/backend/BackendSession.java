package com.trains.backend;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.trains.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BackendSession {
    private static final Logger logger = LoggerFactory.getLogger(BackendSession.class);

    private Session session;
    private TrainService trainService;
    private UserService userService;
    private OrderService orderService;
    private UserOrderService userOrderService;

    public BackendSession(String contactPoint, String keyspace) throws BackendException {
        Cluster cluster = Cluster.builder().addContactPoint(contactPoint).build();
        try {
            session = cluster.connect(keyspace);
        } catch (Exception e) {
            throw new BackendException("Could not connect to the cluster. " + e.getMessage() + ".", e);
        }
        trainService = new TrainService(session);
        userService = new UserService(session);
        orderService = new OrderService(session);
        userOrderService = new UserOrderService(session);
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