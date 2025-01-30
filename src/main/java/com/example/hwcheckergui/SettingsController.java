package com.example.hwcheckergui;

import com.example.hwcheckergui.util.CommandExecutor;
import com.example.hwcheckergui.util.SettingsUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class SettingsController implements Initializable {
    @FXML
    private Button button_cancel;
    @FXML
    private Button button_apply;
    @FXML
    private Button button_choose_path;
    @FXML
    private TextField jdkPathField;
    @FXML
    private MainController mainController;

    @FXML
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    protected void onApplyButtonClick() {
        try {
            SettingsUtils.write(jdkPathField.getText());
        } catch (IOException e) {
            SettingsUtils.delete();
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setContentText(e.toString());
            a.show();
        }
        mainController.setDisabledLaunchCheckerButton(false);
        mainController.setStatusText("");
        Stage stage = (Stage) button_apply.getScene().getWindow();
        stage.close();

    }

    @FXML
    protected void onChoosePathButtonClick() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        Stage stage = (Stage) button_choose_path.getScene().getWindow();
        File selectedDir = directoryChooser.showDialog(stage);
        if (selectedDir != null) {
            jdkPathField.setText(selectedDir.getAbsolutePath());
        }
    }

    @FXML
    protected void onCancelButtonClick() {
        Stage stage = (Stage) button_cancel.getScene().getWindow();
        stage.close();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        button_apply.setDisable(true);
        jdkPathField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (jdkPathField.getText().equals("")) {
                button_apply.setDisable(true);
            } else {
                button_apply.setDisable(false);
            }
        });
        String jdkPath = "";
        try {
            jdkPath = SettingsUtils.read();
            if (jdkPath != null) {
                if (!jdkPath.trim().isBlank()) {
                    jdkPathField.setText(jdkPath);
                }
            } else {
                jdkPathField.setText("");
            }
        } catch (FileNotFoundException e) {
            SettingsUtils.delete();
            jdkPathField.setText("");
        } catch (IOException e) {
            SettingsUtils.delete();
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setContentText(e.toString());
            a.show();
        }
        String result = null;
        try {
            result = CommandExecutor.execute("\"" + jdkPath + "\\javac\" -version", "/", false);
        } catch (IOException e) {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setContentText(e.toString());
            a.show();
        } catch (InterruptedException e) {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setContentText(e.toString());
            a.show();
        }
    }
}
