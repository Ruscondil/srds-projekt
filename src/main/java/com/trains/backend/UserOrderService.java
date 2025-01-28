package com.trains.backend;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.ConsistencyLevel;
import com.trains.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.Date;
import java.util.UUID;

public class UserOrderService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private Session session;
    private static PreparedStatement SELECT_ALL_FROM_USERS_ORDERS;
    private static PreparedStatement INSERT_INTO_USERS_ORDERS;
    private static PreparedStatement DELETE_ALL_FROM_USERS_ORDERS;
    private static PreparedStatement SELECT_ORDERS;

    public UserOrderService(Session session) {
        this.session = session;
        prepareStatements();
    }

    private void prepareStatements() {
        SELECT_ALL_FROM_USERS_ORDERS = session.prepare("SELECT * FROM orders_per_user;").setConsistencyLevel(ConsistencyLevel.valueOf(session.getCluster().getConfiguration().getQueryOptions().getConsistencyLevel().name()));

        INSERT_INTO_USERS_ORDERS = session.prepare("INSERT INTO orders_per_user (order_id, train_id, trip_date, user_id, car, seats_amount) VALUES (?, ?, ?, ?, ?, ?);").setConsistencyLevel(ConsistencyLevel.valueOf(session.getCluster().getConfiguration().getQueryOptions().getConsistencyLevel().name()));

        DELETE_ALL_FROM_USERS_ORDERS = session.prepare("TRUNCATE orders_per_user;").setConsistencyLevel(ConsistencyLevel.valueOf(session.getCluster().getConfiguration().getQueryOptions().getConsistencyLevel().name()));

        SELECT_ORDERS = session.prepare("SELECT * FROM orders_per_user WHERE train_id = ? AND trip_date = ? AND user_id = ?;").setConsistencyLevel(ConsistencyLevel.valueOf(session.getCluster().getConfiguration().getQueryOptions().getConsistencyLevel().name()));

    }

    public String selectAllUsersOrders() {
        StringBuilder builder = new StringBuilder();
        BoundStatement bs = new BoundStatement(SELECT_ALL_FROM_USERS_ORDERS);
        ResultSet rs = session.execute(bs);

        for (Row row : rs) {
            UUID orderId = row.getUUID("order_id");
            int trainId = row.getInt("train_id");
            Date tripDate = row.getTimestamp("trip_date");
            UUID userId = row.getUUID("user_id");
            int car = row.getInt("car");
            int seatsAmount = row.getInt("seats_amount");

            builder.append(String.format("Order ID: %s, Train ID: %d, Trip Date: %s, User Id: %s, Car: %d, Seats Amount: %d\n", orderId, trainId, tripDate, userId, car, seatsAmount));
        }

        return builder.toString();
    }

    public void upsertUserOrder(UUID orderId, int trainId, Timestamp tripDate, UUID userId, int car, int seatsAmount) {
        BoundStatement bs = new BoundStatement(INSERT_INTO_USERS_ORDERS);
        bs.bind(orderId, trainId, tripDate, userId, car, seatsAmount);
        session.execute(bs);
        logger.info("Order " + orderId + " upserted");
    }

    public void deleteAllUsersOrders() {
        BoundStatement bs = new BoundStatement(DELETE_ALL_FROM_USERS_ORDERS);
        session.execute(bs);
        logger.info("All users orders deleted");
    }

    public void selectOrders(int trainId, Timestamp tripDate, UUID userId) {
        BoundStatement bs = new BoundStatement(SELECT_ORDERS);
        bs.bind(trainId, tripDate, userId);
        ResultSet rs = session.execute(bs);

        for (Row row : rs) {
            UUID orderId = row.getUUID("order_id");
            int car = row.getInt("car");
            int seatsAmount = row.getInt("seats_amount");
            System.out.println(String.format("Order ID: %s, Train ID: %d, Trip Date: %s, User Id: %s, Car: %d, Seats Amount: %d", orderId, trainId, tripDate, userId, car, seatsAmount));
        }
    }
}
