/*
 *    Copyright 2019 Frederic Thevenet
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package eu.binjr.sources.demo.jrds;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class JrdsDiskImage implements Closeable {
    private static final Logger logger = LogManager.getLogger(JrdsDiskImage.class);
    private final Path originPath;
    private Path pathToCleanupOnClose;
    private Path rootPath;

    private Path libsPath;
    private Path rrdDir;
    private Path configdir;
    private Path tempDir;

    public static JrdsDiskImage of(Path path) throws IOException {
        return new JrdsDiskImage(path);
    }

    private JrdsDiskImage(Path origin) throws IOException {
        try {
            this.originPath = origin;
            if (!Files.exists(origin)) {
                throw new FileNotFoundException("Target '" + origin + "' does not exist");
            }
            if (!Files.isDirectory(origin)) {
                if (!origin.getFileName().toString().toLowerCase().endsWith(".zip")) {
                    throw new IOException("Target '" + origin + "' is neither a zip file nor a datadir");
                }
                logger.info("Loading image from archive: '" + origin + "'...");
                Path temp = Files.createTempDirectory("binjr-adapter-demo");
                temp.toFile().deleteOnExit();
                this.pathToCleanupOnClose = ZipUtils.unzip(origin, (String path) ->
                        path.startsWith("perfmonitoring/") ||
                                path.startsWith("perfmonitoring\\") ||
                                path.startsWith("timezone"), temp, true, 4);
                rootPath = this.pathToCleanupOnClose.resolve("perfmonitoring");
                libsPath = rootPath.resolve(Paths.get("resources", "extrajava"));
            } else {
                logger.info("Loading image from folder: '" + origin + "'...");
                this.rootPath = Files.find(origin, 10,
                        (p, a) -> p.getFileName().equals(Paths.get("perfmonitoring")) && a.isDirectory())
                        .findFirst()
                        .orElseThrow();
                this.pathToCleanupOnClose = libsPath = Files.createTempDirectory("binjr-adapter-demo_");

            }
            rrdDir = rootPath.resolve("probe");
            configdir = rootPath.resolve("config");
            tempDir = rootPath.resolve("tmp");
            if (!Files.exists(tempDir)) {
                Files.createDirectory(tempDir);
            }
        } catch (Throwable t) {
            // Attempt to clean up
            close();
            throw t;
        }
    }

    @Override
    public void close() {
        try {
            if (pathToCleanupOnClose != null && Files.exists(pathToCleanupOnClose)) {
                logger.debug(() -> "Deleting temporary folder at '" + pathToCleanupOnClose + "'");
                deleteDirectoryTree(pathToCleanupOnClose);
            }
        } catch (IOException e) {
            logger.error("Failed to delete temporary folder at '" + pathToCleanupOnClose + "': " + e.getMessage());
            logger.debug(() -> "Call stack", e);
        }
    }


    public Path getLibsPath() {
        return libsPath;
    }

    private static void deleteDirectoryTree(Path path) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    logger.warn("Temporary file " + path + " could not be deleted: " + e.getMessage());
                    logger.debug("Stack Trace:", e);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path directory, IOException ioException) throws IOException {
                try {
                    Files.delete(directory);
                } catch (DirectoryNotEmptyException e) {
                    logger.warn("Temporary folder " + directory + " could not be deleted: " + e.getMessage());
                    logger.debug("Stack Trace:", e);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public Path getRrdDir() {
        return rrdDir;
    }

    public Path getConfigdir() {
        return configdir;
    }

    public Path getTempDir() {
        return tempDir;
    }

}
