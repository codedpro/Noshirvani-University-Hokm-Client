package com.yourpackage;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RoomView {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;
    private static final Logger LOGGER = Logger.getLogger(RoomView.class.getName());

    private final String username;
    private final String roomCreator;
    private JFrame frame;
    private JPanel teamAPanel;
    private JPanel teamBPanel;
    private JTextArea chatArea;
    private JTextField chatInput;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Socket socket;
    private final int maxPlayers;
    private JButton startGameButton;

    public RoomView(String username, String roomCreator, int maxPlayers) {
        this.username = username;
        this.maxPlayers = maxPlayers;
        this.roomCreator = roomCreator;
        createAndShowGUI();
        setupConnection();
    }

    private void createAndShowGUI() {
        frame = new JFrame("Room");
        frame.setSize(600, 800);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setLayout(null);
        frame.setResizable(false);

        JPanel contentPane = (JPanel) frame.getContentPane();
        contentPane.setBackground(Color.decode("#ffffff"));

        JLabel teamALabel = new JLabel("Team A");
        teamALabel.setFont(new Font("Arial", Font.BOLD, 15));
        teamALabel.setBounds(60, 45, 60, 20);
        frame.add(teamALabel);

        teamAPanel = new JPanel();
        teamAPanel.setLayout(null);
        teamAPanel.setBackground(Color.decode("#f0f0f0"));
        teamAPanel.setBorder(new LineBorder(Color.decode("#cccccc"), 1));
        teamAPanel.setBounds(60, 73, 478, maxPlayers == 2 ? 58 : 117);
        frame.add(teamAPanel);

        JLabel teamBLabel = new JLabel("Team B");
        teamBLabel.setFont(new Font("Arial", Font.BOLD, 15));
        teamBLabel.setBounds(60, maxPlayers == 2 ? 145 : 205, 60, 20);
        frame.add(teamBLabel);

        teamBPanel = new JPanel();
        teamBPanel.setLayout(null);
        teamBPanel.setBackground(Color.decode("#f0f0f0"));
        teamBPanel.setBorder(new LineBorder(Color.decode("#cccccc"), 1));
        teamBPanel.setBounds(62, maxPlayers == 2 ? 175 : 235, 478, maxPlayers == 2 ? 58 : 117);
        frame.add(teamBPanel);

        JLabel chatRoomLabel = new JLabel("ChatRoom");
        chatRoomLabel.setFont(new Font("Arial", Font.BOLD, 15));
        chatRoomLabel.setBounds(62, maxPlayers == 2 ? 245 : 370, 150, 20);
        frame.add(chatRoomLabel);

        chatArea = new JTextArea();
        chatArea.setBounds(62, maxPlayers == 2 ? 275 : 400, 478, 243);
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(new Font("Arial", Font.PLAIN, 15));
        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        chatScrollPane.setBounds(62, maxPlayers == 2 ? 275 : 400, 478, 243);
        frame.add(chatScrollPane);

        chatInput = new JTextField();
        chatInput.setFont(new Font("Arial", Font.PLAIN, 15));
        chatInput.setBounds(62, maxPlayers == 2 ? 515 : 640, 378, 34);
        chatInput.addActionListener(e -> sendMessage());
        chatInput.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateSendButtonState();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateSendButtonState();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateSendButtonState();
            }
        });
        frame.add(chatInput);

        JButton sendButton = new JButton("Send");
        sendButton.setFont(new Font("Arial", Font.BOLD, 14));
        sendButton.setBounds(439, maxPlayers == 2 ? 515 : 640, 100, 33);
        sendButton.setPreferredSize(new Dimension(100, 33));
        sendButton.setBackground(Color.decode("#343332"));
        sendButton.setForeground(Color.decode("#ffffff"));
        sendButton.addActionListener(e -> sendMessage());
        sendButton.setEnabled(false);
        frame.add(sendButton);

        JButton backToMenuButton = new JButton("Back to menu");
        backToMenuButton.setFont(new Font("Arial", Font.BOLD, 14));
        backToMenuButton.setBounds(62, maxPlayers == 2 ? 565 : 690, 232, 35);
        backToMenuButton.setPreferredSize(new Dimension(232, 35));
        backToMenuButton.setBackground(Color.decode("#343332"));
        backToMenuButton.setForeground(Color.decode("#ffffff"));
        backToMenuButton.setBorder(null);
        backToMenuButton.addActionListener(e -> leaveRoom());
        frame.add(backToMenuButton);

        startGameButton = new JButton("Start game");
        startGameButton.setFont(new Font("Arial", Font.BOLD, 14));
        startGameButton.setBounds(308, maxPlayers == 2 ? 565 : 690, 232, 35);
        startGameButton.setPreferredSize(new Dimension(232, 35));
        startGameButton.setBackground(Color.red);
        startGameButton.setForeground(Color.decode("#ffffff"));
        startGameButton.setBorder(null);
        startGameButton.setEnabled(false);
        startGameButton.addActionListener(e -> startGame());
        frame.add(startGameButton);

        frame.setVisible(true);
    }

    private void setupConnection() {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            sendRequest("JOIN_ROOM:" + roomCreator + ":" + username);

            new Thread(() -> {
                try {
                    while (true) {
                        Object received = in.readObject();
                        if (received instanceof String message) {
                            LOGGER.info("Received message: " + message);
                            if (message.startsWith("START_GAME:")) {
                                // Extract teams from the message
                                String[] parts = message.split(":");
                                List<String> teamA = List.of(parts[1].split(","));
                                List<String> teamB = List.of(parts[2].split(","));
                                SwingUtilities.invokeLater(() -> new GameUI(username, roomCreator, out, in, socket));
                                frame.dispose();
                                break;
                            } else if ("KICKED".equals(message)) {
                                JOptionPane.showMessageDialog(frame, "You have been kicked from the room.", "Kicked", JOptionPane.WARNING_MESSAGE);
                                closeRoom(); // Ensure the room is closed and resources are cleaned up
                                break;
                            } else if ("ROOM_CLOSED".equals(message)) {
                                JOptionPane.showMessageDialog(frame, "The room has been closed by the creator.", "Room Closed", JOptionPane.INFORMATION_MESSAGE);
                                closeRoom();
                                break;
                            } else if (message.startsWith("USER_LIST:")) {
                                handleUserListMessage(message);
                            } else {
                                updateChatArea(message);
                            }
                        } else if (received instanceof List) {
                            List<String> users = (List<String>) received;
                            LOGGER.info("Received user list: " + users);
                            displayUsers(users);
                        }
                    }
                } catch (IOException | ClassNotFoundException e) {
                    LOGGER.log(Level.SEVERE, "Error in chat message handling", e);
                }
            }).start();

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error setting up connection", e);
        }
    }

    private void handleUserListMessage(String message) {
        String userList = message.substring("USER_LIST:".length());
        List<String> users = List.of(userList.split(":"));
        displayUsers(users);
    }

    private void updateChatArea(String message) {
        SwingUtilities.invokeLater(() -> chatArea.append(message + "\n"));
    }

    private void displayUsers(List<String> users) {
        SwingUtilities.invokeLater(() -> {
            teamAPanel.removeAll();
            teamBPanel.removeAll();

            List<String> teamA = new ArrayList<>();
            List<String> teamB = new ArrayList<>();

            for (int i = 0; i < users.size(); i++) {
                if (i % 2 == 0) {
                    teamA.add(users.get(i));
                } else {
                    teamB.add(users.get(i));
                }
            }

            // If the number of users is odd, add an empty player to the smaller team
            if (teamA.size() < teamB.size()) {
                teamA.add("Empty Player");
            } else if (teamB.size() < teamA.size()) {
                teamB.add("Empty Player");
            }

            displayTeam(teamA, teamAPanel);
            displayTeam(teamB, teamBPanel);

            if (users.size() == maxPlayers && roomCreator.equals(username)) {
                startGameButton.setEnabled(true);
            } else {
                startGameButton.setEnabled(false);
            }
        });
    }

    private void displayTeam(List<String> team, JPanel teamPanel) {
        for (int i = 0; i < team.size(); i++) {
            String user = team.get(i);
            String imagePath = "/data/boy.png";
            if (user.equals("Empty Player")) {
                imagePath = "/data/nullplayer.png";
            }

            JPanel userPanelItem = new JPanel();
            userPanelItem.setLayout(null);
            userPanelItem.setBackground(Color.decode("#f0f0f0"));
            userPanelItem.setBounds(0, i * 58, 478, 58);

            ImageIcon imageIcon = new ImageIcon(Objects.requireNonNull(getClass().getResource(imagePath)));
            JLabel userPic = new JLabel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Dimension dim = getSize();
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(Color.WHITE);
                    g2.fillOval(0, 0, dim.width - 1, dim.height - 1);
                    g2.drawOval(0, 0, dim.width - 1, dim.height - 1);
                    g2.clip(new Ellipse2D.Float(0, 0, dim.width, dim.height));
                    g.drawImage(imageIcon.getImage(), 0, 0, dim.width, dim.height, null);
                    g2.dispose();
                }
            };

            userPic.setBounds(20, 14, 30, 30);
            userPanelItem.add(userPic);

            JLabel userName = new JLabel(user);
            userName.setFont(new Font("Arial", Font.PLAIN, 14));
            userName.setBounds(60, 14, 200, 30);
            userPanelItem.add(userName);

            if (!user.equals("Empty Player") && roomCreator.equals(username) && !user.equals(username)) {
                JLabel kickLabel = new JLabel("<html><u style='color:red;'>kick</html>");
                kickLabel.setFont(new Font("Arial", Font.BOLD, 12));
                kickLabel.setForeground(Color.RED);
                kickLabel.setBounds(400, 14, 50, 30);
                kickLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                kickLabel.addMouseListener(new java.awt.event.MouseAdapter() {
                    public void mouseClicked(java.awt.event.MouseEvent evt) {
                        kickUser(user);
                    }
                });
                userPanelItem.add(kickLabel);
            }

            teamPanel.add(userPanelItem);
        }
        teamPanel.revalidate();
        teamPanel.repaint();
    }

    private void updateSendButtonState() {
        SwingUtilities.invokeLater(() -> {
            boolean enable = !chatInput.getText().trim().isEmpty();
            for (Component component : frame.getContentPane().getComponents()) {
                if (component instanceof JButton && ((JButton) component).getText().equals("Send")) {
                    component.setEnabled(enable);
                    break;
                }
            }
        });
    }

    private void sendMessage() {
        String message = chatInput.getText();
        sendRequest("CHAT:" + roomCreator + ":" + username + ":" + message);
        chatInput.setText("");
    }

    private void leaveRoom() {
        sendRequest("LEAVE_ROOM:" + roomCreator + ":" + username);
        closeRoom();
    }

    private void closeRoom() {
        try {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            frame.dispose();
            SwingUtilities.invokeLater(() -> new RoomsPage().createAndShowGUI(username));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error closing room connection", e);
        }
    }

    private void kickUser(String user) {
        sendRequest("KICK_USER:" + roomCreator + ":" + user);
    }

    private void startGame() {
        List<String> teamA = new ArrayList<>();
        List<String> teamB = new ArrayList<>();

        for (Component component : teamAPanel.getComponents()) {
            if (component instanceof JPanel userPanel) {
                JLabel nameLabel = (JLabel) userPanel.getComponent(1);
                teamA.add(nameLabel.getText());
            }
        }

        for (Component component : teamBPanel.getComponents()) {
            if (component instanceof JPanel userPanel) {
                JLabel nameLabel = (JLabel) userPanel.getComponent(1);
                teamB.add(nameLabel.getText());
            }
        }

        sendRequest("START_GAME:" + roomCreator + ":" + username + ":" + String.join(",", teamA) + ":" + String.join(",", teamB));
    }

    private void sendRequest(String request) {
        try {
            out.writeObject(request);
            out.flush();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error sending request: " + request, e);
        }
    }
}
