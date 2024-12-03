package com.example.hwcheckergui.Checker;

import com.example.hwcheckergui.Checker.localization.Localization;
import com.example.hwcheckergui.Checker.util.*;
import com.example.hwcheckergui.LaunchInfo;
import com.example.hwcheckergui.util.CommandExecutor;
import javafx.application.Platform;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class Launcher {
    public static final String HW_PREFIX = "HW";
    public static final String JAR_SUFFIX = ".jar";
    public static final String JAVA_SUFFIX = ".java";
    public static final String BUILD_FAILURE = "BUILD FAILURE";
    public static final String BUILD_SUCCESS = "BUILD SUCCESS";
    public static final String CP1251 = "Cp1251";
    public static final String UTF8 = "utf8";
    public static final String CLASS_SUFFIX = ".class";
    public static final String ZIP_EXT = ".zip";
    public static final String SEVEN_Z_EXT = ".7z";
    public static final String RAR_EXT = ".rar";
    public static String javacCmd = "";
    public static String javaCmd = "";
    public static final String endl = "\n";

    public static void launchChecker(LaunchInfo launchInfo) throws IOException, InterruptedException {

        boolean ifDoesNotExistUseMyH2Jar = launchInfo.isUseMyH2Driver();

        CommandExecutor.setWaitSeconds(launchInfo.getWaitSeconds());
        javacCmd = "\"" + launchInfo.getJdkBinPath() + "\\javac" + "\"";
        javaCmd = "\"" + launchInfo.getJdkBinPath() + "\\java" + "\"";
        String javacVersionCmd = CommandExecutor.execute("javac -version", launchInfo.getJdkBinPath(), false);
        String javacVersion = StringUtils.substringBetween(javacVersionCmd, "javac ", ".");
        int hwNumber = 1;

        String hwFolderPathStr = launchInfo.getBaseFolderPath();

        boolean noHWFolders;

        String desiredHW = "";
        String studentsFolder = hwFolderPathStr + "\\Students";
        String testsForHWFolder = launchInfo.getTestsFolderPath();
        if (new File(testsForHWFolder).getName().toUpperCase().contains("HW")) {
            noHWFolders = false;
            desiredHW = new File(testsForHWFolder).getName();
        } else {
            noHWFolders = true;
        }
        unzipProjects(studentsFolder, desiredHW, noHWFolders, launchInfo);
        Platform.runLater(() -> launchInfo.addLog("Unarchived" + endl));
        File hwStudentsFolderPath = new File(studentsFolder);
        String students[] = hwStudentsFolderPath.list();

        RestrictionChecker rc = new RestrictionChecker();
        rc.setRestrictions(hwFolderPathStr + "\\Restrictions\\restriction.txt");
        List<Report> reports = new ArrayList<>();

        AtomicInteger count = new AtomicInteger(1);
        for (String st : students) {
            Platform.runLater(() -> launchInfo.getStatus().setText(count.getAndIncrement() + "/" + students.length + " " + st));
            Platform.runLater(() -> launchInfo.addLog(Localization.getString("Console.Message.student") + " " + st + endl));

            Report report = new Report(st);
            String studentFolder = studentsFolder + "\\" + st;
            String projectPath = null;
            File srcFolder = null;
            if (!noHWFolders) {
                projectPath = studentFolder + "\\" + desiredHW;
                if (new File(projectPath).exists()) {
                    srcFolder = findFolder(new File(projectPath), "src");
                }
                report.setDesiredHW(desiredHW);
            } else {// studentFolder has only 1 archive - 1 work
                srcFolder = findFolder(new File(studentFolder), "src");
            }
            if (srcFolder != null) {
                projectPath = srcFolder.getParent();
            } else {
                report.setProjectIsPresent(false);
            }
            report.setTestsForHWFolder(testsForHWFolder);
            report.setStudentFolder(hwStudentsFolderPath + "\\" + st);
            if (projectPath == null || srcFolder == null) {
                Platform.runLater(() -> launchInfo.addLog("Project" + " " + Localization.getString("Console.Message.isMissing") + " or" + endl));
                Platform.runLater(() -> launchInfo.addLog(st + " " + Localization.getString("Console.Message.projectHasNoSrcFolder")));
                report.setProjectIsPresent(false);
                reports.add(report);
                continue;
            } else {
                report.setProjectIsPresent(true);
            }

            // Проверить, есть ли в проекте студента «запрещенные» Java-конструкции

            List<String> violations = new ArrayList<>();
            List<File> javaFiles = new ArrayList<>();
            List<File> jarFiles = new ArrayList<>();
            findFilesWithEnding(new File(projectPath), javaFiles, JAVA_SUFFIX, false);
            findFilesWithEnding(new File(projectPath), jarFiles, JAR_SUFFIX, false);
            if (ifDoesNotExistUseMyH2Jar) {
                Pattern h2JarPattern = Pattern.compile("h2.*.jar", Pattern.CASE_INSENSITIVE);
                boolean h2Found = false;
                for (File jarFile : jarFiles) {
                    Matcher matcher = h2JarPattern.matcher(jarFile.getName());
                    if (matcher.find()) {
                        h2Found = true;
                        break;
                    }
                }
                if (!h2Found) {
                    jarFiles.add(new File("driver/h2-2.2.222.jar"));
                }
            }
            Platform.runLater(() -> launchInfo.addLog("jar files: " + endl));
            jarFiles.stream().forEach(x -> {
                Platform.runLater(() -> launchInfo.addLog(x.getAbsolutePath() + endl));
            });

            Platform.runLater(() -> launchInfo.addLog("Before findViolations" + endl));
            findViolationsInFiles(javaFiles, violations, rc);
            Platform.runLater(() -> launchInfo.addLog("After findViolations" + endl));

            report.setViolations(violations);

            // Пройти по папкам тестов. Для каждого теста...
            String tests[] = (new File(testsForHWFolder)).list();
            byte testNum = 1;
            int testAmount = tests.length;
            boolean showLogJavac = true;
            boolean showLogJava = true;
            boolean showTestlog = true;
            boolean showLog = true;
            boolean fullMvnBuildLog = true;

            report.setMvn(launchInfo.isMaven());
            if (report.isMvn()) {
                String targetPath = projectPath + "\\target";
                if (new File(targetPath).exists()) {
                    DirDeleter.delete2(new File(targetPath));
                }
            }

            // <COMPILATION>
            // очистим от предыдущих .class

            List<File> classFilesDelete = new ArrayList<>();
            Platform.runLater(() -> launchInfo.addLog("Before find files with ending .class" + endl));
            findFilesWithEnding(new File(studentFolder), classFilesDelete, CLASS_SUFFIX, false);
            Platform.runLater(() -> launchInfo.addLog("After find files with ending .class" + endl));
            Platform.runLater(() -> launchInfo.addLog("Before delete files with ending .class" + endl));
            classFilesDelete.stream().forEach(el -> el.delete());
            Platform.runLater(() -> launchInfo.addLog("After delete files with ending .class" + endl));

            boolean mainJavaExists = false;
            if (!report.isMvn()) {
                ArrayList<String> sourcePaths = new ArrayList<>();
                for (File f : javaFiles) {
                    sourcePaths.add(f.getAbsolutePath().replace(projectPath + "\\", ""));
                }
                String sourcepaths = sourcePaths.stream().collect(Collectors.joining(";"));
                String javaFilesLocalPaths = sourcePaths.stream().collect(Collectors.joining(" "));
                if (showLog) {
                    Platform.runLater(() -> launchInfo.addLog(Localization.getString("Console.Message.startJavacMainJava") + endl));
                }

                for (File f : javaFiles) {
                    if (f.getName().equals("Main.java")) {
                        mainJavaExists = true;
                        String jarLocalPath = "";
                        if (!jarFiles.isEmpty()) {
                            for (File jf : jarFiles) {
                                jarLocalPath += jf.getAbsolutePath().replace((projectPath + "\\"), "");
                                jarLocalPath += ";";
                            }
                        }

                        String command = genJavacCommand(f, projectPath, jarLocalPath, javacVersion, launchInfo, javaFilesLocalPaths, showLogJavac);

                        if (showLogJavac) {
                            String[] commandWrap = { command };
                            String[] projectPathWrap = { projectPath };
                            Platform.runLater(() -> launchInfo.addLog("COMMAND: " + commandWrap[0] + endl));
                            Platform.runLater(() -> launchInfo.addLog("ON PATH: " + projectPathWrap[0] + endl));
                        }

                        String cmdResult = CommandExecutor.execute(command, projectPath, showLogJavac);
                        if (showLogJavac) {
                            String[] cmdResultWrap = { cmdResult };
                            Platform.runLater(() -> launchInfo.addLog(cmdResultWrap[0] + endl));
                        }
                        if (!cmdResult.equals("tooLongNoRespond")) {
                            if (cmdResult.contains("unmappable character")) {
                                command = command.replace(CP1251, UTF8);
                                cmdResult = CommandExecutor.execute(command, projectPath, showLogJavac);
                                if (showLogJavac) {
                                    String[] projectPathWrap = { projectPath };
                                    String[] commandWrap = { command };
                                    Platform.runLater(() -> launchInfo
                                            .addLog("|||||||||||||||||||||AFTER ENCODING SWAP|||||||||||||||||||||||||||||||||" + endl));
                                    Platform.runLater(() -> launchInfo.addLog("COMMAND " + commandWrap[0] + endl));
                                    Platform.runLater(() -> launchInfo.addLog("ON PATH: " + projectPathWrap[0] + endl));
                                }
                            }
                            if (showLogJavac) {
                                String[] cmdResultWrap = { cmdResult };
                                Platform.runLater(() -> launchInfo.addLog(cmdResultWrap[0] + endl));

                            }
                            if (cmdResult.isEmpty()) {
                                report.setJavacSuccess(true);
                                if (showLogJavac) {
                                    Platform.runLater(() -> launchInfo.addLog(Localization.getString("Console.Message.javacSuccessful") + endl));
                                }
                            } else {
                                report.setJavacSuccess(false);
                                report.setJavacFailureMsg(cmdResult);
                                if (showLogJavac) {
                                    Platform.runLater(() -> launchInfo.addLog(Localization.getString("Console.Message.javacFailure") + endl));
                                }
                            }
                        } else {
                            report.setJavacTimeout(true);
                        }
                        break;
                    }
                }
                report.setMainJavaExists(mainJavaExists);
                if (!mainJavaExists) {
                    Platform.runLater(() -> launchInfo.addLog(Localization.getString("Console.Message.mainJavaDoesNotExist") + endl));
                }
                if (showLog) {
                    Platform.runLater(() -> launchInfo.addLog(Localization.getString("Console.Message.endJavacMainJava") + endl));
                }
            } else {
                for (File f : javaFiles) {
                    if (f.getName().equals("Main.java")) {
                        mainJavaExists = true;
                        // check if pom file is configured to produce jar-with-dependencies instead of jar
                        String packName = getPackage(f);
                        String packNameClassName = (packName + (packName.equals("") ? "" : ".") + f.getName()).replace(JAVA_SUFFIX, "");
                        improvePom(new File(projectPath), packNameClassName);

                        String command = genMvnCommand(f, launchInfo.getJdkBinPath()); // GENERATING JAR
                        if (showLogJava) {
                            String[] projectPathWrap = { projectPath };
                            String[] commandWrap = { command };
                            Platform.runLater(() -> launchInfo.addLog("COMMAND " + commandWrap[0] + endl));
                            Platform.runLater(() -> launchInfo.addLog("ON PATH: " + projectPathWrap[0] + endl));
                        }
                        String cmdResult = CommandExecutor.execute(command, projectPath, showLogJava);
                        if (!cmdResult.equals("tooLongNoRespond")) {
                            if (fullMvnBuildLog) {
                                Platform.runLater(() -> launchInfo.addLog("<cmdResutl>" + endl));
                                String[] cmdResultWrap = { cmdResult };
                                Platform.runLater(() -> launchInfo.addLog(cmdResultWrap[0] + endl));
                                Platform.runLater(() -> launchInfo.addLog("</cmdResutl>" + endl));
                            }
                            if (cmdResult.contains(BUILD_SUCCESS)) {
                                report.setMvnSuccess(true);
                                if (showLogJava) {
                                    Platform.runLater(() -> launchInfo.addLog(Localization.getString("Console.Message.mvnSuccessful") + endl));
                                }
                            } else if (cmdResult.contains(BUILD_FAILURE)) {
                                report.setMvnSuccess(false);
                                report.setMvnFailureMsg(cmdResult);
                                if (showLogJava) {
                                    Platform.runLater(() -> launchInfo.addLog(Localization.getString("Console.Message.mvnFailure") + endl));
                                }
                            }

                        } else {
                            report.setMvnTimeout(true);
                        }
                        break;
                    }
                }
                report.setMainJavaExists(mainJavaExists);
                if (!mainJavaExists) {
                    Platform.runLater(() -> launchInfo.addLog(Localization.getString("Console.Message.mainJavaDoesNotExist") + endl));
                }
            }

            // </COMPILATION>

            if (report.isJavacSuccess() || report.isMvnSuccess()) {
                for (String test : tests) {
                    TestInfo testInfo = new TestInfo(test);
                    if (showTestlog) {
                        String[] strWrap = { "\n\nTEST #" + testNum++ + "/" + testAmount + "\n" + projectPath };
                        Platform.runLater(() -> launchInfo.addLog(strWrap[0] + endl));
                    }
                    String inputTest = testsForHWFolder + "\\" + test + "\\" + "INPUT";
                    String outputTest = testsForHWFolder + "\\" + test + "\\" + "OUTPUT";
                    if (!(new File(inputTest).exists())) {
                        testInfo.setTestInputExist(false);
                        Platform.runLater(() -> launchInfo.addLog("Test INPUT Folder does NOT exist" + endl));
                    } else {
                        testInfo.setTestInputExist(true);
                        Platform.runLater(() -> launchInfo.addLog("Test INPUT Folder exists" + endl));
                    }
                    if (!(new File(outputTest).exists())) {
                        Platform.runLater(() -> launchInfo.addLog("Test OUTPUT Folder does NOT exist" + endl));
                        testInfo.setTestOutputExist(false);
                        List<TestInfo> testResults = report.getTestsResults();
                        testResults.add(testInfo);
                        break;
                    }

                    testInfo.setTestOutputExist(true);
                    File inputFiles[] = (new File(inputTest)).listFiles();
                    File outputFiles[] = (new File(outputTest)).listFiles();

                    if (inputFiles.length == 0) {
                        testInfo.setInputFilesAreAbsentInTestFolder(true);
                    }

                    List<File> toChange = new ArrayList<File>(Arrays.asList(inputFiles));
                    List<File> toDelete = new ArrayList<File>(Arrays.asList(outputFiles));
                    Platform.runLater(() -> launchInfo.addLog("Before delete output and put input files" + endl));
                    List<File> deletedOutputFiles = new ArrayList<>();
                    try {
                        Platform.runLater(() -> launchInfo.addLog("Before put input files" + endl));
                        for (File f : toChange) {
                            Platform.runLater(() -> launchInfo.addLog("Before put input file " + f.getAbsolutePath() + endl));
                            Files.write(new File(projectPath + "\\" + f.getName()).toPath(), Files.readAllBytes(f.toPath()));
                            Platform.runLater(() -> launchInfo.addLog("After put input file " + f.getAbsolutePath() + endl));
                        }
                        Platform.runLater(() -> launchInfo.addLog("After put input files" + endl));
                        deleteFiles(new File(projectPath), toDelete, deletedOutputFiles, launchInfo);
                        Platform.runLater(() -> launchInfo.addLog("After delete output files" + endl));
                        if (deletedOutputFiles.isEmpty()) {
                            if (showTestlog) {
                                Platform.runLater(
                                        () -> launchInfo.addLog(Localization.getString("Console.Message.outputFilesToDeleteWereNotLocated") + endl));
                            }
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    Platform.runLater(() -> launchInfo.addLog("After delete output and put input files" + endl));
                    if (!report.isMvn()) {
                        List<File> classFiles = new ArrayList<>();
                        findFilesWithEnding(new File(projectPath), classFiles, CLASS_SUFFIX, false);
                        HashSet<String> classPaths = new HashSet<>();
                        for (File f : classFiles) {
                            classPaths.add(f.getParent());
                        }
                        String classpaths = classPaths.stream().collect(Collectors.joining(" "));

                        if (report.isJavacSuccess()) {
                            if (showLog) {
                                Platform.runLater(() -> launchInfo.addLog("START JAVA MAIN.CLASS" + endl));
                            }
                            for (File f : classFiles) {
                                if (f.getName().equals("Main.class")) {
                                    String jarLocalPath = "";
                                    if (!jarFiles.isEmpty()) {
                                        for (File jf : jarFiles) {
                                            jarLocalPath += jf.getAbsolutePath().replace((projectPath + "\\"), "");
                                            jarLocalPath += ";";
                                        }
                                    }
                                    // GENERATE JAVA COMMAND
                                    String command = genJavaCommand(f, projectPath, jarLocalPath, launchInfo, showLogJava);
                                    if (showLogJava) {
                                        String[] projectPathWrap = { projectPath };
                                        String[] commandWrap = { command };
                                        Platform.runLater(() -> launchInfo.addLog("COMMAND " + commandWrap[0] + endl));
                                        Platform.runLater(() -> launchInfo.addLog("ON PATH: " + projectPathWrap[0] + endl));
                                    }
                                    String cmdResult = CommandExecutor.execute(command, projectPath, showLogJava);
                                    if (cmdResult.equals("tooLongNoRespond")) {
                                        testInfo.setTooLongNoRespond(true);
                                        if (showLogJava) {
                                            Platform.runLater(() -> launchInfo.addLog("too long No respond, jar process terminated" + endl));
                                        }
                                    }
                                    testInfo.setConsoleOutput(cmdResult);
                                    break;
                                }
                            }
                            if (showLog) {
                                Platform.runLater(() -> launchInfo.addLog("END JAVA MAIN.CLASS" + endl));
                            }
                        }
                    } else { // Maven Project
                        for (File f : javaFiles) {
                            if (f.getName().equals("Main.java")) {
                                String command = genMvnJavaJarDependenciesCommand(f, new File(projectPath), javaCmd); // STARTING GENERATED JAR
                                if (showLogJava) {
                                    String[] projectPathWrap = { projectPath };
                                    String[] commandWrap = { command };
                                    Platform.runLater(() -> launchInfo.addLog("COMMAND " + commandWrap[0] + endl));
                                    Platform.runLater(() -> launchInfo.addLog("ON PATH: " + projectPathWrap[0] + endl));
                                }
                                String cmdResult = CommandExecutor.execute(command, projectPath, showLogJava);
                                if (cmdResult.equals("tooLongNoRespond")) {
                                    testInfo.setTooLongNoRespond(true);
                                    if (showLogJava) {
                                        Platform.runLater(() -> launchInfo.addLog("too long No respond, jar process terminated" + endl));
                                    }
                                }
                                testInfo.setConsoleOutput(cmdResult);
                                break;
                            }
                        }
                    }

                    if (showLog) {
                        Platform.runLater(() -> launchInfo.addLog(Localization.getString("Console.Message.checkingTestResults") + endl));
                    }
                    createTestResults(outputFiles, testInfo, projectPath, launchInfo, showLog);
                    List<TestInfo> testResults = report.getTestsResults();
                    testResults.add(testInfo);

                    // remove test files which were put to project folder,
                    List<File> deletedInputFiles = new ArrayList<File>();
                    try {
                        deleteFiles(new File(projectPath), toChange, deletedInputFiles, launchInfo);
                        deleteFiles(new File(projectPath), toDelete, deletedOutputFiles, launchInfo);
                    } catch (IOException e) {
                        Platform.runLater(() -> launchInfo.addLog(e.getMessage() + endl));
                    }
                }
            }
            reports.add(report);
        }

        Platform.runLater(() -> launchInfo.addLog("REPORTS\n\n" + endl));
        String generatedDocxPath = DocGenerator.generate(reports, launchInfo);
        Platform.runLater(() -> launchInfo.getStatus().setText("Completed"));
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(new File(generatedDocxPath));
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        for (Report r : reports) {
            Platform.runLater(() -> launchInfo.addLog(r.getReport() + "\n" + endl));
        }
    }

    private static void createTestResults(File[] outputFiles, TestInfo testInfo, String projectPath, LaunchInfo launchInfo, boolean showLog) {
        List<File> toCompareWith = new ArrayList<>(Arrays.asList(outputFiles));
        for (File f : toCompareWith) {
            if (showLog) {
                Platform.runLater(
                        () -> launchInfo.addLog(Localization.getString("Console.Message.resultForOutputFile") + " - " + f.getName() + " : " + endl));
            }
            File findResult = null;
            findResult = findFile(new File(projectPath), f.getName(), false);
            if ((findResult == null)) {
                if (showLog) {
                    Platform.runLater(() -> launchInfo.addLog(Localization.getString("Console.Message.outputFileIsNotPresentAfterTest") + endl));
                }
            } else {
                List<File> wrongOutputFiles = testInfo.getWrongOutputFiles();
                testInfo.setOutputFilePresent(true);
                boolean areSame = areFilesSame(findResult, f);
                if (areSame) {
                    if (showLog) {
                        Platform.runLater(() -> launchInfo.addLog(Localization.getString("Console.Message.outputFileHasMetExpectations") + endl));
                    }
                } else {
                    if (showLog) {
                        Platform.runLater(() -> launchInfo.addLog(Localization.getString("Console.Message.outputFileHasNotMetExpectations") + endl));
                    }
                    testInfo.setNotSameFilePresent(true);
                    wrongOutputFiles.add(findResult);
                }
                // rename output file
                Path source = Paths.get(findResult.getAbsolutePath());
                try {
                    String ext = getFileExt(findResult.getName());
                    Files.move(source, source.resolveSibling(findResult.getName().replace(ext, "") + testInfo.getTestName() + ext),
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static String getFileExt(String filename) {
        String result = "";
        for (int i = 0; i < filename.length(); i++) {
            if (filename.charAt(filename.length() - 1 - i) == '.') {
                result = filename.substring(filename.length() - 1 - i, filename.length());
                break;
            }
        }
        return result;
    }

    private static boolean isArchive(String fileName) {
        return fileName.contains(ZIP_EXT) || fileName.contains(SEVEN_Z_EXT) || fileName.contains(RAR_EXT);
    }

    private static void improvePom(File projectFolder, String packageMainClass) {
        File[] files = projectFolder.listFiles();
        boolean pluginMavenAssemblyPluginExist = false;
        boolean dependencyMavenAssemblyPluginExist = false;
        for (File f : files) {
            if (f.getName().equals("pom.xml")) {
                // add dependency maven-assembly-plugin
                try {
                    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                    DocumentBuilder db = dbf.newDocumentBuilder();
                    Document doc = db.parse(f);

                    doc.getDocumentElement().normalize();
                    XmlEditor.execute(f, packageMainClass);
                } catch (IOException | SAXException e) {
                    e.printStackTrace();
                } catch (ParserConfigurationException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public static boolean areFilesSame(File fileToCheck, File expectedFile) {
        boolean areFilesEqual = true;
        try (FileReader fr1 = new FileReader(fileToCheck.getAbsolutePath()); FileReader fr2 = new FileReader(expectedFile.getAbsolutePath())) {
            int char1, char2;
            while (true) {
                char1 = fr1.read();
                char2 = fr2.read();
                // Break if both files reach end of file
                if (char1 == -1 && char2 == -1) {
                    break;
                }
                // If characters don't match or one file ends before the other
                if (char1 != char2) {
                    areFilesEqual = false;
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return areFilesEqual;
    }

    public static String genJavaCommand(File f, String projectPath, String jarLocalPath, LaunchInfo launchInfo, final boolean showLogJava) {
        String packName = getPackage(new File(f.getAbsolutePath().replace(CLASS_SUFFIX, JAVA_SUFFIX)));
        String packNameClassName = (packName + (packName.equals("") ? "" : ".") + f.getName()).replace(CLASS_SUFFIX, "");
        String packNameClassNameSlash = packNameClassName.replace(".", "\\");
        String pathWherePackIsStored = (f.getAbsolutePath()).replace(packNameClassNameSlash + CLASS_SUFFIX, "");
        String localpathWherePackIsStored = pathWherePackIsStored.replace((projectPath + "\\"), "");

        if (showLogJava) {
            Platform.runLater(() -> launchInfo.addLog("packName: " + packName + endl));
            Platform.runLater(() -> launchInfo.addLog("packNameClassName: " + packNameClassName + endl));
            Platform.runLater(() -> launchInfo.addLog("packNameClassNameSlash: " + packNameClassNameSlash + endl));
            Platform.runLater(() -> launchInfo.addLog("localpathWherePackIsStored: " + localpathWherePackIsStored + endl));
        }

        String result = f.getAbsolutePath().replace(projectPath + "\\", "").replace(CLASS_SUFFIX, "").replace(localpathWherePackIsStored, "")
                .replace("\\", "/");
        String combinedClassesAndLibsStr = localpathWherePackIsStored + (localpathWherePackIsStored.equals("") ? "" : ";") + jarLocalPath
                + ";src\\main\\java;";
        String command = javaCmd + " ";

        if (!(combinedClassesAndLibsStr.isBlank() || combinedClassesAndLibsStr.isEmpty())) {
            command = command + "-classpath " + combinedClassesAndLibsStr;
        }
        command = command + " " + result;

        return command;
    }

    public static String genJavacCommand(File f, String projectPath, String jarLocalPath, String javacVersion, LaunchInfo launchInfo,
            String javaFilesLocalPaths, final boolean showLogJavac) {

        String packName = getPackage(f);
        String packNameClassName = (packName + (packName.equals("") ? "" : ".") + f.getName()).replace(".java", "");
        String packNameClassNameSlash = packNameClassName.replace(".", "\\");
        String pathWherePackIsStored = (f.getAbsolutePath()).replace(packNameClassNameSlash + ".java", "");
        String localpathWherePackIsStored = pathWherePackIsStored.replace((projectPath + "\\"), "");
        String result = f.getAbsolutePath().replace(projectPath + "\\", "").replace(packNameClassNameSlash, packNameClassName);
        result = f.getAbsolutePath().replace(projectPath + "\\", "");
        String currentEncoding = CP1251;
        String command = javacCmd + " -target " + javacVersion + " -encoding " + currentEncoding;
        if (localpathWherePackIsStored != "") {
            command = command + " -sourcepath " + localpathWherePackIsStored;
        }
        if (jarLocalPath != "") {
            command = command + " -classpath " + jarLocalPath;
        }
        command = command + " " + result + " " + javaFilesLocalPaths;

        if (showLogJavac) {
            String[] localpathWherePackIsStoredWrap = { localpathWherePackIsStored };
            Platform.runLater(() -> launchInfo.addLog("PACKAGE: " + packName + "\nPACKAGE+CLASS: " + packNameClassName + "\nPACKAGE\\: "
                    + packNameClassNameSlash + endl + "pathWherePackIsStored: " + pathWherePackIsStored + endl + "localpathWherePackIsStored: "
                    + localpathWherePackIsStoredWrap[0] + endl + "f.getAbsolutePath(): " + f.getAbsolutePath()));
        }

        return command;
    }

    private static String genMvnCommand(File f, String jdkBinPath) {
        return "set \"JAVA_HOME=" + jdkBinPath.substring(0, jdkBinPath.length() - 4) + "\"" + " && " + "mvn package";
    }

    private static String genMvnJavaJarDependenciesCommand(File f, File projectFolder, String javaCmd) {
        String classNameWithPackage = getClassNameWithPackage(f);
        String targetPath = projectFolder.getAbsolutePath() + "\\target";
        String[] targetFiles = new File(targetPath).list();
        String jarFile = "";
        for (String el : targetFiles) {
            if (el.contains("jar-with-dependencies" + JAR_SUFFIX)) {
                jarFile = el;
                break;
            }
        }
        return javaCmd + " -cp target/" + jarFile + " " + classNameWithPackage;
    }

    private static String getClassNameWithPackage(File f) {
        String packName = getPackage(new File(f.getAbsolutePath().replace(CLASS_SUFFIX, JAVA_SUFFIX)));
        return packName + (packName.equals("") ? "" : ".") + f.getName().replace(JAVA_SUFFIX, "");
    }

    public static File findFile(File folder, String fileName, boolean alreadyInSrc) {
        File result = null;
        for (File fileEntry : folder.listFiles()) {
            if (fileEntry.getName().equals("src") && alreadyInSrc) { // if nested projects - leave
                return null;
            }
        }
        for (File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                if (fileEntry.getName().equals("src")) {
                    result = findFile(fileEntry, fileName, true);
                    if (result != null) {
                        return result;
                    }
                }
                result = findFile(fileEntry, fileName, alreadyInSrc);
                if (result != null) {
                    return result;
                }
            } else {
                if (fileEntry.getName().equals(fileName)) {
                    return fileEntry;
                }
            }
        }
        return result;
    }

    public static File findFolder(File folder, String folderName) {
        File result = null;
        for (File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                if (fileEntry.getName().equalsIgnoreCase("h2")) {
                    continue;
                }
                if (fileEntry.getName().equals(folderName)) {
                    return fileEntry;
                }
                result = findFolder(fileEntry, folderName);
                if (result != null) {
                    return result;
                }
            }
        }
        return result;
    }

    public static void findFilesWithEnding(File folder, List<File> javaFiles, String ending, boolean alreadyInSrc) {
        for (File fileEntry : folder.listFiles()) {
            if (fileEntry.getName().equals("src") && alreadyInSrc) { // if nested projects - leave
                return;
            }
        }
        for (File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                if (fileEntry.getName().equalsIgnoreCase("h2")) {// skipping library files but include h2*.jar
                    findFilesWithEnding(new File(fileEntry.getAbsolutePath() + "\\bin"), javaFiles, ending, true);
                    continue;
                }
                if (fileEntry.getName().equalsIgnoreCase("h2-2.2.222")) {
                    continue;
                }
            }
            if (fileEntry.isDirectory()) {
                if (fileEntry.getName().equals("src")) {
                    findFilesWithEnding(fileEntry, javaFiles, ending, true);
                } else {
                    findFilesWithEnding(fileEntry, javaFiles, ending, alreadyInSrc);
                }
            } else {
                if (fileEntry.getName().endsWith(ending)) {
                    javaFiles.add(fileEntry);
                }
            }
        }
    }

    public static void findViolationsInFiles(List<File> javaFiles, List<String> violations, RestrictionChecker rc) {
        for (File fileEntry : javaFiles) {
            ArrayList<String> usedRestrictionConstructs = new ArrayList<>();
            boolean constructionsAreCorrect = rc.constructionsCorrect(fileEntry, usedRestrictionConstructs, javaFiles);
            if (!constructionsAreCorrect) {
                String constructionsList = String.join(";\n", usedRestrictionConstructs);
                String message = fileEntry.getAbsolutePath() + " has following restricted Java constructions:\n" + constructionsList;
                violations.add(message);
            }
        }
    }

    public static boolean deleteFiles(File folder, List<File> toDelete, List<File> deletedOutputFiles, LaunchInfo launchInfo) throws IOException {
        boolean filesWereChanged = false;
        for (File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                filesWereChanged = filesWereChanged || deleteFiles(fileEntry, toDelete, deletedOutputFiles, launchInfo);
            } else {
                for (File f : toDelete) {
                    if ((f.getName()).equals(fileEntry.getName())) {
                        if (fileEntry.delete()) {
                            deletedOutputFiles.add(fileEntry);
                        } else {
                            Platform.runLater(() -> launchInfo.addLog("file " + fileEntry.getName() + " is not deleted" + endl));
                        }
                    }
                }
            }
        }
        return filesWereChanged;
    }

    private static void unzipProjects(String studentsFolder, String desiredHW, boolean noHWFolders, LaunchInfo launchInfo)
            throws IOException, InterruptedException {
        File hwStudentsFolderPath = new File(studentsFolder);
        String students[] = hwStudentsFolderPath.list();
        String desiredArchive = "";
        for (String st : students) {
            String studentFolder = studentsFolder + "\\" + st;
            List<File> studentFiles = Arrays.asList((new File(studentFolder)).listFiles());
            if (studentFiles.stream().anyMatch(x -> x.isDirectory())) {
                if (studentFiles.stream().anyMatch(x -> isArchive(x.getName()))) {
                    for (File f : studentFiles) {
                        if (f.isDirectory()) {
                            DirDeleter.deleteDirectory(f);
                        }
                    }
                }
            }
            if (!noHWFolders) {
                boolean desiredArchiveExists = false;
                for (File f : studentFiles) {
                    if (f.getName().equalsIgnoreCase(desiredHW + ZIP_EXT)) {
                        desiredArchiveExists = true;
                        desiredArchive = desiredHW + ZIP_EXT;
                    }
                    if (f.getName().equalsIgnoreCase(desiredHW + SEVEN_Z_EXT)) {
                        desiredArchiveExists = true;
                        desiredArchive = desiredHW + SEVEN_Z_EXT;
                    }
                    if (f.getName().equalsIgnoreCase(desiredHW + RAR_EXT)) {
                        desiredArchiveExists = true;
                        desiredArchive = desiredHW + RAR_EXT;
                    }
                }
                if (desiredArchiveExists) {
                    if (studentFiles.stream().anyMatch(desiredHW::equals)) {// удаляем распакованный ранее zip
                        DirDeleter.deleteDirectory(new File(studentFolder + "\\" + desiredHW));
                    }
                    new File(studentFolder + "\\" + desiredHW).mkdirs();
                    Unzipper.unzipFile(Path.of(studentFolder + "\\" + desiredArchive));
                }
            } else if (noHWFolders) {// единственный массив в папке студента
                if (studentFiles.size() != 0) {
                    String archive = "";
                    for (File studentFile : studentFiles) {
                        if (isArchive(studentFile.getName())) {
                            archive = studentFile.getName();
                            break;
                        }
                    }
                    String unarchivedFolder = archive.replace(SEVEN_Z_EXT, "").replace(RAR_EXT, "").replace(ZIP_EXT, "");
                    if (studentFiles.stream().anyMatch(unarchivedFolder::equals)) {// удаляем распакованный ранее архив
                        DirDeleter.delete2(new File(studentFolder + "\\" + unarchivedFolder));
                    }
                    String archiveFilePath = studentFolder + "\\" + archive;
                    if (archive.endsWith(RAR_EXT)) {
                        Unzipper.unRar(new File(archiveFilePath));
                    }
                    if (archive.endsWith(ZIP_EXT)) {

                        if (new File(studentFolder + "\\" + unarchivedFolder).exists()) {
                            DirDeleter.delete2(new File(studentFolder + "\\" + unarchivedFolder));
                        }
                        Files.createDirectory(new File(studentFolder + "\\" + unarchivedFolder).toPath());
                        Platform.runLater(() -> launchInfo.addLog("Unarchiving ZIP: " + archiveFilePath + endl));
                        Unzipper.unzipFile(Path.of(archiveFilePath));
                    }
                    if (archive.endsWith(SEVEN_Z_EXT)) {
                        if (new File(studentFolder + "\\" + unarchivedFolder).exists()) {
                            DirDeleter.delete(new File(studentFolder + "\\" + unarchivedFolder));
                        }
                        Files.createDirectory(new File(studentFolder + "\\" + unarchivedFolder).toPath());
                        try {
                            Unzipper.unSevenZipFile(new File(archiveFilePath));
                        } catch (IOException e) {
                            System.out.println(e.toString());
                        }
                    }
                }
            }
        }
    }

    private static String getPackage(File fileToCheck) {
        try (Scanner sc = new Scanner(fileToCheck)) {
            while (sc.hasNextLine()) {
                String line = sc.nextLine().strip();
                if (line.startsWith("//") || line.startsWith("*") || line.startsWith("/*") || line.isEmpty()) {
                    continue;
                }
                if (line.startsWith("package")) {
                    return line.replace("package", "").strip().replace(";", "");
                }
                if (line.startsWith("import")) {
                    return "";
                }
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        return "";
    }
}
