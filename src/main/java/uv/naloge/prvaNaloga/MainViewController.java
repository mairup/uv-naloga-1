package uv.naloge.prvaNaloga;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
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

public class MainViewController {

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
    private Button executeActionButton;

    @FXML
    private ComboBox<Reservation> reservationComboBox;

    @FXML
    private Spinner<Integer> reservationPositionSpinner;

    @FXML
    private TextArea mainTextArea;

    @FXML
    private Label messageLabel;

    @FXML
    private Label statusLabel; // The Status Bar Label

    public void initialize() {
        initializeSpinner();
        initializeComboBox();
    }

    private void initializeSpinner() {
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 1);
        reservationPositionSpinner.setValueFactory(valueFactory);
    }

    private void initializeComboBox() {
        reservationComboBox.getItems().addAll(
                new Reservation("Jane Doe", "Yoga", "18:00"),
                new Reservation("John Smith", "Pilates", "19:30"),
                new Reservation("Alice Johnson", "Zumba", "17:00"),
                new Reservation("Bob Brown", "CrossFit", "20:00"),
                new Reservation("Charlie Davis", "Spin", "06:00")
        );
        reservationComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            populateFieldsFromSelection(newValue);
            updateReservationPositionFromIndex(reservationComboBox.getSelectionModel().getSelectedIndex());
        });

        if (!reservationComboBox.getItems().isEmpty()) {
            reservationComboBox.getSelectionModel().selectFirst();
        }
    }

    private void updateReservationPositionFromIndex(int index) {
        int position = getReservationPositionFromIndex(index);
         statusLabel.setText("Selected Position: " + position);
    }

    private int getReservationPositionFromIndex(int index) {
        return index + 1;
    }

    private void populateFieldsFromSelection(Reservation selection) {
        if (selection != null) {
            participantNameField.setText(selection.participantName());
            classTypeField.setText(selection.classType());
            classTimeField.setText(selection.classTime());
        }
    }

    @FXML
    public void onToggleParticipantName() {
        participantNameField.setDisable(!toggleParticipantNameItem.isSelected());
        statusLabel.setText("Participant Name " + (toggleParticipantNameItem.isSelected() ? "Enabled" : "Disabled"));
    }

    @FXML
    public void onToggleClassType() {
        classTypeField.setDisable(!toggleClassTypeItem.isSelected());
        statusLabel.setText("Class Type " + (toggleClassTypeItem.isSelected() ? "Enabled" : "Disabled"));
    }

    @FXML
    public void onToggleClassTime() {
        classTimeField.setDisable(!toggleClassTimeItem.isSelected());
        statusLabel.setText("Class Time " + (toggleClassTimeItem.isSelected() ? "Enabled" : "Disabled"));
    }

    @FXML
    public void onExecuteAction() {
        messageLabel.setText("Action Executed");
        statusLabel.setText("Action Executed");
    }

    @FXML
    public void onOpen() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Resource File");
        File file = fileChooser.showOpenDialog(statusLabel.getScene().getWindow());
        if (file != null) {
            statusLabel.setText("File opened: " + file.getName());
        } else {
            statusLabel.setText("Open command cancelled");
        }
    }

    @FXML
    public void onClear() {
        messageLabel.setText("");
        statusLabel.setText("");
    }

    @FXML
    public void onExit() {
        Platform.exit();
    }

    @FXML
    public void onPrintAll() {
        statusLabel.setText("Printing all items...");
        // Placeholder for print functionality
    }
}
