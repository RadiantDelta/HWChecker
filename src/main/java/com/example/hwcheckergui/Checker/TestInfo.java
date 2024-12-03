package com.example.hwcheckergui.Checker;

import com.example.hwcheckergui.Checker.localization.Localization;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TestInfo {
    private static final String endl = "\n";
    private String consoleOutput = "";
    private final String testName;

    private boolean notSameFilePresent;
    private boolean outputFilePresent;
    private boolean tooLongNoRespond;

    private boolean testInputExist;

    private boolean testOutputExist;

    private boolean inputFilesAreAbsentInTestFolder;

    private List<File> wrongOutputFiles;

    public TestInfo(String testName) {
        this.testName = testName;
        wrongOutputFiles = new ArrayList<>();
    }

    public String getSummary() {
        String summary = "";
        summary += endl + testName + endl;
        if (tooLongNoRespond) {
            summary += endl + "too long No respond, jar process terminated" + endl;
        } else {
            summary += endl + (outputFilePresent ? Localization.getString("Console.Message.outputFileIsPresentAfterTest")
                    : Localization.getString("Console.Message.outputFileIsNotPresentAfterTest")) + endl;
            if (outputFilePresent) {
                summary += endl + (notSameFilePresent ? Localization.getString("Console.Message.outputFileHasNotMetExpectations")
                        : Localization.getString("Console.Message.outputFileHasMetExpectations")) + endl;
            }
        }
        return summary;
    }

    public String getDocSummary() {
        String summary = "";
        summary += endl + testName + endl;
        if (testOutputExist) {
            if (tooLongNoRespond) {
                summary += endl + "В ходе теста Программа не дала ответа в течение длительного времени, поэтому программа была остановлена" + endl;
            } else {
                summary += endl + (outputFilePresent ? "Выходной файл присутствует в папке проекта после проведения теста"
                        : "Выходной файл отсутствует в папке проекта после проведения теста") + endl;
                if (outputFilePresent) {
                    summary += endl + (notSameFilePresent ? "Выходной файл НЕ соответствует ожиданиям" : "Выходной файл соответствует ожиданиям")
                            + endl;
                }
                if (!consoleOutput.equals("")) {
                    summary += endl + "Выводит в консоль: " + consoleOutput + endl;
                }
            }
        } else {
            summary += endl + "Папки OUTPUT в тесте не обнаружено" + endl;
        }
        return summary;
    }

    public String getConsoleOutput() {
        return consoleOutput;
    }

    public void setConsoleOutput(String consoleOutput) {
        this.consoleOutput = consoleOutput;
    }

    public boolean isTooLongNoRespond() {
        return tooLongNoRespond;
    }

    public void setTooLongNoRespond(boolean tooLongNoRespond) {
        this.tooLongNoRespond = tooLongNoRespond;
    }

    public String getTestName() {
        return testName;
    }

    public boolean isOutputFilePresent() {
        return outputFilePresent;
    }

    public void setOutputFilePresent(boolean outputFilePresent) {
        this.outputFilePresent = outputFilePresent;
    }

    public List<File> getWrongOutputFiles() {
        return wrongOutputFiles;
    }

    public void setWrongOutputFiles(List<File> wrongOutputFiles) {
        this.wrongOutputFiles = wrongOutputFiles;
    }

    public boolean isNotSameFilePresent() {
        return notSameFilePresent;
    }

    public void setNotSameFilePresent(boolean notSameFilePresent) {
        this.notSameFilePresent = notSameFilePresent;
    }

    public boolean isTestInputExist() {
        return testInputExist;
    }

    public void setTestInputExist(boolean testInputExist) {
        this.testInputExist = testInputExist;
    }

    public boolean isTestOutputExist() {
        return testOutputExist;
    }

    public void setTestOutputExist(boolean testOutputExist) {
        this.testOutputExist = testOutputExist;
    }

    public boolean isInputFilesAreAbsentInTestFolder() {
        return inputFilesAreAbsentInTestFolder;
    }

    public void setInputFilesAreAbsentInTestFolder(boolean inputFilesAreAbsentInTestFolder) {
        this.inputFilesAreAbsentInTestFolder = inputFilesAreAbsentInTestFolder;
    }
}
