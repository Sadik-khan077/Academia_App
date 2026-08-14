import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Dashboard extends JFrame {

    private int userId;
    private String userName = "User";
    private String userRole = "Student";
    private String currentSemester = "N/A";

    private CardLayout cardLayout;
    private JPanel mainContentArea;
    private JLabel selectedCourseHeader;
    private JPanel detailContentPanel;
    private JPanel assignCardPanel;
    private JPanel statCardsPanel;

    private Course currentOpenCourse;

    public static class Course {
        int id;
        String code;
        String title;
        double credit;
        String semester;

        public Course(int id, String code, String title, double credit, String semester) {
            this.id = id;
            this.code = code;
            this.title = title;
            this.credit = credit;
            this.semester = semester;
        }
    }

    public static class CTSummary {
        String title;
        String date;
        double totalMarks;
        int studentCount;

        public CTSummary(String title, String date, double totalMarks, int studentCount) {
            this.title = title;
            this.date = date;
            this.totalMarks = totalMarks;
            this.studentCount = studentCount;
        }
    }

    public static class StudentCTMark {
        String title;
        String date;
        double obtainedMarks;
        double totalMarks;

        public StudentCTMark(String title, String date, double obtainedMarks, double totalMarks) {
            this.title = title;
            this.date = date;
            this.obtainedMarks = obtainedMarks;
            this.totalMarks = totalMarks;
        }
    }

    public static class CTMarkDetail {
        int userId;
        String examRoll;
        String studentName;
        double obtainedMarks;
        double totalMarks;

        public CTMarkDetail(int userId, String examRoll, String studentName, double obtainedMarks, double totalMarks) {
            this.userId = userId;
            this.examRoll = (examRoll != null && !examRoll.isEmpty()) ? examRoll : String.valueOf(userId);
            this.studentName = (studentName != null) ? studentName : "N/A";
            this.obtainedMarks = obtainedMarks;
            this.totalMarks = totalMarks;
        }
    }

    public static class EnrolledStudent {
        int id;
        String examRoll;
        String name;

        public EnrolledStudent(int id, String examRoll, String name) {
            this.id = id;
            this.examRoll = (examRoll != null && !examRoll.isEmpty()) ? examRoll : String.valueOf(id);
            this.name = (name != null) ? name : "N/A";
        }
    }

    public static class Material {
        String filePath;

        public Material(String filePath) {
            this.filePath = filePath;
        }

        public String getFileName() {
            if (filePath == null || filePath.trim().isEmpty()) return "Untitled File";
            return new File(filePath).getName();
        }
    }

    public Dashboard(int userId) {
        this.userId = userId;

        fetchUserData();

        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Academia - " + userRole + " Dashboard");

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(15, 23, 42));

        JPanel sidebar = createSidebar();
        mainPanel.add(sidebar, BorderLayout.WEST);

        JPanel contentPanel = new JPanel(new BorderLayout(0, 35));
        contentPanel.setBackground(new Color(15, 23, 42));
        contentPanel.setBorder(new EmptyBorder(40, 45, 40, 45));

        JPanel headerPanel = createHeaderPanel();
        contentPanel.add(headerPanel, BorderLayout.NORTH);

        cardLayout = new CardLayout();
        mainContentArea = new JPanel(cardLayout);
        mainContentArea.setOpaque(false);

        statCardsPanel = createStatCardsPanel();
        mainContentArea.add(statCardsPanel, "DASHBOARD_CARDS");

        JPanel courseDetailPanel = createCourseDetailViewPanel();
        mainContentArea.add(courseDetailPanel, "COURSE_DETAIL");

        contentPanel.add(mainContentArea, BorderLayout.CENTER);
        mainPanel.add(contentPanel, BorderLayout.CENTER);

        add(mainPanel);
        setVisible(true);
    }

    private void fetchUserData() {
        String sql = "SELECT name, role FROM users WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    this.userName = rs.getString("name");
                    this.userRole = rs.getString("role");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private List<Course> fetchEnrolledCourses() {
        List<Course> courses = new ArrayList<>();
        String sql = "SELECT cd.id, cd.course_code, cd.course_title, cd.credit, cd.semester " +
                "FROM users u " +
                "JOIN batch_semester bs ON TRIM(u.batch) = TRIM(bs.Batch) " +
                "JOIN course_details cd ON TRIM(bs.semester) = TRIM(cd.semester) " +
                "WHERE u.id = ? AND cd.course_title NOT LIKE '%Viva-Voce%'";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String sem = rs.getString("semester");
                    this.currentSemester = sem;
                    courses.add(new Course(
                            rs.getInt("id"),
                            rs.getString("course_code"),
                            rs.getString("course_title"),
                            rs.getDouble("credit"),
                            sem
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return courses;
    }

    private List<Course> fetchTeacherCourses() {
        List<Course> courses = new ArrayList<>();
        String sql = "SELECT cd.id, cd.course_code, cd.course_title, cd.credit, cd.semester " +
                "FROM course_instructor ci " +
                "JOIN course_details cd ON ci.course_code = cd.course_code " +
                "WHERE ci.teacher = ? AND cd.course_title NOT LIKE '%Viva-Voce%'";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {

            pstmt.setString(1, userName);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    courses.add(new Course(
                            rs.getInt("id"),
                            rs.getString("course_code"),
                            rs.getString("course_title"),
                            rs.getDouble("credit"),
                            rs.getString("semester")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return courses;
    }

    private List<CTSummary> fetchUniqueCTsForCourse(String courseCode) {
        List<CTSummary> ctList = new ArrayList<>();
        String sql = "SELECT title, test_date, total_marks, COUNT(DISTINCT user_id) as student_count " +
                "FROM ct_marks WHERE TRIM(course_code) = TRIM(?) " +
                "GROUP BY title, test_date, total_marks ORDER BY test_date DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {

            pstmt.setString(1, courseCode);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ctList.add(new CTSummary(
                            rs.getString("title"),
                            rs.getString("test_date"),
                            rs.getDouble("total_marks"),
                            rs.getInt("student_count")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ctList;
    }

    private List<StudentCTMark> fetchCTMarksForStudent(String courseCode) {
        List<StudentCTMark> list = new ArrayList<>();
        String sql = "SELECT title, test_date, obtained_marks, total_marks " +
                "FROM ct_marks WHERE course_code = ? AND user_id = ? " +
                "ORDER BY test_date DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {

            pstmt.setString(1, courseCode);
            pstmt.setInt(2, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new StudentCTMark(
                            rs.getString("title"),
                            rs.getString("test_date"),
                            rs.getDouble("obtained_marks"),
                            rs.getDouble("total_marks")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private List<CTMarkDetail> fetchAllMarksForCT(String courseCode, String ctTitle) {
        List<CTMarkDetail> markDetails = new ArrayList<>();

        String sql = "SELECT cm.user_id, CAST(u.id AS CHAR) AS exam_roll, u.name, cm.obtained_marks, cm.total_marks " +
                "FROM ct_marks cm " +
                "LEFT JOIN users u ON cm.user_id = u.id " +
                "WHERE TRIM(cm.course_code) = TRIM(?) AND LOWER(TRIM(cm.title)) = LOWER(TRIM(?)) " +
                "ORDER BY u.id ASC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {

            pstmt.setString(1, courseCode);
            pstmt.setString(2, ctTitle);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    markDetails.add(new CTMarkDetail(
                            rs.getInt("user_id"),
                            rs.getString("exam_roll"),
                            rs.getString("name"),
                            rs.getDouble("obtained_marks"),
                            rs.getDouble("total_marks")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return markDetails;
    }

    private List<EnrolledStudent> fetchEnrolledStudentsForCourse(String courseCode) {
        List<EnrolledStudent> students = new ArrayList<>();

        String sql = "SELECT DISTINCT u.id, CAST(u.id AS CHAR) AS exam_roll, u.name " +
                "FROM users u " +
                "JOIN batch_semester bs ON u.batch = bs.Batch " +
                "JOIN course_details cd ON bs.semester = cd.semester " +
                "WHERE cd.course_code = ? AND LOWER(TRIM(u.role)) = 'student' " +
                "ORDER BY u.id ASC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {

            pstmt.setString(1, courseCode);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    students.add(new EnrolledStudent(
                            rs.getInt("id"),
                            rs.getString("exam_roll"),
                            rs.getString("name")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return students;
    }

    private List<Material> fetchMaterialsForCourse(String courseCode) {
        List<Material> materials = new ArrayList<>();
        String sql = "SELECT file_path FROM course_materials WHERE course_code = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {

            pstmt.setString(1, courseCode);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    materials.add(new Material(
                            rs.getString("file_path")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return materials;
    }

    public void refreshDashboard() {
        if (statCardsPanel != null) {
            mainContentArea.remove(statCardsPanel);
        }
        statCardsPanel = createStatCardsPanel();
        mainContentArea.add(statCardsPanel, "DASHBOARD_CARDS");
        cardLayout.show(mainContentArea, "DASHBOARD_CARDS");
        mainContentArea.revalidate();
        mainContentArea.repaint();
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(new Color(30, 41, 59));
        sidebar.setPreferredSize(new Dimension(500, 0));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(51, 65, 85)));

        JLabel logo = new JLabel("Academia");
        logo.setFont(new Font("SansSerif", Font.BOLD, 60));
        logo.setForeground(Color.WHITE);
        logo.setBorder(new EmptyBorder(50, 45, 40, 45));
        sidebar.add(logo, BorderLayout.NORTH);

        JPanel navPanel = new JPanel();
        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.Y_AXIS));
        navPanel.setOpaque(false);
        navPanel.setBorder(new EmptyBorder(10, 35, 10, 35));

        JButton dashBtn = createNavButton("📊  Dashboard", true);
        dashBtn.addActionListener(e -> cardLayout.show(mainContentArea, "DASHBOARD_CARDS"));

        navPanel.add(dashBtn);
        navPanel.add(Box.createRigidArea(new Dimension(0, 22)));

        if ("Teacher".equalsIgnoreCase(userRole)) {
            navPanel.add(createNavButton("📚  My Courses", false));
            navPanel.add(Box.createRigidArea(new Dimension(0, 22)));
            JButton assignBtn = createNavButton("📝  Assign Assignment", false);
            assignBtn.addActionListener(e -> showCreateAssignmentDialog());
            navPanel.add(assignBtn);
            navPanel.add(Box.createRigidArea(new Dimension(0, 22)));
            JButton evaluateBtn = createNavButton("✅  Evaluate Assignments", false);
            evaluateBtn.addActionListener(e -> showEvaluateAssignmentsDialog());
            navPanel.add(evaluateBtn);
        } else {
            navPanel.add(createNavButton("📚  Courses", false));
            navPanel.add(Box.createRigidArea(new Dimension(0, 22)));
            JButton studentAssignBtn = createNavButton("📝  Assignments", false);
            studentAssignBtn.addActionListener(e -> showStudentAssignmentsDialog());
            navPanel.add(studentAssignBtn);
            navPanel.add(Box.createRigidArea(new Dimension(0, 22)));
            JButton gradesBtn = createNavButton("📈  Results & Grades", false);
            navPanel.add(gradesBtn);
        }

        navPanel.add(Box.createRigidArea(new Dimension(0, 22)));
        navPanel.add(createNavButton("🔔  Notices", false));
        navPanel.add(Box.createRigidArea(new Dimension(0, 22)));
        navPanel.add(createNavButton("⚙️  Settings", false));

        sidebar.add(navPanel, BorderLayout.CENTER);

        JPanel logoutPanel = new JPanel(new BorderLayout());
        logoutPanel.setOpaque(false);
        logoutPanel.setBorder(new EmptyBorder(20, 35, 50, 35));

        JButton logoutBtn = new JButton("🚪  Logout");
        logoutBtn.setFont(new Font("SansSerif", Font.BOLD, 34));
        logoutBtn.setForeground(new Color(239, 68, 68));
        logoutBtn.setContentAreaFilled(false);
        logoutBtn.setBorderPainted(false);
        logoutBtn.setFocusPainted(false);
        logoutBtn.setHorizontalAlignment(SwingConstants.LEFT);

        logoutBtn.addActionListener(e -> {
            new LoginRegisterpage();
            dispose();
        });

        logoutPanel.add(logoutBtn, BorderLayout.CENTER);
        sidebar.add(logoutPanel, BorderLayout.SOUTH);

        return sidebar;
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(30, 41, 59));
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(51, 65, 85), 1),
                new EmptyBorder(40, 50, 40, 50)
        ));

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);

        String dashboardTitle = "Teacher".equalsIgnoreCase(userRole) ? "Teacher Dashboard" : "Student Dashboard";
        JLabel title = new JLabel(dashboardTitle);
        title.setFont(new Font("SansSerif", Font.BOLD, 54));
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("Welcome back " + userName + "! Here is your daily overview.");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 26));
        subtitle.setForeground(new Color(148, 163, 184));

        titlePanel.add(title);
        titlePanel.add(Box.createRigidArea(new Dimension(0, 12)));
        titlePanel.add(subtitle);

        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 30, 0));
        rightControls.setOpaque(false);

        JLabel avatar = new JLabel(String.valueOf(userName.charAt(0)).toUpperCase(), SwingConstants.CENTER);
        avatar.setFont(new Font("SansSerif", Font.BOLD, 42));
        avatar.setForeground(Color.WHITE);
        avatar.setOpaque(true);
        avatar.setBackground(new Color(59, 130, 246));
        avatar.setPreferredSize(new Dimension(85, 85));

        JPanel userText = new JPanel();
        userText.setLayout(new BoxLayout(userText, BoxLayout.Y_AXIS));
        userText.setOpaque(false);

        JLabel nameLabel = new JLabel(userName);
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 30));
        nameLabel.setForeground(Color.WHITE);

        JLabel roleLabel = new JLabel(userRole);
        roleLabel.setFont(new Font("SansSerif", Font.PLAIN, 24));
        roleLabel.setForeground(new Color(148, 163, 184));

        userText.add(nameLabel);
        userText.add(Box.createRigidArea(new Dimension(0, 6)));
        userText.add(roleLabel);

        rightControls.add(avatar);
        rightControls.add(userText);

        header.add(titlePanel, BorderLayout.WEST);
        header.add(rightControls, BorderLayout.EAST);

        return header;
    }

    private JPanel createStatCardsPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 40, 0));
        panel.setOpaque(false);

        if ("Teacher".equalsIgnoreCase(userRole)) {
            panel.add(createTeacherCoursesCard());
            panel.add(createTeacherActiveAssignmentsCard());
            panel.add(createTeacherEvaluateCard());
        } else {
            panel.add(createEnrolledCoursesCard());
            panel.add(createStudentAssignmentsCard());
            panel.add(createStudentGradesCard());
        }

        return panel;
    }

    private JPanel createTeacherActiveAssignmentsCard() {
        JPanel card = new JPanel(new BorderLayout(0, 20));
        card.setBackground(new Color(30, 41, 59));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(51, 65, 85), 1),
                new EmptyBorder(35, 35, 30, 35)
        ));

        String sql = "SELECT DISTINCT a.id, a.course_code, a.title, a.description, a.due_date, a.total_marks " +
                "FROM assignments a " +
                "JOIN course_instructor ci ON TRIM(a.course_code) = TRIM(ci.course_code) " +
                "WHERE ci.teacher = ? AND a.due_date >= CURDATE() ORDER BY a.due_date ASC";

        List<Object[]> assignmentRows = new ArrayList<>();
        int activeCount = 0;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, userName);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    activeCount++;
                    assignmentRows.add(new Object[]{
                            rs.getInt("id"),
                            rs.getString("course_code"),
                            rs.getString("title"),
                            rs.getString("description"),
                            rs.getString("due_date"),
                            rs.getDouble("total_marks")
                    });
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        JPanel topHeader = new JPanel(new BorderLayout());
        topHeader.setOpaque(false);

        JLabel titleLabel = new JLabel("Assign Assignment");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 38));
        titleLabel.setForeground(new Color(226, 232, 240));

        JLabel badge = new JLabel(" Active ");
        badge.setFont(new Font("SansSerif", Font.BOLD, 22));
        badge.setForeground(new Color(59, 130, 246));
        badge.setBackground(new Color(15, 23, 42));
        badge.setOpaque(true);
        badge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(59, 130, 246), 1),
                new EmptyBorder(8, 16, 8, 16)
        ));

        topHeader.add(titleLabel, BorderLayout.WEST);
        topHeader.add(badge, BorderLayout.EAST);

        JPanel statRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 0));
        statRow.setOpaque(false);

        JLabel countVal = new JLabel(String.valueOf(activeCount));
        countVal.setFont(new Font("SansSerif", Font.BOLD, 110));
        countVal.setForeground(Color.WHITE);

        JLabel unitLabel = new JLabel("Assigned");
        unitLabel.setFont(new Font("SansSerif", Font.PLAIN, 36));
        unitLabel.setForeground(new Color(148, 163, 184));

        statRow.add(countVal);
        statRow.add(unitLabel);

        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setOpaque(false);
        topContainer.add(topHeader);
        topContainer.add(Box.createRigidArea(new Dimension(0, 12)));
        topContainer.add(statRow);

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setOpaque(false);

        if (assignmentRows.isEmpty()) {
            JLabel emptyLabel = new JLabel("No active assignments found.");
            emptyLabel.setFont(new Font("SansSerif", Font.PLAIN, 24));
            emptyLabel.setForeground(new Color(148, 163, 184));
            listPanel.add(emptyLabel);
        } else {
            for (Object[] rowData : assignmentRows) {
                int assignId = (int) rowData[0];
                String courseCode = (String) rowData[1];
                String title = (String) rowData[2];
                String desc = (String) rowData[3];
                String dueDate = (String) rowData[4];
                double totalMarks = (double) rowData[5];

                JPanel item = new JPanel(new BorderLayout());
                item.setBackground(new Color(15, 23, 42));
                item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
                item.setPreferredSize(new Dimension(0, 90));
                item.setCursor(new Cursor(Cursor.HAND_CURSOR));
                item.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 6, 0, 0, new Color(59, 130, 246)),
                        BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(new Color(51, 65, 85), 1),
                                new EmptyBorder(12, 18, 12, 18)
                        )
                ));

                JLabel tLbl = new JLabel(courseCode + ": " + title);
                tLbl.setFont(new Font("SansSerif", Font.BOLD, 22));
                tLbl.setForeground(Color.WHITE);

                JLabel dLbl = new JLabel("Due: " + dueDate + " | Marks: " + (int) totalMarks + " (Click to edit)");
                dLbl.setFont(new Font("SansSerif", Font.PLAIN, 18));
                dLbl.setForeground(new Color(148, 163, 184));

                JPanel textPanel = new JPanel();
                textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
                textPanel.setOpaque(false);
                textPanel.add(tLbl);
                textPanel.add(Box.createRigidArea(new Dimension(0, 4)));
                textPanel.add(dLbl);

                item.add(textPanel, BorderLayout.CENTER);

                // Click listener to edit assignment
                item.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseClicked(java.awt.event.MouseEvent e) {
                        showEditAssignmentDialog(assignId, courseCode, title, desc, dueDate, totalMarks);
                    }
                });

                listPanel.add(item);
                listPanel.add(Box.createRigidArea(new Dimension(0, 15)));
            }
        }

        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(25);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));

        JButton footerBtn = new JButton("Assign a new Assignment →");
        footerBtn.setFont(new Font("SansSerif", Font.BOLD, 24));
        footerBtn.setForeground(new Color(59, 130, 246));
        footerBtn.setContentAreaFilled(false);
        footerBtn.setBorderPainted(false);
        footerBtn.setFocusPainted(false);
        footerBtn.setHorizontalAlignment(SwingConstants.LEFT);
        footerBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        footerBtn.addActionListener(e -> showCreateAssignmentDialog());

        card.add(topContainer, BorderLayout.NORTH);
        card.add(scrollPane, BorderLayout.CENTER);
        card.add(footerBtn, BorderLayout.SOUTH);

        return card;
    }

    private void showEditAssignmentDialog(int assignId, String courseCode, String currentTitle, String currentDesc, String currentDueDate, double currentMarks) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Edit Assignment", true);
        dialog.setSize(1200, 850);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridLayout(6, 2, 15, 15));
        panel.setBorder(new EmptyBorder(35, 35, 35, 35));
        panel.setBackground(new Color(30, 41, 59));

        // Increased font sizes for labels and content
        Font labelFont = new Font("Segoe UI", Font.BOLD, 20); // Made labels larger
        Font inputFont = new Font("Segoe UI", Font.PLAIN, 18);
        Font buttonFont = new Font("Segoe UI", Font.BOLD, 18);

        JTextField titleField = new JTextField(currentTitle);
        titleField.setFont(inputFont);

        JTextField descField = new JTextField(currentDesc);
        descField.setFont(inputFont);

        JTextField dateField = new JTextField(currentDueDate);
        dateField.setFont(inputFont);

        JTextField marksField = new JTextField(String.valueOf((int) currentMarks));
        marksField.setFont(inputFont);

        JLabel lblTitle = new JLabel("Title:");
        lblTitle.setFont(labelFont);
        lblTitle.setForeground(Color.WHITE);

        JLabel lblDesc = new JLabel("Description:");
        lblDesc.setFont(labelFont);
        lblDesc.setForeground(Color.WHITE);

        JLabel lblDate = new JLabel("Due Date (YYYY-MM-DD):");
        lblDate.setFont(labelFont);
        lblDate.setForeground(Color.WHITE);

        JLabel lblMarks = new JLabel("Total Marks:");
        lblMarks.setFont(labelFont);
        lblMarks.setForeground(Color.WHITE);

        panel.add(lblTitle); panel.add(titleField);
        panel.add(lblDesc); panel.add(descField);
        panel.add(lblDate); panel.add(dateField);
        panel.add(lblMarks); panel.add(marksField);

        JButton saveBtn = new JButton("Update Assignment");
        saveBtn.setFont(buttonFont);
        saveBtn.addActionListener(e -> {
            String sql = "UPDATE assignments SET title = ?, description = ?, due_date = ?, total_marks = ? WHERE id = ?";
            try (Connection con = DBConnection.getConnection();
                 PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setString(1, titleField.getText().trim());
                pstmt.setString(2, descField.getText().trim());
                pstmt.setString(3, dateField.getText().trim());
                pstmt.setDouble(4, Double.parseDouble(marksField.getText().trim()));
                pstmt.setInt(5, assignId);
                pstmt.executeUpdate();
                JOptionPane.showMessageDialog(dialog, "Assignment updated successfully!");
                dialog.dispose();
                refreshDashboard();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Error updating assignment: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        panel.add(new JLabel());
        panel.add(saveBtn);
        dialog.add(panel);
        dialog.setVisible(true);
    }

    private JPanel createTeacherEvaluateCard() {
        JPanel card = new JPanel(new BorderLayout(0, 20));
        card.setBackground(new Color(30, 41, 59));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(51, 65, 85), 1),
                new EmptyBorder(35, 35, 30, 35)
        ));

        String sql = "SELECT DISTINCT a.id, a.course_code, a.title, a.due_date " +
                "FROM assignments a " +
                "JOIN course_instructor ci ON TRIM(a.course_code) = TRIM(ci.course_code) " +
                "WHERE ci.teacher = ? AND a.due_date < CURDATE() ORDER BY a.due_date DESC";

        List<Object[]> pastDueRows = new ArrayList<>();
        int pastDueCount = 0;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, userName);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    pastDueCount++;
                    pastDueRows.add(new Object[]{
                            rs.getInt("id"),
                            rs.getString("course_code"),
                            rs.getString("title"),
                            rs.getString("due_date")
                    });
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        JPanel topHeader = new JPanel(new BorderLayout());
        topHeader.setOpaque(false);

        JLabel titleLabel = new JLabel("Evaluate Submissions");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 38));
        titleLabel.setForeground(new Color(226, 232, 240));

        JLabel badge = new JLabel(" Action Needed ");
        badge.setFont(new Font("SansSerif", Font.BOLD, 22));
        badge.setForeground(new Color(59, 130, 246));
        badge.setBackground(new Color(15, 23, 42));
        badge.setOpaque(true);
        badge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(59, 130, 246), 1),
                new EmptyBorder(8, 16, 8, 16)
        ));

        topHeader.add(titleLabel, BorderLayout.WEST);
        topHeader.add(badge, BorderLayout.EAST);

        JPanel statRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 0));
        statRow.setOpaque(false);

        JLabel countVal = new JLabel(String.valueOf(pastDueCount));
        countVal.setFont(new Font("SansSerif", Font.BOLD, 110));
        countVal.setForeground(Color.WHITE);

        JLabel unitLabel = new JLabel("Past Due");
        unitLabel.setFont(new Font("SansSerif", Font.PLAIN, 36));
        unitLabel.setForeground(new Color(148, 163, 184));

        statRow.add(countVal);
        statRow.add(unitLabel);

        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setOpaque(false);
        topContainer.add(topHeader);
        topContainer.add(Box.createRigidArea(new Dimension(0, 12)));
        topContainer.add(statRow);

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setOpaque(false);

        if (pastDueRows.isEmpty()) {
            JLabel emptyLabel = new JLabel("No past-due assignments to evaluate.");
            emptyLabel.setFont(new Font("SansSerif", Font.PLAIN, 24));
            emptyLabel.setForeground(new Color(148, 163, 184));
            listPanel.add(emptyLabel);
        } else {
            for (Object[] rowData : pastDueRows) {
                int assignId = (int) rowData[0];
                String courseCode = (String) rowData[1];
                String title = (String) rowData[2];
                String dueDate = (String) rowData[3];

                JPanel item = new JPanel(new BorderLayout());
                item.setBackground(new Color(15, 23, 42));
                item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
                item.setPreferredSize(new Dimension(0, 90));
                item.setCursor(new Cursor(Cursor.HAND_CURSOR));
                item.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 6, 0, 0, new Color(239, 68, 68)),
                        BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(new Color(51, 65, 85), 1),
                                new EmptyBorder(12, 18, 12, 18)
                        )
                ));

                JLabel tLbl = new JLabel(courseCode + ": " + title);
                tLbl.setFont(new Font("SansSerif", Font.BOLD, 22));
                tLbl.setForeground(Color.WHITE);

                JLabel dLbl = new JLabel("Due Date Crossed: " + dueDate + " (Click to evaluate)");
                dLbl.setFont(new Font("SansSerif", Font.PLAIN, 18));
                dLbl.setForeground(new Color(148, 163, 184));

                JPanel textPanel = new JPanel();
                textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
                textPanel.setOpaque(false);
                textPanel.add(tLbl);
                textPanel.add(Box.createRigidArea(new Dimension(0, 4)));
                textPanel.add(dLbl);

                item.add(textPanel, BorderLayout.CENTER);

                // Click listener to open past-due student roster & submissions evaluation window
                item.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseClicked(java.awt.event.MouseEvent e) {
                        showPastDueEvaluationDialog(assignId, courseCode, title);
                    }
                });

                listPanel.add(item);
                listPanel.add(Box.createRigidArea(new Dimension(0, 15)));
            }
        }

        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(25);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));

        JButton footerBtn = new JButton("Review student submissions →");
        footerBtn.setFont(new Font("SansSerif", Font.BOLD, 24));
        footerBtn.setForeground(new Color(59, 130, 246));
        footerBtn.setContentAreaFilled(false);
        footerBtn.setBorderPainted(false);
        footerBtn.setFocusPainted(false);
        footerBtn.setHorizontalAlignment(SwingConstants.LEFT);
        footerBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        footerBtn.addActionListener(e -> showEvaluateAssignmentsDialog());

        card.add(topContainer, BorderLayout.NORTH);
        card.add(scrollPane, BorderLayout.CENTER);
        card.add(footerBtn, BorderLayout.SOUTH);

        return card;
    }

    private void showPastDueEvaluationDialog(int assignId, String courseCode, String assignmentTitle) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Evaluate Submissions - " + assignmentTitle, true);
        dialog.setSize(1000, 600);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        // Table columns: Student ID, Student Name, Submission Status, File Path, Obtained Marks
        String[] columnNames = {"Student ID", "Student Name", "Submission Status", "File Path", "Obtained Marks"};

        // Fetch enrolled students and their submission details / existing ct_marks
        String sql = "SELECT u.id AS student_id, u.name, sub.file_path, cm.marks " +
                "FROM users u " +
                "JOIN batch_semester bs ON TRIM(u.batch) = TRIM(bs.Batch) " +
                "JOIN course_details cd ON TRIM(bs.semester) = TRIM(cd.semester) " +
                "LEFT JOIN assignment_submissions sub ON sub.student_id = u.id AND sub.assignment_id = ? " +
                "LEFT JOIN ct_marks cm ON cm.user_id = u.id AND cm.obtained_marks = ? " +
                "WHERE TRIM(cd.course_code) = TRIM(?) AND u.role = 'Student'";

        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 4; // Only 'Obtained Marks' column is editable directly in table
            }
        };

        try (Connection con = DBConnection.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setInt(1, assignId);
            pstmt.setInt(2, assignId);
            pstmt.setString(3, courseCode);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int studentId = rs.getInt("student_id");
                    String studentName = rs.getString("name");
                    String filePath = rs.getString("file_path");
                    Object marks = rs.getObject("marks");

                    String status = (filePath != null && !filePath.isEmpty()) ? "Submitted" : "Not Submitted";
                    model.addRow(new Object[]{
                            studentId,
                            studentName,
                            status,
                            (filePath != null ? filePath : "No Submission"),
                            (marks != null ? marks : "")
                    });
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        JTable table = new JTable(model);
        table.setRowHeight(35);
        table.setFont(new Font("SansSerif", Font.PLAIN, 16));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 16));

        JScrollPane scrollPane = new JScrollPane(table);

        // Action button panel to open selected PDF and save marks
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));

        JButton openPdfBtn = new JButton("Open Selected PDF");
        openPdfBtn.setFont(new Font("SansSerif", Font.BOLD, 16));
        openPdfBtn.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(dialog, "Please select a student row first.");
                return;
            }
            String path = (String) model.getValueAt(selectedRow, 3);
            if (path == null || path.equals("No Submission")) {
                JOptionPane.showMessageDialog(dialog, "This student has not submitted a file.");
                return;
            }
            try {
                File pdfFile = new File(path);
                if (pdfFile.exists()) {
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().open(pdfFile);
                    }
                } else {
                    JOptionPane.showMessageDialog(dialog, "PDF file not found on disk: " + path);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Could not open file: " + ex.getMessage());
            }
        });

        JButton saveMarksBtn = new JButton("Save Updated Marks");
        saveMarksBtn.setFont(new Font("SansSerif", Font.BOLD, 16));
        saveMarksBtn.setBackground(new Color(59, 130, 246));
        saveMarksBtn.setForeground(Color.WHITE);
        saveMarksBtn.addActionListener(e -> {
            String upsertSql = "INSERT INTO ct_marks (assignment_id, student_id, course_code, marks) VALUES (?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE marks = ?";
            try (Connection con = DBConnection.getConnection();
                 PreparedStatement pstmt = con.prepareStatement(upsertSql)) {

                for (int i = 0; i < model.getRowCount(); i++) {
                    int studentId = (int) model.getValueAt(i, 0);
                    Object val = model.getValueAt(i, 4);
                    if (val != null && !val.toString().trim().isEmpty()) {
                        double marksVal = Double.parseDouble(val.toString().trim());
                        pstmt.setInt(1, assignId);
                        pstmt.setInt(2, studentId);
                        pstmt.setString(3, courseCode);
                        pstmt.setDouble(4, marksVal);
                        pstmt.setDouble(5, marksVal);
                        pstmt.executeUpdate();
                    }
                }
                JOptionPane.showMessageDialog(dialog, "Marks successfully saved to ct_marks!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Error saving marks: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        bottomPanel.add(openPdfBtn);
        bottomPanel.add(saveMarksBtn);

        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(bottomPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private JPanel createStudentAssignmentsCard() {
        JPanel card = new JPanel(new BorderLayout(0, 20));
        card.setBackground(new Color(30, 41, 59));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(51, 65, 85), 1),
                new EmptyBorder(35, 35, 30, 35)
        ));

        // --- FETCH DUE ASSIGNMENTS & COUNT ---
        String sql = "SELECT DISTINCT a.id, a.course_code, a.title, a.due_date, a.total_marks " +
                "FROM assignments a " +
                "JOIN course_details cd ON TRIM(a.course_code) = TRIM(cd.course_code) " +
                "JOIN batch_semester bs ON TRIM(cd.semester) = TRIM(bs.semester) " +
                "JOIN users u ON TRIM(u.batch) = TRIM(bs.Batch) " +
                "WHERE u.id = ? AND a.due_date >= CURDATE() ORDER BY a.due_date ASC";

        List<Object[]> assignmentRows = new ArrayList<>();
        int dueCount = 0;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    dueCount++;
                    assignmentRows.add(new Object[]{
                            rs.getInt("id"),
                            rs.getString("course_code"),
                            rs.getString("title"),
                            rs.getString("due_date"),
                            rs.getInt("total_marks")
                    });
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // --- TOP HEADER WITH LARGE COUNT ---
        JPanel topHeader = new JPanel(new BorderLayout());
        topHeader.setOpaque(false);

        JLabel titleLabel = new JLabel("Assignments");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 38));
        titleLabel.setForeground(new Color(226, 232, 240));

        JLabel badge = new JLabel(" Due List ");
        badge.setFont(new Font("SansSerif", Font.BOLD, 22));
        badge.setForeground(new Color(59, 130, 246));
        badge.setBackground(new Color(15, 23, 42));
        badge.setOpaque(true);
        badge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(59, 130, 246), 1),
                new EmptyBorder(8, 16, 8, 16)
        ));

        topHeader.add(titleLabel, BorderLayout.WEST);
        topHeader.add(badge, BorderLayout.EAST);

        // Big Counter Row (matching Courses card style)
        JPanel statRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 0));
        statRow.setOpaque(false);

        JLabel countVal = new JLabel(String.valueOf(dueCount));
        countVal.setFont(new Font("SansSerif", Font.BOLD, 110));
        countVal.setForeground(Color.WHITE);

        JLabel unitLabel = new JLabel("Due");
        unitLabel.setFont(new Font("SansSerif", Font.PLAIN, 36));
        unitLabel.setForeground(new Color(148, 163, 184));

        statRow.add(countVal);
        statRow.add(unitLabel);

        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setOpaque(false);
        topContainer.add(topHeader);
        topContainer.add(Box.createRigidArea(new Dimension(0, 12)));
        topContainer.add(statRow);

        // --- LIST PANEL ---
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setOpaque(false);

        for (Object[] rowData : assignmentRows) {
            final int assignId = (int) rowData[0];
            String courseCode = (String) rowData[1];
            String title = (String) rowData[2];
            String dueDate = (String) rowData[3];
            int totalMarks = (int) rowData[4];

            JPanel item = new JPanel(new BorderLayout());
            item.setBackground(new Color(15, 23, 42));
            item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
            item.setPreferredSize(new Dimension(0, 90));
            item.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(51, 65, 85), 1),
                    new EmptyBorder(12, 18, 12, 18)
            ));

            JPanel left = new JPanel();
            left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
            left.setOpaque(false);

            JLabel tLbl = new JLabel(courseCode + ": " + title);
            tLbl.setFont(new Font("SansSerif", Font.BOLD, 22));
            tLbl.setForeground(Color.WHITE);

            JLabel dLbl = new JLabel("Due: " + dueDate + " | Marks: " + totalMarks);
            dLbl.setFont(new Font("SansSerif", Font.PLAIN, 18));
            dLbl.setForeground(new Color(148, 163, 184));

            left.add(tLbl);
            left.add(Box.createRigidArea(new Dimension(0, 4)));
            left.add(dLbl);

            JButton submitBtn = new JButton("Submit");
            submitBtn.setFont(new Font("SansSerif", Font.BOLD, 18));
            submitBtn.setForeground(Color.WHITE);
            submitBtn.setBackground(new Color(59, 130, 246));
            submitBtn.setFocusPainted(false);
            submitBtn.setBorder(new EmptyBorder(6, 12, 6, 12));
            submitBtn.addActionListener(e -> showStudentSubmitAssignmentDialog(assignId, title, userId));

            item.add(left, BorderLayout.CENTER);
            item.add(submitBtn, BorderLayout.EAST);

            listPanel.add(item);
            listPanel.add(Box.createRigidArea(new Dimension(0, 12)));
        }

        if (dueCount == 0) {
            JLabel noDueLabel = new JLabel("No due assignments");
            noDueLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
            noDueLabel.setForeground(new Color(148, 163, 184));
            noDueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            listPanel.add(Box.createRigidArea(new Dimension(0, 40)));
            listPanel.add(noDueLabel);
        }

        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(25);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));

        card.add(topContainer, BorderLayout.NORTH);
        card.add(scrollPane, BorderLayout.CENTER);

        return card;
    }

    private JPanel createStudentGradesCard() {
        JPanel card = new JPanel(new BorderLayout(0, 20));
        card.setBackground(new Color(30, 41, 59));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(51, 65, 85), 1),
                new EmptyBorder(35, 35, 30, 35)
        ));

        String[] semesters = {"1-1", "1-2", "2-1", "2-2", "3-1", "3-2", "4-1", "4-2"};
        List<String[]> passedSemesters = new ArrayList<>();
        double totalWeightedGpa = 0.0;
        int totalSemestersCount = 0;

        String sql = "SELECT * FROM student_results WHERE user_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    for (String sem : semesters) {
                        double gpa = rs.getDouble(sem);
                        if (!rs.wasNull()) {
                            passedSemesters.add(new String[]{SemLabel(sem), String.format("%.2f", gpa)});
                            totalWeightedGpa += gpa;
                            totalSemestersCount++;

                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        double cgpa = totalSemestersCount > 0 ? (totalWeightedGpa / totalSemestersCount) : 0.0;

        JPanel topHeader = new JPanel(new BorderLayout());
        topHeader.setOpaque(false);

        JLabel titleLabel = new JLabel("Results & Grades");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 38));
        titleLabel.setForeground(new Color(226, 232, 240));

        JLabel badge = new JLabel(" Semesters Passed: " + String.format("%d", totalSemestersCount) + " ");
        badge.setFont(new Font("SansSerif", Font.BOLD, 22));
        badge.setForeground(new Color(59, 130, 246));
        badge.setBackground(new Color(15, 23, 42));
        badge.setOpaque(true);
        badge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(59, 130, 246), 1),
                new EmptyBorder(8, 16, 8, 16)
        ));

        topHeader.add(titleLabel, BorderLayout.WEST);
        topHeader.add(badge, BorderLayout.EAST);

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setOpaque(false);

        if (passedSemesters.isEmpty()) {
            JLabel emptyLabel = new JLabel("No semester passed yet.");
            emptyLabel.setFont(new Font("SansSerif", Font.PLAIN, 24));
            emptyLabel.setForeground(new Color(148, 163, 184));
            listPanel.add(emptyLabel);
        } else {
            for (String[] semData : passedSemesters) {
                JPanel item = new JPanel(new BorderLayout());
                item.setBackground(new Color(15, 23, 42));
                item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
                item.setPreferredSize(new Dimension(0, 90));
                item.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(51, 65, 85), 1),
                        new EmptyBorder(16, 22, 16, 22)
                ));

                JLabel semLbl = new JLabel(semData[0]);
                semLbl.setFont(new Font("SansSerif", Font.BOLD, 24));
                semLbl.setForeground(Color.WHITE);

                JLabel gpaLbl = new JLabel("GPA: " + semData[1]);
                gpaLbl.setFont(new Font("SansSerif", Font.BOLD, 24));
                gpaLbl.setForeground(new Color(59, 130, 246));

                item.add(semLbl, BorderLayout.WEST);
                item.add(gpaLbl, BorderLayout.EAST);

                listPanel.add(item);
                listPanel.add(Box.createRigidArea(new Dimension(0, 15)));
            }
        }

        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(25);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));

        JLabel wbadge = new JLabel("CGPA: " + String.format("%.2f", totalWeightedGpa/totalSemestersCount) + " ");
        wbadge.setFont(new Font("SansSerif", Font.BOLD, 42));
        wbadge.setForeground(new Color(59, 130, 246));
        wbadge.setBackground(new Color(15, 23, 42));
        wbadge.setOpaque(true);
        wbadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(59, 130, 246), 1),
                new EmptyBorder(40, 36, 40, 16)
        ));

        card.add(topHeader, BorderLayout.NORTH);
        card.add(scrollPane, BorderLayout.CENTER);
        card.add(wbadge, BorderLayout.SOUTH);

        return card;
    }

    private JPanel createTeacherCoursesCard() {
        JPanel card = new JPanel(new BorderLayout(0, 20));
        card.setBackground(new Color(30, 41, 59));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(51, 65, 85), 1),
                new EmptyBorder(35, 35, 30, 35)
        ));

        List<Course> courses = fetchTeacherCourses();

        JPanel topHeader = new JPanel(new BorderLayout());
        topHeader.setOpaque(false);

        JLabel titleLabel = new JLabel("My Courses");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 38));
        titleLabel.setForeground(new Color(226, 232, 240));

        JLabel badge = new JLabel(" Instructor ");
        badge.setFont(new Font("SansSerif", Font.BOLD, 22));
        badge.setForeground(new Color(59, 130, 246));
        badge.setBackground(new Color(15, 23, 42));
        badge.setOpaque(true);
        badge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(59, 130, 246), 1),
                new EmptyBorder(8, 16, 8, 16)
        ));

        topHeader.add(titleLabel, BorderLayout.WEST);
        topHeader.add(badge, BorderLayout.EAST);

        JPanel statRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 0));
        statRow.setOpaque(false);

        JLabel countVal = new JLabel(String.valueOf(courses.size()));
        countVal.setFont(new Font("SansSerif", Font.BOLD, 110));
        countVal.setForeground(Color.WHITE);

        JLabel unitLabel = new JLabel("Assigned");
        unitLabel.setFont(new Font("SansSerif", Font.PLAIN, 36));
        unitLabel.setForeground(new Color(148, 163, 184));

        statRow.add(countVal);
        statRow.add(unitLabel);

        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setOpaque(false);
        topContainer.add(topHeader);
        topContainer.add(Box.createRigidArea(new Dimension(0, 12)));
        topContainer.add(statRow);

        JPanel courseListPanel = new JPanel();
        courseListPanel.setLayout(new BoxLayout(courseListPanel, BoxLayout.Y_AXIS));
        courseListPanel.setOpaque(false);

        if (courses.isEmpty()) {
            JLabel emptyLabel = new JLabel("No assigned courses found.");
            emptyLabel.setFont(new Font("SansSerif", Font.PLAIN, 24));
            emptyLabel.setForeground(new Color(148, 163, 184));
            courseListPanel.add(emptyLabel);
        } else {
            for (Course course : courses) {
                JButton courseBtn = createCourseTileButton(course);
                courseListPanel.add(courseBtn);
                courseListPanel.add(Box.createRigidArea(new Dimension(0, 18)));
            }
        }

        JScrollPane scrollPane = new JScrollPane(courseListPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(25);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));

        JLabel footer = new JLabel("Click any course button to manage details →");
        footer.setFont(new Font("SansSerif", Font.PLAIN, 24));
        footer.setForeground(new Color(148, 163, 184));

        card.add(topContainer, BorderLayout.NORTH);
        card.add(scrollPane, BorderLayout.CENTER);
        card.add(footer, BorderLayout.SOUTH);

        return card;
    }

    private JPanel createEnrolledCoursesCard() {
        JPanel card = new JPanel(new BorderLayout(0, 20));
        card.setBackground(new Color(30, 41, 59));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(51, 65, 85), 1),
                new EmptyBorder(35, 35, 30, 35)
        ));

        List<Course> courses = fetchEnrolledCourses();

        JPanel topHeader = new JPanel(new BorderLayout());
        topHeader.setOpaque(false);

        JLabel titleLabel = new JLabel("Courses");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 38));
        titleLabel.setForeground(new Color(226, 232, 240));

        JLabel badge = new JLabel(" Semester " + currentSemester + " ");
        badge.setFont(new Font("SansSerif", Font.BOLD, 22));
        badge.setForeground(new Color(59, 130, 246));
        badge.setBackground(new Color(15, 23, 42));
        badge.setOpaque(true);
        badge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(59, 130, 246), 1),
                new EmptyBorder(8, 16, 8, 16)
        ));

        topHeader.add(titleLabel, BorderLayout.WEST);
        topHeader.add(badge, BorderLayout.EAST);

        JPanel statRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 0));
        statRow.setOpaque(false);

        JLabel countVal = new JLabel(String.valueOf(courses.size()));
        countVal.setFont(new Font("SansSerif", Font.BOLD, 110));
        countVal.setForeground(Color.WHITE);

        JLabel unitLabel = new JLabel("Enrolled");
        unitLabel.setFont(new Font("SansSerif", Font.PLAIN, 36));
        unitLabel.setForeground(new Color(148, 163, 184));

        statRow.add(countVal);
        statRow.add(unitLabel);

        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setOpaque(false);
        topContainer.add(topHeader);
        topContainer.add(Box.createRigidArea(new Dimension(0, 12)));
        topContainer.add(statRow);

        JPanel courseListPanel = new JPanel();
        courseListPanel.setLayout(new BoxLayout(courseListPanel, BoxLayout.Y_AXIS));
        courseListPanel.setOpaque(false);

        if (courses.isEmpty()) {
            JLabel emptyLabel = new JLabel("No enrolled courses found.");
            emptyLabel.setFont(new Font("SansSerif", Font.PLAIN, 24));
            emptyLabel.setForeground(new Color(148, 163, 184));
            courseListPanel.add(emptyLabel);
        } else {
            for (Course course : courses) {
                JButton courseBtn = createCourseTileButton(course);
                courseListPanel.add(courseBtn);
                courseListPanel.add(Box.createRigidArea(new Dimension(0, 18)));
            }
        }

        JScrollPane scrollPane = new JScrollPane(courseListPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(25);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));

        JLabel footer = new JLabel("Click any course button to view details →");
        footer.setFont(new Font("SansSerif", Font.PLAIN, 24));
        footer.setForeground(new Color(148, 163, 184));

        card.add(topContainer, BorderLayout.NORTH);
        card.add(scrollPane, BorderLayout.CENTER);
        card.add(footer, BorderLayout.SOUTH);

        return card;
    }

    private JButton createCourseTileButton(Course course) {
        JButton btn = new JButton();
        btn.setLayout(new BorderLayout(18, 0));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 95));
        btn.setPreferredSize(new Dimension(0, 95));
        btn.setBackground(new Color(15, 23, 42));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 6, 0, 0, new Color(59, 130, 246)),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(51, 65, 85), 1),
                        new EmptyBorder(16, 22, 16, 22)
                )
        ));

        JLabel codeLabel = new JLabel(course.code);
        codeLabel.setFont(new Font("SansSerif", Font.BOLD, 26));
        codeLabel.setForeground(new Color(59, 130, 246));

        JLabel titleLabel = new JLabel(course.title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 26));
        titleLabel.setForeground(Color.WHITE);

        JLabel creditLabel = new JLabel(course.credit + " Cr");
        creditLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        creditLabel.setForeground(new Color(148, 163, 184));

        btn.add(codeLabel, BorderLayout.WEST);
        btn.add(titleLabel, BorderLayout.CENTER);
        btn.add(creditLabel, BorderLayout.EAST);

        btn.addActionListener(e -> openCourseDetail(course));

        return btn;
    }

    private JPanel createStandardCard(String title, String badgeText, String bigValue, String unitText, String footerText) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(new Color(30, 41, 59));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(51, 65, 85), 1),
                new EmptyBorder(45, 40, 45, 40)
        ));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel tLabel = new JLabel(title);
        tLabel.setFont(new Font("SansSerif", Font.BOLD, 38));
        tLabel.setForeground(new Color(226, 232, 240));

        JLabel badge = new JLabel(" " + badgeText + " ");
        badge.setFont(new Font("SansSerif", Font.BOLD, 22));
        badge.setForeground(new Color(59, 130, 246));
        badge.setBackground(new Color(15, 23, 42));
        badge.setOpaque(true);
        badge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(59, 130, 246), 1),
                new EmptyBorder(8, 16, 8, 16)
        ));

        top.add(tLabel, BorderLayout.WEST);
        top.add(badge, BorderLayout.EAST);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        JPanel valueRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 0));
        valueRow.setOpaque(false);

        JLabel val = new JLabel(bigValue);
        val.setFont(new Font("SansSerif", Font.BOLD, 100));
        val.setForeground(Color.WHITE);

        JLabel unit = new JLabel(unitText);
        unit.setFont(new Font("SansSerif", Font.PLAIN, 38));
        unit.setForeground(new Color(148, 163, 184));

        valueRow.add(val);
        valueRow.add(unit);

        center.add(Box.createVerticalGlue());
        center.add(valueRow);
        center.add(Box.createVerticalGlue());

        JLabel footer = new JLabel(footerText);
        footer.setFont(new Font("SansSerif", Font.PLAIN, 26));
        footer.setForeground(new Color(148, 163, 184));

        card.add(top, BorderLayout.NORTH);
        card.add(center, BorderLayout.CENTER);
        card.add(footer, BorderLayout.SOUTH);

        return card;
    }

    private JPanel createCourseDetailViewPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 30));
        panel.setOpaque(false);

        JPanel topHeader = new JPanel(new BorderLayout());
        topHeader.setOpaque(false);

        JButton backBtn = new JButton("← Back to Dashboard");
        backBtn.setFont(new Font("SansSerif", Font.BOLD, 26));
        backBtn.setForeground(new Color(59, 130, 246));
        backBtn.setContentAreaFilled(false);
        backBtn.setBorderPainted(false);
        backBtn.setFocusPainted(false);
        backBtn.addActionListener(e -> cardLayout.show(mainContentArea, "DASHBOARD_CARDS"));

        selectedCourseHeader = new JLabel("Course Details");
        selectedCourseHeader.setFont(new Font("SansSerif", Font.BOLD, 42));
        selectedCourseHeader.setForeground(Color.WHITE);

        topHeader.add(backBtn, BorderLayout.WEST);
        topHeader.add(selectedCourseHeader, BorderLayout.EAST);

        detailContentPanel = new JPanel();
        detailContentPanel.setLayout(new BoxLayout(detailContentPanel, BoxLayout.Y_AXIS));
        detailContentPanel.setOpaque(false);

        JScrollPane scrollPane = new JScrollPane(detailContentPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(25);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));

        panel.add(topHeader, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void openCourseDetail(Course course) {
        this.currentOpenCourse = course;
        selectedCourseHeader.setText(course.code + " : " + course.title);

        detailContentPanel.removeAll();

        detailContentPanel.add(createCTMarksCard(course.code));
        detailContentPanel.add(Box.createRigidArea(new Dimension(0, 35)));

        detailContentPanel.add(createCourseMaterialsCard(course.code));

        detailContentPanel.revalidate();
        detailContentPanel.repaint();

        cardLayout.show(mainContentArea, "COURSE_DETAIL");
    }

    private JPanel createCTMarksCard(String courseCode) {
        JPanel card = new JPanel(new BorderLayout(0, 25));
        card.setBackground(new Color(30, 41, 59));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(51, 65, 85), 1),
                new EmptyBorder(35, 40, 35, 40)
        ));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("📊  Tutorial & CT Marks");
        title.setFont(new Font("SansSerif", Font.BOLD, 34));
        title.setForeground(Color.WHITE);

        JPanel rightHeaderPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        rightHeaderPanel.setOpaque(false);

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setOpaque(false);

        if ("Teacher".equalsIgnoreCase(userRole)) {
            List<CTSummary> ctList = fetchUniqueCTsForCourse(courseCode);

            JButton addCTBtn = new JButton("+ Input CT Mark");
            addCTBtn.setFont(new Font("SansSerif", Font.BOLD, 22));
            addCTBtn.setForeground(Color.WHITE);
            addCTBtn.setBackground(new Color(59, 130, 246));
            addCTBtn.setFocusPainted(false);
            addCTBtn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(37, 99, 235), 1),
                    new EmptyBorder(10, 20, 10, 20)
            ));
            addCTBtn.addActionListener(e -> showInputCTMarkDialog(courseCode));
            rightHeaderPanel.add(addCTBtn);

            JLabel badge = new JLabel(ctList.isEmpty() ? " Pending " : " Evaluated (" + ctList.size() + ") ▲ ");
            badge.setFont(new Font("SansSerif", Font.BOLD, 22));
            badge.setForeground(new Color(59, 130, 246));
            badge.setBackground(new Color(15, 23, 42));
            badge.setOpaque(true);
            badge.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(59, 130, 246), 1),
                    new EmptyBorder(8, 16, 8, 16)
            ));
            rightHeaderPanel.add(badge);

            if (ctList.isEmpty()) {
                JLabel emptyLabel = new JLabel("No CT or tutorial marks published yet for this course.");
                emptyLabel.setFont(new Font("SansSerif", Font.PLAIN, 24));
                emptyLabel.setForeground(new Color(148, 163, 184));
                emptyLabel.setBorder(new EmptyBorder(20, 0, 20, 0));
                listPanel.add(emptyLabel);
            } else {
                for (int i = 0; i < ctList.size(); i++) {
                    listPanel.add(createTeacherCTGroupTile(courseCode, ctList.get(i)));
                    if (i < ctList.size() - 1) listPanel.add(Box.createRigidArea(new Dimension(0, 20)));
                }
            }

        } else {
            List<StudentCTMark> studentCTs = fetchCTMarksForStudent(courseCode);

            JLabel badge = new JLabel(studentCTs.isEmpty() ? " Pending " : " Evaluated (" + studentCTs.size() + ") ▲ ");
            badge.setFont(new Font("SansSerif", Font.BOLD, 22));
            badge.setForeground(new Color(59, 130, 246));
            badge.setBackground(new Color(15, 23, 42));
            badge.setOpaque(true);
            badge.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(59, 130, 246), 1),
                    new EmptyBorder(8, 16, 8, 16)
            ));
            rightHeaderPanel.add(badge);

            if (studentCTs.isEmpty()) {
                JLabel emptyLabel = new JLabel("No tutorial or CT marks published for you yet.");
                emptyLabel.setFont(new Font("SansSerif", Font.PLAIN, 24));
                emptyLabel.setForeground(new Color(148, 163, 184));
                emptyLabel.setBorder(new EmptyBorder(20, 0, 20, 0));
                listPanel.add(emptyLabel);
            } else {
                for (int i = 0; i < studentCTs.size(); i++) {
                    listPanel.add(createStudentCTTile(studentCTs.get(i)));
                    if (i < studentCTs.size() - 1) listPanel.add(Box.createRigidArea(new Dimension(0, 20)));
                }
            }
        }

        header.add(title, BorderLayout.WEST);
        header.add(rightHeaderPanel, BorderLayout.EAST);

        card.add(header, BorderLayout.NORTH);
        card.add(listPanel, BorderLayout.CENTER);

        return card;
    }

    private JPanel createStudentCTTile(StudentCTMark mark) {
        JPanel item = new JPanel(new BorderLayout());
        item.setBackground(new Color(15, 23, 42));
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 105));
        item.setPreferredSize(new Dimension(0, 105));
        item.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(51, 65, 85), 1),
                new EmptyBorder(18, 28, 18, 28)
        ));

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        JLabel nameLabel = new JLabel(mark.title);
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 26));
        nameLabel.setForeground(Color.WHITE);

        JLabel dateLabel = new JLabel("Date: " + mark.date);
        dateLabel.setFont(new Font("SansSerif", Font.PLAIN, 22));
        dateLabel.setForeground(new Color(148, 163, 184));

        left.add(nameLabel);
        left.add(Box.createRigidArea(new Dimension(0, 6)));
        left.add(dateLabel);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.setOpaque(false);

        JLabel obtainedLbl = new JLabel(String.valueOf((int) mark.obtainedMarks));
        obtainedLbl.setFont(new Font("SansSerif", Font.BOLD, 36));
        obtainedLbl.setForeground(new Color(59, 130, 246));

        JLabel totalLbl = new JLabel(" / " + ((int) mark.totalMarks));
        totalLbl.setFont(new Font("SansSerif", Font.BOLD, 32));
        totalLbl.setForeground(new Color(148, 163, 184));

        right.add(obtainedLbl);
        right.add(totalLbl);

        item.add(left, BorderLayout.WEST);
        item.add(right, BorderLayout.EAST);

        return item;
    }

    private JPanel createTeacherCTGroupTile(String courseCode, CTSummary ct) {
        JPanel item = new JPanel(new BorderLayout());
        item.setBackground(new Color(15, 23, 42));
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 105));
        item.setPreferredSize(new Dimension(0, 105));
        item.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(51, 65, 85), 1),
                new EmptyBorder(18, 28, 18, 28)
        ));

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        JLabel nameLabel = new JLabel(ct.title);
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 26));
        nameLabel.setForeground(Color.WHITE);

        JLabel infoLabel = new JLabel("Date: " + ct.date + "   |   Total Marks: " + ((int) ct.totalMarks));
        infoLabel.setFont(new Font("SansSerif", Font.PLAIN, 22));
        infoLabel.setForeground(new Color(148, 163, 184));

        left.add(nameLabel);
        left.add(Box.createRigidArea(new Dimension(0, 6)));
        left.add(infoLabel);

        JButton viewAllBtn = new JButton("View All Marks →");
        viewAllBtn.setFont(new Font("SansSerif", Font.BOLD, 22));
        viewAllBtn.setForeground(Color.WHITE);
        viewAllBtn.setBackground(new Color(59, 130, 246));
        viewAllBtn.setFocusPainted(false);
        viewAllBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(37, 99, 235), 1),
                new EmptyBorder(12, 24, 12, 24)
        ));

        viewAllBtn.addActionListener(e -> showCTMarksTableDialog(courseCode, ct.title));

        item.add(left, BorderLayout.WEST);
        item.add(viewAllBtn, BorderLayout.EAST);

        return item;
    }

    private void showCTMarksTableDialog(String courseCode, String ctTitle) {
        JDialog dialog = new JDialog(this, ctTitle + " - Student Marks (" + courseCode + ")", true);
        dialog.setSize(850, 650);
        dialog.setLocationRelativeTo(this);

        JPanel mainContainer = new JPanel(new BorderLayout(0, 20));
        mainContainer.setBackground(new Color(15, 23, 42));
        mainContainer.setBorder(new EmptyBorder(25, 25, 25, 25));

        JLabel titleLbl = new JLabel("Marks for: " + ctTitle);
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 30));
        titleLbl.setForeground(Color.WHITE);

        List<CTMarkDetail> marks = fetchAllMarksForCT(courseCode, ctTitle);

        String[] columns = {"Exam Roll", "Student Name", "Obtained Marks", "Total Marks"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (CTMarkDetail m : marks) {
            tableModel.addRow(new Object[]{
                    m.examRoll,
                    m.studentName,
                    (int) m.obtainedMarks,
                    (int) m.totalMarks
            });
        }

        JTable table = new JTable(tableModel);
        table.setFont(new Font("SansSerif", Font.PLAIN, 22));
        table.setRowHeight(45);
        table.setBackground(new Color(30, 41, 59));
        table.setForeground(Color.WHITE);
        table.setGridColor(new Color(51, 65, 85));

        JTableHeader tableHeader = table.getTableHeader();
        tableHeader.setFont(new Font("SansSerif", Font.BOLD, 24));
        tableHeader.setBackground(new Color(59, 130, 246));
        tableHeader.setForeground(Color.WHITE);
        tableHeader.setPreferredSize(new Dimension(0, 50));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setBackground(new Color(15, 23, 42));
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(51, 65, 85), 1));

        JButton closeBtn = new JButton("Close");
        closeBtn.setFont(new Font("SansSerif", Font.BOLD, 22));
        closeBtn.setForeground(Color.WHITE);
        closeBtn.setBackground(new Color(239, 68, 68));
        closeBtn.setFocusPainted(false);
        closeBtn.addActionListener(e -> dialog.dispose());

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setOpaque(false);
        bottomPanel.add(closeBtn);

        mainContainer.add(titleLbl, BorderLayout.NORTH);
        mainContainer.add(scrollPane, BorderLayout.CENTER);
        mainContainer.add(bottomPanel, BorderLayout.SOUTH);

        dialog.add(mainContainer);
        dialog.setVisible(true);
    }

    private void showInputCTMarkDialog(String courseCode) {
        List<EnrolledStudent> students = fetchEnrolledStudentsForCourse(courseCode);

        if (students.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No enrolled students found for this course.",
                    "Cannot Input Marks", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog(this, "Batch Submit CT Marks - " + courseCode, true);
        dialog.setSize(900, 750);
        dialog.setLocationRelativeTo(this);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 20));
        mainPanel.setBackground(new Color(15, 23, 42));
        mainPanel.setBorder(new EmptyBorder(25, 25, 25, 25));

        JPanel topFormPanel = new JPanel(new GridLayout(3, 2, 15, 15));
        topFormPanel.setBackground(new Color(30, 41, 59));
        topFormPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(51, 65, 85), 1),
                new EmptyBorder(20, 20, 20, 20)
        ));

        Font labelFont = new Font("SansSerif", Font.BOLD, 22);
        Font fieldFont = new Font("SansSerif", Font.PLAIN, 22);

        JLabel titleLbl = new JLabel("CT / Exam Title (e.g. CT-1):");
        titleLbl.setFont(labelFont);
        titleLbl.setForeground(Color.WHITE);
        JTextField titleField = new JTextField();
        titleField.setFont(fieldFont);

        JLabel dateLbl = new JLabel("Date (YYYY-MM-DD):");
        dateLbl.setFont(labelFont);
        dateLbl.setForeground(Color.WHITE);
        JTextField dateField = new JTextField(java.time.LocalDate.now().toString());
        dateField.setFont(fieldFont);

        JLabel totalLbl = new JLabel("Total Marks:");
        totalLbl.setFont(labelFont);
        totalLbl.setForeground(Color.WHITE);
        JTextField totalField = new JTextField("20");
        totalField.setFont(fieldFont);

        topFormPanel.add(titleLbl);
        topFormPanel.add(titleField);
        topFormPanel.add(dateLbl);
        topFormPanel.add(dateField);
        topFormPanel.add(totalLbl);
        topFormPanel.add(totalField);

        JPanel studentListPanel = new JPanel();
        studentListPanel.setLayout(new BoxLayout(studentListPanel, BoxLayout.Y_AXIS));
        studentListPanel.setBackground(new Color(30, 41, 59));
        studentListPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        List<JTextField> markInputs = new ArrayList<>();

        for (EnrolledStudent student : students) {
            JPanel row = new JPanel(new BorderLayout(15, 0));
            row.setBackground(new Color(15, 23, 42));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));
            row.setPreferredSize(new Dimension(0, 65));
            row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(51, 65, 85), 1),
                    new EmptyBorder(10, 20, 10, 20)
            ));

            JLabel rollAndNameLabel = new JLabel("Roll: " + student.examRoll + "  —  " + student.name);
            rollAndNameLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
            rollAndNameLabel.setForeground(Color.WHITE);

            JTextField markInput = new JTextField("0");
            markInput.setFont(fieldFont);
            markInput.setPreferredSize(new Dimension(120, 40));
            markInput.setHorizontalAlignment(JTextField.CENTER);

            markInputs.add(markInput);

            row.add(rollAndNameLabel, BorderLayout.WEST);
            row.add(markInput, BorderLayout.EAST);

            studentListPanel.add(row);
            studentListPanel.add(Box.createRigidArea(new Dimension(0, 12)));
        }

        JScrollPane scrollPane = new JScrollPane(studentListPanel);
        scrollPane.getViewport().setBackground(new Color(30, 41, 59));
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(51, 65, 85), 1));
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        buttonPanel.setOpaque(false);

        JButton saveBtn = new JButton("Submit All Marks");
        saveBtn.setFont(new Font("SansSerif", Font.BOLD, 22));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setBackground(new Color(59, 130, 246));
        saveBtn.setFocusPainted(false);

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setFont(new Font("SansSerif", Font.BOLD, 22));
        cancelBtn.setForeground(Color.WHITE);
        cancelBtn.setBackground(new Color(239, 68, 68));
        cancelBtn.setFocusPainted(false);

        saveBtn.addActionListener(e -> {
            String ctTitle = titleField.getText().trim();
            String dateStr = dateField.getText().trim();
            String totalStr = totalField.getText().trim();

            if (ctTitle.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please enter an Exam/CT Title.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            double totalMarks;
            try {
                totalMarks = Double.parseDouble(totalStr);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Please enter a valid total marks number.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String insertSql = "INSERT INTO ct_marks (user_id, course_code, title, test_date, obtained_marks, total_marks) VALUES (?, ?, ?, ?, ?, ?)";
            try (Connection con = DBConnection.getConnection()) {
                con.setAutoCommit(false);
                try (PreparedStatement pstmt = con.prepareStatement(insertSql)) {
                    for (int i = 0; i < students.size(); i++) {
                        EnrolledStudent student = students.get(i);
                        double obtained = Double.parseDouble(markInputs.get(i).getText().trim());

                        pstmt.setInt(1, student.id);
                        pstmt.setString(2, courseCode);
                        pstmt.setString(3, ctTitle);
                        pstmt.setString(4, dateStr);
                        pstmt.setDouble(5, obtained);
                        pstmt.setDouble(6, totalMarks);
                        pstmt.addBatch();
                    }
                    pstmt.executeBatch();
                    con.commit();

                    JOptionPane.showMessageDialog(dialog, "All CT marks submitted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    dialog.dispose();

                    if (currentOpenCourse != null) {
                        openCourseDetail(currentOpenCourse);
                    }
                } catch (SQLException ex) {
                    con.rollback();
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(dialog, "Database Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Please ensure all entered marks are valid numbers.", "Input Error", JOptionPane.ERROR_MESSAGE);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

        cancelBtn.addActionListener(e -> dialog.dispose());

        buttonPanel.add(saveBtn);
        buttonPanel.add(cancelBtn);

        mainPanel.add(topFormPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.add(mainPanel);
        dialog.setVisible(true);
    }

    private JPanel createCourseMaterialsCard(String courseCode) {
        JPanel card = new JPanel(new BorderLayout(0, 25));
        card.setBackground(new Color(30, 41, 59));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(51, 65, 85), 1),
                new EmptyBorder(35, 40, 35, 40)
        ));

        List<Material> materials = fetchMaterialsForCourse(courseCode);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("📁  Course Materials & Resources");
        title.setFont(new Font("SansSerif", Font.BOLD, 34));
        title.setForeground(Color.WHITE);

        JPanel rightHeaderPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        rightHeaderPanel.setOpaque(false);

        if ("Teacher".equalsIgnoreCase(userRole)) {
            JButton uploadBtn = new JButton("+ Upload Material");
            uploadBtn.setFont(new Font("SansSerif", Font.BOLD, 22));
            uploadBtn.setForeground(Color.WHITE);
            uploadBtn.setBackground(new Color(59, 130, 246));
            uploadBtn.setFocusPainted(false);
            uploadBtn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(37, 99, 235), 1),
                    new EmptyBorder(10, 20, 10, 20)
            ));

            uploadBtn.addActionListener(e -> handleUploadMaterial(courseCode));
            rightHeaderPanel.add(uploadBtn);
        }

        JLabel badge = new JLabel(" " + materials.size() + " Files ▲ ");
        badge.setFont(new Font("SansSerif", Font.BOLD, 22));
        badge.setForeground(new Color(148, 163, 184));
        badge.setBackground(new Color(15, 23, 42));
        badge.setOpaque(true);
        badge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(51, 65, 85), 1),
                new EmptyBorder(8, 16, 8, 16)
        ));
        rightHeaderPanel.add(badge);

        header.add(title, BorderLayout.WEST);
        header.add(rightHeaderPanel, BorderLayout.EAST);

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setOpaque(false);

        if (materials.isEmpty()) {
            JLabel emptyLabel = new JLabel("No files or resources uploaded for this course yet.");
            emptyLabel.setFont(new Font("SansSerif", Font.PLAIN, 24));
            emptyLabel.setForeground(new Color(148, 163, 184));
            emptyLabel.setBorder(new EmptyBorder(20, 0, 20, 0));
            listPanel.add(emptyLabel);
        } else {
            for (int i = 0; i < materials.size(); i++) {
                Material mat = materials.get(i);
                listPanel.add(createMaterialItem(
                        mat.getFileName(),
                        mat.filePath
                ));
                if (i < materials.size() - 1) {
                    listPanel.add(Box.createRigidArea(new Dimension(0, 20)));
                }
            }
        }

        card.add(header, BorderLayout.NORTH);
        card.add(listPanel, BorderLayout.CENTER);

        return card;
    }

    private JPanel createMaterialItem(String fileName, String filePath) {
        JPanel item = new JPanel(new BorderLayout());
        item.setBackground(new Color(15, 23, 42));
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 105));
        item.setPreferredSize(new Dimension(0, 105));
        item.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(51, 65, 85), 1),
                new EmptyBorder(18, 28, 18, 28)
        ));

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        JLabel nameLabel = new JLabel("📄  " + fileName);
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 26));
        nameLabel.setForeground(Color.WHITE);

        JLabel pathLabel = new JLabel(filePath);
        pathLabel.setFont(new Font("SansSerif", Font.PLAIN, 20));
        pathLabel.setForeground(new Color(148, 163, 184));

        left.add(nameLabel);
        left.add(Box.createRigidArea(new Dimension(0, 6)));
        left.add(pathLabel);

        JButton downloadBtn = new JButton("Open / Download ⬇");
        downloadBtn.setFont(new Font("SansSerif", Font.BOLD, 22));
        downloadBtn.setForeground(Color.WHITE);
        downloadBtn.setBackground(new Color(30, 41, 59));
        downloadBtn.setFocusPainted(false);
        downloadBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(71, 85, 105), 1),
                new EmptyBorder(12, 24, 12, 24)
        ));

        downloadBtn.addActionListener(e -> {
            JOptionPane.showMessageDialog(this,
                    "Opening File: " + fileName + "\nFull Path: " + filePath,
                    "File Material", JOptionPane.INFORMATION_MESSAGE);
        });

        item.add(left, BorderLayout.WEST);
        item.add(downloadBtn, BorderLayout.EAST);

        return item;
    }

    private void handleUploadMaterial(String courseCode) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Course Material File to Upload");
        int userSelection = fileChooser.showOpenDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToUpload = fileChooser.getSelectedFile();
            String selectedFilePath = fileToUpload.getAbsolutePath();

            String insertSql = "INSERT INTO course_materials (course_code, file_path) VALUES (?, ?)";
            try (Connection con = DBConnection.getConnection();
                 PreparedStatement pstmt = con.prepareStatement(insertSql)) {

                pstmt.setString(1, courseCode);
                pstmt.setString(2, selectedFilePath);

                pstmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "Course material uploaded successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);

                if (currentOpenCourse != null) {
                    openCourseDetail(currentOpenCourse);
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private JButton createNavButton(String text, boolean isActive) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 32));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 85));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(18, 28, 18, 28));

        if (isActive) {
            btn.setBackground(new Color(59, 130, 246));
            btn.setForeground(Color.WHITE);
        } else {
            btn.setBackground(new Color(30, 41, 59));
            btn.setForeground(new Color(148, 163, 184));
        }

        return btn;
    }

    public boolean saveAssignment(String courseCode, String title, String description,
                                  String dueDate, String marks, String filePath) {
        String sql = "INSERT INTO assignments (course_code, title, description, due_date, total_marks, file_path) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, courseCode);
            stmt.setString(2, title);
            stmt.setString(3, description);
            stmt.setDate(4, java.sql.Date.valueOf(dueDate));
            stmt.setInt(5, Integer.parseInt(marks));

            if (filePath != null && !filePath.trim().isEmpty()) {
                stmt.setString(6, filePath.trim());
            } else {
                stmt.setNull(6, java.sql.Types.VARCHAR);
            }

            int rows = stmt.executeUpdate();
            return rows > 0;

        } catch (SQLException | IllegalArgumentException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean submitStudentAssignment(int assignmentId, int studentId, String filePath) {
        String checkSql = "SELECT due_date FROM assignments WHERE id = ? AND due_date >= CURDATE()";
        String insertSql = "INSERT INTO assignment_submissions (assignment_id, student_id, submission_date, file_path) VALUES (?, ?, NOW(), ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement checkStmt = con.prepareStatement(checkSql)) {
            checkStmt.setInt(1, assignmentId);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
            }

            try (PreparedStatement pstmt = con.prepareStatement(insertSql)) {
                pstmt.setInt(1, assignmentId);
                pstmt.setInt(2, studentId);
                pstmt.setString(3, filePath);
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean gradeSubmission(int userID, String course_code, String title, Date test_date, double obt_mark, double ttl_mark, int submissionId, double marks, String feedback) {
        String sql = "INSERT INTO ct_marks (user_id, course_code, title, test_date, obtained_marks, total_marks) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {

            pstmt.setInt(1, userID);
            pstmt.setString(2, course_code);
            pstmt.setString(3, title);
            pstmt.setDate(4, new java.sql.Date(test_date.getTime()));
            pstmt.setDouble(5, obt_mark);
            pstmt.setDouble(6, ttl_mark);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void showCreateAssignmentDialog() {
        JDialog dialog = new JDialog((Frame) null, "Assign New Assignment", true);
        // Increased dialog size from 750x650 to 900x750
        dialog.setSize(900, 750);
        dialog.setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 25));
        mainPanel.setBackground(new Color(15, 23, 42));
        // Increased outer padding
        mainPanel.setBorder(new EmptyBorder(35, 35, 35, 35));

        JLabel titleLbl = new JLabel("Create & Publish Assignment");
        // Increased title font size
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 32));
        titleLbl.setForeground(Color.WHITE);

        // Increased vertical/horizontal gaps in grid layout to space out rows
        JPanel formPanel = new JPanel(new GridLayout(6, 2, 20, 20));
        formPanel.setOpaque(false);

        // Larger font sizes for inputs and labels
        Font labelFont = new Font("SansSerif", Font.BOLD, 20);
        Font fieldFont = new Font("SansSerif", Font.PLAIN, 20);

        JLabel courseLbl = new JLabel("Course Code:");
        courseLbl.setFont(labelFont);
        courseLbl.setForeground(Color.WHITE);
        JTextField courseField = new JTextField();
        courseField.setFont(fieldFont);

        JLabel assignTitleLbl = new JLabel("Assignment Title:");
        assignTitleLbl.setFont(labelFont);
        assignTitleLbl.setForeground(Color.WHITE);
        JTextField assignTitleField = new JTextField();
        assignTitleField.setFont(fieldFont);

        JLabel descLbl = new JLabel("Description / Instructions:");
        descLbl.setFont(labelFont);
        descLbl.setForeground(Color.WHITE);
        JTextField descField = new JTextField();
        descField.setFont(fieldFont);

        JLabel dueLbl = new JLabel("Due Date (YYYY-MM-DD):");
        dueLbl.setFont(labelFont);
        dueLbl.setForeground(Color.WHITE);
        JTextField dueField = new JTextField("2026-08-20");
        dueField.setFont(fieldFont);

        JLabel marksLbl = new JLabel("Total Marks:");
        marksLbl.setFont(labelFont);
        marksLbl.setForeground(Color.WHITE);
        JTextField marksField = new JTextField("10");
        marksField.setFont(fieldFont);

        JLabel fileLbl = new JLabel("Attachment (Optional):");
        fileLbl.setFont(labelFont);
        fileLbl.setForeground(Color.WHITE);

        JButton attachBtn = new JButton("Choose File...");
        attachBtn.setFont(fieldFont);

        final String[] selectedPath = {null};

        attachBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            if (chooser.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION) {
                selectedPath[0] = chooser.getSelectedFile().getAbsolutePath();
                attachBtn.setText(chooser.getSelectedFile().getName());
            }
        });

        formPanel.add(courseLbl);
        formPanel.add(courseField);
        formPanel.add(assignTitleLbl);
        formPanel.add(assignTitleField);
        formPanel.add(descLbl);
        formPanel.add(descField);
        formPanel.add(dueLbl);
        formPanel.add(dueField);
        formPanel.add(marksLbl);
        formPanel.add(marksField);
        formPanel.add(fileLbl);
        formPanel.add(attachBtn);

        JButton submitBtn = new JButton("Publish Assignment");
        // Made the submit button font larger and added inner padding for a bigger click area
        submitBtn.setFont(new Font("SansSerif", Font.BOLD, 22));
        submitBtn.setForeground(Color.WHITE);
        submitBtn.setBackground(new Color(59, 130, 246));
        submitBtn.setFocusPainted(false);
        submitBtn.setBorder(new EmptyBorder(12, 0, 12, 0));

        submitBtn.addActionListener(e -> {
            String code = courseField.getText().trim();
            String t = assignTitleField.getText().trim();
            String desc = descField.getText().trim();
            String due = dueField.getText().trim();
            String marksStr = marksField.getText().trim();

            if (code.isEmpty() || t.isEmpty() || marksStr.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please fill in Course, Title, and Marks.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                String filePath = (selectedPath[0] != null && !selectedPath[0].trim().isEmpty())
                        ? selectedPath[0].trim()
                        : null;

                if (saveAssignment(code, t, desc, due, marksStr, filePath)) {
                    JOptionPane.showMessageDialog(dialog, "Assignment published successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);

                    if (assignCardPanel != null) {
                        refreshTeacherAssignmentCard(assignCardPanel, code);
                    }

                    dialog.dispose();
                } else {
                    JOptionPane.showMessageDialog(dialog, "Failed to publish assignment.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Marks must be a valid number.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        mainPanel.add(titleLbl, BorderLayout.NORTH);
        mainPanel.add(formPanel, BorderLayout.CENTER);
        mainPanel.add(submitBtn, BorderLayout.SOUTH);

        dialog.add(mainPanel);
        dialog.setVisible(true);
    }

    private void showStudentAssignmentsDialog() {
        JDialog dialog = new JDialog(this, "Active Course Assignments", true);
        dialog.setSize(950, 600);
        dialog.setLocationRelativeTo(this);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 15));
        mainPanel.setBackground(new Color(15, 23, 42));
        mainPanel.setBorder(new EmptyBorder(25, 25, 25, 25));

        JLabel titleLbl = new JLabel("Assignments for Your Enrolled Semester");
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 26));
        titleLbl.setForeground(Color.WHITE);

        String[] columnNames = {"Assignment ID", "Course Code", "Title", "Description", "Due Date", "Total Marks"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        String sql = "SELECT DISTINCT a.id, a.course_code, a.title, a.description, a.due_date, a.total_marks " +
                "FROM assignments a " +
                "JOIN course_details cd ON TRIM(a.course_code) = TRIM(cd.course_code) " +
                "JOIN batch_semester bs ON TRIM(cd.semester) = TRIM(bs.semester) " +
                "JOIN users u ON TRIM(u.batch) = TRIM(bs.Batch) " +
                "WHERE u.id = ? AND a.due_date >= CURDATE() ORDER BY a.due_date ASC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    tableModel.addRow(new Object[]{
                            rs.getInt("id"),
                            rs.getString("course_code"),
                            rs.getString("title"),
                            rs.getString("description"),
                            rs.getDate("due_date"),
                            rs.getInt("total_marks")
                    });
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        JTable table = new JTable(tableModel);
        table.setFont(new Font("SansSerif", Font.PLAIN, 18));
        table.setRowHeight(40);
        table.setBackground(new Color(30, 41, 59));
        table.setForeground(Color.WHITE);
        table.setGridColor(new Color(51, 65, 85));

        JTableHeader tableHeader = table.getTableHeader();
        tableHeader.setFont(new Font("SansSerif", Font.BOLD, 20));
        tableHeader.setBackground(new Color(59, 130, 246));
        tableHeader.setForeground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setBackground(new Color(15, 23, 42));

        JButton submitSelectedBtn = new JButton("Submit Selected Assignment");
        submitSelectedBtn.setFont(new Font("SansSerif", Font.BOLD, 20));
        submitSelectedBtn.setForeground(Color.WHITE);
        submitSelectedBtn.setBackground(new Color(34, 197, 94));
        submitSelectedBtn.setFocusPainted(false);

        submitSelectedBtn.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(dialog, "Please select an assignment row to submit.", "Selection Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int assignmentId = (int) table.getValueAt(selectedRow, 0);
            String assignTitle = (String) table.getValueAt(selectedRow, 2);
            dialog.dispose();
            showStudentSubmitAssignmentDialog(assignmentId, assignTitle, this.userId);
        });

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setOpaque(false);
        bottomPanel.add(submitSelectedBtn);

        mainPanel.add(titleLbl, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        dialog.add(mainPanel);
        dialog.setVisible(true);
    }

    private void showStudentSubmitAssignmentDialog(int assignmentId, String assignmentTitle, int studentId) {
        JDialog dialog = new JDialog((Frame) null, "Submit: " + assignmentTitle, true);
        dialog.setSize(600, 350);
        dialog.setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 20));
        mainPanel.setBackground(new Color(15, 23, 42));
        mainPanel.setBorder(new EmptyBorder(25, 25, 25, 25));

        JLabel titleLbl = new JLabel("Upload Solution File for: " + assignmentTitle);
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 22));
        titleLbl.setForeground(Color.WHITE);

        final String[] selectedFilePath = {""};

        JButton chooseFileBtn = new JButton("📎 Select PDF/ZIP File");
        chooseFileBtn.setFont(new Font("SansSerif", Font.PLAIN, 20));
        chooseFileBtn.setBackground(new Color(30, 41, 59));
        chooseFileBtn.setForeground(Color.WHITE);
        chooseFileBtn.setFocusPainted(false);

        chooseFileBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            if (chooser.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION) {
                selectedFilePath[0] = chooser.getSelectedFile().getAbsolutePath();
                chooseFileBtn.setText("Selected: " + chooser.getSelectedFile().getName());
            }
        });

        JButton submitBtn = new JButton("Upload & Submit");
        submitBtn.setFont(new Font("SansSerif", Font.BOLD, 20));
        submitBtn.setForeground(Color.WHITE);
        submitBtn.setBackground(new Color(34, 197, 94));
        submitBtn.setFocusPainted(false);

        submitBtn.addActionListener(e -> {
            if (selectedFilePath[0].isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please select a file to submit.", "Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (submitStudentAssignment(assignmentId, studentId, selectedFilePath[0])) {
                JOptionPane.showMessageDialog(dialog, "Assignment submitted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
            } else {
                JOptionPane.showMessageDialog(dialog, "Failed to submit assignment.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        mainPanel.add(titleLbl, BorderLayout.NORTH);
        mainPanel.add(chooseFileBtn, BorderLayout.CENTER);
        mainPanel.add(submitBtn, BorderLayout.SOUTH);

        dialog.add(mainPanel);
        dialog.setVisible(true);
    }

    private void showEvaluateAssignmentsDialog() {
        JDialog dialog = new JDialog(this, "Evaluate Student Submissions", true);
        dialog.setSize(1050, 700);
        dialog.setLocationRelativeTo(this);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 25));
        mainPanel.setBackground(new Color(15, 23, 42));
        mainPanel.setBorder(new EmptyBorder(25, 25, 25, 25));

        JLabel titleLbl = new JLabel("Student Submissions Pending Evaluation");
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 30));
        titleLbl.setForeground(Color.WHITE);

        // Added columns to capture course code, test date, and total marks needed for ct_marks table
        String[] columns = {"Sub ID", "Course Code", "Assignment Title", "Student ID", "Test Date", "Total Marks", "Obtained Marks"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // Only 'Obtained Marks' column (index 6) is editable
                return column == 6;
            }
        };

        // Updated SQL query to join necessary fields from assignments table (including course_code, test_date, total_marks)
        String sql = "SELECT s.id, a.course_code, a.title, s.student_id, a.due_date AS test_date, a.total_marks " +
                "FROM assignment_submissions s " +
                "JOIN assignments a ON s.assignment_id = a.id " +
                "WHERE TRIM(a.course_code) = TRIM(?)";;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("course_code"),
                        rs.getString("title"),
                        rs.getInt("student_id"),
                        rs.getDate("test_date"),
                        rs.getDouble("total_marks"),
                        rs.getObject("obtained_marks") != null ? rs.getDouble("obtained_marks") : ""
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        JTable table = new JTable(tableModel);
        table.setFont(new Font("SansSerif", Font.PLAIN, 18));
        table.setRowHeight(35);
        table.setBackground(new Color(30, 41, 59));
        table.setForeground(Color.WHITE);
        table.setGridColor(new Color(51, 65, 85));

        JTableHeader tableHeader = table.getTableHeader();
        tableHeader.setFont(new Font("SansSerif", Font.BOLD, 18));
        tableHeader.setBackground(new Color(59, 130, 246));
        tableHeader.setForeground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setBackground(new Color(15, 23, 42));

        JButton saveMarksBtn = new JButton("Save Updated Marks");
        saveMarksBtn.setFont(new Font("SansSerif", Font.BOLD, 20));
        saveMarksBtn.setForeground(Color.WHITE);
        saveMarksBtn.setBackground(new Color(59, 130, 246));
        saveMarksBtn.setFocusPainted(false);
        saveMarksBtn.setBorder(new EmptyBorder(10, 20, 10, 20));

        saveMarksBtn.addActionListener(e -> {
            try {
                for (int i = 0; i < table.getRowCount(); i++) {
                    int subId = (int) table.getValueAt(i, 0);
                    String courseCode = (String) table.getValueAt(i, 1);
                    String assignTitle = (String) table.getValueAt(i, 2);
                    int studentId = (int) table.getValueAt(i, 3);
                    java.sql.Date testDate = (java.sql.Date) table.getValueAt(i, 4);
                    double totalMarks = (double) table.getValueAt(i, 5);

                    Object markVal = table.getValueAt(i, 6);
                    if (markVal != null && !markVal.toString().trim().isEmpty()) {
                        double obtainedMarks = Double.parseDouble(markVal.toString().trim());

                        // Call your complete gradeSubmission backend method matching all required parameters
                        gradeSubmission(studentId, courseCode, assignTitle, testDate, obtainedMarks, totalMarks, subId, obtainedMarks, "");
                    }
                }
                JOptionPane.showMessageDialog(dialog, "Marks saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(dialog, "Error saving marks. Please check number formats.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setOpaque(false);
        bottomPanel.add(saveMarksBtn);

        mainPanel.add(titleLbl, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        dialog.add(mainPanel);
        dialog.setVisible(true);
    }

    private String SemLabel(String code) {
        return "Year " + code.charAt(0) + ", Semester " + code.charAt(2);
    }

    public void refreshTeacherAssignmentCard(JPanel assignCardPanel, String courseCode) {
        assignCardPanel.removeAll();
        assignCardPanel.setLayout(new BoxLayout(assignCardPanel, BoxLayout.Y_AXIS));

        String sql = "SELECT title, due_date, total_marks FROM assignments WHERE TRIM(course_code) = TRIM(?) ORDER BY due_date ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, courseCode);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String title = rs.getString("title");
                    String dueDate = rs.getString("due_date");
                    double marks = rs.getDouble("total_marks");

                    JPanel itemPanel = new JPanel(new BorderLayout());
                    itemPanel.setBackground(new Color(30, 41, 59));
                    itemPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

                    JLabel lblInfo = new JLabel("<html><b style='color:#3b82f6;'>" + title + "</b><br>" +
                            "Due: " + dueDate + " | Marks: " + marks + "</html>");
                    lblInfo.setForeground(Color.WHITE);

                    itemPanel.add(lblInfo, BorderLayout.CENTER);
                    assignCardPanel.add(itemPanel);
                    assignCardPanel.add(Box.createRigidArea(new Dimension(0, 8)));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        assignCardPanel.revalidate();
        assignCardPanel.repaint();
    }
}