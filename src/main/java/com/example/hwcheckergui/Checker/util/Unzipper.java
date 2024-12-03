package com.example.hwcheckergui.Checker.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import com.github.junrar.extract.ExtractArchive;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.io.FilenameUtils;

public class Unzipper {
    public static void unzipFile(Path filePathToUnzip) {

        Path parentDir = filePathToUnzip.getParent();
        String fileName = filePathToUnzip.toFile().getName();
        Path targetDir = parentDir.resolve(FilenameUtils.removeExtension(fileName));

        try (ZipFile zip = new ZipFile(filePathToUnzip.toFile(), Charset.forName("ISO-8859-1"))) {
            Enumeration<? extends ZipEntry> entries = zip.entries();

            if (!targetDir.toFile().isDirectory() && !targetDir.toFile().mkdirs()) {
                throw new IOException("failed to create directory " + targetDir);
            }

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                File f = new File(targetDir.resolve(Path.of(entry.getName())).toString());

                if (entry.isDirectory()) {
                    if (!f.isDirectory() && !f.mkdirs()) {
                        throw new IOException("failed to create directory " + f);
                    }
                } else {
                    File parent = f.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("failed to create directory " + parent);
                    }
                    try (InputStream in = zip.getInputStream(entry)) {
                        Files.copy(in, f.toPath());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void unSevenZipFile(File f) throws IOException {
        try (SevenZFile sevenZFile = new SevenZFile(f)) {
            SevenZArchiveEntry entry;
            while ((entry = sevenZFile.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                File curfile = new File(f.getAbsolutePath().replace(".7z", ""), entry.getName());
                File parent = curfile.getParentFile();
                if (!parent.exists()) {
                    parent.mkdirs();
                }
                FileOutputStream out = new FileOutputStream(curfile);
                byte[] content = new byte[(int) entry.getSize()];
                sevenZFile.read(content, 0, content.length);
                out.write(content);
                out.close();
            }
        } catch (Exception e) {
            throw e;
        }
    }

    public static void unRar(File file) {
        ExtractArchive extractArchive = new ExtractArchive();
        File tmpDir = null;
        try {
            tmpDir = File.createTempFile("bip.", ".unrar");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (!(tmpDir.delete())) {
            try {
                throw new IOException("Could not delete temp file: " + tmpDir.getAbsolutePath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (!(tmpDir.mkdir())) {
            try {
                throw new IOException("Could not create temp directory: " + tmpDir.getAbsolutePath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        extractArchive.extractArchive(file, tmpDir);
    }
}
