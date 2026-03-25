package uv.naloge.prvaNaloga;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

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
    private RadioButton registerRadioButton;

    @FXML
    private RadioButton unregisterRadioButton;

    @FXML
    private RadioButton changeReservationRadioButton;

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

    @FXML
    private RadioMenuItem languageSiMenuItem;

    @FXML
    private RadioMenuItem languageEngMenuItem;

    @FXML
    private ResourceBundle resources;

    private boolean isUpdatingSelection;
    private boolean suppressNextSelectionLog;

    // --- Initialization ---

    public void initialize() {
        initializeLanguageMenu();
        initializeSpinner();
        initializeComboBox();
        resetStatus();
    }

    private void initializeLanguageMenu() {
        ToggleGroup languageToggleGroup = new ToggleGroup();
        languageSiMenuItem.setToggleGroup(languageToggleGroup);
        languageEngMenuItem.setToggleGroup(languageToggleGroup);

        Locale currentLocale = AssignmentOneApplication.getCurrentLocale();
        if ("en".equalsIgnoreCase(currentLocale.getLanguage())) {
            languageEngMenuItem.setSelected(true);
            return;
        }

        languageSiMenuItem.setSelected(true);
    }

    private void initializeSpinner() {
        SpinnerValueFactory.IntegerSpinnerValueFactory factory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0,
                0, 0);
        factory.setWrapAround(false);
        reservationPositionSpinner.setValueFactory(factory);
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
                new Reservation("Charlie Davis", "Spin", "06:00"));

        reservationComboBox.getItems().addListener((ListChangeListener<Reservation>) change -> updateSpinnerRange());
        reservationComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue,
                newValue) -> changeSelection(reservationComboBox.getSelectionModel().getSelectedIndex()));

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
        logAction(getFieldToggleMessage(
                translateAndFormat("label.participantName.noColon"),
                toggleParticipantNameItem.isSelected()));
    }

    @FXML
    public void onToggleClassType() {
        classTypeField.setDisable(!toggleClassTypeItem.isSelected());
        logAction(getFieldToggleMessage(
                translateAndFormat("label.classType.noColon"),
                toggleClassTypeItem.isSelected()));
    }

    @FXML
    public void onToggleClassTime() {
        classTimeField.setDisable(!toggleClassTimeItem.isSelected());
        logAction(getFieldToggleMessage(
                translateAndFormat("label.classTime.noColon"),
                toggleClassTimeItem.isSelected()));
    }

    @FXML
    public void onExecuteAction() {
        Toggle selectedToggle = actionToggleGroup.getSelectedToggle();
        if (!(selectedToggle instanceof RadioButton selectedActionButton)) {
            showError(translateAndFormat("error.noAction.title"), translateAndFormat("error.noAction.details"));
            return;
        }

        if (selectedActionButton == registerRadioButton) {
            registerParticipant();
            return;
        }

        if (selectedActionButton == unregisterRadioButton) {
            unregisterParticipant();
            return;
        }

        if (selectedActionButton == changeReservationRadioButton) {
            changeReservation();
            return;
        }

        logAction(translateAndFormat("status.unknownAction"));
    }

    @FXML
    public void onSelectRegisterAction() {
        registerRadioButton.setSelected(true);
    }

    @FXML
    public void onSelectUnregisterAction() {
        unregisterRadioButton.setSelected(true);
    }

    @FXML
    public void onSelectChangeReservationAction() {
        changeReservationRadioButton.setSelected(true);
    }

    @FXML
    public void onPrintAll() {
        logAction(translateAndFormat("status.printingAll"));
        mainTextArea.setText(buildPrintAllContent());
    }

    @FXML
    public void onAbout() {
        try {
            AssignmentOneApplication.openExternalLink(translateAndFormat("about.repositoryUrl"));
            logAction(translateAndFormat("status.openedRepository"));
        } catch (Exception exception) {
            showError(translateAndFormat("error.about.title"), translateAndFormat("error.about.details"));
        }
    }

    @FXML
    public void onLanguageSi() {
        AssignmentOneApplication.switchLanguage(Locale.forLanguageTag("sl"));
    }

    @FXML
    public void onLanguageEng() {
        AssignmentOneApplication.switchLanguage(Locale.forLanguageTag("en"));
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
        fileChooser.setTitle(translateAndFormat("dialog.save.title"));
        fileChooser.getExtensionFilters()
                .add(new FileChooser.ExtensionFilter(translateAndFormat("dialog.fileFilter.text"), "*.txt"));
        File selectedFile = fileChooser.showSaveDialog(statusLabel.getScene().getWindow());
        if (selectedFile == null) {
            logAction(translateAndFormat("status.saveCancelled"));
            return;
        }

        String fileContent = buildPrintAllContent();
        try {
            Files.writeString(selectedFile.toPath(), fileContent, StandardCharsets.UTF_8);
            logAction(translateAndFormat("status.saved", selectedFile.getName()));
        } catch (IOException exception) {
            showError(translateAndFormat("error.saveFailed.title"), exception.getMessage());
        }
    }

    @FXML
    public void onOpen() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(translateAndFormat("dialog.open.title"));
        fileChooser.getExtensionFilters()
                .add(new FileChooser.ExtensionFilter(translateAndFormat("dialog.fileFilter.text"), "*.txt"));
        File selectedFile = fileChooser.showOpenDialog(statusLabel.getScene().getWindow());
        if (selectedFile == null) {
            logAction(translateAndFormat("status.openCancelled"));
            return;
        }

        List<String> fileLines;
        try {
            fileLines = Files.readAllLines(selectedFile.toPath(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            showError(translateAndFormat("error.openFailed.title"), exception.getMessage());
            return;
        }

        List<Reservation> parsedReservations = parseReservationsFromFileLines(fileLines);
        if (parsedReservations == null) {
            return;
        }

        mainTextArea.clear();
        reservationComboBox.getItems().setAll(parsedReservations);
        if (reservationComboBox.getItems().isEmpty()) {
            changeSelection(-1);
        } else {
            changeSelection(0);
        }

        logAction(translateAndFormat("status.openedRows", selectedFile.getName(), parsedReservations.size()));
    }

    // --- Action Logic ---

    private void registerParticipant() {
        if (participantNameField.isDisabled() || classTypeField.isDisabled() || classTimeField.isDisabled()) {
            showError(translateAndFormat("error.invalidInput.title"), translateAndFormat("error.invalidInput.details"));
            return;
        }

        String participantName = getTrimmedParticipantName();
        String classType = getTrimmedClassType();
        String classTime = getTrimmedClassTime();

        if (participantName.isEmpty() || classType.isEmpty() || classTime.isEmpty()) {
            showError(translateAndFormat("error.invalidInput.title"), translateAndFormat("error.invalidInput.details"));
            return;
        }

        Reservation newReservation = new Reservation(participantName, classType, classTime);

        if (hasMatchingReservation(newReservation)) {
            showError(translateAndFormat("error.duplicateEntry.title"),
                    translateAndFormat("error.duplicateEntry.details"));
            return;
        }

        reservationComboBox.getItems().add(newReservation);
        reservationComboBox.getSelectionModel().select(newReservation);
        logAction(translateAndFormat("status.registered", newReservation));
    }

    private void unregisterParticipant() {
        int selectedIndex = reservationComboBox.getSelectionModel().getSelectedIndex();
        if (selectedIndex < 0) {
            showError(translateAndFormat("error.noSelection.title"),
                    translateAndFormat("error.noSelection.unregisterDetails"));
            return;
        }

        int deletedPosition = selectedIndex + 1;
        Reservation removedReservation = reservationComboBox.getItems().remove(selectedIndex);
        logAction(translateAndFormat("status.deletedRecord", deletedPosition, removedReservation));

        if (reservationComboBox.getItems().isEmpty()) {
            clearFields();
            return;
        }

        suppressNextSelectionLog = true;
        int newIndex = Math.min(selectedIndex, reservationComboBox.getItems().size() - 1);
        reservationComboBox.getSelectionModel().select(newIndex);
    }

    private void changeReservation() {
        if (participantNameField.isDisabled() && classTypeField.isDisabled() && classTimeField.isDisabled()) {
            showError(translateAndFormat("error.actionNotAllowed.title"),
                    translateAndFormat("error.actionNotAllowed.details"));
            return;
        }

        int selectedIndex = reservationComboBox.getSelectionModel().getSelectedIndex();
        if (selectedIndex < 0) {
            showError(translateAndFormat("error.noSelection.title"),
                    translateAndFormat("error.noSelection.changeDetails"));
            return;
        }

        String participantName = getTrimmedParticipantName();
        String classType = getTrimmedClassType();
        String classTime = getTrimmedClassTime();

        if (participantName.isEmpty() || classType.isEmpty() || classTime.isEmpty()) {
            showError(translateAndFormat("error.missingInformation.title"),
                    translateAndFormat("error.missingInformation.details"));
            return;
        }

        Reservation updatedReservation = new Reservation(participantName, classType, classTime);

        Reservation currentReservation = reservationComboBox.getItems().get(selectedIndex);
        if (areReservationsEqual(currentReservation, updatedReservation)) {
            logAction(translateAndFormat("status.noChange"));
            return;
        }

        reservationComboBox.getItems().set(selectedIndex, updatedReservation);
        reservationComboBox.getSelectionModel().select(selectedIndex);
        logAction(translateAndFormat("status.changedTo", updatedReservation));
    }

    // --- Selection & State Management ---

    private void changeSelection(int index) {
        if (isUpdatingSelection)
            return;
        isUpdatingSelection = true;
        try {
            int listSize = reservationComboBox.getItems().size();

            if (listSize > 0 && index < 0) {
                index = 0;
            }

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
        if (listSize == 0)
            return 0;
        if (index < 0)
            return 1;
        return Math.min(20, listSize + 1);
    }

    private void updateSpinnerValue(int targetValue) {
        SpinnerValueFactory.IntegerSpinnerValueFactory valueFactory = (SpinnerValueFactory.IntegerSpinnerValueFactory) reservationPositionSpinner
                .getValueFactory();

        if (valueFactory.getValue() == null || valueFactory.getValue() != targetValue) {
            valueFactory.setValue(targetValue);
        }
    }

    private void updateFieldsAndLog(int index, boolean isValidIndex, int spinnerValue) {
        boolean shouldLogSelection = !suppressNextSelectionLog;
        suppressNextSelectionLog = false;

        if (isValidIndex) {
            Reservation selectedReservation = reservationComboBox.getItems().get(index);
            populateFieldsFromSelection(selectedReservation);
            if (shouldLogSelection) {
                logSelectedRecordMessage(spinnerValue, selectedReservation);
            }
            return;
        }

        clearFields();
        if (shouldLogSelection) {
            logSelectedRecordMessage(spinnerValue, null);
        }
    }

    private void updateSpinnerRange() {
        int reservationCount = reservationComboBox.getItems().size();
        SpinnerValueFactory.IntegerSpinnerValueFactory valueFactory = (SpinnerValueFactory.IntegerSpinnerValueFactory) reservationPositionSpinner
                .getValueFactory();

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
            logAction(translateAndFormat("status.selectedRecord", position, translateAndFormat("status.noElement")));
            return;
        }

        logAction(translateAndFormat("status.selectedRecord", position, reservation));
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
                mainTextArea.setText(translateAndFormat("error.invalidFileFormat.textArea", lineNumber, rawLine));
                showError(translateAndFormat("error.invalidFileFormat.title"),
                        translateAndFormat("error.invalidFileFormat.line", lineNumber, rawLine));
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
        statusLabel.setText(translateAndFormat("status.ready"));
    }

    private void clearFields() {
        participantNameField.clear();
        classTypeField.clear();
        classTimeField.clear();
    }

    private String translateAndFormat(String key, Object... parameters) {
        String template = resources.getString(key);
        return java.text.MessageFormat.format(template, parameters);
    }

    private String getFieldToggleMessage(String fieldName, boolean isEnabled) {
        String enabledStatus = isEnabled ? translateAndFormat("status.enabled") : translateAndFormat("status.disabled");
        return translateAndFormat("status.fieldToggle", fieldName, enabledStatus);
    }
}
