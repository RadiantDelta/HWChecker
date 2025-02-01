package com.example.hwcheckergui.Checker;

import com.example.hwcheckergui.Checker.localization.Localization;

import java.util.ArrayList;
import java.util.List;

public class Report {
    private static String endl = "\n";
    private String studentName;
    private String studentFolder;
    private String desiredHW;
    private String testsForHWFolder;
    private String javacFailureMsg;
    private String mvnFailureMsg;

    private boolean multipleMainJavaExists;
    private boolean mainJavaExists;
    private boolean javacSuccess;
    private boolean mvnSuccess;
    private boolean mvnTimeout;
    private boolean javacTimeout;
    private boolean isMvn;
    private List<String> violations;
    private List<TestInfo> testsResults;
    private boolean projectIsPresent = false;
    private String divider = "|||------------------------------------------------------------------|||";

    public Report(String studentName) {
        this.studentName = studentName;
        testsResults = new ArrayList<>();
    }

    public String getReport() {
        String report = "";
        report += endl + endl + Localization.getString("Console.Message.report") + endl + endl;
        report += endl + studentName + endl;
        report += endl + studentFolder + endl;
        if (desiredHW != null) {
            report += endl + desiredHW + endl;
        }

        if (isProjectIsPresent()) {
            report += endl
                    + (isMvn ? Localization.getString("Console.Message.mavenProject") : Localization.getString("Console.Message.notMavenProject"))
                    + endl;

            String violationsString = "";
            for (String violation : violations) {
                violationsString = violationsString + endl + violation;
            }
            if (violationsString.equals("")) {
                report += endl + Localization.getString("Console.Message.RestrictedJavaConstructionsAreNotUsed") + endl;
            } else {
                report += endl + Localization.getString("Console.Message.RestrictedJavaConstructionsAreUsed") + ":" + endl;
                report += endl + violationsString + endl;
            }

            if (!isMvn) {
                if (javacTimeout) {
                    report += endl + "javac" + " " + "Process destroyed due to timeout." + endl;
                } else {
                    report += endl + (javacSuccess ? Localization.getString("Console.Message.javacSuccessful")
                            : Localization.getString("Console.Message.javacFailure") + endl + javacFailureMsg);
                }
            } else {
                if (mvnTimeout) {
                    report += endl + "mvn" + " " + "Process destroyed due to timeout." + endl;
                } else {
                    report += endl + (mvnSuccess ? Localization.getString("Console.Message.mvnSuccessful")
                            : Localization.getString("Console.Message.mvnFailure")) + endl + mvnFailureMsg;
                }
            }

            if (isMvnSuccess() || isJavacSuccess()) {
                String testInfoString = "";
                for (TestInfo testInfo : testsResults) {
                    testInfoString = testInfoString + endl + testInfo.getSummary();
                }
                report += endl + Localization.getString("Console.Message.testResults") + ":" + endl + testInfoString + endl;

            } else {
                report += endl + "Project is not present" + endl;
            }

            report += endl + Localization.getString("Console.Message.end") + endl;
        }
        return report;
    }

