package com.example.hwcheckergui.util;

import java.io.*;

public interface CommandExecutor {

    void setWaitSeconds(int waitSeconds);
    String execute(String command, String path, boolean showLog) throws IOException, InterruptedException ;

}
