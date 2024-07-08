package com.yourpackage;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Room implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(Room.class.getName());

    private final String creator;
    private final List<Player> players;
    private transient List<ObjectOutputStream> clientStreams;
    private final int maxPlayers;
    private boolean isGameStarted;
    private List<String> teamA;
    private List<String> teamB;
    private Deck deck;
    private Player master;
    private String hokmSuit;
    private int currentRound;
    private int totalRounds;
    private int currentPlayerIndex;

    public Room(String creator, int maxPlayers, int totalRounds) {
        this.creator = creator;
        this.maxPlayers = maxPlayers;
        this.players = new ArrayList<>();
        this.clientStreams = new ArrayList<>();
        this.isGameStarted = false;
        this.teamA = new ArrayList<>();
        this.teamB = new ArrayList<>();
        this.totalRounds = totalRounds;
        this.currentRound = 0;
        this.deck = new Deck();
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.clientStreams = new ArrayList<>();
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public String getCreator() {
        return creator;
    }

    public boolean isFull() {
        return players.size() >= maxPlayers;
    }

    public boolean isGameStarted() {
        return isGameStarted;
    }

    public synchronized void closeRoom() {
        for (ObjectOutputStream out : clientStreams) {
            try {
                out.writeObject("ROOM_CLOSED");
                out.flush();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        clientStreams.clear();
        players.clear();
    }

    public List<String> getTeamA() {
        return teamA;
    }

    public synchronized boolean removePlayer(String username) {
        Player playerToRemove = null;
        for (Player player : players) {
            if (player.getName().equals(username)) {
                playerToRemove = player;
                break;
            }
        }
        if (playerToRemove != null) {
            players.remove(playerToRemove);
            clientStreams.remove(playerToRemove.getOutputStream());
            try {
                playerToRemove.getOutputStream().writeObject("KICKED");
                playerToRemove.getOutputStream().flush();
                playerToRemove.closeConnections();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error notifying player of kick", e);
            }
            broadcastUserList();
            return true;
        }
        return false;
    }

    public List<String> getTeamB() {
        return teamB;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public Player getPlayerByName(String username) {
        for (Player player : players) {
            if (player.getName().equals(username)) {
                return player;
            }
        }
        return null;
    }

    public synchronized void notifyKickUser(String username) {
        Player kickedPlayer = getPlayerByName(username);
        if (kickedPlayer != null) {
            try {
                kickedPlayer.getOutputStream().writeObject("KICKED");
                kickedPlayer.getOutputStream().flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                kickedPlayer.closeConnections();
            }
        }
    }

    public synchronized void startGame() {
        if (!isGameStarted && players.size() == maxPlayers) {
            this.isGameStarted = true;
            selectMaster();
            notifyMasterToPickHokem();
        }
    }

    private void selectMaster() {
        int masterIndex = (int) (Math.random() * players.size());
        master = players.get(masterIndex);
        broadcastMessage("MASTER_SELECTED:" + master.getName());
    }

    private void notifyMasterToPickHokem() {
        try {
            master.getOutputStream().writeObject("SELECT_HOKEM");
            master.getOutputStream().flush();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error notifying master to pick Hokem", e);
        }
    }

    public synchronized void setHokmSuit(String hokmSuit) {
        this.hokmSuit = hokmSuit;
        broadcastMessage("HOKM_SELECTED:" + hokmSuit);
        dealRemainingCards();
        startRound();
    }

    private void dealInitialCards() {
        for (Player player : players) {
            List<Card> initialCards = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                initialCards.add(deck.drawCard());
            }
            player.setHand(initialCards);
            sendCardsToPlayer(player);
        }
    }

    private void sendCardsToPlayer(Player player) {
        try {
            player.getOutputStream().writeObject("DEAL_CARDS:" + player.getHand());
            player.getOutputStream().flush();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error sending cards to player", e);
        }
    }

    private void dealRemainingCards() {
        for (Player player : players) {
            while (player.getHand().size() < 13) {
                player.getHand().add(deck.drawCard());
            }
            sendCardsToPlayer(player);
        }
    }

    private void startRound() {
        currentPlayerIndex = players.indexOf(master);
        broadcastMessage("ROUND_START:" + currentRound);
        nextTurn();
    }

    private void nextTurn() {
        if (currentPlayerIndex >= players.size()) {
            currentPlayerIndex = 0;
        }
        Player currentPlayer = players.get(currentPlayerIndex);
        broadcastMessage("PLAYER_TURN:" + currentPlayer.getName());
    }

    public synchronized void playCard(Player player, Card card) {
        if (players.get(currentPlayerIndex).equals(player)) {
            player.getHand().remove(card);
            broadcastMessage("CARD_PLAYED:" + player.getName() + ":" + card);
            currentPlayerIndex++;
            if (isRoundOver()) {
                determineRoundWinner();
            } else {
                nextTurn();
            }
        } else {
            try {
                player.getOutputStream().writeObject("ERROR:Not your turn");
                player.getOutputStream().flush();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error sending error message to player", e);
            }
        }
    }

    private boolean isRoundOver() {
        return players.stream().allMatch(player -> player.getHand().isEmpty());
    }

    private void determineRoundWinner() {
        // Logic to determine round winner and update scores
        currentRound++;
        if (currentRound >= totalRounds) {
            endGame();
        } else {
            deck = new Deck();
            selectMaster();
            dealInitialCards();
        }
    }

    private void endGame() {
        broadcastMessage("GAME_OVER");
        // Logic to determine overall winner
    }

    public synchronized void broadcastMessage(String message) {
        List<ObjectOutputStream> closedStreams = new ArrayList<>();
        for (ObjectOutputStream client : clientStreams) {
            try {
                client.writeObject(message);
                client.flush();
            } catch (IOException e) {
                closedStreams.add(client);
                e.printStackTrace();
            }
        }
        clientStreams.removeAll(closedStreams);
    }

    public synchronized void broadcastUserList() {
        List<ObjectOutputStream> closedStreams = new ArrayList<>();
        for (ObjectOutputStream out : clientStreams) {
            try {
                out.writeObject("USER_LIST:" + String.join(",", teamA) + ":" + String.join(",", teamB));
                out.flush();
            } catch (IOException e) {
                System.out.println("IOException caught: " + e);
                closedStreams.add(out);
            }
        }
        clientStreams.removeAll(closedStreams);
    }

    public synchronized void addPlayer(Player player) {
        if (players.stream().noneMatch(p -> p.getName().equals(player.getName())) && players.size() < maxPlayers) {
            players.add(player);
            clientStreams.add(player.getOutputStream());
            if (teamA.size() <= teamB.size()) {
                teamA.add(player.getName());
            } else {
                teamB.add(player.getName());
            }
            broadcastMessage(player.getName() + " has joined the room.");
            broadcastUserList();
        }
    }

    public synchronized void addClientStream(ObjectOutputStream out) {
        clientStreams.add(out);
    }

    public synchronized List<ObjectOutputStream> getClientStreams() {
        return new ArrayList<>(clientStreams);
    }
}
