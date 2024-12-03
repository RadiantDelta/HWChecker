package com.example.hwcheckergui.Checker.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.stream.Stream;

public class DirDeleter {
    public static boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        return directoryToBeDeleted.delete();
    }

    public static void delete(File directoryToBeDeleted) throws IOException {
        try (Stream<Path> walk = Files.walk(directoryToBeDeleted.toPath())) {
            walk.sorted(Comparator.reverseOrder()).map(Path::toFile).peek(System.out::println).forEach(File::delete);
        }
    }

    public static void delete2(File directoryToBeDeleted) throws IOException {
        Path pathToBeDeleted = directoryToBeDeleted.toPath();

        Files.walkFileTree(pathToBeDeleted, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}