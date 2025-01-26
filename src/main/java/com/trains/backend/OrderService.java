package com.trains.backend;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.ConsistencyLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.Date;
import java.util.UUID;

public class OrderService {
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private Session session;
    private static PreparedStatement SELECT_ALL_FROM_ORDERS;
    private static PreparedStatement INSERT_INTO_ORDERS;
    private static PreparedStatement DELETE_ALL_FROM_ORDERS;
    private static PreparedStatement SELECT_SUM_SEATS_AMOUNT_BY_CAR;
    private static PreparedStatement SELECT_SUM_SEATS_AMOUNT;

    private UserOrderService userOrderService;
    private TrainService trainService;

    public OrderService(Session session) {
        this.session = session;
        prepareStatements();
        userOrderService = new UserOrderService(session);
        trainService = new TrainService(session);
    }

    public UserOrderService getUserOrderService() {
        return userOrderService;
    }

    public TrainService getTrainService() {
        return trainService;
    }

    private void prepareStatements() {
        if (SELECT_ALL_FROM_ORDERS == null) {
            SELECT_ALL_FROM_ORDERS = session.prepare("SELECT * FROM orders;").setConsistencyLevel(ConsistencyLevel.valueOf(session.getCluster().getConfiguration().getQueryOptions().getConsistencyLevel().name()));
        }
        if (INSERT_INTO_ORDERS == null) {
            INSERT_INTO_ORDERS = session.prepare("INSERT INTO orders (order_id, train_id, trip_date, user_id, car, seats_amount) VALUES (?, ?, ?, ?, ?, ?);").setConsistencyLevel(ConsistencyLevel.valueOf(session.getCluster().getConfiguration().getQueryOptions().getConsistencyLevel().name()));
        }
        if (DELETE_ALL_FROM_ORDERS == null) {
            DELETE_ALL_FROM_ORDERS = session.prepare("TRUNCATE orders;").setConsistencyLevel(ConsistencyLevel.valueOf(session.getCluster().getConfiguration().getQueryOptions().getConsistencyLevel().name()));
        }
        if (SELECT_SUM_SEATS_AMOUNT == null) {
            SELECT_SUM_SEATS_AMOUNT = session.prepare("SELECT SUM(seats_amount) FROM orders WHERE train_id = ? AND trip_date = ?").setConsistencyLevel(ConsistencyLevel.valueOf(session.getCluster().getConfiguration().getQueryOptions().getConsistencyLevel().name()));
        }
        if (SELECT_SUM_SEATS_AMOUNT_BY_CAR == null) {
            SELECT_SUM_SEATS_AMOUNT_BY_CAR = session.prepare("SELECT SUM(seats_amount) FROM orders WHERE train_id = ? AND trip_date = ? AND car = ?").setConsistencyLevel(ConsistencyLevel.valueOf(session.getCluster().getConfiguration().getQueryOptions().getConsistencyLevel().name()));
        }
    }

    public String selectAllOrders() {
        StringBuilder builder = new StringBuilder();
        BoundStatement bs = new BoundStatement(SELECT_ALL_FROM_ORDERS);
        ResultSet rs = session.execute(bs);

        for (Row row : rs) {
            UUID orderId = row.getUUID("order_id");
            int trainId = row.getInt("train_id");
            Date tripDate = row.getTimestamp("trip_date");
            UUID userId = row.getUUID("user_id");
            int car = row.getInt("car");
            int seatsAmount = row.getInt("seats_amount");

            builder.append(String.format("Order ID: %s, Train ID: %d, Trip Date: %s, User ID: %s, Car: %d, Seats Amount: %d\n",
                    orderId, trainId, tripDate, userId, car, seatsAmount));
        }

        return builder.toString();
    }

    public void upsertOrder(UUID orderId, int trainId, Timestamp tripDate, UUID userId, int car, int seatsAmount) {
        int reservedSeats = getTakenSeatsByCar(trainId, tripDate.toString(), car);
        String selectedTrain = trainService.selectTrain(trainId, tripDate);
        if (selectedTrain == null) {
            System.out.println("Train not found");
            return;
        }

        int seatsPerCar = Integer.parseInt(selectedTrain.split(",")[3].split(": ")[1]);
        int availableSeats = seatsPerCar - reservedSeats;
         if (availableSeats >= seatsAmount) {
            if (trainService.isValidCar(trainId, tripDate, car)) {
                BoundStatement bs = new BoundStatement(INSERT_INTO_ORDERS);
                bs.bind(orderId, trainId, tripDate, userId, car, seatsAmount);
                session.execute(bs);
                logger.info("Order " + orderId + " upserted");
                userOrderService.upsertUserOrder(orderId, trainId, tripDate, userId, car, seatsAmount);
            } else {
                logger.warn("Invalid car number " + car + " for train " + trainId + " on " + tripDate);
            }
         } else {
            logger.warn("Not enough seats available for order " + orderId);
         }
    }

    public int getTakenSeats(int trainId, String tripDate) {
        BoundStatement bs = new BoundStatement(SELECT_SUM_SEATS_AMOUNT);
        bs.bind(trainId, Timestamp.valueOf(tripDate));
        ResultSet rs = session.execute(bs);
        Row row = rs.one();
        return row != null ? row.getInt(0) : 0;
    }

    public int getTakenSeatsByCar(int trainId, String tripDate, int car) {
        BoundStatement bs = new BoundStatement(SELECT_SUM_SEATS_AMOUNT_BY_CAR);
        bs.bind(trainId, Timestamp.valueOf(tripDate), car);
        ResultSet rs = session.execute(bs);
        Row row = rs.one();
        return row != null ? row.getInt(0) : 0;
    }

    public void deleteAllOrders() {
        BoundStatement bs = new BoundStatement(DELETE_ALL_FROM_ORDERS);
        session.execute(bs);
        logger.info("All orders deleted");
        userOrderService.deleteAllUsersOrders();
    }
}