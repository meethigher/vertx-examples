package top.meethigher;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.NetSocket;

import java.time.Duration;
import java.util.concurrent.locks.LockSupport;

public class Example15 {

    public static Vertx vertx = io.vertx.core.Vertx.vertx(
            new VertxOptions().setMaxEventLoopExecuteTime(Duration.ofDays(1).toNanos())
    );

    public static void main(String[] args) {
        vertx.createNetServer(new NetServerOptions().setTcpNoDelay(true)).connectHandler(socket -> {
            socket.pause();
            socket.write("hello world");
            socket.resume();
        }).listen(8080).onComplete(ar -> {
            if (ar.succeeded()) {
                System.out.println("Server started on port 8080");
                startClient();
            } else {
                ar.cause().printStackTrace();
                System.exit(1);
            }
        });

        LockSupport.park();
    }

    public static void startClient() {
        NetClient netClient = vertx.createNetClient(new NetClientOptions().setTcpNoDelay(true));
        netClient.connect(8080, "127.0.0.1")
                .onComplete(ar -> {
                    if (ar.succeeded()) {
                        NetSocket socket = ar.result();
                        socket.pause();
                        System.out.println(socket.localAddress() + " connected to " + socket.remoteAddress());
                        netClient.connect(8080, "127.0.0.1").onComplete(tar -> {
                            if (tar.succeeded()) {
                                NetSocket tsocket = tar.result();
                                tsocket.pause();
                                System.out.println(tsocket.localAddress() + " connected to " + tsocket.remoteAddress());
                                socket.handler(b -> {
                                    System.out.println(b);
                                });
                                tsocket.resume();
                                socket.resume();
                            } else {
                                tar.cause().printStackTrace();
                                System.exit(1);
                            }
                        });
                    } else {
                        ar.cause().printStackTrace();
                        System.exit(1);
                    }
                });
    }
}
