package top.meethigher;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
public class Example3 {

    public static void main(String[] args) throws Exception {

        Vertx vertx = Vertx.vertx(new VertxOptions().setEventLoopPoolSize(1).setWorkerPoolSize(1));
        // 创建HttpClient时指定的PoolOptions里面的EventLoopSize不会生效。以Vertx的EventLoopSize为主。默认http/1为5并发，http/2为1并发
        HttpClient httpClient = vertx.createHttpClient(new PoolOptions().setHttp2MaxSize(2000).setHttp1MaxSize(2000).setEventLoopSize(2000));
        HttpClient httpsClient = vertx.createHttpClient(new HttpClientOptions().setSsl(true).setTrustAll(true).setConnectTimeout(60000), new PoolOptions().setHttp2MaxSize(2000).setHttp1MaxSize(2000).setEventLoopSize(2000));

        /**
         * 输出当前活着的线程
         */
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
        int total = 2000;
        CountDownLatch countDownLatch = new CountDownLatch(total);
        long start = System.currentTimeMillis();
        for (int i = 0; i < total; i++) {
            int finalI = i;
            vertx.deployVerticle(new AbstractVerticle() {
                @Override
                public void start() throws Exception {
                    httpClient.request(HttpMethod.GET, 4321, "localhost", "/test/test?test=" + finalI)
                    //httpsClient.request(HttpMethod.GET, 443, "reqres.in", "/api/users?page=" + finalI)
                            .onComplete(r -> {
                                if (r.succeeded()) {
                                    HttpClientRequest request = r.result();
                                    log.info("{} send request {}", this, request.absoluteURI());
                                    request.putHeader("User-Agent", "I am Vertx");
                                    request.send()
                                            .onComplete(r1 -> {
                                                if (r1.succeeded()) {
                                                    HttpClientResponse result = r1.result();
                                                    log.info("{} received response with status code {}", this, result.statusCode());
                                                    //这种做法其实对内存要求较大，相当于是一次性将内容写到buffer里了
                                                    result.body().onComplete(re -> {
                                                        if (re.succeeded()) {
                                                            Buffer result1 = re.result();
                                                            log.info("result: {}", result1);
                                                        } else {
                                                            log.warn("warn: ", re.cause());
                                                        }
                                                    });
                                                } else {
                                                    log.error("{} send failed: {}", this, r1.cause().getMessage(), r1.cause());
                                                }

                                                countDownLatch.countDown();
                                            });
                                } else {
                                    log.error("request failed: {}", r.cause().getMessage(), r.cause());
                                    countDownLatch.countDown();
                                }
                            });
                }
            });
        }
        countDownLatch.await();
        log.info("done {} ms", System.currentTimeMillis() - start);
    }
}
