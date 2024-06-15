package com.yourpackage;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RoomView {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;
    private static final Logger LOGGER = Logger.getLogger(RoomView.class.getName());

    private String username;
    private String roomCreator;
    private JFrame frame;
    private JPanel userPanel;
    private JTextArea chatArea;
    private JTextField chatInput;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Socket socket;

    public RoomView(String username, String roomCreator) {
        this.username = username;
        this.roomCreator = roomCreator;
        createAndShowGUI();
        setupConnection();
    }

    private void createAndShowGUI() {
        frame = new JFrame("Room: " + roomCreator);
        frame.setSize(600, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout(10, 10));


        userPanel = new JPanel();
        userPanel.setLayout(new BoxLayout(userPanel, BoxLayout.Y_AXIS));
        JScrollPane userScrollPane = new JScrollPane(userPanel);
        userScrollPane.setPreferredSize(new Dimension(150, frame.getHeight()));
        frame.add(userScrollPane, BorderLayout.WEST);


        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(new Font("Arial", Font.PLAIN, 14));
        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        frame.add(chatScrollPane, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        chatInput = new JTextField();
        chatInput.setFont(new Font("Arial", Font.PLAIN, 14));
        chatInput.addActionListener(e -> sendMessage());
        inputPanel.add(chatInput, BorderLayout.CENTER);

        JButton sendButton = new JButton("Send");
        sendButton.setFont(new Font("Arial", Font.BOLD, 14));
        sendButton.addActionListener(e -> sendMessage());
        inputPanel.add(sendButton, BorderLayout.EAST);

        JButton backButton = new JButton("Back to Home");
        backButton.setFont(new Font("Arial", Font.BOLD, 14));
        backButton.addActionListener(e -> leaveRoom());
        inputPanel.add(backButton, BorderLayout.WEST);

        frame.add(inputPanel, BorderLayout.SOUTH);

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
                        if (received instanceof String) {
                            String message = (String) received;
                            LOGGER.info("Received message: " + message);
                            if ("KICKED".equals(message)) {
                                JOptionPane.showMessageDialog(frame, "You have been kicked from the room.", "Kicked", JOptionPane.WARNING_MESSAGE);
                                closeRoom();
                                break;
                            } else if ("ROOM_CLOSED".equals(message)) {
                                JOptionPane.showMessageDialog(frame, "The room has been closed by the creator.", "Room Closed", JOptionPane.INFORMATION_MESSAGE);
                                closeRoom();
                                break;
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

    private void updateChatArea(String message) {
        SwingUtilities.invokeLater(() -> chatArea.append(message + "\n"));
    }

    private void displayUsers(List<String> users) {
        SwingUtilities.invokeLater(() -> {
            userPanel.removeAll();
            for (String user : users) {
                JPanel userPanelItem = new JPanel(new BorderLayout(5, 5));
                JLabel userLabel = new JLabel(user);
                userLabel.setFont(new Font("Arial", Font.PLAIN, 14));
                userPanelItem.add(userLabel, BorderLayout.CENTER);

                if (roomCreator.equals(username) && !user.equals(username)) {
                    JButton kickButton = new JButton("Kick");
                    kickButton.setFont(new Font("Arial", Font.PLAIN, 12));
                    kickButton.addActionListener(e -> kickUser(user));
                    userPanelItem.add(kickButton, BorderLayout.EAST);
                }

                userPanel.add(userPanelItem);
            }
            userPanel.revalidate();
            userPanel.repaint();
        });
    }

    private void sendMessage() {
        String message = chatInput.getText();
        sendRequest("CHAT:" + roomCreator + ":" + username + ":" + message);
        chatInput.setText("");
    }

    private void kickUser(String user) {
        sendRequest("KICK_USER:" + roomCreator + ":" + user);
    }

    private void sendRequest(String request) {
        try {
            LOGGER.info("Sending request: " + request);
            out.writeObject(request);
            out.flush();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error sending request: " + request, e);
        }
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
}
