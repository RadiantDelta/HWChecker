package com.example.hwcheckergui.Checker.localization;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Localization {

    private static final String FILENAME = "strings.properties";
    private static Locale locale = new Locale("ru");
    private static ResourceBundle RESOURCE_BUNDLE;

    private Localization() {
    }

    public static String getString(String key) {
        ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("strings");
        try {
            return RESOURCE_BUNDLE.getString(key);
        } catch (MissingResourceException e) {
            System.out.println("Missed localization: '" + key + "'");
            return key;
        }
    }

    public static String getString(String key, Object... parameters) {
        String msg = getString(key);
        return MessageFormat.format(msg, parameters);
    }

}
