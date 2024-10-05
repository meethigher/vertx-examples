package top.meethigher;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Example4 {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new AbstractVerticle() {
            @Override
            public void start() throws Exception {
                int port = 4321;
                Router router = Router.router(vertx);
                //注册接口
                router.route("/test/test").handler(t -> {
                    //接口逻辑
                    String testParam = t.request().getParam("test");
                    if (testParam == null) {
                        t.response().setStatusCode(400)
                                .end("missing 'test' query parameter");
                    } else {
                        vertx.setTimer(10000, tt -> {
                            t.response().putHeader("Content-Type", "text/plain")
                                    .end(testParam);
                        });

                    }
                });
                //`/*`表示匹配当前目录及子目录。注意与SpringWeb的区别。
                router.route("/halo/*").handler(t -> {
                    String s = t.request().absoluteURI();
                    t.response().putHeader("Content-Type", "text/html")
                            .end("<h1>Hello World</h1> " + s);
                });
                //不传值表示匹配下面的所有内容
                router.route().handler(t -> {
                    t.response().end("Hello World");
                });

                vertx.createHttpServer()
                        //注册路由
                        .requestHandler(router)
                        .listen(port).onComplete(re -> {
                            if (re.succeeded()) {
                                log.info("http server started on port {}", port);
                            } else {
                                log.error("http server failed to start", re.cause());
                            }
                        });
            }
        });
    }
}