    public String getDocReport() {
        String docReport = "";
        docReport += endl + "Имя студента: " + studentName + endl;
        if (desiredHW != null) {
            docReport += endl + "Работа: " + desiredHW + endl;
        }
        docReport += endl + "Директория студента: " + studentFolder + endl;
        if (isProjectIsPresent()) {
            docReport += endl + "Папка проекта: присутствует" + endl;
        } else {
            docReport += endl + "Папка проекта: отсутствует" + endl;
        }
        if (isProjectIsPresent()) {
            docReport += endl + "Результаты проверки:" + endl;

            String violationsString = "";
            for (String violation : violations) {
                violationsString = violationsString + endl + violation;
            }
            if (violationsString.equals("")) {
                docReport += endl + "Запрещенные Java конструкции НЕ используются" + endl;
            } else {
                docReport += endl + "Используются следующие запрещенные Java конструкции" + ":" + endl;
                docReport += endl + divider + endl;
                docReport += endl + violationsString + endl;
                docReport += endl + divider + endl;
            }
            docReport += endl + "|||------------------- РЕЗУЛЬТАТЫ КОМПИЛЯЦИИ (javac/mvn package) -------------------|||" + endl;

            if (!multipleMainJavaExists) {
                if (mainJavaExists) {
                    if (!isMvn) {// значит javac
                        if (javacTimeout) {
                            docReport += endl + "javac" + " " + "Процесс слишком долго выполнялся." + endl;
                        } else { // javac завершился
                            docReport += endl + (javacSuccess ? "javac выполнена успешно" : "javac ОШИБКА" + endl + divider + javacFailureMsg + divider);
                        }
                    } else {
                        if (mvnTimeout) {
                            docReport += endl + "mvn package" + " " + "Процесс слишком долго выполнялся." + endl;
                        } else {
                            docReport += endl + (mvnSuccess ? "mvn package выполнена успешно" : "mvn package ОШИБКА" + endl + mvnFailureMsg) + endl
                                    + divider + endl;
                        }
                    }
                } else {
                    docReport += endl + "Main.java отсутствует в проекте" + endl;
                }
            } else {
                docReport += endl + "В проекте присутствует больше одного Main.java" + endl;
            }
        }

        return docReport;
    }

    public boolean isMainJavaExists() {
        return mainJavaExists;
    }

    public void setMainJavaExists(boolean mainJavaExists) {
        this.mainJavaExists = mainJavaExists;
    }

    public String getJavacFailureMsg() {
        return javacFailureMsg;
    }

    public void setJavacFailureMsg(String javacFailureMsg) {
        this.javacFailureMsg = javacFailureMsg;
    }

    public String getMvnFailureMsg() {
        return mvnFailureMsg;
    }

    public void setMvnFailureMsg(String mvnFailureMsg) {
        this.mvnFailureMsg = mvnFailureMsg;
    }

    public boolean isJavacTimeout() {
        return javacTimeout;
    }

    public void setJavacTimeout(boolean javacTimeout) {
        this.javacTimeout = javacTimeout;
    }

    public boolean isMvnTimeout() {
        return mvnTimeout;
    }

    public void setMvnTimeout(boolean mvnTimeout) {
        this.mvnTimeout = mvnTimeout;
    }

    public boolean isMvn() {
        return isMvn;
    }

    public void setMvn(boolean mvn) {
        isMvn = mvn;
    }

    public boolean isMvnSuccess() {
        return mvnSuccess;
    }

    public void setMvnSuccess(boolean mvnSuccess) {
        this.mvnSuccess = mvnSuccess;
    }

    public List<TestInfo> getTestsResults() {
        return testsResults;
    }

    public void setTestsResults(List<TestInfo> testsResults) {
        this.testsResults = testsResults;
    }

    public String getTestsForHWFolder() {
        return testsForHWFolder;
    }

    public void setTestsForHWFolder(String testsForHWFolder) {
        this.testsForHWFolder = testsForHWFolder;
    }

    public String getDesiredHW() {
        return desiredHW;
    }

    public void setDesiredHW(String desiredHW) {
        this.desiredHW = desiredHW;
    }

    public List<String> getViolations() {
        return violations;
    }

    public void setViolations(List<String> violations) {
        this.violations = violations;
    }

    public String getStudentName() {
        return studentName;
    }

    public boolean isJavacSuccess() {
        return javacSuccess;
    }

    public void setJavacSuccess(boolean javacSuccess) {
        this.javacSuccess = javacSuccess;
    }

    public String getStudentFolder() {
        return studentFolder;
    }

    public void setStudentFolder(String studentFolder) {
        this.studentFolder = studentFolder;
    }

    public boolean isProjectIsPresent() {
        return projectIsPresent;
    }

    public void setProjectIsPresent(boolean projectisPresent) {
        this.projectIsPresent = projectisPresent;
    }

    public boolean isMultipleMainJavaExists() {
        return multipleMainJavaExists;
    }

    public void setMultipleMainJavaExists(boolean multipleMainJavaExists) {
        this.multipleMainJavaExists = multipleMainJavaExists;
    }
}