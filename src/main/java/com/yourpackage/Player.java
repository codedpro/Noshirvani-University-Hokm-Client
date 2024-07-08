package com.yourpackage;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;

public class Player implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String name;
    private final transient ObjectOutputStream outputStream;
    private List<Card> hand; // Assuming Card class exists

    public Player(String name, ObjectOutputStream outputStream) {
        this.name = name;
        this.outputStream = outputStream;
    }

    public String getName() {
        return name;
    }

    public ObjectOutputStream getOutputStream() {
        return outputStream;
    }

    public List<Card> getHand() {
        return hand;
    }

    public void setHand(List<Card> hand) {
        this.hand = hand;
    }

    public void closeConnections() {
        try {
            if (outputStream != null) {
                outputStream.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing output stream for player " + name);
            e.printStackTrace();
        }
    }
}
