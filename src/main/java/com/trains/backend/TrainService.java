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

public class TrainService {
    private static final Logger logger = LoggerFactory.getLogger(TrainService.class);

    private Session session;
    private static PreparedStatement SELECT_ALL_FROM_TRAINS;
    private static PreparedStatement INSERT_INTO_TRAINS;
    private static PreparedStatement DELETE_ALL_FROM_TRAINS;
    private static PreparedStatement SELECT_AVAILABLE_TRAINS;

    public TrainService(Session session) {
        this.session = session;
        prepareStatements();
    }

    private void prepareStatements() {
        SELECT_ALL_FROM_TRAINS = session.prepare("SELECT * FROM trains;");
        INSERT_INTO_TRAINS = session.prepare("INSERT INTO trains (train_id, trip_date, cars, seats_per_car) VALUES (?, ?, ?, ?);");
        DELETE_ALL_FROM_TRAINS = session.prepare("TRUNCATE trains;");
        SELECT_AVAILABLE_TRAINS = session.prepare("SELECT train_id, trip_date, cars, seats_per_car FROM trains LIMIT ?;");
    }

    public String selectAllTrains() {
        StringBuilder builder = new StringBuilder();
        BoundStatement bs = new BoundStatement(SELECT_ALL_FROM_TRAINS);
        ResultSet rs = session.execute(bs);

        for (Row row : rs) {
            int trainId = row.getInt("train_id");
            Date tripDate = row.getTimestamp("trip_date");
            int cars = row.getInt("cars");
            int seatsPerCar = row.getInt("seats_per_car");

            builder.append(String.format("Train ID: %d, Trip Date: %s, Cars: %d, Seats: %d \n", trainId, tripDate, cars, seatsPerCar));
        }

        return builder.toString();
    }

    public void upsertTrain(int trainId, Timestamp tripDate, int cars, int seatsPerCar) {
        BoundStatement bs = new BoundStatement(INSERT_INTO_TRAINS);
        bs.bind(trainId, tripDate, cars, seatsPerCar);
        session.execute(bs);
        logger.info("Train " + trainId + " upserted");
    }

    public List<String> getAvailableTrains(int limit) {
        List<String> trains = new ArrayList<>();
        BoundStatement bs = new BoundStatement(SELECT_AVAILABLE_TRAINS);
        bs.bind(limit);
        ResultSet rs = session.execute(bs);

        for (Row row : rs) {
            int trainId = row.getInt("train_id");
            String tripDate = row.getTimestamp("trip_date").toString();
            int cars = row.getInt("cars");
            int seatsPerCar = row.getInt("seats_per_car");
            
            trains.add(String.format("Train ID: %d, Departure: %s, Cars: %d, Seats Per Car: %d", trainId, tripDate, cars, seatsPerCar));
        }

        return trains;
    }

    public void deleteAllTrains() {
        BoundStatement bs = new BoundStatement(DELETE_ALL_FROM_TRAINS);
        session.execute(bs);
        logger.info("All trains deleted");
    }
}