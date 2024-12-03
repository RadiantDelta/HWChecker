package com.example.hwcheckergui.Checker.util;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.example.hwcheckergui.Checker.Report;
import com.example.hwcheckergui.Checker.TestInfo;
import com.example.hwcheckergui.LaunchInfo;
import org.docx4j.jaxb.Context;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.exceptions.InvalidFormatException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.docx4j.wml.*;

public class DocGenerator {
    private static String endl = "\n";

    public static String generate(List<Report> reportList, LaunchInfo launchInfo) {
        WordprocessingMLPackage wordPackage = null;
        try {
            wordPackage = WordprocessingMLPackage.createPackage();
        } catch (InvalidFormatException e) {
            throw new RuntimeException(e);
        }
        MainDocumentPart mainDocumentPart = wordPackage.getMainDocumentPart();
        mainDocumentPart.addStyledParagraphOfText("Title", "Отчет!");
        mainDocumentPart
                .addParagraphOfText(reportList.get(0).getDesiredHW() == null ? "" : "Номер домашней работы " + reportList.get(0).getDesiredHW());
        mainDocumentPart.addParagraphOfText("Maven: " + (launchInfo.isMaven() ? "ДА" : "НЕТ"));
        mainDocumentPart.addParagraphOfText("Время ожидания ответа TIME (сек): " + launchInfo.getWaitSeconds());
        mainDocumentPart.addParagraphOfText("Папка с Students, Tests, Restrictions: " + launchInfo.getBaseFolderPath());
        mainDocumentPart.addParagraphOfText("Папка с Test1, Test2,...: " + launchInfo.getTestsFolderPath());
        mainDocumentPart.addParagraphOfText("Папка с jdk bin: " + launchInfo.getJdkBinPath());
        mainDocumentPart.addParagraphOfText("Использовать h2 driver из проверяющей системы: " + (launchInfo.isUseMyH2Driver() ? "ДА" : "НЕТ"));

        mainDocumentPart.addStyledParagraphOfText("Title", "Краткий обзор");
        Tbl overview = createFullTable(reportList);
        mainDocumentPart.addObject(overview);
        mainDocumentPart.addStyledParagraphOfText("Title", "Тесты");
        Tbl testResults = createTestsTable(reportList, launchInfo);
        mainDocumentPart.addObject(testResults);

        mainDocumentPart.addStyledParagraphOfText("Title", "Подробный обзор");

        for (Report report : reportList) {
            mainDocumentPart.addStyledParagraphOfText("Title", report.getStudentName());
            String rep = report.getDocReport();
            String[] repParts = rep.split(endl);
            for (String s : repParts) {
                mainDocumentPart.addParagraphOfText(s);
            }

            for (TestInfo ti : report.getTestsResults()) {
                String result = "";
                result += ti.getDocSummary();

                String[] tiParts = result.split(endl);
                for (String s : tiParts) {
                    addTabbedParagraph(mainDocumentPart, s);
                }

            }
        }

        ObjectFactory factory = Context.getWmlObjectFactory();
        P p = factory.createP();
        R r = factory.createR();
        Text t = factory.createText();
        t.setValue("someText");
        r.getContent().add(t);
        p.getContent().add(r);
        RPr rpr = factory.createRPr();
        BooleanDefaultTrue b = new BooleanDefaultTrue();
        rpr.setB(b);
        rpr.setI(b);
        rpr.setCaps(b);
        Color green = factory.createColor();
        green.setVal("green");
        rpr.setColor(green);
        r.setRPr(rpr);
        mainDocumentPart.getContent().add(p);

        String timeStamp = new SimpleDateFormat("yyyy.MM.dd(HH.mm.ss)").format(new java.util.Date());
        File exportFile = new File("report_" + timeStamp + ".docx");
        try {
            wordPackage.save(exportFile);
        } catch (Docx4JException e) {
            throw new RuntimeException(e);
        }
        return exportFile.getAbsolutePath();
    }

