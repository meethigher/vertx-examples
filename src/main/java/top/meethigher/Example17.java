package top.meethigher;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.locks.LockSupport;

public class Example17 {


    private static final Logger log = LoggerFactory.getLogger(Example17.class);

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();

        HttpServer server = vertx.createHttpServer();

        // 处理 HTTP 请求
        server.requestHandler(req -> {
            if ("/hello".equals(req.path())) {
                req.response()
                        .putHeader("content-type", "text/plain")
                        .end("HTTP " + System.currentTimeMillis());
            } else {
                req.response()
                        .setStatusCode(404)
                        .end("Not Found");
            }
        });

        // 处理 WebSocket 请求
        server.webSocketHandler(ws -> {
            if ("/ws".equals(ws.path())) {
                ws.handler(buffer -> {
                    // 收到消息后，原封不动回写
                    ws.writeTextMessage("WebSocket " + System.currentTimeMillis());
                });
            } else {
                ws.reject();
            }
        });

        // 监听端口
        server.listen(8080, res -> {
            if (res.succeeded()) {
                log.info("http server started on port {}", res.result().actualPort());
            } else {
                log.error("http server start failed", res.cause());
            }
        });
    }
}
