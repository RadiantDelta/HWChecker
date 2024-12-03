package com.example.hwcheckergui.Checker.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class RestrictionChecker {

    private static String WILDCARD_SUFFIX = "*";
    private static String SINGLE_COMMENT_PREFIX = "//";

    private List<String> restrictions;

    public RestrictionChecker() {
        restrictions = new ArrayList<>();
    }

    public void setRestrictions(String restrictionsFilePath) {
        File restrictionsFile = new File(restrictionsFilePath);
        try (Scanner sc = new Scanner(restrictionsFile)) {
            while (sc.hasNextLine()) {
                String data = sc.nextLine();
                restrictions.add(data);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isRestrictedImport(String imp) {
        boolean restricted = false;
        for (String restr : restrictions) {
            if (restr.endsWith(WILDCARD_SUFFIX)) {
                if ((restr.substring(0, restr.length() - 2)).equals(removeImportClassNameWithDot(imp))) {
                    restricted = true;
                    break;
                }
            } else if (restr.equals(imp)) {
                restricted = true;
                break;
            }
        }
        return restricted;
    }

    private String removeImportClassNameWithDot(String s) {
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() > 0 && sb.charAt(sb.length() - 1) != '.') {
            sb.setLength(sb.length() - 1);
        }
        sb.setLength(sb.length() - 1); // remove dot
        return sb.toString();
    }

    public boolean constructionsCorrect(File fileToCheck, List<String> usedRestrictedExpr, List<File> filesToCheck) {
        List<String> imports = new ArrayList<>();
        boolean restrictedConstrExist = false;
        try (Scanner sc = new Scanner(fileToCheck)) {
            while (sc.hasNextLine()) {
                String line = sc.nextLine().strip();
                for (String restr : restrictions) {
                    boolean isNameOfUserClass = false;
                    for (File ff : filesToCheck) {
                        if (ff.getName().replace(".java", "").equals(restr)) {
                            isNameOfUserClass = true;
                        }
                    }
                    if (line.contains(restr) && !isNameOfUserClass) {
                        restrictedConstrExist = true;
                        usedRestrictedExpr.add(line);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        boolean restrictedImportsPresent = false;
        for (String imp : imports) {
            if (isRestrictedImport(imp)) {
                restrictedImportsPresent = true;
                usedRestrictedExpr.add(imp);
            }
        }
        if (restrictedImportsPresent || restrictedConstrExist) {
            return false;
        } else {
            return true;
        }
    }
}