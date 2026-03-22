package uv.naloge.prvaNaloga;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class MainViewController {

    // --- FXML Fields ---

    @FXML
    private CheckMenuItem toggleParticipantNameItem;

    @FXML
    private CheckMenuItem toggleClassTypeItem;

    @FXML
    private CheckMenuItem toggleClassTimeItem;

    @FXML
    private TextField participantNameField;

    @FXML
    private TextField classTypeField;

    @FXML
    private TextField classTimeField;

    @FXML
    private ToggleGroup actionToggleGroup;

    @FXML
    private ComboBox<Reservation> reservationComboBox;

    @FXML
    private Spinner<Integer> reservationPositionSpinner;

    @FXML
    private TextArea mainTextArea;

    @FXML
    private Label statusLabel;

    private boolean isUpdatingSelection;

    // --- Initialization ---

    public void initialize() {
        initializeSpinner();
        initializeComboBox();
        resetStatus();
    }

    private void initializeSpinner() {
        reservationPositionSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 0, 0));
        reservationPositionSpinner.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                changeSelection(newValue - 1);
            }
        });
    }

    private void initializeComboBox() {
        reservationComboBox.getItems().addAll(
                new Reservation("Jane Doe", "Yoga", "18:00"),
                new Reservation("John Smith", "Pilates", "19:30"),
                new Reservation("Alice Johnson", "Zumba", "17:00"),
                new Reservation("Bob Brown", "CrossFit", "20:00"),
                new Reservation("Charlie Davis", "Spin", "06:00")
        );

        reservationComboBox.getItems().addListener((ListChangeListener<Reservation>) change -> updateSpinnerRange());
        reservationComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) ->
                changeSelection(reservationComboBox.getSelectionModel().getSelectedIndex())
        );

        updateSpinnerRange();
        if (reservationComboBox.getItems().isEmpty()) {
            changeSelection(-1);
        } else {
            changeSelection(0);
        }
    }

    // --- FXML Event Handlers ---

    @FXML
    public void onToggleParticipantName() {
        participantNameField.setDisable(!toggleParticipantNameItem.isSelected());
        logAction("Participant Name " + (toggleParticipantNameItem.isSelected() ? "Enabled" : "Disabled"));
    }

    @FXML
    public void onToggleClassType() {
        classTypeField.setDisable(!toggleClassTypeItem.isSelected());
        logAction("Class Type " + (toggleClassTypeItem.isSelected() ? "Enabled" : "Disabled"));
    }

    @FXML
    public void onToggleClassTime() {
        classTimeField.setDisable(!toggleClassTimeItem.isSelected());
        logAction("Class Time " + (toggleClassTimeItem.isSelected() ? "Enabled" : "Disabled"));
    }

    @FXML
    public void onExecuteAction() {
        RadioButton selectedActionButton = (RadioButton) actionToggleGroup.getSelectedToggle();
        if (selectedActionButton == null) {
            showError("No Action Selected", "Please select an action to execute.");
            return;
        }

        String selectedActionName = selectedActionButton.getText();
        switch (selectedActionName) {
            case "Register Participant" -> registerParticipant();
            case "Unregister Participant" -> unregisterParticipant();
            case "Change Reservation" -> changeReservation();
            default -> logAction("Unknown Action");
        }
    }

    @FXML
    public void onPrintAll() {
        logAction("Printing all items...");
        mainTextArea.setText(buildPrintAllContent());
    }

    @FXML
    public void onClear() {
        mainTextArea.clear();
        resetStatus();
    }

    @FXML
    public void onExit() {
        Platform.exit();
    }

    @FXML
    public void onSave() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Reservations");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        File selectedFile = fileChooser.showSaveDialog(statusLabel.getScene().getWindow());
        if (selectedFile == null) {
            logAction("Save command cancelled");
            return;
        }

        String fileContent = buildPrintAllContent();
        try {
            Files.writeString(selectedFile.toPath(), fileContent, StandardCharsets.UTF_8);
            logAction("Saved: " + selectedFile.getName());
        } catch (IOException exception) {
            showError("Save Failed", exception.getMessage());
        }
    }

    @FXML
    public void onOpen() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Resource File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        File selectedFile = fileChooser.showOpenDialog(statusLabel.getScene().getWindow());
        if (selectedFile == null) {
            logAction("Open command cancelled");
            return;
        }

        List<String> fileLines;
        try {
            fileLines = Files.readAllLines(selectedFile.toPath(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            showError("Open Failed", exception.getMessage());
            return;
        }

        List<Reservation> parsedReservations = parseReservationsFromFileLines(fileLines);
        if (parsedReservations == null) {
            return;
        }

        reservationComboBox.getItems().setAll(parsedReservations);
        if (reservationComboBox.getItems().isEmpty()) {
            changeSelection(-1);
        } else {
            changeSelection(0);
        }

        logAction("Opened: " + selectedFile.getName() + " (" + parsedReservations.size() + " rows loaded)");
    }

    // --- Action Logic ---

    private void registerParticipant() {
        if (participantNameField.isDisabled() || classTypeField.isDisabled() || classTimeField.isDisabled()) {
            showError("Invalid Input", "All fields must be enabled and filled to register.");
            return;
        }

        String participantName = getTrimmedParticipantName();
        String classType = getTrimmedClassType();
        String classTime = getTrimmedClassTime();

        if (participantName.isEmpty() || classType.isEmpty() || classTime.isEmpty()) {
            showError("Invalid Input", "All fields must be enabled and filled to register.");
            return;
        }

        Reservation newReservation = new Reservation(participantName, classType, classTime);

        if (hasMatchingReservation(newReservation)) {
            showError("Duplicate Entry", "This reservation already exists.");
            return;
        }

        reservationComboBox.getItems().add(newReservation);
        reservationComboBox.getSelectionModel().select(newReservation);
        logAction("Registered: " + newReservation);
    }

    private void unregisterParticipant() {
        int selectedIndex = reservationComboBox.getSelectionModel().getSelectedIndex();
        if (selectedIndex < 0) {
            showError("No Selection", "Please select a participant to unregister.");
            return;
        }

        Reservation removedReservation = reservationComboBox.getItems().remove(selectedIndex);
        logAction("Unregistered: " + removedReservation);

        if (reservationComboBox.getItems().isEmpty()) {
            clearFields();
            return;
        }

        int newIndex = Math.min(selectedIndex, reservationComboBox.getItems().size() - 1);
        reservationComboBox.getSelectionModel().select(newIndex);
    }

    private void changeReservation() {
        if (participantNameField.isDisabled() && classTypeField.isDisabled() && classTimeField.isDisabled()) {
            showError("Action Not Allowed", "Enable at least one field using the Edit menu to change reservation.");
            return;
        }

        int selectedIndex = reservationComboBox.getSelectionModel().getSelectedIndex();
        if (selectedIndex < 0) {
            showError("No Selection", "Please select a participant to change.");
            return;
        }

        String participantName = getTrimmedParticipantName();
        String classType = getTrimmedClassType();
        String classTime = getTrimmedClassTime();

        if (participantName.isEmpty() || classType.isEmpty() || classTime.isEmpty()) {
            showError("Missing Information", "Please fill in all fields to change reservation.");
            return;
        }

        Reservation updatedReservation = new Reservation(participantName, classType, classTime);

        Reservation currentReservation = reservationComboBox.getItems().get(selectedIndex);
        if (areReservationsEqual(currentReservation, updatedReservation)) {
            logAction("No change happened (data identical).");
            return;
        }

        reservationComboBox.getItems().set(selectedIndex, updatedReservation);
        reservationComboBox.getSelectionModel().select(selectedIndex);
        logAction("Changed to: " + updatedReservation);
    }

    // --- Selection & State Management ---

    private void changeSelection(int index) {
        if (isUpdatingSelection) return;
        isUpdatingSelection = true;
        try {
            int listSize = reservationComboBox.getItems().size();
            boolean isValidIndex = index >= 0 && index < listSize;

            updateComboBoxSelection(index, isValidIndex);
            
            int targetSpinnerValue = calculateTargetSpinnerValue(index, listSize, isValidIndex);
            updateSpinnerValue(targetSpinnerValue);

            updateFieldsAndLog(index, isValidIndex, targetSpinnerValue);
        } finally {
            isUpdatingSelection = false;
        }
    }

    private void updateComboBoxSelection(int targetIndex, boolean isValidIndex) {
        int currentIndex = reservationComboBox.getSelectionModel().getSelectedIndex();
        if (isValidIndex) {
            if (currentIndex != targetIndex) {
                reservationComboBox.getSelectionModel().select(targetIndex);
            }
        } else {
            if (currentIndex != -1) {
                reservationComboBox.getSelectionModel().clearSelection();
            }
        }
    }

    private int calculateTargetSpinnerValue(int index, int listSize, boolean isValidIndex) {
        if (isValidIndex) {
            return index + 1;
        }
        return (listSize == 0) ? 0 : Math.min(20, listSize + 1);
    }

    private void updateSpinnerValue(int targetValue) {
        SpinnerValueFactory.IntegerSpinnerValueFactory valueFactory =
                (SpinnerValueFactory.IntegerSpinnerValueFactory) reservationPositionSpinner.getValueFactory();

        if (valueFactory.getValue() == null || valueFactory.getValue() != targetValue) {
            valueFactory.setValue(targetValue);
        }
    }

    private void updateFieldsAndLog(int index, boolean isValidIndex, int spinnerValue) {
        if (isValidIndex) {
            Reservation selectedReservation = reservationComboBox.getItems().get(index);
            populateFieldsFromSelection(selectedReservation);
            logSelectedRecordMessage(spinnerValue, selectedReservation);
            return;
        }

        clearFields();
        logSelectedRecordMessage(spinnerValue, null);
    }

    private void updateSpinnerRange() {
        int reservationCount = reservationComboBox.getItems().size();
        SpinnerValueFactory.IntegerSpinnerValueFactory valueFactory =
                (SpinnerValueFactory.IntegerSpinnerValueFactory) reservationPositionSpinner.getValueFactory();

        if (reservationCount == 0) {
            resetSpinnerForEmptyList(valueFactory);
            return;
        }

        int maxPosition = Math.min(20, reservationCount + 1);
        valueFactory.setMin(1);
        valueFactory.setMax(maxPosition);

        if (valueFactory.getValue() == null || valueFactory.getValue() < 1) {
            valueFactory.setValue(1);
        } else if (valueFactory.getValue() > maxPosition) {
            valueFactory.setValue(maxPosition);
        }
    }

    private void resetSpinnerForEmptyList(SpinnerValueFactory.IntegerSpinnerValueFactory valueFactory) {
        valueFactory.setMin(0);
        valueFactory.setMax(0);
        valueFactory.setValue(0);
    }

    private void logSelectedRecordMessage(int position, Reservation reservation) {
        if (reservation == null) {
            logAction("Selected record [" + position + "]: No element");
            return;
        }

        logAction("Selected record [" + position + "]: " + reservation);
    }

    private void populateFieldsFromSelection(Reservation reservation) {
        participantNameField.setText(reservation.participantName());
        classTypeField.setText(reservation.classType());
        classTimeField.setText(reservation.classTime());
    }

    // --- Data Helpers & Parsing ---

    private boolean hasMatchingReservation(Reservation reservationToFind) {
        for (Reservation reservation : reservationComboBox.getItems())
            if (areReservationsEqual(reservation, reservationToFind))
                return true;

        return false;
    }

    private boolean areReservationsEqual(Reservation firstReservation, Reservation secondReservation) {
        return firstReservation.participantName().equals(secondReservation.participantName())
                && firstReservation.classType().equals(secondReservation.classType())
                && firstReservation.classTime().equals(secondReservation.classTime());
    }

    private String getTrimmedParticipantName() {
        return participantNameField.getText() == null ? "" : participantNameField.getText().trim();
    }

    private String getTrimmedClassType() {
        return classTypeField.getText() == null ? "" : classTypeField.getText().trim();
    }

    private String getTrimmedClassTime() {
        return classTimeField.getText() == null ? "" : classTimeField.getText().trim();
    }

    private String buildPrintAllContent() {
        StringBuilder printAllContent = new StringBuilder();
        for (Reservation reservation : reservationComboBox.getItems()) 
            printAllContent.append(reservation).append("\n");

        return printAllContent.toString();
    }

    private Reservation parseReservationLine(String line) {
        String[] columns = line.split(";", -1);
        if (columns.length != 3) {
            return null;
        }

        String participantName = columns[0].trim();
        String classType = columns[1].trim();
        String classTime = columns[2].trim();
        if (participantName.isEmpty() || classType.isEmpty() || classTime.isEmpty()) {
            return null;
        }

        return new Reservation(participantName, classType, classTime);
    }

    private List<Reservation> parseReservationsFromFileLines(List<String> fileLines) {
        List<Reservation> parsedReservations = new ArrayList<>();
        int lineNumber = 0;

        for (String rawLine : fileLines) {
            lineNumber++;
            String line = rawLine == null ? "" : rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }

            Reservation parsedReservation = parseReservationLine(line);
            if (parsedReservation == null) {
                mainTextArea.setText("Invalid file format at line " + lineNumber + ":\n" + rawLine);
                showError("Invalid File Format", "Invalid line " + lineNumber + ": " + rawLine);
                return null;
            }

            if (!parsedReservations.contains(parsedReservation)) {
                parsedReservations.add(parsedReservation);
            }
        }

        return parsedReservations;
    }

    // --- UI Utilities ---

    private void logAction(String message) {
        resetStyle();
        statusLabel.setText(message);
    }

    private void showError(String errorTitle, String errorDetails) {
        statusLabel.setText("Error: " + errorTitle + " - " + errorDetails);
        statusLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
    }

    private void resetStyle() {
        statusLabel.setStyle("-fx-text-fill: black; -fx-font-weight: normal;");
    }

    private void resetStatus() {
        resetStyle();
        statusLabel.setText("Ready");
    }

    private void clearFields() {
        participantNameField.clear();
        classTypeField.clear();
        classTimeField.clear();
    }
}
