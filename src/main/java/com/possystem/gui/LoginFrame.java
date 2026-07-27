package com.possystem.gui;

import com.possystem.dao.UserDAO;
import com.possystem.model.User;
import com.possystem.util.NYCSkylinePanel;

import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {

    private final JTextField usernameField = new JTextField(16);
    private final JPasswordField passwordField = new JPasswordField(16);
    private final UserDAO userDAO = new UserDAO();

    public LoginFrame() {
        setTitle("NY Coffee Co. - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(520, 460);
        setLocationRelativeTo(null);
        setResizable(false);

        // Sunset-over-Manhattan backdrop behind the login card, for a New York City vibe.
        NYCSkylinePanel background = new NYCSkylinePanel(false);
        background.setLayout(new GridBagLayout());
        setContentPane(background);

        // Frosted white "marquee" card, floating over the skyline, holding the actual form.
        JPanel card = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 246));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(new Color(212, 175, 55));
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 20, 20);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("NY Coffee Co.", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(new Color(30, 42, 58));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        card.add(title, gbc);

        JLabel tagline = new JLabel("Brewed in the Heart of NYC", SwingConstants.CENTER);
        tagline.setFont(new Font("SansSerif", Font.ITALIC, 13));
        tagline.setForeground(new Color(180, 140, 40));
        gbc.gridy = 1;
        card.add(tagline, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 2; gbc.gridx = 0;
        card.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        card.add(usernameField, gbc);

        gbc.gridy = 3; gbc.gridx = 0;
        card.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        card.add(passwordField, gbc);

        JButton loginBtn = new JButton("Sign In");
        gbc.gridy = 4; gbc.gridx = 0; gbc.gridwidth = 2;
        card.add(loginBtn, gbc);

        loginBtn.addActionListener(e -> attemptLogin());
        passwordField.addActionListener(e -> attemptLogin());

        background.add(card, new GridBagConstraints());
    }

    private void attemptLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter username and password.", "Missing info", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            User user = userDAO.authenticate(username, password);
            if (user == null) {
                JOptionPane.showMessageDialog(this, "Invalid username or password.", "Login failed", JOptionPane.ERROR_MESSAGE);
                return;
            }
            new MainDashboard(user).setVisible(true);
            dispose();
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this,
                "Could not connect to database.\nDetails: " + ex.getMessage(),
                "Connection error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
