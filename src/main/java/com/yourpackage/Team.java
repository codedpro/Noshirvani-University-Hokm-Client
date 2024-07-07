package com.yourpackage;

import java.util.ArrayList;
import java.util.List;

public class Team {
    private String name;
    private int score;
    private List<String> players;

    public Team(String name) {
        this.name = name;
        this.score = 0;
        this.players = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public int getScore() {
        return score;
    }

    public void addPlayer(String player) {
        players.add(player);
    }

    public void removePlayer(String player) {
        players.remove(player);
    }

    public List<String> getPlayers() {
        return players;
    }

    public void incrementScore() {
        score++;
    }

    public void decrementScore() {
        if (score > 0) {
            score--;
        }
    }
}
