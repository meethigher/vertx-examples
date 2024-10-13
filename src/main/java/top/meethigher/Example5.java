package top.meethigher;

import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Example5 {
    public static void main(String[] args) {
        fileCopy();
    }

    public static void fileCopy() {
        Vertx vertx = Vertx.vertx();
        FileSystem fs = vertx.fileSystem();
        /**
         * 非阻塞方法
         */
        fs.copy("D:/Downloads/yjwj_2024-06-05-10-50.zip", "D:/Desktop/test.zip")
                .onComplete(re -> {
                    if (re.succeeded()) {
                        log.info("copy success");
                    } else {
                        log.error("copy failed, ", re.cause());
                    }
                });
        /**
         * 对应的阻塞方法
         */
        fs.copyBlocking("D:/Downloads/yjwj_2024-06-05-10-50.zip", "D:/Desktop/test1.zip");
        log.info("done");
    }
}
