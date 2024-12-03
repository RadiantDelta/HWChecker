package com.example.hwcheckergui.util;

import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.*;

public class CommandExecutor {

    private static int waitSeconds;

    public static int getWaitSeconds() {
        return waitSeconds;
    }

    public static void setWaitSeconds(int waitSeconds) {
        CommandExecutor.waitSeconds = waitSeconds;
    }

    public static String execute(String command, String path, boolean showLog) throws IOException, InterruptedException {
        String result = "";
        File folder = new File(path);
        Logger logger = Logger.getLogger(CommandExecutor.class.getName());
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
