package com.trains.backend;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class OrderService {
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private Session session;
    private static PreparedStatement SELECT_ALL_FROM_ORDERS;
    private static PreparedStatement INSERT_INTO_ORDERS;
    private static PreparedStatement DELETE_ALL_FROM_ORDERS;
    private static PreparedStatement SELECT_AVAILABLE_TRAINS;

    public OrderService(Session session) {
        this.session = session;
        prepareStatements();
    }

    private void prepareStatements() {
        SELECT_ALL_FROM_ORDERS = session.prepare("SELECT * FROM orders;");
        INSERT_INTO_ORDERS = session.prepare("INSERT INTO orders (order_id, train_id, trip_date, user_id, car, seats_amount) VALUES (?, ?, ?, ?, ?, ?);");
        DELETE_ALL_FROM_ORDERS = session.prepare("TRUNCATE orders;");
        SELECT_AVAILABLE_TRAINS = session.prepare("SELECT train_id, trip_date FROM orders LIMIT ?;");
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
        BoundStatement bs = new BoundStatement(INSERT_INTO_ORDERS);
        bs.bind(orderId, trainId, tripDate, userId, car, seatsAmount);
        session.execute(bs);
        logger.info("Order " + orderId + " upserted");
    }

    public List<String> getAvailableTrains(int limit) {
        List<String> trains = new ArrayList<>();
        BoundStatement bs = new BoundStatement(SELECT_AVAILABLE_TRAINS);
        bs.bind(limit);
        ResultSet rs = session.execute(bs);

        for (Row row : rs) {
            int trainId = row.getInt("train_id");
            String tripDate = row.getTimestamp("trip_date").toString();
            trains.add(String.format("Train ID: %d, Departure: %s", trainId, tripDate));
        }

        return trains;
    }

    public void deleteAllOrders() {
        BoundStatement bs = new BoundStatement(DELETE_ALL_FROM_ORDERS);
        session.execute(bs);
        logger.info("All orders deleted");
    }
}