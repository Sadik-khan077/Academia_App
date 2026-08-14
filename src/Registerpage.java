import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;

public class Registerpage extends JFrame {

    public JLabel statusLabel;

    public Registerpage() {

        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Academia - Register");

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(new Color(15, 23, 42)); // Dark Slate Background
        add(mainPanel);

        JPanel cardPanel = new JPanel();
        cardPanel.setLayout(new BoxLayout(cardPanel, BoxLayout.Y_AXIS));
        cardPanel.setBackground(new Color(30, 41, 59)); // Lighter Slate Card

        cardPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(51, 65, 85), 2),
                new EmptyBorder(60, 120, 60, 120)
        ));

        JLabel titleLabel = new JLabel("REGISTER");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 68));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Create your Academia student account");
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 22));
        subtitleLabel.setForeground(new Color(148, 163, 184));
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 0, 5, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;

        JLabel idLabel = createFormLabel("Exam Roll (ID):");
        gbc.gridy = 0;
        formPanel.add(idLabel, gbc);

        JTextField idTextField = createStyledTextField();
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 14, 0);
        formPanel.add(idTextField, gbc);

        JLabel nameLabel = createFormLabel("Full Name:");
        gbc.gridy = 2;
        gbc.insets = new Insets(5, 0, 5, 0);
        formPanel.add(nameLabel, gbc);

        JTextField nameTextField = createStyledTextField();
        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 14, 0);
        formPanel.add(nameTextField, gbc);

        JLabel passLabel = createFormLabel("Password:");
        gbc.gridy = 4;
        gbc.insets = new Insets(5, 0, 5, 0);
        formPanel.add(passLabel, gbc);

        JPasswordField passwordField = createStyledPasswordField();
        gbc.gridy = 5;
        gbc.insets = new Insets(0, 0, 14, 0);
        formPanel.add(passwordField, gbc);

        JLabel batchLabel = createFormLabel("Batch Number:");
        gbc.gridy = 6;
        gbc.insets = new Insets(5, 0, 5, 0);
        formPanel.add(batchLabel, gbc);

        JTextField batchTextField = createStyledTextField();
        gbc.gridy = 7;
        gbc.insets = new Insets(0, 0, 24, 0);
        formPanel.add(batchTextField, gbc);

        JButton registerButton = new JButton("Register Account");
        registerButton.setFont(new Font("SansSerif", Font.BOLD, 26));
        registerButton.setBackground(new Color(16, 185, 129));
        registerButton.setForeground(Color.WHITE);
        registerButton.setPreferredSize(new Dimension(600, 65)); // 600px width × 65px height
        registerButton.setFocusPainted(false);
        registerButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        registerButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

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

        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String idStr = idTextField.getText().trim();
                String nameInput = nameTextField.getText().trim();
                String passInput = new String(passwordField.getPassword()).trim();
                String batchStr = batchTextField.getText().trim();

                if (idStr.isEmpty() || nameInput.isEmpty() || passInput.isEmpty() || batchStr.isEmpty()) {
                    statusLabel.setText("Please fill out all fields.");
                    statusLabel.setForeground(new Color(239, 68, 68));
                    return;
                }

                try {
                    int idInput = Integer.parseInt(idStr);
                    int batchInput = Integer.parseInt(batchStr);

                    String sql = "INSERT INTO users (id, name, role, password, batch) VALUES (?, ?, ?, ?, ?)";

                    try (Connection con = DBConnection.getConnection();
                         PreparedStatement pstmt = con.prepareStatement(sql)) {

                        pstmt.setInt(1, idInput);
                        pstmt.setString(2, nameInput);
                        pstmt.setString(3, "student");
                        pstmt.setString(4, passInput);
                        pstmt.setInt(5, batchInput);

                        int rowsInserted = pstmt.executeUpdate();

                        if (rowsInserted > 0) {
                            statusLabel.setText("Registration Successful! Opening Login...");
                            statusLabel.setForeground(new Color(16, 185, 129));

                            Timer timer = new Timer(1000, new ActionListener() {
                                @Override
                                public void actionPerformed(ActionEvent evt) {
                                    new Loginpage();
                                    dispose();
                                }
                            });
                            timer.setRepeats(false);
                            timer.start();
                        }

                    } catch (SQLException ex) {
                        if (ex.getErrorCode() == 1062) {
                            statusLabel.setText("Error: User with Exam Roll " + idInput + " already exists!");
                        } else {
                            statusLabel.setText("Database Error: " + ex.getMessage());
                        }
                        statusLabel.setForeground(new Color(239, 68, 68));
                    }

                } catch (NumberFormatException ex) {
                    statusLabel.setText("Error: Exam Roll and Batch must be valid numbers.");
                    statusLabel.setForeground(new Color(239, 68, 68));
                }
            }
        });

        gbc.gridy = 8;
        gbc.insets = new Insets(10, 0, 5, 0);
        formPanel.add(registerButton, gbc);

        cardPanel.add(titleLabel);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        cardPanel.add(subtitleLabel);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 30)));
        cardPanel.add(formPanel);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 18)));
        cardPanel.add(statusLabel);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 18)));
        cardPanel.add(backButton);

        mainPanel.add(cardPanel);
        setVisible(true);
    }

    private JLabel createFormLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 20)); // Increased label size
        label.setForeground(new Color(226, 232, 240));
        return label;
    }

    private JTextField createStyledTextField() {
        JTextField textField = new JTextField();
        textField.setFont(new Font("SansSerif", Font.PLAIN, 22)); // Increased text font
        textField.setBackground(new Color(15, 23, 42));
        textField.setForeground(Color.WHITE);
        textField.setCaretColor(Color.WHITE);
        textField.setPreferredSize(new Dimension(600, 58)); // 600px width × 58px height
        textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(71, 85, 105), 1),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));
        return textField;
    }

    private JPasswordField createStyledPasswordField() {
        JPasswordField passwordField = new JPasswordField();
        passwordField.setFont(new Font("SansSerif", Font.PLAIN, 22)); // Increased text font
        passwordField.setBackground(new Color(15, 23, 42));
        passwordField.setForeground(Color.WHITE);
        passwordField.setCaretColor(Color.WHITE);
        passwordField.setPreferredSize(new Dimension(600, 58)); // 600px width × 58px height
        passwordField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(71, 85, 105), 1),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));
        return passwordField;
    }
}