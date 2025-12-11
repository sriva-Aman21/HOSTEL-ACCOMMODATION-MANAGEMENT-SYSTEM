import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout; // <-- FIXED: Added missing import
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class HostelManagementSystem extends JFrame {
    
    // ====================================================================
    // --- DATABASE CONFIGURATION (MongoDB) ---
    // Change these values to match your MongoDB server settings
    private static final String MONGO_URI = "mongodb://localhost:27017";
    private static final String DB_NAME = "HostelManagementDB";
    private static final String STUDENTS_COLLECTION = "students";
    private static final String STAFF_COLLECTION = "staff";
    private static final String SUGGESTIONS_COLLECTION = "suggestions";
    private static final String FEES_COLLECTION = "fees";
    
    private MongoClient mongoClient;
    private MongoDatabase database;
    
    // Helper method to initialize MongoDB connection
    private void initializeMongoDB() {
        try {
            mongoClient = MongoClients.create(MONGO_URI);
            database = mongoClient.getDatabase(DB_NAME);
            System.out.println("Connected to MongoDB successfully.");
        } catch (Exception e) {
            System.err.println("MongoDB connection failed. The application will use local file storage.");
            JOptionPane.showMessageDialog(this, 
                "MongoDB connection failed. The application will use local file storage.", 
                "Database Connection Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private MongoCollection<Document> getStudentsCollection() {
        return database.getCollection(STUDENTS_COLLECTION);
    }
    
    private MongoCollection<Document> getStaffCollection() {
        return database.getCollection(STAFF_COLLECTION);
    }
    
    private MongoCollection<Document> getSuggestionsCollection() {
        return database.getCollection(SUGGESTIONS_COLLECTION);
    }
    
    private MongoCollection<Document> getFeesCollection() {
        return database.getCollection(FEES_COLLECTION);
    }
    // ====================================================================
    
    // --- COLOR PALETTE (Final Optimized) ---
    private static final Color PRIMARY_COLOR = new Color(0, 102, 204);      
    private static final Color ACCENT_COLOR = new Color(10, 100, 10);       // Dark Green for selection/accents
    private static final Color DELETE_COLOR = new Color(220, 50, 50);       
    private static final Color BACKGROUND_GRAY = new Color(200, 200, 210); 
    
    public static final Color CARD_BACKGROUND = new Color(255, 255, 255); // White
    public static final Color TABLE_STRIPE = new Color(215, 215, 215);    // Visible Gray stripe
    private static final Color STATUS_RESOLVED_COLOR = new Color(34, 139, 34); // Forest Green
    private static final Color STATUS_PENDING_COLOR = new Color(255, 140, 0); // Dark Orange

    // --- FONT STYLES (Unchanged) ---
    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 28);
    private static final Font HEADER_FONT = new Font("Segoe UI", Font.BOLD, 18);
    private static final Font LABEL_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font BUTTON_FONT = new Font("Segoe UI", Font.BOLD, 14);

    // --- Data Model & File Constant ---
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
    
    // --- ENHANCEMENT FIELDS ---
    private JTextField studentSearchField; 

    // --- GUI Components ---
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JLabel welcomeLabel;

    // --- Register Dialog Fields ---
    private JTextField nameField, parentNameField, studentMobileField, parentMobileField, roomField;
    
    // --- STAFF Fields ---
    private JTextField staffNameField, staffMobileField, staffIDField;
    private JTextArea staffAddressArea; 
    private JComboBox<String> staffRoleComboBox;
    
    private JComboBox<String> courseComboBox, branchComboBox;
    
    // --- Constants ---
    private static final String MOCK_USER = "admin";
    private static final String MOCK_PASS = "1234";

    public HostelManagementSystem() {
        // L&F Setup
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
            } catch (Exception ex) { }
        }
        
        UIManager.put("TableHeader.background", PRIMARY_COLOR.darker());
        UIManager.put("TableHeader.opaque", Boolean.TRUE);

        // Initialize MongoDB connection
        initializeMongoDB();
        
        loadAllData();

        setTitle("Hostel Management System - Data Persistent & Accounts");
        setSize(1000, 750); // Increased size for new panels
        
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE); 
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveAllData();
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
        mainPanel.add(viewStudentFeedbackPanel(), "StudentFeedbackStatus"); // NEW PANEL

        add(mainPanel);
        cardLayout.show(mainPanel, "Login");

        setVisible(true);
    }
    
    private void initializeGlobalComponents() {
        usernameField = new JTextField();
        passwordField = new JPasswordField();
        welcomeLabel = new JLabel("Welcome!", SwingConstants.CENTER);
        welcomeLabel.setFont(TITLE_FONT.deriveFont(24f));
        studentSearchField = new JTextField(15); 
    }

    // ====================================================================
    // --- JDBC PERSISTENCE CORE LOGIC ---
    // ====================================================================

    private void loadAllData() {
        try {
            if (database != null) {
                loadStudentsFromDB();
                loadStaffFromDB();
                loadSuggestionsFromDB(); 
                System.out.println("Data loaded successfully from MongoDB.");
            } else {
                throw new Exception("MongoDB not initialized");
            }
        } catch (Exception e) {
            System.err.println("MongoDB connection or loading failed. Falling back to file storage. Error: " + e.getMessage());
            loadStudentsFromFile(); 
            loadStaffFromFile(); 
            loadSuggestionsFromFile();
        }
    }

    private void saveAllData() {
        try {
            if (database != null) {
                try {
                    saveStudentsToDB();
                    System.out.println("✓ Students saved to MongoDB.");
                } catch (Exception e) {
                    System.err.println("✗ Error saving students: " + e.getMessage());
                    e.printStackTrace();
                }
                
                try {
                    saveStaffToDB();
                    System.out.println("✓ Staff saved to MongoDB.");
                } catch (Exception e) {
                    System.err.println("✗ Error saving staff: " + e.getMessage());
                    e.printStackTrace();
                }
                
                try {
                    saveSuggestionsToDB();
                    System.out.println("✓ Suggestions saved to MongoDB.");
                } catch (Exception e) {
                    System.err.println("✗ Error saving suggestions: " + e.getMessage());
                    e.printStackTrace();
                }
                
                try {
                    saveFeesToDB();
                    System.out.println("✓ Fees saved to MongoDB.");
                } catch (Exception e) {
                    System.err.println("✗ Error saving fees: " + e.getMessage());
                    e.printStackTrace();
                }
                System.out.println("Data saved successfully to MongoDB.");
            } else {
                throw new Exception("MongoDB not initialized");
            }
        } catch (Exception e) {
            System.err.println("MongoDB saving failed. Saving to local files as backup. Error: " + e.getMessage());
            saveStudentsToFile();
            saveStaffToFile();
            saveSuggestionsToFile();
        }
    }

    private void loadStudentsFromDB() {
        studentList.clear();
        try {
            MongoCollection<Document> collection = getStudentsCollection();
            for (Document doc : collection.find()) {
                try {
                    byte[] data = doc.get("serialized_data", org.bson.types.Binary.class).getData();
                    if (data != null) {
                        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
                             ObjectInputStream ois = new ObjectInputStream(bis)) {
                            Student student = (Student) ois.readObject();
                            studentList.add(student);
                        }
                    }
                } catch (IOException | ClassNotFoundException e) {
                    System.err.println("Error deserializing student data: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading students from MongoDB: " + e.getMessage());
        }
    }

    private void saveStudentsToDB() {
        try {
            MongoCollection<Document> collection = getStudentsCollection();
            collection.deleteMany(Filters.empty());
            
            for (Student student : studentList) {
                try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                     ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                    oos.writeObject(student);
                    byte[] data = bos.toByteArray();
                    
                    Document doc = new Document()
                        .append("name", student.name)
                        .append("room", student.room)
                        .append("course", student.course)
                        .append("branch", student.branch)
                        .append("parentName", student.parentName)
                        .append("studentMobile", student.studentMobile)
                        .append("parentMobile", student.parentMobile)
                        .append("serialized_data", data);
                    
                    collection.insertOne(doc);
                } catch (IOException e) {
                    System.err.println("Error serializing student: " + student.name);
                }
            }
        } catch (Exception e) {
            System.err.println("Error saving students to MongoDB: " + e.getMessage());
        }
    }
    
    private void loadStaffFromDB() {
        staffList.clear();
        try {
            if (database == null) {
                System.err.println("Database is null, cannot load staff from MongoDB");
                return;
            }
            MongoCollection<Document> collection = getStaffCollection();
            int count = 0;
            for (Document doc : collection.find()) {
                Staff staff = new Staff(
                    doc.getString("id"),
                    doc.getString("name"),
                    doc.getString("role"),
                    doc.getString("mobile"),
                    doc.getString("address")
                );
                staffList.add(staff);
                count++;
            }
            System.out.println("Loaded " + count + " staff members from MongoDB.");
        } catch (Exception e) {
            System.err.println("Error loading staff from MongoDB: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void saveStaffToDB() {
        try {
            MongoCollection<Document> collection = getStaffCollection();
            collection.deleteMany(Filters.empty());
            
            for (Staff staff : staffList) {
                Document doc = new Document()
                    .append("id", staff.id)
                    .append("name", staff.name)
                    .append("role", staff.role)
                    .append("mobile", staff.mobile)
                    .append("address", staff.address);
                
                collection.insertOne(doc);
            }
            System.out.println("Staff saved to MongoDB: " + staffList.size() + " records.");
        } catch (Exception e) {
            System.err.println("Error saving staff to MongoDB: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // --- UPDATED SUGGESTION JDBC LOGIC ---
    private void loadSuggestionsFromDB() {
        suggestionList.clear();
        try {
            if (database == null) {
                System.err.println("Database is null, cannot load suggestions from MongoDB");
                return;
            }
            MongoCollection<Document> collection = getSuggestionsCollection();
            int count = 0;
            for (Document doc : collection.find()) {
                Suggestion suggestion = new Suggestion(
                    doc.getString("sender"),
                    doc.getString("type"),
                    doc.getString("body")
                );
                suggestion.timestamp = doc.getString("timestamp");
                suggestion.status = doc.getString("status");
                suggestion.adminResponse = doc.getString("adminResponse");
                suggestionList.add(suggestion);
                count++;
            }
            System.out.println("Loaded " + count + " suggestions from MongoDB.");
        } catch (Exception e) {
            System.err.println("Error loading suggestions from MongoDB: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void saveSuggestionsToDB() {
        try {
            MongoCollection<Document> collection = getSuggestionsCollection();
            collection.deleteMany(Filters.empty());
            
            for (Suggestion s : suggestionList) {
                Document doc = new Document()
                    .append("timestamp", s.timestamp)
                    .append("sender", s.sender)
                    .append("type", s.type)
                    .append("body", s.body)
                    .append("status", s.status)
                    .append("adminResponse", s.adminResponse);
                
                collection.insertOne(doc);
            }
            System.out.println("Suggestions saved to MongoDB: " + suggestionList.size() + " records.");
        } catch (Exception e) {
            System.err.println("Error saving suggestions to MongoDB: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void saveFeesToDB() {
        try {
            MongoCollection<Document> collection = getFeesCollection();
            collection.deleteMany(Filters.empty());
            
            int totalFeeRecords = 0;
            for (Student student : studentList) {
                for (Object paymentObj : student.payments) {
                    if (paymentObj instanceof FeeRecord) {
                        FeeRecord fee = (FeeRecord) paymentObj;
                        Document doc = new Document()
                            .append("studentName", student.name)
                            .append("studentRoom", student.room)
                            .append("date", fee.date)
                            .append("amount", fee.amount)
                            .append("notes", fee.notes);
                        
                        collection.insertOne(doc);
                        totalFeeRecords++;
                    }
                }
            }
            System.out.println("Fees saved to MongoDB: " + totalFeeRecords + " payment records.");
        } catch (Exception e) {
            System.err.println("Error saving fees to MongoDB: " + e.getMessage());
            e.printStackTrace();
        }
    }
    // --------------------------------------------------------------------

    // --- Legacy File Persistence (Retained for migration fallback) ---
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
        } catch (FileNotFoundException e) { System.out.println("Student data file not found. Starting fresh."); } 
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
        } catch (FileNotFoundException e) { System.out.println("Staff data file not found. Starting fresh."); } 
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
        } catch (FileNotFoundException e) { System.out.println("Suggestion data file not found. Starting fresh."); } 
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
        styleButton(okBtn, ACCENT_COLOR, Color.WHITE);
        
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
            
            // REMOVED checkAndDisplayAlerts() call as requested
            
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
            saveAllData(); 
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
        // ... (Registration logic unchanged)
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
            saveAllData(); 
        }
        editingIndex = -1;
    }

    // --- 4. STUDENT MANAGEMENT PANEL (Enhanced with Search Filter) ---
    private JPanel viewStudentsPanel() {
        // ... (Panel setup unchanged)
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(BACKGROUND_GRAY); 
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel header = new JLabel("Registered Student Records", SwingConstants.CENTER);
        header.setFont(HEADER_FONT);
        header.setForeground(PRIMARY_COLOR.darker());
        panel.add(header, BorderLayout.NORTH);
        
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.setBackground(BACKGROUND_GRAY);
        
        JLabel searchLabel = new JLabel("Search by Name or Room:");
        searchLabel.setFont(LABEL_FONT);
        
        studentSearchField.setFont(LABEL_FONT);
        studentSearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void changedUpdate(DocumentEvent e) { filterStudents(); }
            @Override
            public void removeUpdate(DocumentEvent e) { filterStudents(); }
            @Override
            public void insertUpdate(DocumentEvent e) { filterStudents(); }
        });
        
        searchPanel.add(searchLabel);
        searchPanel.add(studentSearchField);

        String[] columnNames = {"Name", "Room", "Course/Branch", "Parent", "Parent Mobile", "Student Mobile"};
        studentTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        studentTable = new JTable(studentTableModel);
        
        studentTable.getTableHeader().setFont(BUTTON_FONT.deriveFont(Font.BOLD, 14f));
        studentTable.getTableHeader().setBackground(PRIMARY_COLOR);
        studentTable.getTableHeader().setForeground(Color.WHITE);
        
        studentTable.setSelectionBackground(ACCENT_COLOR); 
        studentTable.setSelectionForeground(Color.WHITE);  

        studentTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                if (c instanceof JComponent jc) {
                    jc.setOpaque(true); 
                }

                if (c != null) {
                    c.setForeground(Color.BLACK); 

                    if (!isSelected) {
                        c.setBackground(row % 2 == 0 ? TABLE_STRIPE : CARD_BACKGROUND); 
                    } else {
                        c.setForeground(Color.WHITE);
                    }
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

        JPanel centerContainer = new JPanel(new BorderLayout(0, 10));
        centerContainer.setBackground(BACKGROUND_GRAY);
        centerContainer.add(searchPanel, BorderLayout.NORTH);
        centerContainer.add(scrollPane, BorderLayout.CENTER);
        
        panel.add(centerContainer, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }
    
    private void filterStudents() {
        String filterText = studentSearchField.getText().toLowerCase();
        
        studentTableModel.setRowCount(0);
        
        for (Student s : studentList) {
            String studentData = s.name.toLowerCase() + s.room.toLowerCase();
            
            if (filterText.isEmpty() || studentData.contains(filterText)) {
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


    private void refreshStudentTable() {
        filterStudents();
    }
    
    // --- 5. FEE MANAGEMENT PANEL (Unchanged) ---
    private JPanel feeManagementPanel() {
        // ... (Fee Management Panel unchanged)
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
        
        feeStatusTable.getTableHeader().setFont(BUTTON_FONT.deriveFont(Font.BOLD, 14f));
        feeStatusTable.getTableHeader().setBackground(PRIMARY_COLOR.darker()); 
        feeStatusTable.getTableHeader().setForeground(Color.WHITE);
        
        feeStatusTable.setSelectionBackground(ACCENT_COLOR); 
        feeStatusTable.setSelectionForeground(Color.WHITE);  
        
        feeStatusTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                if (c instanceof JComponent jc) {
                    jc.setOpaque(true); 
                }
                
                if (c != null) {
                    c.setForeground(Color.BLACK); 

                    if (!isSelected) {
                        c.setBackground(row % 2 == 0 ? TABLE_STRIPE : CARD_BACKGROUND);
                    } else {
                        c.setForeground(Color.WHITE);
                    }
                    
                    if (column == 4 && value != null && !isSelected) { 
                        try {
                            String balanceText = value.toString().replace("₹ ", "");
                            double balance = Double.parseDouble(balanceText);
                            if (balance > 0.0) {
                                c.setForeground(DELETE_COLOR); 
                                c.setFont(c.getFont().deriveFont(Font.BOLD));
                            } else {
                                c.setForeground(new Color(34, 139, 34)); 
                                c.setFont(c.getFont().deriveFont(Font.PLAIN));
                            }
                        } catch (NumberFormatException e) {
                            c.setForeground(Color.BLACK);
                        }
                    } else if (column == 4 && isSelected) {
                        c.setForeground(Color.WHITE);
                    }
                }
                
                return c;
            }
        });
        
        feeStatusTable.setRowHeight(25);
        feeStatusTable.setFont(LABEL_FONT);
        feeStatusTable.setGridColor(BACKGROUND_GRAY.darker());
        
        JScrollPane scrollPane = new JScrollPane(feeStatusTable); 
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
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = feeStatusTable.rowAtPoint(e.getPoint());
                
                if (row >= 0 && row < studentList.size()) { 
                    showPaymentDialog(studentList.get(row));
                }
            }
        });

        return panel;
    }
    
    // --- 6. STAFF MANAGEMENT PANEL (Unchanged) ---
    private void showStaffManagementPanel() {
        refreshStaffTable();
        cardLayout.show(mainPanel, "StaffManagement");
    }
    
    private JPanel staffManagementPanel() {
        // ... (Staff Management Panel unchanged)
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(BACKGROUND_GRAY); 
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel header = new JLabel("Hostel Staff Directory", SwingConstants.CENTER);
        header.setFont(HEADER_FONT);
        header.setForeground(PRIMARY_COLOR.darker());
        panel.add(header, BorderLayout.NORTH);

        String[] columnNames = {"ID", "Name", "Role", "Mobile", "Address"}; 
        staffTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        staffTable = new JTable(staffTableModel);
        
        staffTable.getTableHeader().setFont(BUTTON_FONT.deriveFont(Font.BOLD, 14f));
        staffTable.getTableHeader().setBackground(PRIMARY_COLOR); 
        staffTable.getTableHeader().setForeground(Color.WHITE);   
        
        staffTable.setSelectionBackground(ACCENT_COLOR); 
        staffTable.setSelectionForeground(Color.WHITE);  
        staffTable.setRowHeight(25);
        staffTable.setFont(LABEL_FONT);
        staffTable.setGridColor(BACKGROUND_GRAY.darker());
        
        staffTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                if (c instanceof JComponent jc) {
                    jc.setOpaque(true); 
                }

                if (c != null) {
                    c.setForeground(Color.BLACK); 

                    if (!isSelected) {
                        c.setBackground(row % 2 == 0 ? TABLE_STRIPE : CARD_BACKGROUND); 
                    } else {
                        c.setForeground(Color.WHITE);
                    }
                }
                return c;
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(staffTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(BACKGROUND_GRAY.darker(), 1));

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
        
        JButton deleteStaffBtn = new JButton("❌ Delete Staff"); 
        styleButton(deleteStaffBtn, DELETE_COLOR, Color.WHITE);
        deleteStaffBtn.addActionListener(e -> handleStaffDelete()); 

        buttonPanel.add(backBtn);
        buttonPanel.add(addStaffBtn);
        buttonPanel.add(editStaffBtn);
        buttonPanel.add(deleteStaffBtn);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }

    // --- 7. HELPLINE & SUGGESTION PANEL (UPDATED WITH STATUS CHECK BUTTON) ---
    private JPanel helplinePanel() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setBackground(BACKGROUND_GRAY); 
        panel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

        JLabel header = new JLabel("National Helpline & Feedback Center", SwingConstants.CENTER);
        header.setFont(TITLE_FONT.deriveFont(22f));
        header.setForeground(PRIMARY_COLOR.darker());
        panel.add(header, BorderLayout.NORTH);

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
        inputGrid.add(new JLabel("")); 

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
            saveAllData(); 
            
            JOptionPane.showMessageDialog(this, "Thank you! Your " + type + " has been recorded.", "Feedback Submitted", JOptionPane.INFORMATION_MESSAGE);
            
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
        
        // ADDED REVIEW BUTTON (FOR ADMIN)
        JButton reviewBtn = new JButton("Review Submitted Feedback (Admin)");
        styleButton(reviewBtn, ACCENT_COLOR.darker(), Color.WHITE);
        reviewBtn.addActionListener(e -> cardLayout.show(mainPanel, "ReviewSuggestions"));
        
        // ADDED STATUS CHECK BUTTON (FOR STUDENTS)
        JButton checkStatusBtn = new JButton("Check Feedback Status (Student)");
        styleButton(checkStatusBtn, PRIMARY_COLOR.darker(), Color.WHITE);
        checkStatusBtn.addActionListener(e -> cardLayout.show(mainPanel, "StudentFeedbackStatus"));

        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
        southPanel.setBackground(BACKGROUND_GRAY);
        southPanel.add(reviewBtn);
        southPanel.add(checkStatusBtn); // Added student status check
        southPanel.add(backBtn);
        panel.add(southPanel, BorderLayout.SOUTH);

        return panel;
    }
    
    // --- 9. NEW STUDENT FEEDBACK STATUS PANEL ---
    private JPanel viewStudentFeedbackPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(BACKGROUND_GRAY);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

        JLabel header = new JLabel("Check Status of Your Submitted Feedback", SwingConstants.CENTER);
        header.setFont(HEADER_FONT);
        header.setForeground(PRIMARY_COLOR.darker());
        panel.add(header, BorderLayout.NORTH);

        // Input Field for Student ID/Room/Name
        JTextField studentIdField = new JTextField(20);
        JButton searchBtn = new JButton("Show My Feedback");
        styleButton(searchBtn, ACCENT_COLOR, Color.WHITE);

        JPanel searchBarPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        searchBarPanel.setBackground(BACKGROUND_GRAY);
        searchBarPanel.add(new JLabel("Enter Your Name/Room:"));
        searchBarPanel.add(studentIdField);
        searchBarPanel.add(searchBtn);
        
        panel.add(searchBarPanel, BorderLayout.NORTH);

        // Display Area for Results
        JTextArea resultArea = new JTextArea(15, 50);
        resultArea.setEditable(false);
        resultArea.setFont(LABEL_FONT);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        
        JScrollPane scrollPane = new JScrollPane(resultArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        searchBtn.addActionListener(e -> {
            String senderId = studentIdField.getText().trim();
            if (senderId.isEmpty()) {
                resultArea.setText("Please enter your Name or Room Number to check status.");
                return;
            }
            
            // Reload suggestions to get the latest status
            try {
                loadSuggestionsFromDB();
            } catch (Exception ex) {
                loadSuggestionsFromFile();
            }

            // Filter feedback by sender
            String searchLower = senderId.toLowerCase();
            String output = suggestionList.stream()
                .filter(s -> s.sender.toLowerCase().contains(searchLower) || s.type.toLowerCase().contains(searchLower))
                .map(s -> String.format(
                    "--- %s (Submitted by: %s on %s) ---\n" +
                    "Status: %s\n" +
                    "Issue: %s\n" +
                    "Warden Response: %s\n\n",
                    s.type, s.sender, s.timestamp, s.status, s.body, s.adminResponse.isEmpty() ? "No official response yet." : s.adminResponse
                ))
                .collect(Collectors.joining(""));

            if (output.isEmpty()) {
                resultArea.setText("No feedback found matching '" + senderId + "'.\nEnsure you enter the name/room exactly as submitted (case-insensitive search is used).");
            } else {
                resultArea.setText("--- YOUR SUBMITTED FEEDBACK STATUS ---\n\n" + output);
            }
        });

        // Bottom Panel
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        bottomPanel.setBackground(BACKGROUND_GRAY);
        JButton backBtn = new JButton("<< Back to Helpline");
        styleButton(backBtn, PRIMARY_COLOR, Color.WHITE);
        backBtn.addActionListener(e -> cardLayout.show(mainPanel, "Helpline"));
        bottomPanel.add(backBtn);
        
        panel.add(bottomPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    // --- 8. SUGGESTION REVIEW PANEL (UPDATED WITH STATUS AND RESPONSE) ---
    private JPanel reviewSuggestionsPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(BACKGROUND_GRAY); 
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JLabel header = new JLabel("Review Student Feedback and Complaints (Admin View)", SwingConstants.CENTER);
        header.setFont(HEADER_FONT);
        header.setForeground(PRIMARY_COLOR.darker());
        panel.add(header, BorderLayout.NORTH);

        // UPDATED COLUMNS
        String[] columnNames = {"Timestamp", "Type", "Sender/Room", "Details", "Status", "Warden Response"}; 
        suggestionTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        
        JTable table = new JTable(suggestionTableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        table.getTableHeader().setFont(BUTTON_FONT.deriveFont(Font.BOLD, 14f));
        table.getTableHeader().setBackground(PRIMARY_COLOR.darker()); 
        table.getTableHeader().setForeground(Color.WHITE);
        
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.getColumnModel().getColumn(3).setPreferredWidth(250); // Details
        table.getColumnModel().getColumn(5).setPreferredWidth(250); // Response
        table.getColumnModel().getColumn(4).setPreferredWidth(80); // Status
        table.getColumnModel().getColumn(0).setPreferredWidth(120); // Timestamp
        
        table.setRowHeight(40); 
        
        table.setFont(LABEL_FONT);
        table.setSelectionBackground(ACCENT_COLOR);
        table.setSelectionForeground(Color.WHITE);

        // Renderer for status coloring and tooltips
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (c instanceof JComponent) { ((JComponent) c).setOpaque(true); }
                
                String status = (String) table.getValueAt(row, 4); 
                
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? TABLE_STRIPE : CARD_BACKGROUND);
                    c.setForeground(Color.BLACK);
                    
                    // Apply color based on Status
                    if (column == 4 && status != null) {
                        if (status.equals("Resolved")) {
                            c.setForeground(STATUS_RESOLVED_COLOR.darker());
                            c.setFont(c.getFont().deriveFont(Font.BOLD));
                        } else if (status.equals("Pending")) {
                            c.setForeground(STATUS_PENDING_COLOR);
                            c.setFont(c.getFont().deriveFont(Font.BOLD));
                        }
                    }
                } else {
                    c.setForeground(Color.WHITE);
                }
                
                // Set Tooltip for full visibility of truncated text
                if (column == 3 || column == 5) {
                    ((JComponent) c).setToolTipText(value != null ? value.toString() : null);
                } else {
                    ((JComponent) c).setToolTipText(null);
                }
                
                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(BACKGROUND_GRAY.darker(), 1));

        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 25, 10));
        buttonPanel.setBackground(BACKGROUND_GRAY);
        
        JButton refreshBtn = new JButton("↻ Refresh / Load");
        styleButton(refreshBtn, PRIMARY_COLOR, Color.WHITE);
        refreshBtn.addActionListener(e -> refreshSuggestionsTable());
        
        // NEW STATUS BUTTON
        JButton updateStatusBtn = new JButton("✅ Update Status/Respond");
        styleButton(updateStatusBtn, ACCENT_COLOR, Color.WHITE);
        updateStatusBtn.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Please select a feedback entry to update.", "Selection Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            // Get the actual suggestion object using the list index (table isn't filtered here)
            Suggestion suggestionToUpdate = suggestionList.get(selectedRow);
            updateSuggestionStatusDialog(suggestionToUpdate);
        });

        JButton deleteSelectedBtn = new JButton("🗑️ Delete Selected Feedback");
        styleButton(deleteSelectedBtn, DELETE_COLOR, Color.WHITE);
        deleteSelectedBtn.addActionListener(e -> handleSuggestionDelete(table));

        JButton backBtn = new JButton("<< Back to Helpline");
        styleButton(backBtn, PRIMARY_COLOR, Color.WHITE);
        backBtn.addActionListener(e -> cardLayout.show(mainPanel, "Helpline"));

        buttonPanel.add(refreshBtn);
        buttonPanel.add(updateStatusBtn); // Added new button
        buttonPanel.add(deleteSelectedBtn);
        buttonPanel.add(backBtn);

        panel.add(scrollPane, BorderLayout.CENTER); 
        panel.add(buttonPanel, BorderLayout.SOUTH);

        refreshSuggestionsTable(); // Initial load
        return panel;
    }
    
    private void refreshSuggestionsTable() {
        if (suggestionTableModel != null) {
            suggestionTableModel.setRowCount(0);

            for (Suggestion s : suggestionList) {
                Object[] rowData = {
                    s.timestamp, 
                    s.type,
                    s.sender,
                    s.body,
                    s.status, // NEW
                    s.adminResponse // NEW
                };
                suggestionTableModel.addRow(rowData);
            }
        }
    }
    
    private void updateSuggestionStatusDialog(Suggestion s) {
        JDialog dialog = new JDialog(this, "Update Feedback Status for " + s.sender, true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(500, 350);
        dialog.setLocationRelativeTo(this);

        JPanel dialogMainPanel = new JPanel(new BorderLayout(10, 10));
        dialogMainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel statusPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        
        // 1. Current Status Display
        statusPanel.add(new JLabel("Current Status:"));
        JLabel currentStatusLabel = new JLabel(s.status);
        currentStatusLabel.setFont(currentStatusLabel.getFont().deriveFont(Font.BOLD, 14f));
        statusPanel.add(currentStatusLabel);
        
        // 2. New Status Selection
        String[] statuses = {"Pending", "In Progress", "Resolved", "Rejected"};
        JComboBox<String> statusComboBox = new JComboBox<>(statuses);
        statusComboBox.setSelectedItem(s.status);
        
        statusPanel.add(new JLabel("New Status:"));
        statusPanel.add(statusComboBox);

        // 3. Admin Response Area
        JTextArea responseArea = new JTextArea(s.adminResponse, 5, 20);
        responseArea.setLineWrap(true);
        responseArea.setWrapStyleWord(true);
        
        dialogMainPanel.add(statusPanel, BorderLayout.NORTH);
        dialogMainPanel.add(new JScrollPane(responseArea), BorderLayout.CENTER);
        
        JButton saveBtn = new JButton("Save Status and Response");
        styleButton(saveBtn, ACCENT_COLOR.darker(), Color.WHITE);

        saveBtn.addActionListener(e -> {
            s.status = (String) statusComboBox.getSelectedItem();
            s.adminResponse = responseArea.getText().trim();
            
            if (s.adminResponse.isEmpty() && !s.status.equals("Pending")) {
                JOptionPane.showMessageDialog(dialog, "Please provide a brief response/reason when changing status.", "Missing Response", JOptionPane.WARNING_MESSAGE);
                return;
            }

            saveAllData(); // Save changes
            refreshSuggestionsTable(); // Refresh the admin table
            dialog.dispose();
            JOptionPane.showMessageDialog(this, "Status updated and response recorded.", "Success", JOptionPane.INFORMATION_MESSAGE);
        });
        
        dialog.add(dialogMainPanel, BorderLayout.CENTER);
        dialog.add(saveBtn, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void handleSuggestionDelete(JTable table) {
        // ... (Deletion logic unchanged)
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
            saveAllData(); 
            refreshSuggestionsTable(); 
            JOptionPane.showMessageDialog(this, "Feedback deleted.", "Deletion Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    // --- STAFF CRUD METHODS ---
    // ... (Staff methods unchanged)
    
    private void registerStaff(Staff staffToEdit) {
         // ... (Staff registration logic unchanged)
         staffNameField = new JTextField(20);
         staffMobileField = new JTextField(20);
         staffIDField = new JTextField(10);
         staffAddressArea = new JTextArea(3, 20); 
         
         String[] roles = {"Housekeeping", "Security", "Maintenance", "Office Staff"};
         staffRoleComboBox = new JComboBox<>(roles);
         
         String dialogTitle = "Register New Staff Member";
         boolean isEditing = (staffToEdit != null);
         
         if (isEditing && staffToEdit != null) {
             dialogTitle = "Edit Staff Record";
             staffNameField.setText(staffToEdit.name);
             staffMobileField.setText(staffToEdit.mobile);
             staffIDField.setText(staffToEdit.id);
             staffIDField.setEditable(false); 
             staffRoleComboBox.setSelectedItem(staffToEdit.role);
             staffAddressArea.setText(staffToEdit.address); 
         } else {
             // New staff record
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
             String address = staffAddressArea.getText().trim(); 
 
             if (name.isEmpty() || id.isEmpty() || mobile.isEmpty() || address.isEmpty()) { 
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
             
             Staff newOrUpdatedStaff = new Staff(id, name, role, mobile, address); 
 
             if (isEditing && staffToEdit != null) {
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
             saveAllData(); 
         }
    }
    
    private void refreshStaffTable() {
         // ... (Staff table refresh unchanged)
         if (staffTableModel != null) {
               staffTableModel.setRowCount(0);
               
             for (Staff s : staffList) {
                 Object[] rowData = {
                     s.id, 
                     s.name, 
                     s.role, 
                     s.mobile,
                     s.address 
                 };
                 staffTableModel.addRow(rowData);
             }
         }
    }
    
    private void handleStaffEdit() {
         // ... (Staff edit logic unchanged)
         int selectedRow = staffTable.getSelectedRow();
         if (selectedRow == -1) {
             JOptionPane.showMessageDialog(this, "Please select a staff record to edit.", "Selection Error", JOptionPane.WARNING_MESSAGE);
             return;
         }
         
         Staff staffToEdit = staffList.get(selectedRow);
         registerStaff(staffToEdit); 
    }
    
    private void handleStaffDelete() {
         // ... (Staff delete logic unchanged)
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
             saveAllData(); 
             JOptionPane.showMessageDialog(this, "Staff record deleted successfully.", "Deletion Success", JOptionPane.INFORMATION_MESSAGE);
         }
    }

    private void showFeeManagementPanel() {
        refreshFeeTable();
        cardLayout.show(mainPanel, "FeeManagement");
    }

    private void refreshFeeTable() {
         // ... (Fee table refresh logic unchanged)
         if (feeStatusTable != null) {
            
             try {
                loadStudentsFromDB();
            } catch (Exception e) {
                loadStudentsFromFile();
            }

            DefaultTableModel feeModel = (DefaultTableModel) feeStatusTable.getModel();
            feeModel.setRowCount(0);

            for (Student s : studentList) {
                Object[] rowData = {
                    s.name,
                    s.room,
                    String.format("₹ %.2f", s.totalHostelFee),
                    String.format("₹ %.2f", s.getFeeSubmitted()),
                    String.format("₹ %.2f", s.getFeeBalance()),
                    "Add Payment / View Details" 
                };
                feeModel.addRow(rowData);
            }
        }
    }
    
    private void showViewStudentsPanel() {
        studentSearchField.setText("");
        refreshStudentTable();
        cardLayout.show(mainPanel, "ViewStudents");
    }
    
    private void handleEdit() {
        // ... (Handle Edit logic unchanged)
        int viewRow = studentTable.getSelectedRow();
        if (viewRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a student record to edit.", "Selection Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String studentName = (String) studentTable.getValueAt(viewRow, 0);
        Student studentToEdit = studentList.stream()
                                           .filter(s -> s.name.equals(studentName))
                                           .findFirst()
                                           .orElse(null);
        
        if(studentToEdit != null) {
            registerStudent(studentToEdit);
        } else {
             JOptionPane.showMessageDialog(this, "Error finding student record.", "Internal Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void handleDelete() {
         // ... (Handle Delete logic unchanged)
         int viewRow = studentTable.getSelectedRow();
         if (viewRow == -1) {
             JOptionPane.showMessageDialog(this, "Please select a student record to delete.", "Selection Error", JOptionPane.WARNING_MESSAGE);
             return;
         }
         
         String studentName = (String) studentTable.getValueAt(viewRow, 0);
         Student studentToDelete = studentList.stream()
                                              .filter(s -> s.name.equals(studentName))
                                              .findFirst()
                                              .orElse(null);
         
         if (studentToDelete == null) {
             JOptionPane.showMessageDialog(this, "Error finding student record.", "Internal Error", JOptionPane.ERROR_MESSAGE);
             return;
         }
         
         int confirm = JOptionPane.showConfirmDialog(this, 
             "Are you sure you want to delete the record for " + studentToDelete.name + " (Room " + studentToDelete.room + ")?", 
             "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
             
         if (confirm == JOptionPane.YES_OPTION) {
             studentList.remove(studentToDelete); 
             refreshStudentTable();
             saveAllData(); 
             JOptionPane.showMessageDialog(this, "Record deleted successfully.", "Deletion Success", JOptionPane.INFORMATION_MESSAGE);
         }
    }
    
    private void showPaymentDialog(Student student) {
        // ... (Payment dialog logic unchanged)
        
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
                saveAllData(); 
                
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

    // --- Data Model Classes (UPDATED SUGGESTION) ---
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
    
    private static class Staff implements Serializable { 
        private static final long serialVersionUID = 3L;
        String id;
        String name;
        String role;
        String mobile;
        String address; 

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
    
    private static class Suggestion implements Serializable { 
        private static final long serialVersionUID = 5L; // Increased serialVersionUID
        String sender;
        String type; 
        String body;
        String timestamp;
        
        String status; // NEW: Status of the request/complaint
        String adminResponse; // NEW: Warden's response

        public Suggestion(String sender, String type, String body) {
            this.sender = sender;
            this.type = type;
            this.body = body;
            this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            this.status = "Pending"; // Default Status
            this.adminResponse = ""; // Default empty response
        }
    }

    public static void main(String[] args) {
        // Ensure you have updated your MySQL database schema:
        // ALTER TABLE suggestions ADD COLUMN status VARCHAR(20) DEFAULT 'Pending';
        // ALTER TABLE suggestions ADD COLUMN admin_response TEXT DEFAULT '';
        SwingUtilities.invokeLater(HostelManagementSystem::new);
    }
}
