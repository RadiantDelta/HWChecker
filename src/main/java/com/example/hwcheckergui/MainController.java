package com.example.hwcheckergui;

import com.example.hwcheckergui.Checker.Launcher;
import com.example.hwcheckergui.util.SettingsUtils;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import org.apache.commons.lang3.SystemUtils;

import java.io.*;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainController implements Initializable {

    private boolean execWasInterrupted = false;
    private Window window;
    private Thread t;
    private Task<Void> task;

    private boolean isLinux;

    @FXML
    private ScrollPane scrollPane;
    @FXML
    private TextField baseFolderPathField;
    @FXML
    private TextField testsFolderPathField;
    @FXML
    private TextField waitSecondsField;

    @FXML
    public TextArea logText_;
    @FXML
    public Text statusText;
    @FXML
    private Button button_launch_checker;
    @FXML
    private Button button_interrupt;
    @FXML
    private Button button_choose_base_folder_path;
    @FXML
    private Button button_choose_tests_folder_path;

    @FXML
    private RadioButton radiobutton_maven;
    @FXML
    private RadioButton radio_button_use_my_h2_driver;


    public boolean isLinux() {
        return isLinux;
    }

    public void setStatusText(String status) {
        statusText.setText(status);
    }

    public void setDisabledLaunchCheckerButton(boolean disabled) {
        button_launch_checker.setDisable(disabled);
    }

    @FXML
    protected void onInterruptButtonClick() {
        Platform.runLater(() -> {
            if (t != null && t.isAlive()) {
                task.cancel(true);
                t.interrupt();
                execWasInterrupted = true;
            }
        });
        resetButtons();
        statusText.setText(statusText.getText() + " ПРЕРВАНО");
    }

    @FXML
    protected void onChooseTestsFolderPathButtonClick() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        if (!testsFolderPathField.getText().isEmpty()) {
            directoryChooser.setInitialDirectory(new File(testsFolderPathField.getText()));
        }
        Stage stage = (Stage) button_choose_tests_folder_path.getScene().getWindow();
        File selectedDir = directoryChooser.showDialog(stage);
        if ((selectedDir != null) && (Arrays.stream(selectedDir.listFiles()).anyMatch((x) -> x.getName().toLowerCase().contains("test")))) {
            testsFolderPathField.setText(selectedDir.getAbsolutePath());
            button_launch_checker.setDisable(false);
        }
        if (testsFolderPathField.getText().equals("")) {
            button_launch_checker.setDisable(true);
        }
    }

    @FXML
    protected void onChooseBaseFolderPathButtonClick() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        if (!baseFolderPathField.getText().isEmpty()) {
            directoryChooser.setInitialDirectory(new File(baseFolderPathField.getText()));
        }
        Stage stage = (Stage) button_choose_base_folder_path.getScene().getWindow();
        File selectedDir = directoryChooser.showDialog(stage);
        if ((selectedDir != null) && (Arrays.stream(selectedDir.listFiles()).anyMatch((x) -> x.getName().equalsIgnoreCase("students")))) {
            baseFolderPathField.setText(selectedDir.getAbsolutePath());
            button_launch_checker.setDisable(false);
        }
        if (baseFolderPathField.getText().equals("")) {
            button_launch_checker.setDisable(true);
        }
    }

    @FXML
    protected void onCleanLogButtonClick() {
        logText_.setText("");
    }

    @FXML
    protected void onLaunchCheckerButtonClick() {
        try {
            String jdkPath = SettingsUtils.read();
            task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {

                    LaunchInfo launchInfo = new LaunchInfo();
                    launchInfo.setMaven(radiobutton_maven.isSelected());
                    launchInfo.setUseMyH2Driver(radio_button_use_my_h2_driver.isSelected());
                    launchInfo.setStatus(statusText);
                    launchInfo.setWaitSeconds(Integer.parseInt(waitSecondsField.getText()));
                    launchInfo.setJdkBinPath(jdkPath);
                    launchInfo.setLogText(logText_);
                    launchInfo.addLog(new SimpleDateFormat("\n\nyyyy.MM.dd(HH.mm.ss)\n\n").format(new java.util.Date()));
                    launchInfo.setBaseFolderPath(baseFolderPathField.getText());
                    launchInfo.setTestsFolderPath(testsFolderPathField.getText());
                    button_launch_checker.setDisable(true);
                    button_interrupt.setDisable(false);

                    launchInfo.setLinux(isLinux);

                    Launcher l = new Launcher();

                    l.launchChecker(launchInfo);
                    return null;
                }
            };

            task.setOnSucceeded(event -> resetButtons());
            task.setOnFailed(event -> {
                resetButtons();
                if (!execWasInterrupted) {
                    Throwable exception = task.getException();
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Task Error");
                    alert.setHeaderText("An error occurred");

                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    exception.printStackTrace(pw);
                    TextArea area = new TextArea(sw.toString());
                    area.setWrapText(true);
                    area.setEditable(false);
                    alert.getDialogPane().setContent(area);
                    alert.setResizable(true);
                    alert.showAndWait();
                }
                execWasInterrupted = false;
            });

            t = new Thread(task);
            t.start();
        } catch (IOException e) {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setContentText(e.toString());
            a.show();
        }
    }

    private void resetButtons() {
        button_launch_checker.setDisable(false);
        button_interrupt.setDisable(true);
    }

    @FXML
    protected void onSettingsButtonClick() {

        try {
            FXMLLoader fxmlLoader = new FXMLLoader(HWCheckerApplication.class.getResource("settings-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 500, 300);
            Stage stage = new Stage();
            stage.setTitle("Settings");
            stage.setScene(scene);
            stage.show();
            SettingsController controller = fxmlLoader.getController();
            controller.setMainController(this);
        } catch (Exception e) {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setContentText(e.toString());
            a.show();
        }
    }

    private boolean isValid(String text) {
        Pattern my_pattern = Pattern.compile("[^0-9 ]", Pattern.CASE_INSENSITIVE);
        Matcher my_match = my_pattern.matcher(text);
        boolean check = my_match.find();
        if (check) {
            return false;
        } else {
            return true;
        }
    }

    @FXML
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

//        isLinux = true;

        isLinux = SystemUtils.IS_OS_LINUX;

        Platform.runLater(() -> {
            button_launch_checker.setDisable(false);
            window = button_launch_checker.getScene().getWindow();
            window.addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, this::closeWindowEvent);
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scrollPane.setFitToWidth(true);
            scrollPane.setContent(logText_);
            UnaryOperator<TextFormatter.Change> numberValidationFormatter = change -> {
                if (change.getText().matches("\\d+")) {
                    return change; // if change is a number
                } else {
                    change.setText(""); // else make no change
                    return change;
                }
            };

            TextFormatter tf = new TextFormatter(numberValidationFormatter);
            waitSecondsField.setTextFormatter(tf);
        });

        waitSecondsField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (waitSecondsField.getText().equals("")) {
                button_launch_checker.setDisable(true);
            } else {
                button_launch_checker.setDisable(false);
            }
        });

        if (baseFolderPathField.getText().equals("")) {
            button_launch_checker.setDisable(true);
        }

        try {
            SettingsUtils.read();
        } catch (FileNotFoundException e) {
            SettingsUtils.delete();
            statusText.setText("Путь к jdk не задан");
            button_launch_checker.setDisable(true);
        } catch (IOException e) {
            SettingsUtils.delete();
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setContentText(e.toString());
            a.show();
        }
    }

    private void closeWindowEvent(WindowEvent event) {
        if (t != null && t.isAlive()) {
            t.interrupt();
        }
        Platform.exit();
    }

}
