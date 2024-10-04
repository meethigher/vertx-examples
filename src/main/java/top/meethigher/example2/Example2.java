package top.meethigher.example2;

import io.vertx.core.Vertx;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Example2 {


    /**
     * 该案例主要用于使用vertx来实现异步的事件注册与监听。
     * 本质跟springboot一样，发布和监听都依赖于处于同一个容器。
     * 对于vertx来说，就是同一个实例里。
     */
    public static void main(String[] args) {
        log.info("running...");
        Vertx vertx = Vertx.vertx();

        String eventName = "test";

        vertx.setTimer(1000, t -> {
            vertx.eventBus().publish(eventName, "hello world");
        });

        vertx.eventBus().consumer(eventName, t -> {
            Object body = t.body();
            log.info(body.toString());
        });

        log.info("done");
    }
}
