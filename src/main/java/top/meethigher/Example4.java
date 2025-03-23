package top.meethigher;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
public class Example4 {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new AbstractVerticle() {
            @Override
            public void start() throws Exception {
                int port = 4321;
                Router router = Router.router(vertx);

                // order值越小，优先级越高

                //注册接口
                router.route("/test/test").order(1).handler(t -> {
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
                router.route("/halo/*").order(2).handler(t -> {
                    // 开启chunked分片传输。content-length与transfer-encoding是矛盾的。
                    // content-length需要服务器在内存中计算好内容长度，适用于数据较少时传输；而transfer-encoding则是分块流式传输，适用于大文件传输。
                    // nginx自身不能直接开启chunked。需要借助插件或者实际后端服务。
                    t.response().setChunked(true);
                    t.response().end("hello world " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\n"
                            + t.request().absoluteURI());
                });
                //不传值表示匹配下面的所有内容
                router.route().order(3).handler(t -> {
                    t.response().end("Hello World");
                });

                // 注册静态资源路径，若访问的内容是404，则会回转到/*的路由上
                StaticHandler staticHandler = StaticHandler.create("D:/3Develop/www")
                        .setDirectoryListing(true)
                        .setAlwaysAsyncFS(true)
                        .setIndexPage("index.html");
                router.route("/static/*").order(Integer.MIN_VALUE).handler(staticHandler);

                vertx.createHttpServer(new HttpServerOptions()
//                                .setSsl(true)
//                                .setKeyCertOptions(new PemKeyCertOptions()//使用自签名证书开启ssl
//                                        .addCertPath("/usr/local/nginx/conf/cert/certificate.pem")
//                                        .addKeyPath("/usr/local/nginx/conf/cert/private.key")
//                                )
                        )
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
