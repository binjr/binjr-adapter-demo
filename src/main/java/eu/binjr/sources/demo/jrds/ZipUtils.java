/*
 *    Copyright 2020 Frederic Thevenet
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

import eu.binjr.common.logging.Logger;
import eu.binjr.common.logging.Profiler;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipUtils {
    private static final Logger logger = Logger.create(ZipUtils.class);
    private static final int MAX_BLOCKING_QUEUE_CAPACITY = 100000;

    public static Path unzip(Path zipFilePath, Predicate<String> filter, Path outputDirectory, int nbThreads) throws Exception {
        return unzip(zipFilePath, filter, outputDirectory, false, nbThreads);
    }

    public static Path unzip(Path zipFilePath, Predicate<String> filter, Path outputDirectory, boolean deleteOnExit, int nbThreads) throws Exception {
        final AtomicBoolean taskDone = new AtomicBoolean(false);
        final AtomicBoolean taskAborted = new AtomicBoolean(false);
        AtomicLong nbFiles = new AtomicLong(0);
        try (Profiler p = Profiler.start(e -> logger.perf("Unzipped " + nbFiles.get() + " files from  " + zipFilePath + " in parallel on " + nbThreads + " threads: " + e.toMilliString()))) {
            try (ZipFile zipFile = new ZipFile(zipFilePath.toFile())) {
                final Enumeration<? extends ZipEntry> entries = zipFile.entries();
                AtomicInteger threadNum = new AtomicInteger(0);
                ArrayBlockingQueue<ZipEntry> queue = new ArrayBlockingQueue<>(Math.min(zipFile.size(), MAX_BLOCKING_QUEUE_CAPACITY));
                ExecutorService consumerPool = Executors.newFixedThreadPool(nbThreads, r -> {
                    Thread thread = new Thread(r);
                    thread.setName("unzip-thread-" + threadNum.incrementAndGet());
                    return thread;
                });
                List<Future<Integer>> results = new ArrayList<>();
                for (int i = 0; i < nbThreads; i++) {
                    results.add(consumerPool.submit(() -> {
                        int nbFileUnzipped = 0;
                        do {
                            List<ZipEntry> todo = new ArrayList<>();
                            queue.drainTo(todo, 8);
                            try {
                                for (ZipEntry zipEntry : todo) {
                                    File file = new File(outputDirectory + File.separator + zipEntry.getName());
                                    if (deleteOnExit) {
                                        file.deleteOnExit();
                                    }
                                    new File(file.getParent()).mkdirs();
                                    try (FileOutputStream fileStream = new FileOutputStream(file)) {
                                        copyStream(zipFile.getInputStream(zipEntry), fileStream);
                                    }
                                    nbFileUnzipped++;
                                }
                            } catch (Throwable t) {
                                // Signal that worker thread was aborted and rethrow
                                taskAborted.set(true);
                                queue.clear();
                                throw t;
                            }
                        } while (!taskDone.get() && !taskAborted.get() && !Thread.currentThread().isInterrupted());
                        return nbFileUnzipped;
                    }));
                }
                while (!taskAborted.get() && entries.hasMoreElements()) {
                    final ZipEntry zipEntry = entries.nextElement();
                    if (!zipEntry.isDirectory() && filter.test(zipEntry.getName())) {
                        queue.offer(zipEntry);
                    }
                }
                while (!taskAborted.get() && queue.size() > 0) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                try {
                    taskDone.set(true);
                    consumerPool.shutdown();
                    consumerPool.awaitTermination(30, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    logger.error("Termination interrupted", e);
                }
                for (Future<Integer> f : results) {
                    //signal exceptions that may have happened on thread pool
                    try {
                        logger.trace("Thread unzipped " + f.get() + " files");
                        nbFiles.addAndGet(f.get());
                    } catch (InterruptedException e) {
                        logger.error("Getting result from worker was interrupted", e);
                    } catch (ExecutionException e) {
                        //rethrow execution exceptions
                        throw e;
                    }
                }
            }
        }
        return outputDirectory;
    }

    private static void copyStream(InputStream in, OutputStream out) throws IOException {
        int length = 0;
        byte[] buffer = new byte[8192];
        while ((length = in.read(buffer)) > 0) {
            out.write(buffer, 0, length);
        }
    }
}
