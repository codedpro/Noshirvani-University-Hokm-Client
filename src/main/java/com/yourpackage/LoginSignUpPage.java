package com.yourpackage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoginSignUpPage {
    private static final Logger LOGGER = Logger.getLogger(LoginSignUpPage.class.getName());

    public void createAndShowGUI() {
        JFrame frame = new JFrame();
        frame.setTitle("Hokm Game Login/Sign up page");
        frame.setSize(400, 320);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.getContentPane().setBackground(new Color(255, 255, 255));

        frame.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        JLabel imageLabel = new JLabel();
        try {
            ImageIcon imageIcon = new ImageIcon(Objects.requireNonNull(getClass().getResource("/data/HOKM3.png")));
            imageLabel.setIcon(imageIcon);
        } catch (NullPointerException e) {
            LOGGER.log(Level.SEVERE, "Image resource not found: /assets/HOKM3.png", e);
            JOptionPane.showMessageDialog(frame, "Image not found: /assets/HOKM3.png", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error occurred while loading image: /assets/HOKM3.png", e);
            JOptionPane.showMessageDialog(frame, "Unexpected error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        frame.add(imageLabel, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1;
        gbc.gridx = 0;
        JLabel usernameLabel = new JLabel("Username :");
        usernameLabel.setFont(new Font("Arial", Font.BOLD, 16));
        frame.add(usernameLabel, gbc);

        JTextField username = new JTextField(10);
        gbc.gridx = 1;
        frame.add(username, gbc);

        gbc.gridy = 2;
        gbc.gridx = 0;
        JLabel passwordLabel = new JLabel("Password :");
        passwordLabel.setFont(new Font("Arial", Font.BOLD, 16));
        frame.add(passwordLabel, gbc);

        JTextField password = new JTextField(10);
        gbc.gridx = 1;
        frame.add(password, gbc);

        JButton signUpButton = new JButton("Sign up");
        configureButton(signUpButton, Color.red);
        gbc.gridy = 3;
        gbc.gridx = 0;
        frame.add(signUpButton, gbc);

        JButton loginButton = new JButton("Login");
        configureButton(loginButton, Color.black);
        gbc.gridx = 1;
        frame.add(loginButton, gbc);

        addMouseListener(signUpButton, Color.decode("#ed0000"), Color.red);
        addMouseListener(loginButton, Color.decode("#262626"), Color.black);

        signUpButton.addActionListener(e -> handleSignUp(username.getText(), password.getText(), frame));
        loginButton.addActionListener(e -> handleLogin(username.getText(), password.getText(), frame));

        frame.setVisible(true);
    }

    private void configureButton(JButton button, Color bgColor) {
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setBackground(bgColor);
        button.setForeground(Color.white);
        button.setBorderPainted(false);
        button.setPreferredSize(new Dimension(120, 30));
    }

    private void addMouseListener(JButton button, Color hoverColor, Color originalColor) {
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                button.setBackground(hoverColor);
                button.setForeground(Color.WHITE);
            }

            public void mouseExited(MouseEvent evt) {
                button.setBackground(originalColor);
                button.setForeground(Color.WHITE);
            }
        });
    }

    private void handleSignUp(String usernameSTR, String passwordSTR, JFrame frame) {
        if (usernameSTR.isEmpty() || passwordSTR.isEmpty()) {
            showErrorDialog(frame, "You have not entered the " + (usernameSTR.isEmpty() ? "username" : "password") + "!");
            return;
        }
        Users usersObj = new Users();
        String registrationResult = usersObj.registerUser(usernameSTR, passwordSTR);
        if (Objects.equals(registrationResult, "This username is taken!")) {
            showErrorDialog(frame, "This username is taken!");
        } else {
            showInfoDialog(frame, "Registration successful:) now login");
        }
    }

    private void handleLogin(String usernameSTR, String passwordSTR, JFrame frame) {
        if (usernameSTR.isEmpty() || passwordSTR.isEmpty()) {
            showErrorDialog(frame, "You have not entered the " + (usernameSTR.isEmpty() ? "username" : "password") + "!");
            return;
        }

        Users usersObj = new Users();
        String loginResult = usersObj.loginUser(usernameSTR, passwordSTR);
        if (Objects.equals(loginResult, "username or password is incorrect!")) {
            showErrorDialog(frame, "username or password is incorrect!");
        } else {
            showInfoDialog(frame, loginResult);
            frame.dispose();
            new RoomsPage().createAndShowGUI(usernameSTR);
        }
    }

    private void showErrorDialog(JFrame frame, String message) {
        JOptionPane.showMessageDialog(frame, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showInfoDialog(JFrame frame, String message) {
        JOptionPane.showMessageDialog(frame, message, "Information", JOptionPane.INFORMATION_MESSAGE);
    }
}
