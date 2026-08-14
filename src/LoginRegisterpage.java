import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LoginRegisterpage extends JFrame {

    public LoginRegisterpage() {

        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Welcome to Academia");

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(new Color(15, 23, 42));
        add(mainPanel);

        JPanel cardPanel = new JPanel();
        cardPanel.setLayout(new BoxLayout(cardPanel, BoxLayout.Y_AXIS));
        cardPanel.setBackground(new Color(30, 41, 59));

        cardPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(51, 65, 85), 2),
                new EmptyBorder(80, 110, 80, 110)
        ));

        JLabel titleLabel = new JLabel("Welcome to ACADEMIA");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 68));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Welcome! Please select an option to continue.");
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 22));
        subtitleLabel.setForeground(new Color(148, 163, 184)); // Soft gray text
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 35, 0));
        buttonPanel.setOpaque(false);

        JButton loginButton = createStyledButton("Login", new Color(59, 130, 246), Color.WHITE);
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Loginpage();
                dispose();
            }
        });

        JButton registerButton = createStyledButton("Register", new Color(16, 185, 129), Color.WHITE);
        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Registerpage();
                dispose();
            }
        });

        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);

        cardPanel.add(titleLabel);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        cardPanel.add(subtitleLabel);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 50)));
        cardPanel.add(buttonPanel);

        mainPanel.add(cardPanel);
        setVisible(true);
    }

    private JButton createStyledButton(String text, Color bg, Color fg) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 24));
        button.setBackground(bg);
        button.setForeground(fg);
        button.setPreferredSize(new Dimension(220, 65));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        return button;
    }
}