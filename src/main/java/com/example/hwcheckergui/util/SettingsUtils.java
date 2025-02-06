package com.example.hwcheckergui.util;

import java.io.*;

public class SettingsUtils {
    public static String read() throws IOException {
        BufferedReader br = new BufferedReader(new FileReader("settings.txt"));
        String line = br.readLine();
        br.close();
        return line;
    }


    public static void write(String str) throws IOException {
        Writer fileWriter = new FileWriter("settings.txt");
        fileWriter.write(str);
        fileWriter.close();
    }

    public static void delete() {
        File f = new File("settings.txt");
        f.delete();
    }
}
