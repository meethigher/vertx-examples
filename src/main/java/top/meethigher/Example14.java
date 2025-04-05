package top.meethigher;

import io.vertx.core.Vertx;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetSocket;

public class Example14 {


    private static void startServer() {
        Vertx vertx = Vertx.vertx();
        vertx.createNetServer().connectHandler(socket -> {
            socket.close();
        }).listen(8080).onFailure(e -> {
            e.printStackTrace();
            System.exit(1);
        });
    }

    /**
     * 实现client的重连机制
     */
    private static void startClient() {
        Vertx vertx = Vertx.vertx();
        NetClient netClient = vertx.createNetClient();
        connect(vertx, netClient);
    }

    private static void connect(Vertx vertx, NetClient netClient) {
        netClient.connect(8080, "127.0.0.1")
                .onComplete(ar -> {
                    if (ar.succeeded()) {
                        NetSocket socket = ar.result();
                        socket.pause();

                        reconnectDelay = MIN_DELAY;
                        System.out.println("重连成功");

                        socket.closeHandler(t -> {
                            System.out.println("连接被关闭咯");
                            reconnect(vertx, netClient);
                        });
                        socket.resume();
                    } else {
                        ar.cause().printStackTrace();
                        reconnect(vertx, netClient);
                    }
                });
    }


    private static final long MIN_DELAY = 3000;
    private static final long MAX_DELAY = 60000;
    private static long reconnectDelay = MIN_DELAY;

    /**
     * 自动重连: 指数退避策略
     */
    private static void reconnect(Vertx vertx, NetClient netClient) {
        vertx.setTimer(reconnectDelay, id -> {
            System.out.println("开始重连");
            connect(vertx, netClient);
            reconnectDelay = Math.min(reconnectDelay * 2, MAX_DELAY);
        });
    }


    public static void main(String[] args) {
        startServer();
        startClient();
    }
}
