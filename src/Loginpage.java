import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;

public class Loginpage extends JFrame {

    public JLabel statusLabel;

    public Loginpage() {

        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Academia - Login");

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(new Color(15, 23, 42)); // Dark Slate Background
        add(mainPanel);

        JPanel cardPanel = new JPanel();
        cardPanel.setLayout(new BoxLayout(cardPanel, BoxLayout.Y_AXIS));
        cardPanel.setBackground(new Color(30, 41, 59)); // Lighter Slate Card

        cardPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(51, 65, 85), 2),
                new EmptyBorder(70, 110, 70, 110)
        ));

        JLabel titleLabel = new JLabel("LOGIN");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 68));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Enter your credentials to access your portal");
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 22));
        subtitleLabel.setForeground(new Color(148, 163, 184));
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 0, 6, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;

        JLabel userLabel = createFormLabel("Exam Roll (ID):");
        gbc.gridy = 0;
        formPanel.add(userLabel, gbc);

        JTextField userTextField = createStyledTextField();
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 20, 0);
        formPanel.add(userTextField, gbc);

        JLabel passLabel = createFormLabel("Password:");
        gbc.gridy = 2;
        gbc.insets = new Insets(6, 0, 6, 0);
        formPanel.add(passLabel, gbc);

        JPasswordField passwordField = createStyledPasswordField();
        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 25, 0);
        formPanel.add(passwordField, gbc);

        JButton loginButton = new JButton("Login");
        loginButton.setFont(new Font("SansSerif", Font.BOLD, 26));
        loginButton.setBackground(new Color(59, 130, 246));
        loginButton.setForeground(Color.WHITE);
        loginButton.setPreferredSize(new Dimension(500, 65));
        loginButton.setFocusPainted(false);
        loginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JButton backButton = new JButton("← Back to Main Menu");
        backButton.setFont(new Font("SansSerif", Font.PLAIN, 20));
        backButton.setForeground(new Color(148, 163, 184));
        backButton.setContentAreaFilled(false);
        backButton.setBorderPainted(false);
        backButton.setFocusPainted(false);
        backButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        backButton.addActionListener(e -> {
            new LoginRegisterpage();
            dispose();
        });

        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String ID = userTextField.getText().trim();
                String password = new String(passwordField.getPassword());

                if (ID.isEmpty() || password.isEmpty()) {
                    statusLabel.setText("Please enter both ID and password.");
                    statusLabel.setForeground(new Color(239, 68, 68));
                    return;
                }

                // Disable button and show loading state
                loginButton.setEnabled(false);
                statusLabel.setText("Authenticating...");
                statusLabel.setForeground(new Color(148, 163, 184));

                // Perform database query off the EDT using SwingWorker
                new SwingWorker<Boolean, Void>() {
                    private String userName = "";
                    private int id = 0;
                    private String errorMessage = "";

                    @Override
                    protected Boolean doInBackground() throws Exception {
                        try {
                            id = Integer.parseInt(ID);
                        } catch (NumberFormatException ex) {
                            errorMessage = "Error: ID must be a valid number.";
                            return false;
                        }

                        String sql = "SELECT * FROM users WHERE id = ? AND password = ?";

                        try (Connection con = DBConnection.getConnection();
                             PreparedStatement pstmt = con.prepareStatement(sql)) {

                            pstmt.setInt(1, id);
                            pstmt.setString(2, password);

                            try (ResultSet rs = pstmt.executeQuery()) {
                                if (rs.next()) {
                                    userName = rs.getString("name");
                                    return true;
                                } else {
                                    errorMessage = "Authentication Failed: Invalid ID or Password.";
                                    return false;
                                }
                            }
                        } catch (SQLException ex) {
                            errorMessage = "Database Connection Error!";
                            ex.printStackTrace();
                            return false;
                        }
                    }

                    @Override
                    protected void done() {
                        loginButton.setEnabled(true);
                        try {
                            boolean success = get();
                            if (success) {
                                statusLabel.setText("Login Successful! Welcome, " + userName);
                                statusLabel.setForeground(new Color(16, 185, 129));

                                // Short delay to render the success message before loading the dashboard
                                Timer timer = new Timer(150, evt -> {
                                    new Dashboard(id);
                                    dispose();
                                });
                                timer.setRepeats(false);
                                timer.start();

                            } else {
                                statusLabel.setText(errorMessage);
                                statusLabel.setForeground(new Color(239, 68, 68));
                            }
                        } catch (Exception ex) {
                            statusLabel.setText("An unexpected error occurred.");
                            statusLabel.setForeground(new Color(239, 68, 68));
                            ex.printStackTrace();
                        }
                    }
                }.execute();
            }
        });

        gbc.gridy = 4;
        gbc.insets = new Insets(10, 0, 10, 0);
        formPanel.add(loginButton, gbc);

        cardPanel.add(titleLabel);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 12)));
        cardPanel.add(subtitleLabel);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 40)));
        cardPanel.add(formPanel);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        cardPanel.add(statusLabel);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        cardPanel.add(backButton);

        mainPanel.add(cardPanel);
        setVisible(true);
    }

    private JLabel createFormLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 20));
        label.setForeground(new Color(226, 232, 240));
        return label;
    }

    private JTextField createStyledTextField() {
        JTextField textField = new JTextField();
        textField.setFont(new Font("SansSerif", Font.PLAIN, 22));
        textField.setBackground(new Color(15, 23, 42));
        textField.setForeground(Color.WHITE);
        textField.setCaretColor(Color.WHITE);
        textField.setPreferredSize(new Dimension(500, 60));
        textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(71, 85, 105), 1),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));
        return textField;
    }

    private JPasswordField createStyledPasswordField() {
        JPasswordField passwordField = new JPasswordField();
        passwordField.setFont(new Font("SansSerif", Font.PLAIN, 22));
        passwordField.setBackground(new Color(15, 23, 42));
        passwordField.setForeground(Color.WHITE);
        passwordField.setCaretColor(Color.WHITE);
        passwordField.setPreferredSize(new Dimension(500, 60));
        passwordField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(71, 85, 105), 1),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));
        return passwordField;
    }
}