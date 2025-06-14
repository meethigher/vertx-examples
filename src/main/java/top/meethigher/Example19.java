package top.meethigher;

import io.vertx.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.LockSupport;

public class Example19 {
    private static final Vertx vertx = Vertx.vertx();
    private static final Logger log = LoggerFactory.getLogger(Example19.class);

    public static Future<String> getFuture() {
        // Promise用法
        final Promise<String> promise = Promise.promise();
        vertx.setTimer(5000, id -> {
            if (ThreadLocalRandom.current().nextBoolean()) {
                promise.complete("succeed");
            } else {
                promise.fail("failed");
            }
        });
        return promise.future();
    }

    public static void main(String[] args) {
        Handler<AsyncResult<String>> completion = ar -> {
            if (ar.succeeded()) {
                log.info("test succeed");
            } else {
                log.error("test failed", ar.cause());
            }
        };
        getFuture().onComplete(completion);
        getFuture().onComplete(v -> {
            completion.handle(Future.failedFuture(new RuntimeException("hh")));
        });

        for (int i = 0; i < 10; i++) {
            getFuture().onComplete(ar -> {
                if (ar.succeeded()) {
                    log.info("future completed");
                } else {
                    log.error("future failed");
                }
            });
        }

        LockSupport.park();
    }
}
