package top.meethigher;

import io.vertx.core.Vertx;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.CopyOptions;
import io.vertx.core.file.FileSystem;
import io.vertx.core.file.OpenOptions;
import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Slf4j
public class Example5 {

    private static final Vertx vertx = Vertx.vertx();


    private static final String sourcePath = "D:/Desktop/40gb.txt";

    private static final String targetPath = "D:/Desktop/temp.txt";

    private static final int bufferSize = 8192;

    private static final FileSystem fs = vertx.fileSystem();

    /**
     * 40gb 耗时29819 ms，峰值内存占用24mb
     */
    private static void originDiyFileCopy() {
        long start = System.currentTimeMillis();
        try (FileInputStream fis = new FileInputStream(sourcePath);
             FileOutputStream fos = new FileOutputStream(targetPath)) {
            byte[] buffer = new byte[bufferSize];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
            fos.flush();
        } catch (Exception ignore) {
        }
        log.info("{} ms", System.currentTimeMillis() - start);
    }


    /**
     * 40gb 耗时14100 ms，峰值内存占用25mb
     */
    private static void originFileCopy() {
        long start = System.currentTimeMillis();
        try {
            Files.copy(Paths.get(sourcePath), Paths.get(targetPath), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ignore) {

        }
        log.info("{} ms", System.currentTimeMillis() - start);
    }

    /**
     * 40gb 耗时177459 ms，峰值内存300mb
     */
    private static void vertxDiyFileCopy() {
        long l = System.currentTimeMillis();
        fs.open(sourcePath, new OpenOptions(), ar -> {
            if (ar.succeeded()) {
                AsyncFile sourceFile = ar.result();
                sourceFile.setReadBufferSize(bufferSize);
                sourceFile.pause();
                fs.open(targetPath, new OpenOptions().setTruncateExisting(true), ar1 -> {
                    if (ar1.succeeded()) {
                        AsyncFile targetFile = ar1.result();
                        sourceFile.pipeTo(targetFile).eventually(() -> sourceFile.close());
                        sourceFile.endHandler(t -> {
                            log.info("{} ms", System.currentTimeMillis() - l);
                        });


//                        targetFile.drainHandler(v -> {
//                            sourceFile.resume();
//                        });
//                        sourceFile.handler(buffer -> {
//                            targetFile.write(buffer);
//                            if (targetFile.writeQueueFull()) {
//                                sourceFile.pause();
//                            }
//                        });
//                        sourceFile.endHandler(v -> {
//                            log.info("end");
//                            sourceFile.close();
//                            targetFile.close();
//                        });
                        sourceFile.resume();
                    } else {
                        log.error("open targetPath failed", ar1.cause());
                    }
                });
            } else {
                log.error("open sourcePath failed", ar.cause());
            }
        });

//        final AsyncFile targetFile = vertx.fileSystem().openBlocking(targetPath, new OpenOptions());
//
//        vertx.fileSystem()
//                .open(sourcePath, new OpenOptions())
//                .compose(sourceFile -> sourceFile.pipeTo(targetFile).eventually(v -> sourceFile.close()))
//                .onComplete(result -> {
//                    if (result.succeeded()) {
//                        log.info("Copy done");
//                    } else {
//                        log.info("Cannot copy file " + result.cause().getMessage());
//                    }
//                });
    }

    /**
     * 40gb 耗时14356 ms，内存占用23mb
     */
    private static void vertxFileCopy() {
        long l = System.currentTimeMillis();
        /**
         * 非阻塞方法
         */
        fs.copy(sourcePath, targetPath, new CopyOptions().setReplaceExisting(true))
                .onComplete(re -> {
                    if (re.succeeded()) {
                        log.info("{} ms", System.currentTimeMillis() - l);
                    } else {
                        log.error("copy failed, ", re.cause());
                    }
                });
        /**
         * 对应的阻塞方法
         */
        //fs.copyBlocking("D:/Downloads/yjwj_2024-06-05-10-50.zip", "D:/Desktop/test1.zip");
        //log.info("done");
    }

    public static void main(String[] args) {
        MemoryMonitor memoryMonitor = new MemoryMonitor();
        memoryMonitor.start();
        vertx.setPeriodic(1000, t -> {
            log.info("used memory {}", memoryMonitor.convertBytes(memoryMonitor.getUsedMemory()));
        });
        log.info("used memory {}", memoryMonitor.convertBytes(memoryMonitor.getUsedMemory()));

        vertxDiyFileCopy();
    }
}
