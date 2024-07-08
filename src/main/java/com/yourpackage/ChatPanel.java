package com.yourpackage;

import javax.swing.*;
import java.awt.*;
import java.io.ObjectOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChatPanel extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(ChatPanel.class.getName());
    private JTextArea chatArea;
    private JTextField chatInput;
    private ObjectOutputStream out;
    private String username;
    private String roomCreator;

    public ChatPanel(String username, String roomCreator, ObjectOutputStream out) {
        this.username = username;
        this.roomCreator = roomCreator;
        this.out = out;
        setLayout(new BorderLayout());
        createChatUI();
    }

    private void createChatUI() {
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        add(chatScrollPane, BorderLayout.CENTER);

        chatInput = new JTextField();
        chatInput.addActionListener(e -> sendMessage());
        add(chatInput, BorderLayout.SOUTH);
    }

    private void sendMessage() {
        String message = chatInput.getText();
        sendRequest("CHAT:" + roomCreator + ":" + username + ":" + message);
        chatInput.setText("");
    }

    private void sendRequest(String request) {
        try {
            out.writeObject(request);
            out.flush();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error sending request: " + request, e);
        }
    }

    public void updateChatArea(String message) {
        SwingUtilities.invokeLater(() -> chatArea.append(message + "\n"));
    }

    public String getUsername() {
        return username;
    }
}
