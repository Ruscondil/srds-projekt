package com.trains.backend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

import java.sql.Timestamp;
import java.util.Date;
import java.util.UUID;

/*
 * For error handling done right see: 
 * https://www.datastax.com/dev/blog/cassandra-error-handling-done-right
 * 
 * Performing stress tests often results in numerous WriteTimeoutExceptions, 
 * ReadTimeoutExceptions (thrown by Cassandra replicas) and 
 * OpetationTimedOutExceptions (thrown by the client). Remember to retry
 * failed operations until success (it can be done through the RetryPolicy mechanism:
 * https://stackoverflow.com/questions/30329956/cassandra-datastax-driver-retry-policy )
 */

public class BackendSession {

	private static final Logger logger = LoggerFactory.getLogger(BackendSession.class);

	private Session session;

	public BackendSession(String contactPoint, String keyspace) throws BackendException {

		Cluster cluster = Cluster.builder().addContactPoint(contactPoint).build();
		try {
			session = cluster.connect(keyspace);
		} catch (Exception e) {
			throw new BackendException("Could not connect to the cluster. " + e.getMessage() + ".", e);
		}
		prepareStatements();
	}

	private static PreparedStatement SELECT_ALL_FROM_USERS;
	private static PreparedStatement INSERT_INTO_USERS;
	private static PreparedStatement DELETE_ALL_FROM_USERS;
	private static PreparedStatement SELECT_ALL_FROM_ORDERS;
	private static PreparedStatement INSERT_INTO_ORDERS;
	private static PreparedStatement DELETE_ALL_FROM_ORDERS;


	private static final String USER_FORMAT = "- %-10s  %-16s %-10s %-10s\n";
	private static final String ORDER_FORMAT = "Order ID: %s, Train ID: %d, Trip Date: %s, User ID: %s, Car: %d, Seats Amount: %d\n";
	// private static final SimpleDateFormat df = new
	// SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	private void prepareStatements() throws BackendException {
		try {
			// Statements for the 'users' table
			SELECT_ALL_FROM_USERS = session.prepare("SELECT * FROM users;");
			INSERT_INTO_USERS = session.prepare("INSERT INTO users (user_id, name) VALUES (?, ?);");
			DELETE_ALL_FROM_USERS = session.prepare("TRUNCATE users;");

			// Statements for the 'orders' table
			SELECT_ALL_FROM_ORDERS = session.prepare("SELECT * FROM orders;");
			INSERT_INTO_ORDERS = session.prepare("INSERT INTO orders (order_id, train_id, trip_date, user_id, car, seats_amount) VALUES (?, ?, ?, ?, ?, ?);");
			DELETE_ALL_FROM_ORDERS = session.prepare("TRUNCATE orders;");

			// Statements for the 'trains' table
			//SELECT_ALL_FROM_TRAINS = session.prepare("SELECT * FROM trains;");
			//INSERT_INTO_TRAINS = session.prepare("INSERT INTO trains (train_id, cars, seats_per_car) VALUES (?, ?, ?);");
			//DELETE_ALL_FROM_TRAINS = session.prepare("TRUNCATE trains;");
		} catch (Exception e) {
			throw new BackendException("Could not prepare statements. " + e.getMessage() + ".", e);
		}

		logger.info("Statements prepared");
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

	public String selectAllOrders() throws BackendException {
		StringBuilder builder = new StringBuilder();
		BoundStatement bs = new BoundStatement(SELECT_ALL_FROM_ORDERS);

		ResultSet rs = null;

		try {
			rs = session.execute(bs);
		} catch (Exception e) {
			throw new BackendException("Could not perform a query. " + e.getMessage() + ".", e);
		}

		for (Row row : rs) {
			UUID orderId = row.getUUID("order_id");
			int trainId = row.getInt("train_id");
			Date tripDate = row.getTimestamp("trip_date");
			UUID userId = row.getUUID("user_id");
			int car = row.getInt("car");
			int seatsAmount = row.getInt("seats_amount");

			builder.append(String.format(
					ORDER_FORMAT,
					orderId, trainId, tripDate, userId, car, seatsAmount
			));
		}

		return builder.toString();
	}

	public void upsertOrder(UUID orderId, int trainId, Timestamp tripDate, UUID userId, int car, int seatsAmount) throws BackendException {
		BoundStatement bs = new BoundStatement(INSERT_INTO_ORDERS);
		bs.bind(orderId, trainId, tripDate, userId, car, seatsAmount);

		try {
			session.execute(bs);
		} catch (Exception e) {
			throw new BackendException("Could not perform an upsert. " + e.getMessage() + ".", e);
		}

		logger.info("Order " + orderId + " upserted");
	}

	public void deleteAllOrders() throws BackendException {
		BoundStatement bs = new BoundStatement(DELETE_ALL_FROM_ORDERS);

		try {
			session.execute(bs);
		} catch (Exception e) {
			throw new BackendException("Could not perform a delete operation. " + e.getMessage() + ".", e);
		}

		logger.info("All orders deleted");
	}

}
