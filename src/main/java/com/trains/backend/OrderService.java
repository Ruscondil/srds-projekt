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

    public OrderService(Session session) {
        this.session = session;
        prepareStatements();
    }

    private void prepareStatements() {
        SELECT_ALL_FROM_ORDERS = session.prepare("SELECT * FROM orders;");
        INSERT_INTO_ORDERS = session.prepare("INSERT INTO orders (order_id, train_id, trip_date, user_id, car, seats_amount) VALUES (?, ?, ?, ?, ?, ?);");
        DELETE_ALL_FROM_ORDERS = session.prepare("TRUNCATE orders;");
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

    public int getReservedSeats(int trainId, String tripDate, int car) {
    String query = "SELECT SUM(seats_amount) FROM orders WHERE train_id = ? AND trip_date = ? AND car = ?";
    BoundStatement bs = new BoundStatement(session.prepare(query));
    bs.bind(trainId, Timestamp.valueOf(tripDate), car);
    ResultSet rs = session.execute(bs);
    Row row = rs.one();
    return row != null ? row.getInt(0) : 0;
}

    public void deleteAllOrders() {
        BoundStatement bs = new BoundStatement(DELETE_ALL_FROM_ORDERS);
        session.execute(bs);
        logger.info("All orders deleted");
    }
}