package com.trains.backend;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.trains.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private Session session;
    private static PreparedStatement SELECT_ALL_FROM_USERS;
    private static PreparedStatement INSERT_INTO_USERS;
    private static PreparedStatement DELETE_ALL_FROM_USERS;

    public UserService(Session session) {
        this.session = session;
        prepareStatements();
    }

    private void prepareStatements() {
        SELECT_ALL_FROM_USERS = session.prepare("SELECT * FROM users;");
        INSERT_INTO_USERS = session.prepare("INSERT INTO users (user_id, name) VALUES (?, ?);");
        DELETE_ALL_FROM_USERS = session.prepare("TRUNCATE users;");
    }

    public String selectAllUsers() {
        StringBuilder builder = new StringBuilder();
        BoundStatement bs = new BoundStatement(SELECT_ALL_FROM_USERS);
        ResultSet rs = session.execute(bs);

        for (Row row : rs) {
            UUID userId = row.getUUID("user_id");
            String name = row.getString("name");

            builder.append(String.format("User ID: %s, User name: %s\n", userId, name));
        }

        return builder.toString();
    }

    public List<Client> getAllUsers() {
        List<Client> users = new ArrayList<>();
        BoundStatement bs = new BoundStatement(SELECT_ALL_FROM_USERS);
        ResultSet rs = session.execute(bs);

        for (Row row : rs) {
            UUID userId = row.getUUID("user_id");
            String name = row.getString("name");
            users.add(new Client(userId, name));
        }

        return users;
    }

    public Client getUser(UUID userId) {
        String query = "SELECT * FROM users WHERE user_id = ?";
        BoundStatement bs = new BoundStatement(session.prepare(query));
        bs.bind(userId);
        ResultSet rs = session.execute(bs);
        Row row = rs.one();
        if (row != null) {
            String name = row.getString("name");
            return new Client(userId, name);
        }
        return null;
    }

    public void upsertUser(UUID userId, String name) {
        BoundStatement bs = new BoundStatement(INSERT_INTO_USERS);
        bs.bind(userId, name);
        session.execute(bs);
        logger.info("User " + userId + " upserted");
    }

    public void deleteAllUsers() {
        BoundStatement bs = new BoundStatement(DELETE_ALL_FROM_USERS);
        session.execute(bs);
        logger.info("All users deleted");
    }

    public Client createNewUser() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter your name: ");
        String name = scanner.nextLine();
        UUID userId = UUID.randomUUID();
        upsertUser(userId, name);
        return new Client(userId, name);
    }
}