package top.meethigher;

import io.vertx.core.Vertx;
import io.vertx.core.http.*;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.proxy.handler.ProxyHandler;
import io.vertx.httpproxy.HttpProxy;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Example6 {
    public static void main(String[] args) {
        int port = 8080;
        Vertx vertx = Vertx.vertx();
        HttpServer httpServer = vertx.createHttpServer();
        Router router = Router.router(vertx);
        HttpClient httpClient = vertx.createHttpClient();
        HttpClient httpsClient = vertx.createHttpClient(new HttpClientOptions().setSsl(true).setTrustAll(true));

        HttpProxy httpProxy = HttpProxy.reverseProxy(httpClient);
        HttpProxy httpsProxy = HttpProxy.reverseProxy(httpsClient);

        // 默认处理逻辑ProxyHandler，这个是由vertx-web-proxy扩展提供
        router.route("/halo/*").handler(ProxyHandler.create(httpProxy, 4321, "10.0.0.1"));
        router.route().handler(ProxyHandler.create(httpsProxy, 443, "meethigher.top"));

        // 自定义逻辑，自己实现
        router.route("/api/*").handler(ctx -> {
            HttpServerRequest request = ctx.request();
            httpsClient.request(HttpMethod.valueOf(request.method().name()), 443, "reqres.in", request.uri())
                    .onSuccess(r -> {
                        r.headers().setAll(request.headers());
                        r.putHeader("Host", "reqres.in");
                        r.send()
                                .onSuccess(r1 -> {
                                    ctx.response()
                                            .setStatusCode(r1.statusCode())
                                            .headers().setAll(r1.headers());
                                    r1.handler(data -> {
                                        ctx.response().write(data);
                                    });
                                    r1.endHandler(v -> ctx.response().end());

                                })
                                .onFailure(e1 -> {
                                    ctx.response().setStatusCode(500).end(e1.getMessage());
                                });
                    })
                    .onFailure(e -> {
                        ctx.response().setStatusCode(500).end("Internal Server Error");
                    });
        });

        httpServer.requestHandler(router).listen(port).onSuccess(t -> {
            log.info("http server started on port {}", port);
        });
    }
}
