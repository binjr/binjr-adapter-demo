/*
 *    Copyright 2019-2020 Frederic Thevenet
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

import eu.binjr.common.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class JrdsDiskImage implements Closeable {
    private static final Logger logger = LogManager.getLogger(JrdsDiskImage.class);
    private final Path originPath;
    private Path pathToCleanupOnClose;
    private Path rootPath;

    private Path libsPath;
    private Path rrdDir;
    private Path configdir;
    private Path tempDir;

    public static JrdsDiskImage of(Path path) throws Exception {
        return new JrdsDiskImage(path);
    }

    private JrdsDiskImage(Path origin) throws Exception {
        try {
            this.originPath = origin;
            if (!Files.exists(origin)) {
                throw new FileNotFoundException("Target '" + origin + "' does not exist");
            }
            boolean openFromJar = origin.getFileSystem().provider().getScheme().equals("jar");
            if (openFromJar || !Files.isDirectory(origin)) {
                logger.info("Loading image from archive: '" + origin + "'...");
                Path temp = Files.createTempDirectory("binjr-adapter-demo");
                temp.toFile().deleteOnExit();
                if (openFromJar) {
                    IOUtils.copyDirectory(originPath, temp, StandardCopyOption.REPLACE_EXISTING);
                    this.pathToCleanupOnClose = temp;
                } else {
                    if (!origin.getFileName().toString().toLowerCase().endsWith(".zip")) {
                        throw new IOException("Target '" + origin + "' is neither a Jar file, zip file nor a directory");
                    }
                    this.pathToCleanupOnClose = ZipUtils.unzip(origin, (String path) ->
                            path.startsWith("perfmonitoring/") ||
                                    path.startsWith("perfmonitoring\\") ||
                                    path.startsWith("timezone"), temp, true, 4);
                }
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
        if (pathToCleanupOnClose != null && Files.exists(pathToCleanupOnClose)) {
            IOUtils.attemptDeleteTempPath(pathToCleanupOnClose);
        }
    }

    public Path getLibsPath() {
        return libsPath;
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
