package com.yourpackage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GameUI {
    private static final Logger LOGGER = Logger.getLogger(GameUI.class.getName());
    private ChatPanel chatPanel;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;
    private Socket socket;
    private String username;
    private String roomCreator;
    private JPanel playersPanel;
    private Team teamA;
    private Team teamB;
    private JLabel teamAScore;
    private JLabel teamBScore;
    private JLabel teamARoundWins;
    private JLabel teamBRoundWins;
    private JPanel handPanel;
    private List<JButton> handButtons;
    private JPanel hokmSelectionPanel;
    private boolean isMyTurn = false;
    private String currentSuit = "";

    private List<JLabel> playedCardLabels;

    public GameUI(String username, String roomCreator, ObjectOutputStream out, ObjectInputStream in, Socket socket) {
        this.username = username;
        this.roomCreator = roomCreator;
        this.out = out;
        this.in = in;
        this.socket = socket;
        this.teamA = new Team("Team A");
        this.teamB = new Team("Team B");
        this.handButtons = new ArrayList<>();
        this.playedCardLabels = new ArrayList<>();

        createAndShowGUI();
        setupConnection();
        requestPlayerList();
    }

    private void createAndShowGUI() {
        JFrame frame = new JFrame("Hokm Game");
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setLayout(null);

        JLabel background = new JLabel(new ImageIcon(Objects.requireNonNull(getClass().getResource("/data/background.jpg"))));
        background.setBounds(0, 0, 800, 600);
        frame.setContentPane(background);

        teamAScore = new JLabel("Team A: 0");
        teamAScore.setBounds(20, 20, 100, 30);
        frame.add(teamAScore);

        teamBScore = new JLabel("Team B: 0");
        teamBScore.setBounds(20, 60, 100, 30);
        frame.add(teamBScore);

        teamARoundWins = new JLabel("Team A Rounds: 0");
        teamARoundWins.setBounds(20, 100, 150, 30);
        frame.add(teamARoundWins);

        teamBRoundWins = new JLabel("Team B Rounds: 0");
        teamBRoundWins.setBounds(20, 140, 150, 30);
        frame.add(teamBRoundWins);

        playersPanel = new JPanel(null);
        playersPanel.setOpaque(false);
        playersPanel.setBounds(0, 0, 800, 600);
        frame.add(playersPanel);

        JButton chatButton = new JButton(new ImageIcon(Objects.requireNonNull(getClass().getResource("/data/icon.png"))));
        chatButton.setBounds(740, 20, 40, 40);
        chatButton.addActionListener(e -> toggleChatPanel());
        frame.add(chatButton);

        chatPanel = new ChatPanel(username, roomCreator, out);
        chatPanel.setBounds(400, 0, 400, 600);
        chatPanel.setVisible(false);
        frame.add(chatPanel);

        handPanel = new JPanel();
        handPanel.setBounds(50, 400, 700, 150);
        handPanel.setOpaque(false);
        frame.add(handPanel);

        hokmSelectionPanel = new JPanel();
        hokmSelectionPanel.setBounds(300, 200, 200, 100);
        hokmSelectionPanel.setOpaque(false);
        hokmSelectionPanel.setVisible(false);
        frame.add(hokmSelectionPanel);

        frame.setVisible(true);
    }

    private void setupConnection() {
        new Thread(() -> {
            try {
                while (true) {
                    Object received = in.readObject();
                    if (received instanceof String message) {
                        LOGGER.info("Received message: " + message);
                        if (message.startsWith("PLAYER_LIST:")) {
                            String[] parts = message.split(":", 4);
                            if (parts.length == 4) {
                                String teamAMessage = parts[2].trim();
                                String teamBMessage = parts[3].trim();
                                LOGGER.info("Team A: " + teamAMessage + ", Team B: " + teamBMessage);
                                updatePlayersList(teamAMessage, teamBMessage);
                            }
                        } else if (message.startsWith("CHAT:")) {
                            chatPanel.updateChatArea(message.substring(5));
                        } else if (message.startsWith("SCORE_UPDATE:")) {
                            updateScores(message);
                        } else if (message.startsWith("TURN_WINNER:")) {
                            handleTurnWinner(message);
                        } else if (message.startsWith("START_GAME:")) {
                            handleStartGame(message);
                        } else if (message.startsWith("MASTER_SELECTED:")) {
                            String master = message.split(":")[1];
                            currentSuit = master;
                            LOGGER.info("Master is " + master);
                        } else if (message.startsWith("DEAL_CARDS:")) {
                            DealCards(Collections.singletonList(message));
                        } else if (message.contains("SELECT_HOKM")) {
                            showHokmSelection();
                        } else if (message.startsWith("CARD_PLAYED:")) {
                            handlePlayCard(message);
                        } else if (message.startsWith("PLAYER_TURN:")) {
                            handleTurn(message);
                        } else if (message.startsWith("HOKM_SELECTED:")) {
                            handleHokmSelected(message);
                        } else if (message.startsWith("ROUND_START:")) {
                            handleRoundStart(message);
                        } else if (message.startsWith("ROUND_WINS_UPDATE:")) {
                            handleRoundWinsUpdate(message);
                        } else if (message.startsWith("TEAM_WINS_ROUND:")) {
                            handleTeamWinsRound(message);
                        } else if (message.startsWith("GAME_OVER:")) {
                            handleGameOver(message);
                        } else {
                            LOGGER.info("Unknown message received");
                            chatPanel.updateChatArea(message);
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error in game message handling", e);
            }
        }).start();
    }

    private void handleTurn(String message) {
        String player = message.split(":")[1];
        isMyTurn = player.equals(username);
        if (isMyTurn) {
            JOptionPane.showMessageDialog(null, "It's your turn!", "Turn", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private List<String> extractPlayerNames(String part) {
        return Arrays.stream(part.replace("Team A: ", "").replace("Team B: ", "").split(","))
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .toList();
    }

    private void requestPlayerList() {
        try {
            out.writeObject("PLAYER_LIST:" + username);
            out.flush();
            LOGGER.log(Level.INFO, "Player list requested");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error requesting player list", e);
        }
    }

    private void updatePlayersList(String teamAMessage, String teamBMessage) {
        List<String> teamA = extractPlayerNames(teamAMessage);
        List<String> teamB = extractPlayerNames(teamBMessage);
        LOGGER.log(Level.INFO, "Team A: " + teamA + ", Team B: " + teamB);
        SwingUtilities.invokeLater(() -> {
            playersPanel.removeAll();
            setupHokmGrid(teamA, teamB);
            playersPanel.revalidate();
            playersPanel.repaint();
        });
    }

    private void updateScores(String message) {
        String[] parts = message.split(":");
        int scoreA = Integer.parseInt(parts[2]);
        int scoreB = Integer.parseInt(parts[4]);
        SwingUtilities.invokeLater(() -> {
            teamAScore.setText("Team A: " + scoreA);
            teamBScore.setText("Team B: " + scoreB);
        });
    }

    private void handleTurnWinner(String message) {
        String winner = message.split(":")[1];
        JOptionPane.showMessageDialog(null, winner + " won the turn!", "Turn Winner", JOptionPane.INFORMATION_MESSAGE);
        clearPlayedCards();
    }

    private void clearPlayedCards() {
        SwingUtilities.invokeLater(() -> {
            playedCardLabels.forEach(playersPanel::remove);
            playedCardLabels.clear();
            playersPanel.revalidate();
            playersPanel.repaint();
        });
    }

    private void handleStartGame(String message) throws IOException {
        String[] parts = message.split(":");
        List<String> teamA = List.of(parts[1].split(","));
        List<String> teamB = List.of(parts[2].split(","));
        displayTeams(teamA, teamB);
        out.writeObject("GAME_STARTED:" + roomCreator + ":" + username);
        out.flush();
    }

    private void displayTeams(List<String> teamA, List<String> teamB) {
        SwingUtilities.invokeLater(() -> {
            playersPanel.removeAll();

            if (teamA.size() + teamB.size() == 4) {
                setupHokmGrid(teamA, teamB);
            }

            playersPanel.revalidate();
            playersPanel.repaint();
        });
    }

    private void setupHokmGrid(List<String> teamA, List<String> teamB) {
        String[] positions = new String[4];
        Arrays.fill(positions, "");

        if (teamA.contains(username)) {
            positions[0] = username;
            positions[2] = teamA.stream().filter(p -> !p.equals(username)).findFirst().orElse("");
            positions[1] = teamB.size() > 0 ? teamB.get(0) : "";
            positions[3] = teamB.size() > 1 ? teamB.get(1) : "";
        } else if (teamB.contains(username)) {
            positions[0] = username;
            positions[2] = teamB.stream().filter(p -> !p.equals(username)).findFirst().orElse("");
            positions[1] = teamA.size() > 0 ? teamA.get(0) : "";
            positions[3] = teamA.size() > 1 ? teamA.get(1) : "";
        }

        for (int i = 0; i < positions.length; i++) {
            if (!positions[i].isEmpty()) {
                String playerName = positions[i];
                JLabel playerLabel = new JLabel(playerName, SwingConstants.CENTER);
                playerLabel.setBounds(i % 2 == 0 ? 350 : (i == 1 ? 700 : 20), i < 2 ? 50 : 500, 100, 30);
                playersPanel.add(playerLabel);
            }
        }
    }

    private void DealCards(List<String> cards) {
        SwingUtilities.invokeLater(() -> {
            handPanel.removeAll();
            handButtons.clear();
            String cardsMessage = cards.get(0);
            String[] cardList = cardsMessage.substring("DEAL_CARDS:[".length(), cardsMessage.length() - 1).split(", ");

            for (String card : cardList) {
                String trimmedCard = card.trim();
                String imagePath = "/Cards/" + trimmedCard + ".png";

                ImageIcon cardIcon = createImageIcon(imagePath);
                if (cardIcon != null) {
                    JButton cardButton = new JButton(cardIcon);
                    cardButton.setActionCommand(trimmedCard);
                    cardButton.addActionListener(new CardButtonListener());
                    handPanel.add(cardButton);
                    handButtons.add(cardButton);
                } else {
                    LOGGER.log(Level.WARNING, "Image not found: " + imagePath);
                }
            }

            handPanel.revalidate();
            handPanel.repaint();
        });
    }

    protected ImageIcon createImageIcon(String path) {
        java.net.URL imgURL = getClass().getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL);
        } else {
            LOGGER.log(Level.WARNING, "Couldn't find file: " + path);
            return null;
        }
    }

    private class CardButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String card = e.getActionCommand();
            playCard(card);
        }
    }

    private void playCard(String card) {
        if (!isMyTurn) {
            JOptionPane.showMessageDialog(null, "It's not your turn!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!canPlayCard(card)) {
            JOptionPane.showMessageDialog(null, "You cannot play this card!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            out.writeObject("PLAY_CARD:" + roomCreator + ":" + username + ":" + card);
            out.flush();
            removeCardFromHand(card);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error sending played card", e);
        }
    }


    private boolean canPlayCard(String card) {
        // Extract the suit of the card the player wants to play
        String[] cardParts = card.split("-");
        String cardSuit = cardParts[0];

        // If playedCardLabels is empty, it's the player's turn to start
        if (playedCardLabels.isEmpty()) {
            return true;
        }

        // Extract the suit of the first played card
        String[] firstCardParts = playedCardLabels.get(0).getText().split("-");
        String firstCardSuit = firstCardParts[0];

        // Check if the player has any cards of the first played suit
        boolean hasSuit = handButtons.stream()
                .anyMatch(button -> button.getActionCommand().startsWith(firstCardSuit));

        if (hasSuit) {
            // If the player has cards of the first suit, they can only play cards of that suit
            return cardSuit.equals(firstCardSuit);
        } else {
            // If the player does not have any cards of the first suit, they can play any card
            return true;
        }
    }


    private void showHokmSelection() {
        LOGGER.info("Showing Hokm selection panel");
        SwingUtilities.invokeLater(() -> {
            hokmSelectionPanel.setVisible(true);

            // Disable all hand buttons while Hokm selection is ongoing
            for (JButton button : handButtons) {
                button.setEnabled(false);
            }

            // Remove any previous components from the panel
            hokmSelectionPanel.removeAll();

            // Add suit selection buttons
            String[] suits = {"Hearts", "Diamonds", "Clubs", "Spades"};
            for (String suit : suits) {
                JButton suitButton = new JButton(suit);
                suitButton.addActionListener(e -> selectHokm(suit)); // Action listener to select Hokm suit
                hokmSelectionPanel.add(suitButton);
            }

            // Ensure the panel updates correctly
            hokmSelectionPanel.revalidate();
            hokmSelectionPanel.repaint();
        });
    }

    private void selectHokm(String suit) {
        LOGGER.info("Selected Hokm suit: " + suit);

        // Hide Hokm selection panel once a suit is selected
        hokmSelectionPanel.setVisible(false);

        // Re-enable hand buttons after Hokm selection
        SwingUtilities.invokeLater(() -> {
            for (JButton button : handButtons) {
                button.setEnabled(true);
            }
        });

        // Send selected Hokm suit to the server if allowed (e.g., if the player is the room creator)
        try {
            if (username.equals(currentSuit)) {
                out.writeObject("SET_HOKM:" + roomCreator + ":" + suit);
                out.flush();
                LOGGER.info("Sent Hokm selection to server: " + suit);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error sending Hokm selection", e);
        }
    }

    private void handlePlayCard(String message) {
        SwingUtilities.invokeLater(() -> {
            try {
                String[] parts = message.split(":");
                String player = parts[1];
                String card = parts[2];

                displayPlayedCard(player, card);

                if (player.equals(username)) {
                    isMyTurn = false;
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error handling played card", e);
            }
        });
    }

    private void displayPlayedCard(String player, String card) {
        SwingUtilities.invokeLater(() -> {
            JLabel cardLabel = new JLabel(card);
            int x = 0, y = 0;

            if (teamA.contains(player) || teamB.contains(player)) {
                int index = (teamA.contains(player) ? teamA.indexOf(player) : teamB.indexOf(player)) % 4;
                switch (index) {
                    case 0 -> {
                        x = 350;
                        y = 400;
                    }
                    case 1 -> {
                        x = 700;
                        y = 250;
                    }
                    case 2 -> {
                        x = 350;
                        y = 100;
                    }
                    case 3 -> {
                        x = 0;
                        y = 250;
                    }
                }
            }

            cardLabel.setBounds(x, y, 100, 30);
            playersPanel.add(cardLabel);
            playedCardLabels.add(cardLabel);
            playersPanel.revalidate();
            playersPanel.repaint();
        });
    }

    private void removeCardFromHand(String card) {
        SwingUtilities.invokeLater(() -> {
            for (JButton button : handButtons) {
                if (button.getActionCommand().equals(card)) {
                    handPanel.remove(button);
                    handButtons.remove(button);
                    break;
                }
            }
            handPanel.revalidate();
            handPanel.repaint();
        });
    }

    private void handleHokmSelected(String message) {
        String selectedHokm = message.split(":")[1];
        currentSuit = selectedHokm;
        JOptionPane.showMessageDialog(null, "Hokm selected: " + selectedHokm, "Hokm Selected", JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleRoundStart(String message) {
        JOptionPane.showMessageDialog(null, "A new round has started!", "Round Start", JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleRoundWinsUpdate(String message) {
        String[] parts = message.split(":");
        int teamARounds = Integer.parseInt(parts[2]);
        int teamBRounds = Integer.parseInt(parts[4]);
        SwingUtilities.invokeLater(() -> {
            teamARoundWins.setText("Team A Rounds: " + teamARounds);
            teamBRoundWins.setText("Team B Rounds: " + teamBRounds);
        });
    }

    private void handleTeamWinsRound(String message) {
        String winningTeam = message.split(":")[1];
        JOptionPane.showMessageDialog(null, winningTeam + " wins the round!", "Round Winner", JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleGameOver(String message) {
        String winningTeam = message.split(":")[1];
        JOptionPane.showMessageDialog(null, winningTeam + " wins the game!", "Game Over", JOptionPane.INFORMATION_MESSAGE);
    }

    private void toggleChatPanel() {
        chatPanel.setVisible(!chatPanel.isVisible());
    }
}