    private static Tbl createTestsTable(List<Report> reportList, LaunchInfo launchInfo) {
        ObjectFactory factory = new ObjectFactory();
        Tbl table = factory.createTbl();
        String[] testsNames = new File(launchInfo.getTestsFolderPath()).list();
        ArrayList<String> columnNames = new ArrayList<>(Arrays.asList("student"));
        columnNames.addAll(List.of(testsNames));
        int cols = columnNames.size();
        setTableColumnNames(table, columnNames);
        for (Report r : reportList) {
            Tr tableRow = factory.createTr();
            for (int col = 0; col < cols; col++) {
                Tc tableCell = factory.createTc();
                P paragraph = factory.createP();
                Text text = factory.createText();
                if (col == 0) {
                    text.setValue(r.getStudentName());
                } else {
                    if (r.isProjectIsPresent()) {
                        if (r.isJavacSuccess() || r.isMvnSuccess()) {
                            List<TestInfo> testResults = r.getTestsResults();
                            TestInfo testResult = testResults.get(col - 1);
                            if (!testResult.isTestOutputExist()) {
                                text.setValue("wrong test file struct");
                            } else if (testResult.isTooLongNoRespond()) {
                                text.setValue("too long");
                            } else if (!testResult.isOutputFilePresent()) {
                                if (testResult.getConsoleOutput().contains("Exception")) {
                                    text.setValue("Exception см. подробно");
                                } else {
                                    text.setValue("no output file");
                                }
                            } else if (!testResult.getConsoleOutput().equals("")) {
                                if (testResult.getConsoleOutput().contains("Exception")) {
                                    text.setValue("Exception см. подробно");
                                } else {
                                    text.setValue("см. подробно");
                                    if (!testResult.isNotSameFilePresent()) {
                                        text.setValue("+");
                                    }
                                }
                            } else if (!testResult.isNotSameFilePresent()) {
                                text.setValue("+");
                            } else {
                                text.setValue("wrong result");
                            }
                        } else {
                            text.setValue("");
                        }

                    }
                }
                R run = factory.createR();
                run.getContent().add(text);
                paragraph.getContent().add(run);
                tableCell.getContent().add(paragraph);
                tableRow.getContent().add(tableCell);
            }
            table.getContent().add(tableRow);
        }
        return table;
    }

    private static Tbl createFullTable(List<Report> reportList) {
        int rows = reportList.size() + 1;
        ObjectFactory factory = new ObjectFactory();
        Tbl table = factory.createTbl();
        List<String> columnNames = Arrays.asList("student", "src", "violations", "compiled", "tests passed", "compiled > TIME");
        int cols = columnNames.size();
        setTableColumnNames(table, columnNames);
        for (Report r : reportList) {
            Tr tableRow = factory.createTr();
            for (int col = 0; col < cols; col++) {
                Tc tableCell = factory.createTc();
                P paragraph = factory.createP();
                Text text = factory.createText();
                if (col == 0) {
                    text.setValue(r.getStudentName());
                }
                if (col == 1) {
                    text.setValue(r.isProjectIsPresent() ? "+" : "-");
                }
                if (col == 2) {
                    if (r.isProjectIsPresent()) {
                        text.setValue(r.getViolations().isEmpty() ? "-" : "+");
                    }
                }
                if (col == 3) {
                    if (r.isProjectIsPresent()) {
                        if (r.isJavacSuccess() || r.isMvnSuccess()) {
                            text.setValue("+");
                        } else {
                            text.setValue("-");
                        }
                    }
                }

                if (col == 4) {
                    if (r.isProjectIsPresent()) {
                        if (r.isJavacSuccess() || r.isMvnSuccess()) {
                            List<TestInfo> testResults = r.getTestsResults();
                            long amountOfCorrectTests = testResults.stream().filter(x -> {
                                if (!x.isOutputFilePresent()) {
                                    return false;
                                } else if (!x.isNotSameFilePresent()) {
                                    return true;
                                } else {
                                    return false;
                                }
                            }).count();
                            text.setValue(amountOfCorrectTests + "/" + testResults.size());
                        } else {
                            text.setValue("");
                        }
                    }
                }

                if (col == 5) {
                    if (r.isProjectIsPresent()) {
                        text.setValue((r.isJavacTimeout() || r.isMvnTimeout()) ? "+" : "-");
                    }
                }

                R run = factory.createR();
                run.getContent().add(text);
                paragraph.getContent().add(run);
                tableCell.getContent().add(paragraph);
                tableRow.getContent().add(tableCell);
            }
            table.getContent().add(tableRow);
        }

        return table;
    }

    private static void setTableColumnNames(Tbl table, List<String> names) {
        ObjectFactory factory = new ObjectFactory();
        Tr tableRow = factory.createTr();
        for (String name : names) {
            Tc tableCell = factory.createTc();
            P paragraph = factory.createP();
            Text text = factory.createText();
            text.setValue(name);
            R run = factory.createR();
            run.getContent().add(text);
            paragraph.getContent().add(run);
            tableCell.getContent().add(paragraph);
            tableRow.getContent().add(tableCell);
        }
        table.getContent().add(tableRow);
    }

    private static void addTabbedParagraph(MainDocumentPart mainDocumentPart, String str) {
        ObjectFactory obj = new ObjectFactory();
        P para = obj.createP();
        R run = obj.createR();
        R.Tab tab = obj.createRTab();
        Text text = obj.createText();
        text.setValue(str);
        run.getContent().add(tab);
        run.getContent().add(text);
        para.getContent().add(run);
        mainDocumentPart.addObject(para);
    }
}
