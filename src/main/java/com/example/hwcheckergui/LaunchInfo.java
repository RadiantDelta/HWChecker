package com.example.hwcheckergui;

import com.example.hwcheckergui.util.CommandExecutor;
import com.example.hwcheckergui.util.UnixCommandExecutor;
import com.example.hwcheckergui.util.WinCommandExecutor;
import javafx.scene.control.TextArea;
import javafx.scene.text.Text;

public class LaunchInfo {
    private Text status;
    private String jdkBinPath;
    private TextArea logText;
    private String baseFolderPath;
    private String testsFolderPath;
    private int waitSeconds;
    private boolean isMaven;
    private boolean useMyH2Driver;
    private boolean isLinux;


    public String getSeparator() {
        return isLinux ? ":" : ";";
    }
    public String getSlash() {
        return isLinux ? "/" : "\\";
    }

    public CommandExecutor getCe() {
        return isLinux ? new UnixCommandExecutor() : new WinCommandExecutor();
    }

    public void setLinux(boolean linux) {
        isLinux = linux;
    }

    public boolean isMaven() {
        return isMaven;
    }

    public void setMaven(boolean maven) {
        isMaven = maven;
    }

    public boolean isUseMyH2Driver() {
        return useMyH2Driver;
    }

    public void setUseMyH2Driver(boolean useMyH2Driver) {
        this.useMyH2Driver = useMyH2Driver;
    }

    public int getWaitSeconds() {
        return waitSeconds;
    }

    public void setWaitSeconds(int waitSeconds) {
        this.waitSeconds = waitSeconds;
    }

    public String getTestsFolderPath() {
        return testsFolderPath;
    }

    public void setTestsFolderPath(String testsFolderPath) {
        this.testsFolderPath = testsFolderPath;
    }

    public String getBaseFolderPath() {
        return baseFolderPath;
    }

    public void setBaseFolderPath(String baseFolderPath) {
        this.baseFolderPath = baseFolderPath;
    }

    public void setLogText(TextArea logText) {
        this.logText = logText;
    }

    public void addLog(String log) {
        logText.setText(log + logText.getText());
    }

    public String getJdkBinPath() {
        return jdkBinPath;
    }

    public void setJdkBinPath(String jdkBinPath) {
        this.jdkBinPath = jdkBinPath;
    }

    public Text getStatus() {
        return status;
    }

    public void setStatus(Text status) {
        this.status = status;
    }
}
