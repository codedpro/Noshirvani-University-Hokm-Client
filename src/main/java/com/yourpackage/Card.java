package com.yourpackage;

import java.io.Serializable;

public class Card implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String suit;
    private final String rank;
    private final int power;

    public Card(String suit, String rank, int power) {
        this.suit = suit;
        this.rank = rank;
        this.power = power;
    }

    public String getSuit() {
        return suit;
    }

    public String getRank() {
        return rank;
    }

    public int getPower() {
        return power;
    }

    @Override
    public String toString() {
        return suit + "-" + rank;
    }
}
