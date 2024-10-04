package top.meethigher;

import io.vertx.core.*;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public class Example1 {

    public static void main(String[] args) {
        log.info("start");
        verticle();
        log.info("done");
    }

    private static void eventBus() {
        Vertx vertx = Vertx.vertx();
        vertx.eventBus().consumer("test", t -> {
            Object body = t.body();
            log.info("consumer: {}", body.toString());
        });
        vertx.eventBus().publish("test", "hello world");
    }

    private static void verticle() {
        Vertx vertx = Vertx.vertx();
        //部署标准Verticle
        vertx.deployVerticle(new AbstractVerticle() {
            @Override
            public void start() throws Exception {
                log.info("eventLoopVerticle start");
            }

        }).onFailure(e -> {
            log.error("error", e);
        });
        //部署工作者Verticle
        vertx.deployVerticle(new AbstractVerticle() {
            @Override
            public void start() throws Exception {
                log.info("workerVerticle start");
            }
        }, new DeploymentOptions().setThreadingModel(ThreadingModel.WORKER)).onFailure(e -> {
            log.error("error", e);
        });
    }

    private static void basic() {
        /**
         * workerPoolSize 指定用于处理阻塞任务的工作线程池的大小。默认20个
         * eventLoopPoolSize 用于处理事件回调时执行的逻辑。默认CPU实际线程的2倍。因此在注册事件回调时的逻辑不要阻塞，如果必须要执行阻塞逻辑，就丢给workerPool
         */
        Vertx tVertx = Vertx.vertx();
        Vertx vertx = Vertx.vertx(new VertxOptions().setEventLoopPoolSize(5).setWorkerPoolSize(1));
        /**
         * 执行过程中，注意线程的名称。
         * 使用WorkerPool执行阻塞任务，并使用EventLoopPool执行阻塞完成后的回调。
         */
        vertx.executeBlocking(() -> {
                    //模拟阻塞任务的耗时
                    log.info("simulate blocking task execution time...");
                    TimeUnit.SECONDS.sleep(5);
                    if (System.currentTimeMillis() % 2 == 0) {
                        int i = 1 / 0;
                    }
                    return "Hello World";
                })
                .onComplete(re -> {
                    if (re.succeeded()) {
                        log.info("success, result:{}", re.result());
                    } else {
                        log.error("failure, result:", re.cause());
                    }
                })
        //以下写法相同
        //onSuccess和onFailure是对onComplete更精确的封装，本质还是基于onComplete。
        //.onSuccess(r -> {
        //    log.info("success, result:{}", r);
        //}).onFailure(e -> {
        //    log.error("failure, result:", e);
        //})
        ;
        //一次性定时任务
        vertx.setTimer(5000, t -> {
            log.info("timer id:{}", t);
        });
        //周期性定时任务，并注册取消定时任务的逻辑
        vertx.setPeriodic(5000, t -> {
            log.info("periodic id:{}", t);
            if (System.currentTimeMillis() % 2 == 0) {
                vertx.cancelTimer(t);
                log.info("cancel timer:{}", t);
            }
        });
    }
}
