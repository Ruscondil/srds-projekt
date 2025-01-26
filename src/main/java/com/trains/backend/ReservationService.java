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

    public ReservationService(Session session) {
        this.session = session;
        prepareStatements();
    }

    private void prepareStatements() {
        if (INSERT_INTO_RESERVATIONS == null) {
            INSERT_INTO_RESERVATIONS = session.prepare("INSERT INTO reservations (res_id, train_id, trip_date, user_id, car, seats_amount) VALUES (?, ?, ?, ?, ?, ?);").setConsistencyLevel(ConsistencyLevel.valueOf(session.getCluster().getConfiguration().getQueryOptions().getConsistencyLevel().name()));
        }
        if (DELETE_FROM_RESERVATIONS == null) {
            DELETE_FROM_RESERVATIONS = session.prepare("DELETE FROM reservations WHERE train_id = ? AND trip_date = ? AND car = ? AND res_id = ?;").setConsistencyLevel(ConsistencyLevel.valueOf(session.getCluster().getConfiguration().getQueryOptions().getConsistencyLevel().name()));
        }
        if (DELETE_ALL_FROM_RESERVATIONS == null) {
            DELETE_ALL_FROM_RESERVATIONS = session.prepare("TRUNCATE reservations;").setConsistencyLevel(ConsistencyLevel.valueOf(session.getCluster().getConfiguration().getQueryOptions().getConsistencyLevel().name()));
        }
        if (SELECT_SUM_RESERVED_SEATS_BY_CAR == null) {
            SELECT_SUM_RESERVED_SEATS_BY_CAR = session.prepare("SELECT SUM(seats_amount) FROM reservations WHERE train_id = ? AND trip_date = ? AND car = ?;").setConsistencyLevel(ConsistencyLevel.valueOf(session.getCluster().getConfiguration().getQueryOptions().getConsistencyLevel().name()));
        }
        if (SELECT_RESERVATIONS_BY_CAR == null) {
            SELECT_RESERVATIONS_BY_CAR = session.prepare("SELECT res_id, seats_amount FROM reservations WHERE train_id = ? AND trip_date = ? AND car = ?").setConsistencyLevel(ConsistencyLevel.valueOf(session.getCluster().getConfiguration().getQueryOptions().getConsistencyLevel().name()));
        }
    }

    public void reserveSeats(UUID resId, int trainId, Timestamp tripDate, UUID userId, int car, int seatsAmount) {
        BoundStatement bs = new BoundStatement(INSERT_INTO_RESERVATIONS);
        bs.bind(resId, trainId, tripDate, userId, car, seatsAmount);
        session.execute(bs);
        logger.info("Reservation " + resId + " created");
    }

    public void confirmReservation(UUID resId, UUID orderId, int trainId, Timestamp tripDate, UUID userId, int car, int seatsAmount, OrderService orderService) {
        orderService.upsertOrder(orderId, trainId, tripDate, userId, car, seatsAmount);

        BoundStatement bs = new BoundStatement(DELETE_FROM_RESERVATIONS);
        bs.bind(trainId, tripDate, car, resId);
        session.execute(bs);
        logger.info("Reservation " + resId + " deleted");
    }

    public int getSumReservedSeatsByCar(int trainId, String tripDate, int car) {
        BoundStatement bs = new BoundStatement(SELECT_SUM_RESERVED_SEATS_BY_CAR);
        bs.bind(trainId, Timestamp.valueOf(tripDate), car);
        ResultSet rs = session.execute(bs);
        Row row = rs.one();
        return row != null ? row.getInt(0) : 0;
    }

    public void deleteAllReservations() {
        BoundStatement bs = new BoundStatement(DELETE_ALL_FROM_RESERVATIONS);
        session.execute(bs);
        logger.info("All reservations deleted");
    }

    public void resolveConflictsForAllCars(int trainId, Timestamp tripDate, OrderService orderService) {
        String selectedTrain = orderService.getTrainService().selectTrain(trainId, tripDate);
        if (selectedTrain == null) {
            System.out.println("Train not found");
            return;
        }

        int cars = Integer.parseInt(selectedTrain.split(",")[2].split(": ")[1]);
        for (int car = 1; car <= cars; car++) {
            resolveConflicts(trainId, tripDate, car, orderService);
        }
    }

    public void resolveConflicts(int trainId, Timestamp tripDate, int car, OrderService orderService) {
        int reservedSeats = orderService.getTakenSeatsByCar(trainId, tripDate.toString(), car);
        int reservedSeatsInReservations = getSumReservedSeatsByCar(trainId, tripDate.toString(), car);
        int totalReservedSeats = reservedSeats + reservedSeatsInReservations;

        String selectedTrain = orderService.getTrainService().selectTrain(trainId, tripDate);
        if (selectedTrain == null) {
            System.out.println("Train not found");
            return;
        }

        int seatsPerCar = Integer.parseInt(selectedTrain.split(",")[3].split(": ")[1]);
        if (totalReservedSeats > seatsPerCar) {
            int excessSeats = totalReservedSeats - seatsPerCar;
            cancelExcessReservations(trainId, tripDate, car, excessSeats);
        }
    }

    private void cancelExcessReservations(int trainId, Timestamp tripDate, int car, int excessSeats) {
        BoundStatement bs = new BoundStatement(SELECT_RESERVATIONS_BY_CAR);
        bs.bind(trainId, tripDate, car);
        ResultSet rs = session.execute(bs);

        for (Row row : rs) {
            UUID resId = row.getUUID("res_id");
            int seatsAmount = row.getInt("seats_amount");

            if (seatsAmount <= excessSeats) {
                deleteReservation(trainId, tripDate, car, resId);
                excessSeats -= seatsAmount;
            } else {
                updateReservation(trainId, tripDate, car, resId, seatsAmount - excessSeats);
                break;
            }
        }
    }

    private void deleteReservation(int trainId, Timestamp tripDate, int car, UUID resId) {
        BoundStatement bs = new BoundStatement(DELETE_FROM_RESERVATIONS);
        bs.bind(trainId, tripDate, car, resId);
        session.execute(bs);
        logger.info("Reservation " + resId + " deleted due to conflict resolution");
    }

    private void updateReservation(int trainId, Timestamp tripDate, int car, UUID resId, int newSeatsAmount) {
        String query = "UPDATE reservations SET seats_amount = ? WHERE train_id = ? AND trip_date = ? AND car = ? AND res_id = ?";
        BoundStatement bs = new BoundStatement(session.prepare(query));
        bs.bind(newSeatsAmount, trainId, tripDate, car, resId);
        session.execute(bs);
        logger.info("Reservation " + resId + " updated to " + newSeatsAmount + " seats due to conflict resolution");
    }
}
