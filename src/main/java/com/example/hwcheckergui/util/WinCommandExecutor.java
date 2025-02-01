package com.example.hwcheckergui.util;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class WinCommandExecutor implements CommandExecutor {

    private int waitSeconds;

    public void setWaitSeconds(int sec) {
        waitSeconds = sec;
    }

    public String execute(String command, String path, boolean showLog) throws IOException, InterruptedException {
        File folder = new File(path);
        File output = new File("someplace");
        ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/C", command).directory(new File(folder.getPath())).redirectOutput(output);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        long pid = process.pid();
        if (!process.waitFor(waitSeconds, TimeUnit.SECONDS)) {
            String cmd = "taskkill /F /T /PID " + pid;
            Runtime.getRuntime().exec(cmd);
            return "tooLongNoRespond";
        }
        return FileUtils.readFileToString(output);
    }
}
