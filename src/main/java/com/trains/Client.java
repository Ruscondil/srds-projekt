package com.trains;


import java.util.UUID;

public class Client {
    private UUID userId;
    private String name;

    public Client(UUID id, String name) {
        this.userId = id;
        this.name = name;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}