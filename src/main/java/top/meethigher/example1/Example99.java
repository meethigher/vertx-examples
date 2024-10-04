package top.meethigher.example1;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.*;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
public class Example99 {


    public static void main(String[] args) throws Exception {
        /**
         * workerPoolSize 指定用于处理阻塞任务的工作线程池的大小。默认20个
         * eventLoopPoolSize 用于处理非阻塞的 I/O 操作和定时器的事件循环线程池大小。默认CPU实际线程的2倍
         */
        Vertx vertx = Vertx.vertx(new VertxOptions().setEventLoopPoolSize(1).setWorkerPoolSize(2));
        // 创建HttpClient时指定的PoolOptions里面的EventLoopSize不会生效。以Vertx的EventLoopSize为主
        HttpClient httpClient = vertx.createHttpClient(new PoolOptions().setHttp2MaxSize(2000).setHttp1MaxSize(2000).setEventLoopSize(2));
        /**
         * 在 Vert.x 中，Verticle 是基本的执行单元，它代表了一个可以独立运行的组件。
         * 每个 Verticle 都可以包含处理逻辑，如 HTTP 处理、数据库交互或其他异步操作。
         */
        int total = 2;
        for (int i = 0; i < total; i++) {
            vertx.deployVerticle(new AbstractVerticle() {
                @Override
                public void start() throws Exception {
                    vertx.setPeriodic(5000, t -> {
                        vertx.executeBlocking(() -> {
                            ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
                            int i = threadGroup.activeCount();
                            Thread[] threads = new Thread[i];
                            threadGroup.enumerate(threads);
                            List<String> list = new ArrayList<>();
                            for (Thread thread : threads) {
                                list.add(thread.getName());
                            }
                            //模拟耗时操作
                            try {
                                log.info("calculating...");
                                TimeUnit.SECONDS.sleep(5);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            return String.join(",", list);
                        }).onComplete(r -> {
                            if (r.succeeded()) {
                                log.info("threads:\n{}", r.result());
                            } else {
                                log.warn(r.cause().getMessage(), r.cause());
                            }
                        });

                    });
                }
            });
        }
        CountDownLatch countDownLatch = new CountDownLatch(total);
        long start = System.currentTimeMillis();
        for (int i = 0; i < total; i++) {
            vertx.deployVerticle(new AbstractVerticle() {
                @Override
                public void start() throws Exception {
                    httpClient.request(HttpMethod.GET, 4321, "localhost", "/test/test")
                            .onComplete(r -> {
                                if (r.succeeded()) {
                                    HttpClientRequest request = r.result();
                                    log.info("{} send request {}", this, request.absoluteURI());
                                    request.send()
                                            .onComplete(r1 -> {
                                                if (r1.succeeded()) {
                                                    HttpClientResponse result = r1.result();
                                                    log.info("{} received response with status code " + result.statusCode(), this);
                                                } else {
                                                    log.error("{} send failed: {}", this, r1.cause().getMessage(), r1.cause());
                                                }
                                                countDownLatch.countDown();
                                            });
                                } else {
                                    log.error("request failed: {}", r.cause().getMessage(), r.cause());
                                }
                            });
                }
            });
        }
        countDownLatch.await();
        log.info("done {} ms", System.currentTimeMillis() - start);
    }
}
