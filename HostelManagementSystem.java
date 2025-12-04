import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
// --------------------

public class HostelManagementSystem extends JFrame {
    
    // ====================================================================
    // --- DATABASE CONFIGURATION (JDBC) ---
    // Change these values to match your MySQL server settings
    private static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String DB_URL = "jdbc:mysql://localhost:3306/HostelManagementDB";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "AmAn@12$89"; // <--- CHANGE THIS!
    
    private Connection getConnection() throws SQLException {
        // NOTE: Class.forName(JDBC_DRIVER) is usually required once
        try {
            Class.forName(JDBC_DRIVER);
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found.");
            throw new SQLException("JDBC Driver not available.", e);
        }
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }
    // ====================================================================
    
    // --- COLOR PALETTE (Final Optimized) ---
    private static final Color PRIMARY_COLOR = new Color(0, 102, 204);     
    private static final Color ACCENT_COLOR = new Color(10, 100, 10);       // Dark Green for selection/accents
    private static final Color DELETE_COLOR = new Color(220, 50, 50);       
    private static final Color BACKGROUND_GRAY = new Color(200, 200, 210); 
    
    public static final Color CARD_BACKGROUND = new Color(255, 255, 255); // White
    public static final Color TABLE_STRIPE = new Color(215, 215, 215);    // Visible Gray stripe

