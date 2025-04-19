package top.meethigher;

import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.NetSocket;
import io.vertx.core.streams.WriteStream;

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
                                // 在编程时，handler注册的buffer，需要手动开启resume
//                                socket.handler(b -> {
//                                    System.out.println(b);
//                                });
                                // 在编程时，若使用了pipeTo，则不需要手动再去开启resume
                                socket.pipeTo(new WriteStream<Buffer>() {
                                    @Override
                                    public WriteStream<Buffer> exceptionHandler(Handler<Throwable> handler) {
                                        return null;
                                    }

                                    @Override
                                    public Future<Void> write(Buffer data) {
                                        System.out.println(data);
                                        return null;
                                    }

                                    @Override
                                    public void write(Buffer data, Handler<AsyncResult<Void>> handler) {
                                        System.out.println(data);
                                    }

                                    @Override
                                    public void end(Handler<AsyncResult<Void>> handler) {

                                    }

                                    @Override
                                    public WriteStream<Buffer> setWriteQueueMaxSize(int maxSize) {
                                        return null;
                                    }

                                    @Override
                                    public boolean writeQueueFull() {
                                        return false;
                                    }

                                    @Override
                                    public WriteStream<Buffer> drainHandler(Handler<Void> handler) {
                                        return null;
                                    }
                                });
                                tsocket.resume();
//                                socket.resume();
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
