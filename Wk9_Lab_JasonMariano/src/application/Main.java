package application;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Main extends Application {

    // Your MySQL login details, these have to match what you use in Workbench
    private static final String DB_URL = "jdbc:mysql://127.0.0.1:3306/hospital_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "class123";

    // These are up here instead of inside start() so the button methods can use them too
    private TextField patientIdField = new TextField();
    private TextField nameField = new TextField();
    private TextField ageField = new TextField();
    private ComboBox<String> genderBox = new ComboBox<>();
    private TextField phoneField = new TextField();
    private TextField diagnosisField = new TextField();
    private DatePicker admissionDatePicker = new DatePicker();

    // The search box and the table on the right side
    private TextField searchField = new TextField();
    private TableView<Patient> patientTable = new TableView<>();

    // This shows success or error messages at the bottom of the window
    private Label statusLabel = new Label("Ready.");

    @Override
    public void start(Stage stage) {

        // Top of the window: the title and subtitle stacked in a VBox
        Label titleLabel = new Label("Hospital Management System");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        
        Label subtitleLabel = new Label("Patient Registration Records");

        VBox topBox = new VBox(titleLabel, subtitleLabel);
        topBox.setPadding(new Insets(10));
        topBox.setAlignment(Pos.CENTER);

        // Left side: the patient form
        Label formTitle = new Label("Patient Details");

        // The database makes the ID, so the user can't type in this box
        patientIdField.setEditable(false);

        // These are the choices that show up in the gender dropdown
        genderBox.getItems().addAll("Female", "Male", "Non-binary", "Prefer not to say");

        // The grey hint shows the phone format the validation expects
        phoneField.setPromptText("e.g. 416-555-5555");

        // The GridPane lines up the labels in column 0 and the inputs in column 1
        GridPane formGrid = new GridPane();
        formGrid.setHgap(10);
        formGrid.setVgap(10);

        // add() takes the control, then the column, then the row
        formGrid.add(new Label("Patient ID:"), 0, 0);
        formGrid.add(patientIdField, 1, 0);
        formGrid.add(new Label("Full name:"), 0, 1);
        formGrid.add(nameField, 1, 1);
        formGrid.add(new Label("Age:"), 0, 2);
        formGrid.add(ageField, 1, 2);
        formGrid.add(new Label("Gender:"), 0, 3);
        formGrid.add(genderBox, 1, 3);
        formGrid.add(new Label("Phone:"), 0, 4);
        formGrid.add(phoneField, 1, 4);
        formGrid.add(new Label("Diagnosis / reason:"), 0, 5);
        formGrid.add(diagnosisField, 1, 5);
        formGrid.add(new Label("Admission date:"), 0, 6);
        formGrid.add(admissionDatePicker, 1, 6);

        // The four form buttons, two per row
        Button addButton = new Button("Add Patient");
        Button updateButton = new Button("Update");
        Button deleteButton = new Button("Delete");
        Button clearButton = new Button("Clear");

        HBox buttonRow1 = new HBox(addButton, updateButton);
        buttonRow1.setSpacing(10);
        HBox buttonRow2 = new HBox(deleteButton, clearButton);
        buttonRow2.setSpacing(10);

        // Stack the form title, the grid and the two button rows on top of each other
        VBox formBox = new VBox(formTitle, formGrid, buttonRow1, buttonRow2);
        formBox.setSpacing(10);

        // Right side: the search bar and the table of patients
        Label recordsTitle = new Label("Patient Records");

        searchField.setPromptText("Search by name or phone");
        Button searchButton = new Button("Search");
        Button showAllButton = new Button("Show All");

        HBox searchRow = new HBox(searchField, searchButton, showAllButton);
        searchRow.setSpacing(10);

        // The table columns are set up in their own method further down
        setupTable();

        VBox recordsBox = new VBox(recordsTitle, searchRow, patientTable);
        recordsBox.setSpacing(10);

        // The middle of the window puts the form and the records side by side
        HBox centerBox = new HBox(formBox, recordsBox);
        centerBox.setSpacing(20);
        centerBox.setPadding(new Insets(10));

        // Bottom of the window: the status message
        statusLabel.setPadding(new Insets(10));

        // The BorderPane holds everything: top, center and bottom
        BorderPane root = new BorderPane();
        root.setTop(topBox);
        root.setCenter(centerBox);
        root.setBottom(statusLabel);

        // Put the BorderPane in a scene, then show it in the window
        Scene scene = new Scene(root, 1000, 550);
        stage.setTitle("Hospital Management System");
        stage.setScene(scene);
        stage.show();

        // When Show All is clicked, reload every patient from the database
        showAllButton.setOnAction(event -> loadAllPatients());

        // When Add Patient is clicked, check the form and save the new patient
        addButton.setOnAction(event -> addPatient());

        // When Clear is clicked, empty out the form
        clearButton.setOnAction(event -> clearForm());

        // When Update is clicked, save the changes to the selected patient
        updateButton.setOnAction(event -> updatePatient());

        // When Delete is clicked, ask first and then remove the selected patient
        deleteButton.setOnAction(event -> deletePatient());

        // When Search is clicked, find patients whose name or phone matches what was typed
        searchButton.setOnAction(event -> searchPatients());

        // When you click a row in the table, copy that patient into the form
        patientTable.setOnMouseClicked(event -> fillFormFromSelectedRow());

        // Load the patients right away so the table isn't empty when the app opens
        loadAllPatients();
    }

    // This builds the table columns and links each one to a getter in Patient
    private void setupTable() {
        // "patientId" means the column calls getPatientId() on each patient
        TableColumn<Patient, Integer> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("patientId"));

        TableColumn<Patient, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Patient, Integer> ageColumn = new TableColumn<>("Age");
        ageColumn.setCellValueFactory(new PropertyValueFactory<>("age"));

        TableColumn<Patient, String> genderColumn = new TableColumn<>("Gender");
        genderColumn.setCellValueFactory(new PropertyValueFactory<>("gender"));

        TableColumn<Patient, String> phoneColumn = new TableColumn<>("Phone");
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));

        TableColumn<Patient, String> diagnosisColumn = new TableColumn<>("Diagnosis");
        diagnosisColumn.setCellValueFactory(new PropertyValueFactory<>("diagnosis"));

        TableColumn<Patient, LocalDate> dateColumn = new TableColumn<>("Admitted");
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("admissionDate"));

        // Add all the columns to the table in the order we want them to show
        patientTable.getColumns().add(idColumn);
        patientTable.getColumns().add(nameColumn);
        patientTable.getColumns().add(ageColumn);
        patientTable.getColumns().add(genderColumn);
        patientTable.getColumns().add(phoneColumn);
        patientTable.getColumns().add(diagnosisColumn);
        patientTable.getColumns().add(dateColumn);
    }

    // This opens a connection to MySQL using the login details at the top
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    // This saves a new patient from the form into the database
    private void addPatient() {
        // If something in the form is wrong, stop here and don't save anything
        if (validateForm() == false) {
            return;
        }

        // Each ? is a blank that we fill in below, so the user's typing never goes straight into the SQL
        String sql = "INSERT INTO patients (name, age, gender, phone, diagnosis, admission_date) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // Fill in the blanks in order, 1 is the first ?, 2 is the second ?, and so on
            ps.setString(1, nameField.getText().trim());
            ps.setInt(2, Integer.parseInt(ageField.getText().trim()));
            ps.setString(3, genderBox.getValue());
            ps.setString(4, phoneField.getText().trim());
            ps.setString(5, diagnosisField.getText().trim());
            ps.setDate(6, java.sql.Date.valueOf(admissionDatePicker.getValue()));

            // executeUpdate() runs the INSERT and tells us how many rows were added
            int rowsAdded = ps.executeUpdate();

            if (rowsAdded > 0) {
                loadAllPatients();
                clearForm();
                showSuccess("Patient added successfully.");
            } else {
                showError("Patient was not added.");
            }

        } catch (SQLException e) {
            showError("Database error: " + e.getMessage());
        }
    }

    // This takes the patient you clicked in the table and puts their info into the form
    private void fillFormFromSelectedRow() {
        Patient selected = patientTable.getSelectionModel().getSelectedItem();

        // If you clicked an empty part of the table there's no patient, so just stop
        if (selected == null) {
            return;
        }

        // setText() needs a String, so String.valueOf() turns the numbers into text first
        patientIdField.setText(String.valueOf(selected.getPatientId()));
        nameField.setText(selected.getName());
        ageField.setText(String.valueOf(selected.getAge()));
        genderBox.setValue(selected.getGender());
        phoneField.setText(selected.getPhone());
        diagnosisField.setText(selected.getDiagnosis());
        admissionDatePicker.setValue(selected.getAdmissionDate());

        showInfo("Selected patient #" + selected.getPatientId() + ". Edit the form, then click Update or Delete.");
    }

    // This gives back the ID in the Patient ID box, or -1 if no patient is selected
    private int getSelectedPatientId() {
        String idText = patientIdField.getText().trim();

        if (idText.isEmpty()) {
            return -1;
        }

        return Integer.parseInt(idText);
    }

    // This saves the edited form over the selected patient in the database
    private void updatePatient() {
        int patientId = getSelectedPatientId();

        // You have to pick a patient from the table first, or we don't know who to update
        if (patientId == -1) {
            showError("Select a patient in the table before clicking Update.");
            return;
        }

        // Same checks as adding a patient
        if (validateForm() == false) {
            return;
        }

        // The WHERE part makes sure we only change the one patient with this ID
        String sql = "UPDATE patients SET name = ?, age = ?, gender = ?, phone = ?, "
                   + "diagnosis = ?, admission_date = ? WHERE patient_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, nameField.getText().trim());
            ps.setInt(2, Integer.parseInt(ageField.getText().trim()));
            ps.setString(3, genderBox.getValue());
            ps.setString(4, phoneField.getText().trim());
            ps.setString(5, diagnosisField.getText().trim());
            ps.setDate(6, java.sql.Date.valueOf(admissionDatePicker.getValue()));
            // The 7th ? is the ID in the WHERE part
            ps.setInt(7, patientId);

            int rowsUpdated = ps.executeUpdate();

            if (rowsUpdated > 0) {
                loadAllPatients();
                clearForm();
                showSuccess("Patient #" + patientId + " updated successfully.");
            } else {
                showError("No patient found with ID " + patientId + ".");
            }

        } catch (SQLException e) {
            showError("Database error: " + e.getMessage());
        }
    }

    // This removes the selected patient from the database after asking the user to confirm
    private void deletePatient() {
        int patientId = getSelectedPatientId();

        // You have to pick a patient from the table first, or we don't know who to delete
        if (patientId == -1) {
            showError("Select a patient in the table before clicking Delete.");
            return;
        }

        // Pop up a box asking if they're sure, since a delete can't be undone
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setHeaderText(null);
        alert.setContentText("Are you sure you want to delete patient #" + patientId
                + " (" + nameField.getText() + ")?");

        // showAndWait() pauses here until the user clicks OK or Cancel
        alert.showAndWait();

        // If they clicked anything other than OK, cancel the delete
        if (alert.getResult() != ButtonType.OK) {
            showInfo("Delete cancelled.");
            return;
        }

        // Only delete the one patient with this ID
        String sql = "DELETE FROM patients WHERE patient_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, patientId);
            int rowsDeleted = ps.executeUpdate();

            if (rowsDeleted > 0) {
                loadAllPatients();
                clearForm();
                showSuccess("Patient #" + patientId + " deleted successfully.");
            } else {
                showError("No patient found with ID " + patientId + ".");
            }

        } catch (SQLException e) {
            showError("Database error: " + e.getMessage());
        }
    }

    // This finds patients whose name or phone has the search text anywhere in it
    private void searchPatients() {
        String keyword = searchField.getText().trim();

        // Don't search for nothing
        if (keyword.isEmpty()) {
            showError("Type a name or phone number to search.");
            return;
        }

        // LIKE lets us match part of a name or phone instead of the whole thing
        String sql = "SELECT * FROM patients WHERE name LIKE ? OR phone LIKE ? ORDER BY patient_id";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // The % signs mean "anything can come before or after", so "416" matches anywhere in the phone
            ps.setString(1, "%" + keyword + "%");
            ps.setString(2, "%" + keyword + "%");

            // The ResultSet gets its own try so it closes too
            try (ResultSet rs = ps.executeQuery()) {
                int count = fillTable(rs);

                if (count == 0) {
                    showError("No patients found matching \"" + keyword + "\".");
                } else {
                    showSuccess("Found " + count + " patient(s) matching \"" + keyword + "\".");
                }
            }

        } catch (SQLException e) {
            showError("Database error: " + e.getMessage());
        }
    }

    // This checks the form and returns true if everything is okay, or false if something is wrong
    private boolean validateForm() {
        String name = nameField.getText().trim();
        String ageText = ageField.getText().trim();
        String gender = genderBox.getValue();
        String phone = phoneField.getText().trim();
        String diagnosis = diagnosisField.getText().trim();
        LocalDate admissionDate = admissionDatePicker.getValue();

        // Every field has to be filled in, and an empty dropdown or date box comes back as null
        if (name.isEmpty() || ageText.isEmpty() || gender == null
                || phone.isEmpty() || diagnosis.isEmpty() || admissionDate == null) {
            showError("Please fill in all fields.");
            return false;
        }

        // Try to turn the age into a number, and if it has letters in it parseInt will fail
        int age;
        try {
            age = Integer.parseInt(ageText);
        } catch (NumberFormatException e) {
            showError("Age must be a whole number.");
            return false;
        }

        // The age has to be between 0 and 125
        if (age < 0 || age > 125) {
            showError("Age must be between 0 and 125.");
            return false;
        }

        // The phone number has to look like a real phone number
        if (isValidPhone(phone) == false) {
            showError("Phone must have 10 digits (e.g. 416-555-0101).");
            return false;
        }

        // You can't be admitted on a day that hasn't happened yet
        if (admissionDate.isAfter(LocalDate.now())) {
            showError("Admission date cannot be in the future.");
            return false;
        }

        return true;
    }

    // A phone is okay if it has exactly 10 digits, and spaces, dashes and brackets are allowed too
    private boolean isValidPhone(String phone) {
        int digitCount = 0;

        // Look at each character one at a time
        for (int i = 0; i < phone.length(); i++) {
            char c = phone.charAt(i);

            if (Character.isDigit(c)) {
                digitCount++;
            } else if (c != ' ' && c != '-' && c != '(' && c != ')') {
                // Anything that isn't a digit, space, dash or bracket means it's not a phone number
                return false;
            }
        }

        return digitCount == 10;
    }

    // This empties every box in the form and unselects the table
    private void clearForm() {
        patientIdField.clear();
        nameField.clear();
        ageField.clear();
        genderBox.setValue(null);
        phoneField.clear();
        diagnosisField.clear();
        admissionDatePicker.setValue(null);
        patientTable.getSelectionModel().clearSelection();
        showInfo("Form cleared.");
    }

    // This gets every patient from the database and shows them in the table
    private void loadAllPatients() {
        String sql = "SELECT * FROM patients ORDER BY patient_id";

        // Try-with-resources closes the connection, statement and result set for us when we're done
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            int count = fillTable(rs);
            showInfo("Showing all patients: " + count + " record(s).");

        } catch (SQLException e) {
            // If anything goes wrong with the database, show it at the bottom instead of crashing
            showError("Database error: " + e.getMessage());
        }
    }

    // This goes through the results row by row and adds each patient to the table
    private int fillTable(ResultSet rs) throws SQLException {
        // Empty the table first so we don't end up with the same patients twice
        patientTable.getItems().clear();
        int count = 0;

        // rs.next() moves to the next row and gives false when there are no rows left
        while (rs.next()) {
            Patient patient = new Patient(
                    rs.getInt("patient_id"),
                    rs.getString("name"),
                    rs.getInt("age"),
                    rs.getString("gender"),
                    rs.getString("phone"),
                    rs.getString("diagnosis"),
                    rs.getDate("admission_date").toLocalDate());

            patientTable.getItems().add(patient);
            count++;
        }

        return count;
    }

    // Green text for when something worked
    private void showSuccess(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: green;");
    }

    // Red text for when something went wrong
    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: red;");
    }

    // Plain black text for normal info
    private void showInfo(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: black;");
    }

    public static void main(String[] args) {
        launch(args);
    }
}