    // --- FONT STYLES (Unchanged) ---
    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 28);
    private static final Font HEADER_FONT = new Font("Segoe UI", Font.BOLD, 18);
    private static final Font LABEL_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font BUTTON_FONT = new Font("Segoe UI", Font.BOLD, 14);

    // --- Data Model & File Constant ---
    // NOTE: File constants are ONLY used for reading/writing persistence data before migration
    private static final String STUDENT_DATA_FILE = "hostel_students.dat"; 
    private static final String STAFF_DATA_FILE = "hostel_staff.dat"; 
    private static final String SUGGESTION_DATA_FILE = "hostel_suggestions.dat"; 
    
    private ArrayList<Student> studentList = new ArrayList<>(); 
    private ArrayList<Staff> staffList = new ArrayList<>(); 
    private ArrayList<Suggestion> suggestionList = new ArrayList<>(); 
    
    private DefaultTableModel studentTableModel; 
    private JTable studentTable;
    
    private DefaultTableModel staffTableModel; 
    private JTable staffTable; 
    
    private DefaultTableModel feeTableModel; 
    private JTable feeStatusTable; 
    
    private DefaultTableModel suggestionTableModel; 
    
    private int editingIndex = -1; 
    private int editingStaffIndex = -1; 

    // --- GUI Components ---
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JLabel welcomeLabel;

    // --- Register Dialog Fields ---
    private JTextField nameField, parentNameField, studentMobileField, parentMobileField, roomField;
    
    // --- NEW STAFF Fields ---
    private JTextField staffNameField, staffMobileField, staffIDField;
    private JTextArea staffAddressArea; 
    private JComboBox<String> staffRoleComboBox;
    
    private JComboBox<String> courseComboBox, branchComboBox;
    
    // --- Constants ---
    private static final String MOCK_USER = "admin";
    private static final String MOCK_PASS = "1234";

    public HostelManagementSystem() {
        // FIX: Switch to Nimbus L&F for consistent custom styling
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            try {
                 UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                // Ignore
            }
        }
        
        // --- FINAL HEADER VISIBILITY FIX ---
        UIManager.put("TableHeader.background", PRIMARY_COLOR.darker());
        UIManager.put("TableHeader.opaque", Boolean.TRUE);
        // --- END FINAL FIX ---

        // Attempt to load data from MySQL first, fall back to files if DB fails/is empty
        loadAllData();

        setTitle("Hostel Management System - Data Persistent & Accounts");
        setSize(950, 700); 
        
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE); 
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveAllData(); // Save ALL data to MySQL
                dispose(); 
                System.exit(0); 
            }
        });
        
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        initializeGlobalComponents(); 
        
        mainPanel.add(loginPanel(), "Login");
        mainPanel.add(dashboardPanel(), "Dashboard");
        mainPanel.add(viewStudentsPanel(), "ViewStudents"); 
        mainPanel.add(feeManagementPanel(), "FeeManagement"); 
        mainPanel.add(staffManagementPanel(), "StaffManagement"); 
        mainPanel.add(helplinePanel(), "Helpline"); 
        mainPanel.add(reviewSuggestionsPanel(), "ReviewSuggestions"); 

        add(mainPanel);
        cardLayout.show(mainPanel, "Login");

        setVisible(true);
    }
    
    private void initializeGlobalComponents() {
        usernameField = new JTextField();
        passwordField = new JPasswordField();
        welcomeLabel = new JLabel("Welcome!", SwingConstants.CENTER);
        welcomeLabel.setFont(TITLE_FONT.deriveFont(24f));
    }
    
    // ====================================================================
    // --- JDBC PERSISTENCE METHODS ---
    // ====================================================================

    private void loadAllData() {
        try {
            loadStudentsFromDB();
            loadStaffFromDB();
            // loadSuggestionsFromDB(); // Suggestion logic is complex due to structure, omitted for brevity
        } catch (SQLException e) {
            System.err.println("Database connection or loading failed. Falling back to file storage.");
            // Fallback to old file system if DB connection fails
            loadStudentsFromFile(); 
            loadStaffFromFile(); 
            // Note: Suggestions usually don't need complex migration on startup
            loadSuggestionsFromFile();
            JOptionPane.showMessageDialog(this, "Could not connect to MySQL. Using local file storage.", "DB Error", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void saveAllData() {
        try {
            saveStudentsToDB();
            saveStaffToDB();
            // saveSuggestionsToDB();
        } catch (SQLException e) {
            System.err.println("Database saving failed. Saving to local files as backup.");
            saveStudentsToFile();
            saveStaffToFile();
            saveSuggestionsToFile();
        }
    }

    private void loadStudentsFromDB() throws SQLException {
        studentList.clear();
        String sql = "SELECT * FROM students";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                // NOTE: This assumes fee data is not yet in the DB and uses mock values
                Student student = new Student(
                    rs.getString("name"),
                    rs.getString("parent_name"),
                    rs.getString("course"),
                    rs.getString("branch"),
                    rs.getString("mobile"), // studentMobile
                    rs.getString("mobile"), // parentMobile (Simplification: uses one field)
                    rs.getString("room")
                );
                studentList.add(student);
            }
        }
    }

    private void saveStudentsToDB() throws SQLException {
        String insertSql = "INSERT INTO students (name, room, course, branch, parent_name, mobile) VALUES (?, ?, ?, ?, ?, ?)";
        String deleteSql = "DELETE FROM students"; // Simplification: Delete all then re-insert

        try (Connection conn = getConnection();
             PreparedStatement deleteStmt = conn.prepareStatement(deleteSql);
             PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {

            // 1. Clear existing data
            deleteStmt.executeUpdate();

            // 2. Insert all current data
            for (Student student : studentList) {
                insertStmt.setString(1, student.name);
                insertStmt.setString(2, student.room);
                insertStmt.setString(3, student.course);
                insertStmt.setString(4, student.branch);
                insertStmt.setString(5, student.parentName);
                insertStmt.setString(6, student.studentMobile);
                insertStmt.addBatch();
            }
            insertStmt.executeBatch();
        }
    }
    
    // Placeholder methods (you would need to implement these similarly)
    private void loadStaffFromDB() throws SQLException { staffList.clear(); }
    private void saveStaffToDB() throws SQLException { /* Implementation */ }
    // --------------------------------------------------------------------

    // --- Legacy File Persistence (Used for seamless file-to-DB migration) ---
    private void saveStudentsToFile() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(STUDENT_DATA_FILE))) {
            oos.writeObject(studentList);
        } catch (IOException e) { System.err.println("Error saving student data: " + e.getMessage()); }
    }
    
    private void loadStudentsFromFile() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(STUDENT_DATA_FILE))) {
            @SuppressWarnings("unchecked") 
            ArrayList<Student> loadedList = (ArrayList<Student>) ois.readObject();
            studentList = loadedList;
        } catch (FileNotFoundException e) { System.out.println("Student data file not found."); } 
        catch (IOException | ClassNotFoundException e) { System.err.println("Error loading student data: " + e.getMessage()); }
    }
    
    private void saveStaffToFile() { 
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(STAFF_DATA_FILE))) {
            oos.writeObject(staffList);
        } catch (IOException e) { System.err.println("Error saving staff data: " + e.getMessage()); }
    }
    
    private void loadStaffFromFile() { 
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(STAFF_DATA_FILE))) {
            @SuppressWarnings("unchecked") 
            ArrayList<Staff> loadedList = (ArrayList<Staff>) ois.readObject();
            staffList = loadedList;
        } catch (FileNotFoundException e) { System.out.println("Staff data file not found."); } 
        catch (IOException | ClassNotFoundException e) { System.err.println("Error loading staff data: " + e.getMessage()); }
    }
    
    private void saveSuggestionsToFile() { 
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(SUGGESTION_DATA_FILE))) {
            oos.writeObject(suggestionList);
        } catch (IOException e) { System.err.println("Error saving suggestion data: " + e.getMessage()); }
    }
    
    private void loadSuggestionsFromFile() { 
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(SUGGESTION_DATA_FILE))) {
            @SuppressWarnings("unchecked") 
            ArrayList<Suggestion> loadedList = (ArrayList<Suggestion>) ois.readObject();
            suggestionList = loadedList;
        } catch (FileNotFoundException e) { System.out.println("Suggestion data file not found."); } 
        catch (IOException | ClassNotFoundException e) { System.err.println("Error loading suggestion data: " + e.getMessage()); }
    }


    // --- 1. LOGIN PANEL (Unchanged) ---
    private JPanel loginPanel() {
        JPanel panel = new JPanel(new GridBagLayout()); 
        panel.setBackground(BACKGROUND_GRAY); 
        
        JPanel loginCard = new JPanel(new BorderLayout(20, 20));
        loginCard.setPreferredSize(new Dimension(400, 300));
        loginCard.setBackground(CARD_BACKGROUND); 
        loginCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(PRIMARY_COLOR.darker(), 2),
            BorderFactory.createEmptyBorder(30, 40, 30, 40)
        ));

        JLabel title = new JLabel("Hostel Management Login", SwingConstants.CENTER);
        title.setFont(TITLE_FONT);
        title.setForeground(PRIMARY_COLOR);
        loginCard.add(title, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridLayout(3, 2, 10, 15));
        formPanel.setBackground(CARD_BACKGROUND); 

        JLabel userLabel = new JLabel("Username:");
        userLabel.setFont(LABEL_FONT);
        
        usernameField.setFont(LABEL_FONT);
        usernameField.setForeground(Color.BLACK); 
        
        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(LABEL_FONT);
        
        passwordField.setFont(LABEL_FONT);
        passwordField.setForeground(Color.BLACK); 
        
        formPanel.add(userLabel);
        formPanel.add(usernameField);
        formPanel.add(passLabel);
        formPanel.add(passwordField);

        loginCard.add(formPanel, BorderLayout.CENTER);

        JButton okBtn = new JButton("LOGIN"); 
        okBtn.setFont(BUTTON_FONT);
        okBtn.setBackground(ACCENT_COLOR); 
        okBtn.setForeground(Color.WHITE);
        okBtn.setOpaque(true); 
        okBtn.setBorderPainted(false);
        okBtn.setFocusPainted(false);
        okBtn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        okBtn.putClientProperty("JButton.buttonType", "roundRect"); 
        
        Action loginAction = new AbstractAction("Login") {
            @Override
            public void actionPerformed(ActionEvent e) {
                attemptLogin();
            }
        };
        okBtn.addActionListener(loginAction);
        passwordField.addActionListener(loginAction);
        
        loginCard.add(okBtn, BorderLayout.SOUTH);
        
        panel.add(loginCard); 
        return panel;
    }

    private void attemptLogin() {
        String user = usernameField.getText();
        String pass = new String(passwordField.getPassword());
        
        if (user.equals(MOCK_USER) && pass.equals(MOCK_PASS)) {
            welcomeLabel.setText("Welcome, " + user + "!");
            cardLayout.show(mainPanel, "Dashboard");
            passwordField.setText("");
        } else {
            JOptionPane.showMessageDialog(this, "Invalid Username or Password", "Login Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // --- 2. DASHBOARD PANEL (Unchanged) ---
    private JPanel dashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setBackground(BACKGROUND_GRAY); 
        panel.setBorder(BorderFactory.createEmptyBorder(40, 50, 40, 50));

        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        headerPanel.setBackground(BACKGROUND_GRAY); 
        headerPanel.add(welcomeLabel);
        panel.add(headerPanel, BorderLayout.NORTH);

        // Grid size adjusted to fit the new button (6 rows)
        JPanel buttonGrid = new JPanel(new GridLayout(6, 1, 0, 20)); 
        buttonGrid.setBorder(BorderFactory.createEmptyBorder(10, 150, 10, 150));
        buttonGrid.setBackground(BACKGROUND_GRAY); 

        JButton regBtn = createDashboardButton("➕ Register New Student", PRIMARY_COLOR, e -> registerStudent(null));
        JButton viewBtn = createDashboardButton("📋 View/Manage Students", PRIMARY_COLOR, e -> showViewStudentsPanel());
        JButton staffBtn = createDashboardButton("🧑‍💼 Staff Directory & Management", PRIMARY_COLOR, e -> showStaffManagementPanel()); 
        JButton feeBtn = createDashboardButton("💲 Accounts & Fee Management", PRIMARY_COLOR, e -> showFeeManagementPanel());
        
        // NEW HELPLINE BUTTON
        JButton helplineBtn = createDashboardButton("☎️ Helpline & Suggestion Box", new Color(0, 150, 136), e -> cardLayout.show(mainPanel, "Helpline")); 
        
        JButton logoutBtn = new JButton("👋 Logout");
        styleButton(logoutBtn, DELETE_COLOR, Color.WHITE);
        logoutBtn.addActionListener(e -> {
            saveAllData(); // Save ALL data
            usernameField.setText("");
            passwordField.setText("");
            cardLayout.show(mainPanel, "Login");
        });

        buttonGrid.add(regBtn);
        buttonGrid.add(viewBtn);
        buttonGrid.add(staffBtn); 
        buttonGrid.add(feeBtn);
        buttonGrid.add(helplineBtn); 
        buttonGrid.add(logoutBtn);

        panel.add(buttonGrid, BorderLayout.CENTER);
        return panel;
    }

    // --- Helper function for styling buttons (Unchanged) ---
    private JButton createDashboardButton(String text, Color bgColor, ActionListener listener) {
        JButton button = new JButton(text);
        styleButton(button, bgColor, Color.WHITE);
        button.addActionListener(listener);
        return button;
    }

    private void styleButton(JButton button, Color bgColor, Color fgColor) {
        button.setFont(BUTTON_FONT.deriveFont(16f));
        button.setBackground(bgColor);
        button.setForeground(fgColor);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(15, 30, 15, 30));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);
        button.setBorderPainted(false);
    }

    // --- 3. STUDENT REGISTRATION METHOD (Unchanged Logic) ---
    private void registerStudent(Student studentToEdit) {
        nameField = new JTextField(20);
        parentNameField = new JTextField(20);
        studentMobileField = new JTextField(20);
        parentMobileField = new JTextField(20);
        roomField = new JTextField(5);
        
        String[] courses = {"B.Tech", "M.Tech", "BCA", "MCA", "B.A.", "M.Sc."};
        String[] branches = {"CS", "IT", "EEE", "Mech", "Civil", "Arts", "Science"};

        courseComboBox = new JComboBox<>(courses);
        branchComboBox = new JComboBox<>(branches);
        
        String dialogTitle = "Register New Student";
        
        if (studentToEdit != null) {
            dialogTitle = "Edit Student Record";
            editingIndex = studentList.indexOf(studentToEdit);
            
            nameField.setText(studentToEdit.name);
            parentNameField.setText(studentToEdit.parentName);
            studentMobileField.setText(studentToEdit.studentMobile);
            parentMobileField.setText(studentToEdit.parentMobile);
            roomField.setText(studentToEdit.room);
            courseComboBox.setSelectedItem(studentToEdit.course);
            branchComboBox.setSelectedItem(studentToEdit.branch);
        } else {
            editingIndex = -1; 
        }

        JPanel regPanel = new JPanel();
        regPanel.setLayout(new BoxLayout(regPanel, BoxLayout.Y_AXIS));
        
        JPanel gridPanel = new JPanel(new GridLayout(6, 2, 10, 5));

        gridPanel.add(new JLabel("Student Name:"));
        gridPanel.add(nameField);
        gridPanel.add(new JLabel("Parent Name:"));
        gridPanel.add(parentNameField);
        gridPanel.add(new JLabel("Course:"));
        gridPanel.add(courseComboBox);
        gridPanel.add(new JLabel("Branch:"));
        gridPanel.add(branchComboBox);
        gridPanel.add(new JLabel("Student Mobile:"));
        gridPanel.add(studentMobileField);
        gridPanel.add(new JLabel("Parent Mobile:"));
        gridPanel.add(parentMobileField);
        
        gridPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); 
        
        JPanel roomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        roomPanel.add(new JLabel("Room Number:"));
        roomPanel.add(roomField);

        regPanel.add(gridPanel);
        regPanel.add(roomPanel);

        int result = JOptionPane.showConfirmDialog(this, regPanel, dialogTitle,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        
        if (result == JOptionPane.OK_OPTION) {
            String name = nameField.getText().trim();
            String parentName = parentNameField.getText().trim();
            String course = (String) courseComboBox.getSelectedItem();
            String branch = (String) branchComboBox.getSelectedItem();
            String studentMobile = studentMobileField.getText().trim();
            String parentMobile = parentMobileField.getText().trim();
            String room = roomField.getText().trim();
            
            if (name.isEmpty() || parentName.isEmpty() || studentMobile.isEmpty() || parentMobile.isEmpty() || room.isEmpty()) {
                 JOptionPane.showMessageDialog(this, "All fields are required!", "Error", JOptionPane.ERROR_MESSAGE);
                 return;
            }
            if (!studentMobile.matches("\\d+") || !parentMobile.matches("\\d+") || !room.matches("\\d+")) { 
                 JOptionPane.showMessageDialog(this, "Mobile Number and Room Number must be numeric!", "Error", JOptionPane.ERROR_MESSAGE);
                 return;
            }
            
            Student newOrUpdatedStudent;

            if (editingIndex != -1) {
                newOrUpdatedStudent = studentToEdit; 
                newOrUpdatedStudent.name = name;
                newOrUpdatedStudent.parentName = parentName;
                newOrUpdatedStudent.course = course;
                newOrUpdatedStudent.branch = branch;
                newOrUpdatedStudent.studentMobile = studentMobile;
                newOrUpdatedStudent.parentMobile = parentMobile;
                newOrUpdatedStudent.room = room;
                
                JOptionPane.showMessageDialog(this, "Student Updated Successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                newOrUpdatedStudent = new Student(name, parentName, course, branch, studentMobile, parentMobile, room);
                studentList.add(newOrUpdatedStudent);
                JOptionPane.showMessageDialog(this, "Student Registered Successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            }
            
            refreshStudentTable();
            saveAllData(); // Save ALL data
        }
        editingIndex = -1;
    }

    // --- 4. STUDENT MANAGEMENT PANEL ---
    private JPanel viewStudentsPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(BACKGROUND_GRAY); 
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel header = new JLabel("Registered Student Records", SwingConstants.CENTER);
        header.setFont(HEADER_FONT);
        header.setForeground(PRIMARY_COLOR.darker());
        panel.add(header, BorderLayout.NORTH);

        String[] columnNames = {"Name", "Room", "Course/Branch", "Parent", "Parent Mobile", "Student Mobile"};
        studentTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        studentTable = new JTable(studentTableModel);
        
        // --- Table Styling ---
        studentTable.getTableHeader().setFont(BUTTON_FONT.deriveFont(Font.BOLD, 14f));
        studentTable.getTableHeader().setBackground(PRIMARY_COLOR); // Deep Blue Header
        studentTable.getTableHeader().setForeground(Color.WHITE);   // White Text
        
        studentTable.setSelectionBackground(ACCENT_COLOR); 
        studentTable.setSelectionForeground(Color.WHITE);  

        // Add row striping for visibility
        studentTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                if (c instanceof JComponent) {
                    ((JComponent) c).setOpaque(true); 
                }

                c.setForeground(Color.BLACK); 

                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? TABLE_STRIPE : CARD_BACKGROUND); 
                } else {
                    c.setForeground(Color.WHITE);
                }
                return c;
            }
        });
        
        studentTable.setRowHeight(25);
        studentTable.setFont(LABEL_FONT);
        studentTable.setGridColor(BACKGROUND_GRAY.darker());
        
        JScrollPane scrollPane = new JScrollPane(studentTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(BACKGROUND_GRAY.darker(), 1));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 25, 10));
        buttonPanel.setBackground(BACKGROUND_GRAY);
        
        JButton backBtn = new JButton("<< Back to Dashboard");
        styleButton(backBtn, PRIMARY_COLOR, Color.WHITE);
        backBtn.addActionListener(e -> cardLayout.show(mainPanel, "Dashboard"));
        
        JButton editBtn = new JButton("✏️ Edit Selected");
        styleButton(editBtn, ACCENT_COLOR.darker(), Color.WHITE);
        editBtn.addActionListener(e -> handleEdit());
        
        JButton deleteBtn = new JButton("❌ Delete Selected"); 
        styleButton(deleteBtn, DELETE_COLOR, Color.WHITE);
        deleteBtn.addActionListener(e -> handleDelete());

        buttonPanel.add(backBtn);
        buttonPanel.add(editBtn);
        buttonPanel.add(deleteBtn);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }
    
    // --- 5. FEE MANAGEMENT PANEL ---
    private JPanel feeManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(BACKGROUND_GRAY);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel header = new JLabel("Hostel Fee Status and Management", SwingConstants.CENTER);
        header.setFont(HEADER_FONT);
        header.setForeground(PRIMARY_COLOR.darker());
        panel.add(header, BorderLayout.NORTH);

        String[] columnNames = {"Name", "Room", "Total Fee", "Submitted", "Balance", "Action"};
        feeTableModel = new DefaultTableModel(columnNames, 0) { 
            @Override
            public boolean isCellEditable(int row, int column) { return false; } 
        };
        feeStatusTable = new JTable(feeTableModel); 
        
        // --- Table Styling ---
        feeStatusTable.getTableHeader().setFont(BUTTON_FONT.deriveFont(Font.BOLD, 14f));
        feeStatusTable.getTableHeader().setBackground(PRIMARY_COLOR.darker()); 
        feeStatusTable.getTableHeader().setForeground(Color.WHITE);
        
        feeStatusTable.setSelectionBackground(ACCENT_COLOR); 
        feeStatusTable.setSelectionForeground(Color.WHITE);  
        
        // Add row striping for visibility and balance renderer 
        feeStatusTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                if (c instanceof JComponent) {
                    ((JComponent) c).setOpaque(true); 
                }
                
                c.setForeground(Color.BLACK); 

                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? TABLE_STRIPE : CARD_BACKGROUND);
                } else {
                    c.setForeground(Color.WHITE);
                }
                
                // Existing Balance Renderer Logic (overrides foreground only if not selected)
                if (column == 4 && value != null && !isSelected) { 
                    try {
                        String balanceText = value.toString().replace("₹ ", "");
                        double balance = Double.parseDouble(balanceText);
                        if (balance > 0.0) {
                            c.setForeground(DELETE_COLOR); // Red if balance is due
                            c.setFont(c.getFont().deriveFont(Font.BOLD));
                        } else {
                            c.setForeground(new Color(34, 139, 34)); // Green if paid
                            c.setFont(c.getFont().deriveFont(Font.PLAIN));
                        }
                    } catch (Exception e) {
                        c.setForeground(Color.BLACK);
                    }
                } else if (column == 4 && isSelected) {
                    c.setForeground(Color.WHITE);
                }
                
                return c;
            }
        });
        
        feeStatusTable.setRowHeight(25);
        feeStatusTable.setFont(LABEL_FONT);
        feeStatusTable.setGridColor(BACKGROUND_GRAY.darker());
        
        JScrollPane scrollPane = new JScrollPane(feeStatusTable); // CORRECTED TYPE
        scrollPane.setBorder(BorderFactory.createLineBorder(BACKGROUND_GRAY.darker(), 1));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.setBackground(BACKGROUND_GRAY);
        JButton backBtn = new JButton("<< Back to Dashboard");
        styleButton(backBtn, PRIMARY_COLOR, Color.WHITE);
        backBtn.addActionListener(e -> cardLayout.show(mainPanel, "Dashboard"));
        buttonPanel.add(backBtn);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        feeStatusTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int row = feeStatusTable.rowAtPoint(e.getPoint());
                
                if (row >= 0) { 
                    showPaymentDialog(studentList.get(row));
                }
            }
        });

        return panel;
    }
    
    // --- 6. STAFF MANAGEMENT PANEL ---
    private void showStaffManagementPanel() {
        refreshStaffTable();
        cardLayout.show(mainPanel, "StaffManagement");
    }
    
    private JPanel staffManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(BACKGROUND_GRAY); 
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel header = new JLabel("Hostel Staff Directory", SwingConstants.CENTER);
        header.setFont(HEADER_FONT);
        header.setForeground(PRIMARY_COLOR.darker());
        panel.add(header, BorderLayout.NORTH);

        // UPDATED: Removed "Actions" column, Added "Address" column
        String[] columnNames = {"ID", "Name", "Role", "Mobile", "Address"}; 
        staffTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        staffTable = new JTable(staffTableModel);
        
        // --- Table Styling ---
        staffTable.getTableHeader().setFont(BUTTON_FONT.deriveFont(Font.BOLD, 14f));
        staffTable.getTableHeader().setBackground(PRIMARY_COLOR); // Deep Blue Header
        staffTable.getTableHeader().setForeground(Color.WHITE);   // White Text
        
        staffTable.setSelectionBackground(ACCENT_COLOR); 
        staffTable.setSelectionForeground(Color.WHITE);  
        staffTable.setRowHeight(25);
        staffTable.setFont(LABEL_FONT);
        staffTable.setGridColor(BACKGROUND_GRAY.darker());
        
        // Staff Table Renderer (Same logic for visibility guarantee)
        staffTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                if (c instanceof JComponent) {
                    ((JComponent) c).setOpaque(true); 
                }

                c.setForeground(Color.BLACK); 

                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? TABLE_STRIPE : CARD_BACKGROUND); 
                } else {
                    c.setForeground(Color.WHITE);
                }
                return c;
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(staffTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(BACKGROUND_GRAY.darker(), 1));

        // Control Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 25, 10));
        buttonPanel.setBackground(BACKGROUND_GRAY);
        
        JButton backBtn = new JButton("<< Back to Dashboard");
        styleButton(backBtn, PRIMARY_COLOR, Color.WHITE);
        backBtn.addActionListener(e -> cardLayout.show(mainPanel, "Dashboard"));
        
        JButton addStaffBtn = new JButton("➕ Add New Staff");
        styleButton(addStaffBtn, new Color(34, 139, 34), Color.WHITE);
        addStaffBtn.addActionListener(e -> registerStaff(null)); 

        JButton editStaffBtn = new JButton("✏️ Edit Staff");
        styleButton(editStaffBtn, ACCENT_COLOR.darker(), Color.WHITE);
        editStaffBtn.addActionListener(e -> handleStaffEdit());
        
        // DELETION BUTTON
        JButton deleteStaffBtn = new JButton("❌ Delete Staff"); 
        styleButton(deleteStaffBtn, DELETE_COLOR, Color.WHITE);
        deleteStaffBtn.addActionListener(e -> handleStaffDelete()); 

        buttonPanel.add(backBtn);
        buttonPanel.add(addStaffBtn);
        buttonPanel.add(editStaffBtn);
        buttonPanel.add(deleteStaffBtn); // ADDED DELETE BUTTON

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }

    // --- 7. HELPLINE & SUGGESTION PANEL (Unchanged) ---
    private JPanel helplinePanel() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setBackground(BACKGROUND_GRAY); 
        panel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

        JLabel header = new JLabel("National Helpline & Feedback Center", SwingConstants.CENTER);
        header.setFont(TITLE_FONT.deriveFont(22f));
        header.setForeground(PRIMARY_COLOR.darker());
        panel.add(header, BorderLayout.NORTH);

        // --- CENTER: Helplines and Form Split ---
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.5);
        splitPane.setDividerLocation(0.5);
        splitPane.setBackground(BACKGROUND_GRAY);

        // Left Side: Helplines Display
        JTextArea helplineArea = new JTextArea();
        helplineArea.setEditable(false);
        helplineArea.setFont(LABEL_FONT);
        helplineArea.setMargin(new Insets(10, 10, 10, 10));
        
        String helplineText = 
            "--- STUDENT HELPLINES (India) ---\n\n" +
            "1. National Emergency Number:\n" +
            "   112 (Police, Ambulance, Fire)\n\n" +
            "2. National Mental Health Helpline (KIRAN):\n" +
            "   1800-599-0019 (Stress, Depression, Anxiety)\n\n" +
            "3. Child Helpline (POCSO/Abuse):\n" +
            "   1098 (For support and rescue)\n\n" +
            "4. Anti-Poison Information Centre:\n" +
            "   1066\n\n" +
            "5. Student Suicide Prevention (Sneha India):\n" +
            "   044-24640050 / 044-24640060";
            
        helplineArea.setText(helplineText);
        helplineArea.setBorder(BorderFactory.createTitledBorder("Important Contacts"));
        
        // Right Side: Suggestion/Complaint Form
        JPanel formPanel = new JPanel(new BorderLayout(10, 10));
        formPanel.setBorder(BorderFactory.createTitledBorder("Submit Complaint or Suggestion (Optional Sender)"));
        formPanel.setBackground(CARD_BACKGROUND);
        
        // Form Fields
        JTextField senderField = new JTextField();
        JComboBox<String> typeComboBox = new JComboBox<>(new String[]{"Suggestion", "Complaint (Anonymous)", "Maintenance Request"});
        JTextArea bodyArea = new JTextArea(8, 25);
        bodyArea.setLineWrap(true);
        bodyArea.setWrapStyleWord(true);

        JPanel inputGrid = new JPanel(new GridLayout(3, 2, 10, 10));
        inputGrid.setBackground(CARD_BACKGROUND);
        inputGrid.add(new JLabel("Your Name/Room (Optional):"));
        inputGrid.add(senderField);
        inputGrid.add(new JLabel("Type of Feedback:"));
        inputGrid.add(typeComboBox);
        inputGrid.add(new JLabel("Details:"));
        inputGrid.add(new JLabel("")); // Placeholder

        JButton submitBtn = new JButton("Submit Feedback");
        styleButton(submitBtn, PRIMARY_COLOR.darker(), Color.WHITE);
        submitBtn.setFont(BUTTON_FONT);
        
        submitBtn.addActionListener(e -> {
            String sender = senderField.getText().trim().isEmpty() ? "Anonymous" : senderField.getText().trim();
            String type = (String) typeComboBox.getSelectedItem();
            String body = bodyArea.getText().trim();

            if (body.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter details before submitting.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            suggestionList.add(new Suggestion(sender, type, body));
            saveSuggestionsToFile();
            
            JOptionPane.showMessageDialog(this, "Thank you! Your " + type + " has been recorded.", "Feedback Submitted", JOptionPane.INFORMATION_MESSAGE);
            
            // Clear form
            senderField.setText("");
            bodyArea.setText("");
        });
        
        formPanel.add(inputGrid, BorderLayout.NORTH);
        formPanel.add(new JScrollPane(bodyArea), BorderLayout.CENTER);
        formPanel.add(submitBtn, BorderLayout.SOUTH);


        splitPane.setLeftComponent(new JScrollPane(helplineArea));
        splitPane.setRightComponent(formPanel);
        panel.add(splitPane, BorderLayout.CENTER);
        
        // Bottom: Back button
        JButton backBtn = new JButton("<< Back to Dashboard");
        styleButton(backBtn, PRIMARY_COLOR, Color.WHITE);
        backBtn.addActionListener(e -> cardLayout.show(mainPanel, "Dashboard"));
        
        // ADDED REVIEW BUTTON
        JButton reviewBtn = new JButton("Review Submitted Feedback");
        styleButton(reviewBtn, ACCENT_COLOR.darker(), Color.WHITE);
        reviewBtn.addActionListener(e -> cardLayout.show(mainPanel, "ReviewSuggestions"));

        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
        southPanel.setBackground(BACKGROUND_GRAY);
        southPanel.add(reviewBtn);
        southPanel.add(backBtn);
        panel.add(southPanel, BorderLayout.SOUTH);

        return panel;
    }
    
    // --- 8. SUGGESTION REVIEW PANEL (FIXED VISIBILITY AND SCROLLING) ---
    private JPanel reviewSuggestionsPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(BACKGROUND_GRAY); 
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JLabel header = new JLabel("Review Student Feedback and Complaints", SwingConstants.CENTER);
        header.setFont(HEADER_FONT);
        header.setForeground(PRIMARY_COLOR.darker());
        panel.add(header, BorderLayout.NORTH);

        String[] columnNames = {"Timestamp", "Type", "Sender/Room", "Details"}; 
        suggestionTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        
        JTable table = new JTable(suggestionTableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setFont(BUTTON_FONT.deriveFont(Font.BOLD, 14f));
        table.getTableHeader().setBackground(PRIMARY_COLOR.darker()); 
        table.getTableHeader().setForeground(Color.WHITE);
        
        // FIX: Force Auto-Resize Off and set large width for the Detail column
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.getColumnModel().getColumn(3).setPreferredWidth(450); // Make details column wide
        
        // FIX: Increased Row Height to accommodate potential multi-line rendering
        table.setRowHeight(40); 
        
        table.setFont(LABEL_FONT);
        table.setSelectionBackground(ACCENT_COLOR);
        table.setSelectionForeground(Color.WHITE);

        // Renderer for visibility
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (c instanceof JComponent) { ((JComponent) c).setOpaque(true); }
                c.setForeground(Color.BLACK); 
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? TABLE_STRIPE : CARD_BACKGROUND);
                } else {
                    c.setForeground(Color.WHITE);
                }
                
                // Set Tooltip for full visibility of truncated text
                if (column == 3 && value != null) {
                    ((JComponent) c).setToolTipText(value.toString());
                } else {
                    ((JComponent) c).setToolTipText(null);
                }
                
                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(table); // JScrollPane adds the scrolling
        scrollPane.setBorder(BorderFactory.createLineBorder(BACKGROUND_GRAY.darker(), 1));

        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 25, 10));
        buttonPanel.setBackground(BACKGROUND_GRAY);
        
        JButton refreshBtn = new JButton("↻ Refresh / Load");
        styleButton(refreshBtn, PRIMARY_COLOR, Color.WHITE);
        refreshBtn.addActionListener(e -> refreshSuggestionsTable());

        JButton deleteSelectedBtn = new JButton("🗑️ Delete Selected Feedback");
        styleButton(deleteSelectedBtn, DELETE_COLOR, Color.WHITE);
        deleteSelectedBtn.addActionListener(e -> handleSuggestionDelete(table));

        JButton backBtn = new JButton("<< Back to Helpline");
        styleButton(backBtn, PRIMARY_COLOR, Color.WHITE);
        backBtn.addActionListener(e -> cardLayout.show(mainPanel, "Helpline"));

        buttonPanel.add(refreshBtn);
        buttonPanel.add(deleteSelectedBtn);
        buttonPanel.add(backBtn);

        panel.add(scrollPane, BorderLayout.CENTER); // ADDED SCROLLPANE HERE
        panel.add(buttonPanel, BorderLayout.SOUTH);

        refreshSuggestionsTable(); // Initial load
        return panel;
    }
    
    private void refreshSuggestionsTable() {
        if (suggestionTableModel != null) {
            suggestionTableModel.setRowCount(0);
            
            // Ensure data is current
            loadSuggestionsFromFile(); 

            for (Suggestion s : suggestionList) {
                Object[] rowData = {
                    s.timestamp, 
                    s.type,
                    s.sender,
                    s.body
                };
                suggestionTableModel.addRow(rowData);
            }
        }
    }
    
    private void handleSuggestionDelete(JTable table) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a feedback entry to delete.", "Selection Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        Suggestion suggestionToDelete = suggestionList.get(selectedRow);
        
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Delete entry by " + suggestionToDelete.sender + " (" + suggestionToDelete.type + ")?", 
            "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            
        if (confirm == JOptionPane.YES_OPTION) {
            suggestionList.remove(selectedRow);
            saveSuggestionsToFile(); // Save immediately after removing from list
            refreshSuggestionsTable(); // Refresh the table from the updated list
            JOptionPane.showMessageDialog(this, "Feedback deleted.", "Deletion Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    // --- NEW STAFF CRUD METHODS ---
    
    private void registerStaff(Staff staffToEdit) {
        staffNameField = new JTextField(20);
        staffMobileField = new JTextField(20);
        staffIDField = new JTextField(10);
        staffAddressArea = new JTextArea(3, 20); // Initialize new address input
        
        String[] roles = {"Housekeeping", "Security", "Maintenance", "Office Staff"};
        staffRoleComboBox = new JComboBox<>(roles);
        
        String dialogTitle = "Register New Staff Member";
        boolean isEditing = (staffToEdit != null);
        
        if (isEditing) {
            dialogTitle = "Edit Staff Record";
            editingStaffIndex = staffList.indexOf(staffToEdit);
            staffNameField.setText(staffToEdit.name);
            staffMobileField.setText(staffToEdit.mobile);
            staffIDField.setText(staffToEdit.id);
            staffIDField.setEditable(false); 
            staffRoleComboBox.setSelectedItem(staffToEdit.role);
            staffAddressArea.setText(staffToEdit.address); // Load address
        } else {
            editingStaffIndex = -1;
        }

        JPanel regPanel = new JPanel();
        regPanel.setLayout(new BoxLayout(regPanel, BoxLayout.Y_AXIS));

        JPanel gridPanel = new JPanel(new GridLayout(4, 2, 10, 10));

        gridPanel.add(new JLabel("Staff Name:"));
        gridPanel.add(staffNameField);
        gridPanel.add(new JLabel("Staff ID (Unique):"));
        gridPanel.add(staffIDField);
        gridPanel.add(new JLabel("Role:"));
        gridPanel.add(staffRoleComboBox);
        gridPanel.add(new JLabel("Mobile Number:"));
        gridPanel.add(staffMobileField);

        JPanel addressPanel = new JPanel(new BorderLayout(5, 5));
        addressPanel.setBorder(BorderFactory.createTitledBorder("Address"));
        addressPanel.add(new JScrollPane(staffAddressArea), BorderLayout.CENTER);

        regPanel.add(gridPanel);
        regPanel.add(addressPanel);

        int result = JOptionPane.showConfirmDialog(this, regPanel, dialogTitle,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        
        if (result == JOptionPane.OK_OPTION) {
            String name = staffNameField.getText().trim();
            String id = staffIDField.getText().trim();
            String mobile = staffMobileField.getText().trim();
            String role = (String) staffRoleComboBox.getSelectedItem();
            String address = staffAddressArea.getText().trim(); // Capture new address

            if (name.isEmpty() || id.isEmpty() || mobile.isEmpty() || address.isEmpty()) { // Added address validation
                 JOptionPane.showMessageDialog(this, "All fields are required!", "Error", JOptionPane.ERROR_MESSAGE);
                 return;
            }
            if (!mobile.matches("\\d+")) { 
                 JOptionPane.showMessageDialog(this, "Mobile Number must be numeric!", "Error", JOptionPane.ERROR_MESSAGE);
                 return;
            }
            
            if (!isEditing && staffList.stream().anyMatch(s -> s.id.equals(id))) {
                 JOptionPane.showMessageDialog(this, "Staff ID must be unique!", "Error", JOptionPane.ERROR_MESSAGE);
                 return;
            }
            
            Staff newOrUpdatedStaff = new Staff(id, name, role, mobile, address); // Pass address to constructor

            if (isEditing) {
                // Update properties of the existing object to preserve it in the list
                staffToEdit.name = name;
                staffToEdit.role = role;
                staffToEdit.mobile = mobile;
                staffToEdit.address = address; 
                JOptionPane.showMessageDialog(this, "Staff Updated Successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                staffList.add(newOrUpdatedStaff);
                JOptionPane.showMessageDialog(this, "Staff Registered Successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            }
            
            refreshStaffTable();
            saveStaffToFile(); 
        }
        editingStaffIndex = -1;
    }
    
    private void refreshStaffTable() {
        if (staffTableModel != null) {
             staffTableModel.setRowCount(0);

            for (Staff s : staffList) {
                Object[] rowData = {
                    s.id, 
                    s.name, 
                    s.role, 
                    s.mobile,
                    s.address // Display the address
                };
                staffTableModel.addRow(rowData);
            }
        }
    }
    
    private void handleStaffEdit() {
        int selectedRow = staffTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a staff record to edit.", "Selection Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        Staff staffToEdit = staffList.get(selectedRow);
        registerStaff(staffToEdit); 
    }
    
    private void handleStaffDelete() {
        int selectedRow = staffTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a staff record to delete.", "Selection Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        Staff staffToDelete = staffList.get(selectedRow);
        
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to delete the record for staff member " + staffToDelete.name + " (" + staffToDelete.role + ")?", 
            "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            
        if (confirm == JOptionPane.YES_OPTION) {
            staffList.remove(selectedRow);
            refreshStaffTable();
            saveStaffToFile(); 
            JOptionPane.showMessageDialog(this, "Staff record deleted successfully.", "Deletion Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // --- Helper Methods (Student & Fee Management) ---

    private void showFeeManagementPanel() {
        refreshFeeTable();
        cardLayout.show(mainPanel, "FeeManagement");
    }

    private void refreshFeeTable() {
        if (feeStatusTable != null) {
            DefaultTableModel feeTableModel = (DefaultTableModel) feeStatusTable.getModel();
            feeTableModel.setRowCount(0);

            for (Student s : studentList) {
                Object[] rowData = {
                    s.name,
                    s.room,
                    String.format("₹ %.2f", s.totalHostelFee),
                    String.format("₹ %.2f", s.getFeeSubmitted()),
                    String.format("₹ %.2f", s.getFeeBalance()),
                    "Add Payment / View Details" 
                };
                feeTableModel.addRow(rowData);
            }
        }
    }
    
    private void showViewStudentsPanel() {
        refreshStudentTable();
        cardLayout.show(mainPanel, "ViewStudents");
    }

    private void refreshStudentTable() {
        if (studentTableModel != null) {
            studentTableModel.setRowCount(0); 

            for (Student s : studentList) {
                Object[] rowData = {
                    s.name, 
                    s.room, 
                    s.course + " / " + s.branch,
                    s.parentName,
                    s.parentMobile,
                    s.studentMobile
                };
                studentTableModel.addRow(rowData);
            }
        }
    }
    
    private void handleEdit() {
        int selectedRow = studentTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a student record to edit.", "Selection Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        Student studentToEdit = studentList.get(selectedRow);
        registerStudent(studentToEdit); 
    }
    
    private void handleDelete() {
        int selectedRow = studentTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a student record to delete.", "Selection Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        Student studentToDelete = studentList.get(selectedRow);
        
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to delete the record for " + studentToDelete.name + " (Room " + studentToDelete.room + ")?", 
            "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            
        if (confirm == JOptionPane.YES_OPTION) {
            studentList.remove(selectedRow);
            refreshStudentTable();
            saveStudentsToFile(); 
            JOptionPane.showMessageDialog(this, "Record deleted successfully.", "Deletion Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void allocateRoom() {
        if (studentList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No students available for room allocation.", "Allocation Status", JOptionPane.WARNING_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, 
                studentList.size() + " Rooms successfully allocated to all registered students.", 
                "Allocation Complete", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void showPaymentDialog(Student student) {
        JDialog paymentDialog = new JDialog(this, "Manage Fees for " + student.name, true);
        paymentDialog.setSize(450, 450);
        paymentDialog.setLayout(new BorderLayout(10, 10));
        paymentDialog.setLocationRelativeTo(this);
        
        // Summary Panel
        JPanel summaryPanel = new JPanel(new GridLayout(3, 2));
        summaryPanel.setBorder(BorderFactory.createTitledBorder("Current Status"));
        
        JLabel balanceLabel = new JLabel("Balance Due:");
        balanceLabel.setFont(BUTTON_FONT.deriveFont(Font.BOLD));
        
        JLabel balanceValueLabel = new JLabel(String.format("₹ %.2f", student.getFeeBalance()));
        balanceValueLabel.setFont(BUTTON_FONT.deriveFont(Font.BOLD));
        
        summaryPanel.add(new JLabel("Total Fee:"));
        summaryPanel.add(new JLabel(String.format("₹ %.2f", student.totalHostelFee)));
        summaryPanel.add(new JLabel("Submitted:"));
        summaryPanel.add(new JLabel(String.format("₹ %.2f", student.getFeeSubmitted())));
        summaryPanel.add(balanceLabel);
        summaryPanel.add(balanceValueLabel);
        
        // Payment Entry Panel (Unchanged)
        JTextField amountField = new JTextField(10);
        JTextField dateField = new JTextField(java.time.LocalDate.now().toString());
        JTextField notesField = new JTextField(15);
        
        JPanel entryPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        entryPanel.setBorder(BorderFactory.createTitledBorder("Add New Payment"));
        entryPanel.add(new JLabel("Amount (₹):"));
        entryPanel.add(amountField);
        entryPanel.add(new JLabel("Date (YYYY-MM-DD):"));
        entryPanel.add(dateField);
        entryPanel.add(new JLabel("Notes:"));
        entryPanel.add(notesField);
        
        JButton saveBtn = new JButton("Record Payment");
        styleButton(saveBtn, new Color(34, 139, 34), Color.WHITE); // Dark Green Save button
        
        saveBtn.addActionListener(e -> {
            try {
                double amount = Double.parseDouble(amountField.getText());
                String date = dateField.getText();
                String notes = notesField.getText();

                if (amount <= 0) throw new IllegalArgumentException("Amount must be positive.");
                if (amount > student.getFeeBalance()) throw new IllegalArgumentException("Payment exceeds remaining balance.");
                
                student.payments.add(new FeeRecord(date, amount, notes));
                saveStudentsToFile(); 
                
                JOptionPane.showMessageDialog(paymentDialog, "Payment recorded successfully.");
                refreshFeeTable();
                paymentDialog.dispose();
                
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(paymentDialog, "Invalid numeric amount entered.", "Error", JOptionPane.ERROR_MESSAGE);
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(paymentDialog, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // View Payments Panel (for demonstration)
        JTextArea historyArea = new JTextArea();
        historyArea.append("--- Payment History ---\n");
        for (FeeRecord fr : student.payments) {
            historyArea.append(String.format("%s: ₹ %.2f (%s)\n", fr.date, fr.amount, fr.notes));
        }
        historyArea.setEditable(false);
        
        paymentDialog.add(summaryPanel, BorderLayout.NORTH);
        
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(new JScrollPane(historyArea), BorderLayout.CENTER);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

        paymentDialog.add(centerPanel, BorderLayout.CENTER);
        
        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(entryPanel, BorderLayout.CENTER);
        southPanel.add(saveBtn, BorderLayout.SOUTH);
        southPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        paymentDialog.add(southPanel, BorderLayout.SOUTH);

        paymentDialog.setVisible(true);
    }

    // --- Data Model Classes (Unchanged) ---
    private static class FeeRecord implements Serializable {
        private static final long serialVersionUID = 2L;
        String date;
        double amount;
        String notes;

        public FeeRecord(String date, double amount, String notes) {
            this.date = date;
            this.amount = amount;
            this.notes = notes;
        }
    }
    
    private static class Staff implements Serializable { // UPDATED STAFF MODEL
        private static final long serialVersionUID = 3L;
        String id;
        String name;
        String role;
        String mobile;
        String address; // NEW ADDRESS FIELD

        public Staff(String id, String name, String role, String mobile, String address) {
            this.id = id;
            this.name = name;
            this.role = role;
            this.mobile = mobile;
            this.address = address;
        }
    }


    private static class Student implements Serializable { 
        private static final long serialVersionUID = 1L; 
        
        String name;
        String parentName;
        String course;
        String branch;
        String studentMobile;
        String parentMobile;
        String room;

        double totalHostelFee; 
        ArrayList<FeeRecord> payments;
        
        public Student(String name, String parentName, String course, String branch, String studentMobile, String parentMobile, String room) {
            this.name = name;
            this.parentName = parentName;
            this.course = course;
            this.branch = branch;
            this.studentMobile = studentMobile;
            this.parentMobile = parentMobile;
            this.room = room;
            
            this.payments = new ArrayList<>(); 
            this.totalHostelFee = 50000.00;
        }
        
        public double getFeeSubmitted() {
            if (payments == null) {
                return 0.0;
            }
            return payments.stream().mapToDouble(p -> p.amount).sum();
        }
        
        public double getFeeBalance() {
            return totalHostelFee - getFeeSubmitted();
        }
    }
    
    private static class Suggestion implements Serializable { // NEW SUGGESTION MODEL
        private static final long serialVersionUID = 4L;
        String sender;
        String type; // Complaint or Suggestion
        String body;
        String timestamp;

        public Suggestion(String sender, String type, String body) {
            this.sender = sender;
            this.type = type;
            this.body = body;
            this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(HostelManagementSystem::new);
    }
}