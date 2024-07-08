package com.yourpackage;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
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

    public GameUI(String username, String roomCreator, ObjectOutputStream out, ObjectInputStream in, Socket socket) {
        this.username = username;
        this.roomCreator = roomCreator;
        this.out = out;
        this.in = in;
        this.socket = socket;
        this.teamA = new Team("Team A");
        this.teamB = new Team("Team B");

        createAndShowGUI();
        setupConnection();
        requestPlayerList(); // Request player list when the game starts
    }

    private void createAndShowGUI() {
        JFrame frame = new JFrame("Hokm Game");
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setLayout(null);

        // Set background
        JLabel background = new JLabel(new ImageIcon(Objects.requireNonNull(getClass().getResource("/data/background.jpg"))));
        background.setBounds(0, 0, 800, 600);
        frame.setContentPane(background);

        // Scoreboard
        teamAScore = new JLabel("Team A: 0");
        teamAScore.setBounds(20, 20, 100, 30);
        frame.add(teamAScore);

        teamBScore = new JLabel("Team B: 0");
        teamBScore.setBounds(20, 60, 100, 30);
        frame.add(teamBScore);

        // Player positions panel
        playersPanel = new JPanel(null);
        playersPanel.setOpaque(false);
        playersPanel.setBounds(0, 0, 800, 600);
        frame.add(playersPanel);

        // Chat icon and panel
        JButton chatButton = new JButton(new ImageIcon(Objects.requireNonNull(getClass().getResource("/data/icon.png"))));
        chatButton.setBounds(740, 20, 40, 40);
        chatButton.addActionListener(e -> toggleChatPanel());
        frame.add(chatButton);

        chatPanel = new ChatPanel(username, roomCreator, out);
        chatPanel.setBounds(400, 0, 400, 600);
        chatPanel.setVisible(false);
        frame.add(chatPanel);

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
                        } else if (message.startsWith("START_GAME:")) {
                            handleStartGame(message);
                        } else {
                            chatPanel.updateChatArea(message);
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error in game message handling", e);
            }
        }).start();
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
        int scoreA = Integer.parseInt(parts[1]);
        int scoreB = Integer.parseInt(parts[2]);

        SwingUtilities.invokeLater(() -> {
            teamAScore.setText("Team A: " + scoreA);
            teamBScore.setText("Team B: " + scoreB);
        });
    }

    private void handleStartGame(String message) {
        String[] parts = message.split(":");
        List<String> teamA = List.of(parts[1].split(","));
        List<String> teamB = List.of(parts[2].split(","));
        displayTeams(teamA, teamB);
    }

    private void displayTeams(List<String> teamA, List<String> teamB) {
        SwingUtilities.invokeLater(() -> {
            // Clear previous team display if any
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
        Arrays.fill(positions, ""); // Initialize positions with empty strings

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

        int[][] coordinates = {
                {350, 500}, // Bottom (current user)
                {350, 20},  // Top (opposite teammate)
                {20, 250},  // Left (opponent 1)
                {680, 250}  // Right (opponent 2)
        };

        if (positions[2].isEmpty() && positions[3].isEmpty()) { // Only 2 players
            coordinates = new int[][] {
                    {350, 500}, // Bottom (current user)
                    {350, 20}   // Top (opponent)
            };
        }

        for (int i = 0; i < coordinates.length; i++) {
            if (!positions[i].isEmpty()) {
                int x = coordinates[i][0];
                int y = coordinates[i][1];
                addPlayerLabel(positions[i], x, y);
            }
        }
    }

    private void addPlayerLabel(String name, int x, int y) {
        JPanel playerPanel = new JPanel();
        playerPanel.setLayout(new BorderLayout());
        playerPanel.setBounds(x, y, 100, 100);
        playerPanel.setOpaque(false);

        JLabel nameLabel = new JLabel(name);
        nameLabel.setHorizontalAlignment(SwingConstants.CENTER);
        nameLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        playerPanel.add(nameLabel, BorderLayout.SOUTH);

        JLabel profilePic = new JLabel(new ImageIcon(Objects.requireNonNull(getClass().getResource("/data/boy.png"))));
        profilePic.setHorizontalAlignment(SwingConstants.CENTER);
        playerPanel.add(profilePic, BorderLayout.CENTER);

        playersPanel.add(playerPanel);
    }

    private void toggleChatPanel() {
        chatPanel.setVisible(!chatPanel.isVisible());
    }
}
