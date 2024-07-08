    package com.yourpackage;

    import javax.swing.*;
    import java.awt.*;
    import java.io.IOException;
    import java.io.ObjectInputStream;
    import java.io.ObjectOutputStream;
    import java.net.Socket;
    import java.util.List;
    import java.util.Objects;
    import java.util.logging.Level;
    import java.util.logging.Logger;

    public class RoomsPage {
        private static final String SERVER_ADDRESS = "localhost";
        private static final int SERVER_PORT = 12345;
        private static final Logger LOGGER = Logger.getLogger(RoomsPage.class.getName());

        private JPanel roomsPanel;
        private JFrame frame;

        public void createAndShowGUI(String username) {
            frame = new JFrame();
            frame.setTitle("Hokm Rooms page");
            frame.setSize(600, 800);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);
            frame.setResizable(false);
            frame.getContentPane().setBackground(new Color(255, 255, 255));

            frame.getContentPane().setLayout(new BorderLayout());

            JPanel headerPanel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 10, 10, 10);

            gbc.gridwidth = 1;
            gbc.gridy = 0;
            gbc.gridx = 0;
            JLabel usernameLabel = new JLabel("Username : " + username);
            usernameLabel.setFont(new Font("Arial", Font.BOLD, 15));
            headerPanel.add(usernameLabel, gbc);

            gbc.gridx = 1;
            int matchWon = 0;
            JLabel matchWonLabel = new JLabel("Match won : " + matchWon);
            matchWonLabel.setFont(new Font("Arial", Font.BOLD, 15));
            headerPanel.add(matchWonLabel, gbc);

            gbc.gridx = 2;
            int matchLoses = 0;
            JLabel matchLosesLabel = new JLabel("Match loses : " + matchLoses);
            matchLosesLabel.setFont(new Font("Arial", Font.BOLD, 15));
            headerPanel.add(matchLosesLabel, gbc);

            gbc.gridx = 3;
            JButton newGameButton = new JButton();
            configureButton(newGameButton, "New Game");
            headerPanel.add(newGameButton, gbc);

            newGameButton.addActionListener(e -> showPlayerOptions(username));

            frame.add(headerPanel, BorderLayout.NORTH);

            roomsPanel = new JPanel(new GridBagLayout());
            JScrollPane scrollPane = new JScrollPane(roomsPanel);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            frame.add(scrollPane, BorderLayout.CENTER);

            frame.setVisible(true);
            fetchAndDisplayRooms(username);

            Timer timer = new Timer(2000, e -> fetchAndDisplayRooms(username));
            timer.start();
        }

        private void configureButton(JButton button, String text) {
            button.setBorderPainted(false);
            button.setText(text);
            button.setFont(new Font("Arial", Font.BOLD, 15));
            button.setBackground(Color.red);
            button.setForeground(Color.white);
        }

        private void fetchAndDisplayRooms(String username) {
            try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                out.writeObject("GET_ROOMS");
                out.flush();

                List<Room> rooms = (List<Room>) in.readObject();

                roomsPanel.removeAll();

                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(10, 10, 10, 10);
                gbc.gridx = 0;
                gbc.gridy = 0;
                gbc.gridwidth = 1;
                gbc.fill = GridBagConstraints.HORIZONTAL;

                for (Room room : rooms) {
                    if (room.getCreator() != null) {
                        JPanel roomPanel = createRoomPanel(room, username);
                        roomsPanel.add(roomPanel, gbc);
                        gbc.gridy++;
                    }
                }

                for (int i = rooms.size(); i < 3; i++) {
                    JPanel emptyRoomPanel = createEmptyRoomPanel();
                    roomsPanel.add(emptyRoomPanel, gbc);
                    gbc.gridy++;
                }

                roomsPanel.revalidate();
                roomsPanel.repaint();

            } catch (IOException | ClassNotFoundException e) {
                LOGGER.log(Level.SEVERE, "Error fetching and displaying rooms", e);
                JOptionPane.showMessageDialog(null, "Error fetching rooms: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void showPlayerOptions(String username) {
            JFrame optionFrame = new JFrame("Select Players");
            optionFrame.setSize(300, 150);
            optionFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            optionFrame.setLocationRelativeTo(null);
            optionFrame.setResizable(false);
            optionFrame.setLayout(new GridLayout(1, 2));

            JButton twoPlayerButton = new JButton("2 Players");
            JButton fourPlayerButton = new JButton("4 Players");

            configureButton(twoPlayerButton, "2 Players");
            configureButton(fourPlayerButton, "4 Players");

            twoPlayerButton.addActionListener(e -> {
                optionFrame.dispose();
                createRoom(username, frame, 2, 3);
            });

            fourPlayerButton.addActionListener(e -> {
                optionFrame.dispose();
                createRoom(username, frame, 4, 4);
            });

            optionFrame.add(twoPlayerButton);
            optionFrame.add(fourPlayerButton);

            optionFrame.setVisible(true);
        }

        private void createRoom(String username, JFrame frame, int maxPlayers, int rounds) {
            try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                LOGGER.info("Sending request to create room");
                out.writeObject("CREATE_ROOM:" + username + ":" + maxPlayers + ":" + rounds);
                out.flush();
                LOGGER.info("Request sent: CREATE_ROOM:" + username + ":" + maxPlayers);

                String response = (String) in.readObject();
                LOGGER.info("Response from server: " + response);
                if (response.startsWith("ROOM_CREATED")) {
                    JOptionPane.showMessageDialog(null, "Room created successfully!");
                    fetchAndDisplayRooms(username); // Fetch and display rooms immediately after creating a room
                    frame.dispose();
                    new RoomView(username, username, maxPlayers);
                } else {
                    JOptionPane.showMessageDialog(null, "Failed to create room! Response: " + response);
                }

            } catch (IOException | ClassNotFoundException e) {
                LOGGER.log(Level.SEVERE, "Error creating room", e);
                JOptionPane.showMessageDialog(null, "Error creating room: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }



        private JPanel createRoomPanel(Room room, String username) {
            JPanel panel = new JPanel();
            panel.setPreferredSize(new Dimension(500, 200));
            panel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
            panel.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 20, 10, 20);

            gbc.gridx = 0;
            gbc.gridy = 0;
            JLabel roomCreatorLabel = new JLabel();
            roomCreatorLabel.setText("Room creator : " + (room.getCreator() != null ? room.getCreator() : "Unknown"));
            roomCreatorLabel.setFont(new Font("Arial", Font.BOLD, 13));
            panel.add(roomCreatorLabel, gbc);

            gbc.gridx = 1;
            gbc.gridy = 0;
            JButton joinRoomButton = new JButton();
            configureButton(joinRoomButton, "Join room");
            panel.add(joinRoomButton, gbc);

            if (room.getCreator() == null || room.isFull()) {
                joinRoomButton.setEnabled(false);
            } else {
                joinRoomButton.addActionListener(e -> {

                    frame.dispose();
                    new RoomView(username, room.getCreator(), room.getMaxPlayers());
                });
            }

            List<Player> players = room.getPlayers();
            for (int i = 0; i < room.getMaxPlayers(); i++) {
                gbc.gridy = 1;
                gbc.gridx = i;
                JLabel playerImageLabel = new JLabel();
                ImageIcon playerIcon = (i < players.size() && room.getCreator() != null)
                        ? new ImageIcon(Objects.requireNonNull(getClass().getResource("/data/boy.png")))
                        : new ImageIcon(Objects.requireNonNull(getClass().getResource("/data/nullPlayer.png")));

                if (playerIcon.getImageLoadStatus() == MediaTracker.ERRORED) {
                    LOGGER.log(Level.WARNING, "Failed to load image: " + (i < players.size() ? "boy.png" : "nullPlayer.png"));
                }
                playerImageLabel.setIcon(playerIcon);
                panel.add(playerImageLabel, gbc);

                gbc.gridy = 2;
                JLabel playerLabel = new JLabel();
                playerLabel.setText(i < players.size() ? (players.get(i).getName()) : "Empty Slot");
                panel.add(playerLabel, gbc);
            }

            return panel;
        }

        private JPanel createEmptyRoomPanel() {
            JPanel panel = new JPanel();
            panel.setPreferredSize(new Dimension(500, 200));
            panel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
            panel.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 20, 10, 20);

            gbc.gridx = 0;
            gbc.gridy = 0;
            JLabel roomCreatorLabel = new JLabel("Room creator: Empty");
            roomCreatorLabel.setFont(new Font("Arial", Font.BOLD, 13));
            panel.add(roomCreatorLabel, gbc);

            for (int i = 0; i < 4; i++) {
                gbc.gridy = 1;
                gbc.gridx = i;
                JLabel playerImageLabel = new JLabel();
                ImageIcon playerIcon = new ImageIcon(Objects.requireNonNull(getClass().getResource("/data/nullPlayer.png")));

                if (playerIcon.getImageLoadStatus() == MediaTracker.ERRORED) {
                    LOGGER.log(Level.WARNING, "Failed to load image: nullPlayer.png");
                }
                playerImageLabel.setIcon(playerIcon);
                panel.add(playerImageLabel, gbc);

                gbc.gridy = 2;
                JLabel playerLabel = new JLabel("Empty Slot");
                panel.add(playerLabel, gbc);
            }

            return panel;
        }
    }
