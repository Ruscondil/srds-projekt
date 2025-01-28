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
import java.util.UUID;

public class ReservationService {
    private static final Logger logger = LoggerFactory.getLogger(ReservationService.class);

    private Session session;
    private static PreparedStatement INSERT_INTO_RESERVATIONS;
    private static PreparedStatement DELETE_FROM_RESERVATIONS;
    private static PreparedStatement DELETE_ALL_FROM_RESERVATIONS;
    private static PreparedStatement SELECT_SUM_RESERVED_SEATS_BY_CAR;
    private static PreparedStatement SELECT_RESERVATIONS_BY_CAR;
    private static PreparedStatement SELECT_RESERVED_SEATS;
    private static PreparedStatement SELECT_RESERVATION;

    private OrderService orderService;

    public ReservationService(Session session) {
        this.session = session;
        prepareStatements();
        orderService = new OrderService(session);
    }
     public OrderService getOrderService() {
        return orderService;
    }

    private void prepareStatements() {
        INSERT_INTO_RESERVATIONS = session.prepare("INSERT INTO reservations (res_id, train_id, trip_date, user_id, car, seats_amount) VALUES (?, ?, ?, ?, ?, ?);").setConsistencyLevel(ConsistencyLevel.valueOf(session.getCluster().getConfiguration().getQueryOptions().getConsistencyLevel().name()));

        DELETE_FROM_RESERVATIONS = session.prepare("DELETE FROM reservations WHERE train_id = ? AND trip_date = ? AND car = ? AND res_id = ?;").setConsistencyLevel(ConsistencyLevel.valueOf(session.getCluster().getConfiguration().getQueryOptions().getConsistencyLevel().name()));

        DELETE_ALL_FROM_RESERVATIONS = session.prepare("TRUNCATE reservations;").setConsistencyLevel(ConsistencyLevel.valueOf(session.getCluster().getConfiguration().getQueryOptions().getConsistencyLevel().name()));

        SELECT_RESERVED_SEATS = session.prepare("SELECT SUM(seats_amount) FROM reservations WHERE train_id = ? AND trip_date = ?;").setConsistencyLevel(ConsistencyLevel.valueOf(session.getCluster().getConfiguration().getQueryOptions().getConsistencyLevel().name()));

        SELECT_SUM_RESERVED_SEATS_BY_CAR = session.prepare("SELECT SUM(seats_amount) FROM reservations WHERE train_id = ? AND trip_date = ? AND car = ?;").setConsistencyLevel(ConsistencyLevel.valueOf(session.getCluster().getConfiguration().getQueryOptions().getConsistencyLevel().name()));

        SELECT_RESERVATIONS_BY_CAR = session.prepare("SELECT res_id, seats_amount FROM reservations WHERE train_id = ? AND trip_date = ? AND car = ?").setConsistencyLevel(ConsistencyLevel.valueOf(session.getCluster().getConfiguration().getQueryOptions().getConsistencyLevel().name()));

        SELECT_RESERVATION = session.prepare("SELECT * FROM reservations WHERE train_id = ? AND trip_date = ? AND car = ? AND res_id = ?;").setConsistencyLevel(ConsistencyLevel.valueOf(session.getCluster().getConfiguration().getQueryOptions().getConsistencyLevel().name()));

    }

    public int reserveSeats(UUID resId, int trainId, Timestamp tripDate, UUID userId, int car, int seatsAmount, int CarCapacity) {
        BoundStatement bs = new BoundStatement(SELECT_SUM_RESERVED_SEATS_BY_CAR);
        bs.bind(trainId, tripDate, car);
        ResultSet rs = session.execute(bs);
        Row row = rs.one();
        int numOfResSeats = row != null ? row.getInt(0) : 0;
        int numOfSeats = orderService.getTakenSeatsByCar(trainId, tripDate.toString(), car);
        if (numOfSeats + seatsAmount + numOfResSeats > CarCapacity) {
            logger.warn("Not enough seats available for reservation " + resId);
            return 0;
        }
        bs = new BoundStatement(INSERT_INTO_RESERVATIONS);
        bs.bind(resId, trainId, tripDate, userId, car, seatsAmount);
        session.execute(bs);
        //logger.info("Reservation " + resId + " created");
        return 1;
    }

    public int confirmReservation(UUID resId, UUID orderId, int trainId, Timestamp tripDate, UUID userId, int car, int seatsAmount, OrderService orderService) {
        BoundStatement bs = new BoundStatement(SELECT_RESERVATION);
        bs.bind(trainId, tripDate, car, resId);
        Row res = session.execute(bs).one();
        if (res == null) {
            logger.warn("Reservation " + resId + " not found");
            return 0;
        }
        orderService.upsertOrder(orderId, trainId, tripDate, userId, car, seatsAmount);

        bs = new BoundStatement(DELETE_FROM_RESERVATIONS);
        bs.bind(trainId, tripDate, car, resId);
        session.execute(bs);
        //logger.info("Reservation " + resId + " deleted");
        return 1;
    }

    public int getSumReservedSeatsByCar(int trainId, String tripDate, int car) {
        BoundStatement bs = new BoundStatement(SELECT_SUM_RESERVED_SEATS_BY_CAR);
        bs.bind(trainId, Timestamp.valueOf(tripDate), car);
        ResultSet rs = session.execute(bs);
        Row row = rs.one();
        return row != null ? row.getInt(0) : 0;
    }

    public int getReservedSeats(int trainId, String tripDate) {
        BoundStatement bs = new BoundStatement(SELECT_RESERVED_SEATS);
        bs.bind(trainId, Timestamp.valueOf(tripDate));
        ResultSet rs = session.execute(bs);
        Row row = rs.one();
        return row != null ? row.getInt(0) : 0;
    }

    public void deleteAllReservations() {
        BoundStatement bs = new BoundStatement(DELETE_ALL_FROM_RESERVATIONS);
        session.execute(bs);
        logger.info("All reservations deleted");
    }

    public void deleteReservation(int trainId, Timestamp tripDate, int car, UUID resId) {
        BoundStatement bs = new BoundStatement(DELETE_FROM_RESERVATIONS);
        bs.bind(trainId, tripDate, car, resId);
        session.execute(bs);
        //logger.info("Reservation " + resId + " deleted due to conflict resolution");
    }

    private void updateReservation(int trainId, Timestamp tripDate, int car, UUID resId, int newSeatsAmount) {
        String query = "UPDATE reservations SET seats_amount = ? WHERE train_id = ? AND trip_date = ? AND car = ? AND res_id = ?";
        BoundStatement bs = new BoundStatement(session.prepare(query));
        bs.bind(newSeatsAmount, trainId, tripDate, car, resId);
        session.execute(bs);
        //logger.info("Reservation " + resId + " updated to " + newSeatsAmount);
    }
}
