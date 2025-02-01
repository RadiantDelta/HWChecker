package com.example.hwcheckergui.util;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class UnixCommandExecutor implements CommandExecutor {

    private int waitSeconds;

    private String suPassword;

    public UnixCommandExecutor() {}

    public UnixCommandExecutor(String suPassword){
        this.suPassword = suPassword;
    }

    public void setWaitSeconds(int sec) {
        waitSeconds = sec;
    }

    public String execute(String commands, String path, boolean showLog) throws IOException, InterruptedException {
        File output = new File("someplace");

        String sudo = "echo " + suPassword + " | sudo -S sh -c ";
        String commandBuilder =  sudo + "\"" + commands  + "\"";

        commandBuilder = "cd \"" + path + "\"" + " && (" + commandBuilder + ")";
        ProcessBuilder builder = new ProcessBuilder("/bin/bash", "-c", commandBuilder)
                .redirectOutput(output);
        builder.redirectErrorStream(true);

//        System.out.println("COMand: " + commandBuilder);

        Process process = builder.start();
        long pid = process.pid();
        if (!process.waitFor(waitSeconds, TimeUnit.SECONDS)) {
            String cmd = "kill -9 " + pid;
            Runtime.getRuntime().exec(cmd);
            return "tooLongNoRespond";
        }
        return FileUtils.readFileToString(output);
    }

}
