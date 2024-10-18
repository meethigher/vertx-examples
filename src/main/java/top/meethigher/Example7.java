package top.meethigher;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetSocket;
import lombok.extern.slf4j.Slf4j;

import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

@Slf4j
public class Example7 {

    private static Vertx vertx = Vertx.vertx();

    private static final int port = 22;

    private static final String host = "127.0.0.1";

    public static void main(String[] args) throws Exception {
        tcpReverse();
    }

    private static void tcpClient() throws Exception {
        Socket socket = new Socket(host, port);
        try (OutputStream os = socket.getOutputStream()) {
            for (int i = 0; i < 10; i++) {
                os.write(new String(i + "\n").getBytes(StandardCharsets.UTF_8));
            }
        }
        socket.close();
    }

    /**
     * 学习handler
     */
    private static void tcpServer1() {
        /**
         * Handler是一个接口，用于处理异步的回调，当事件发生时，通知线程过来处理
         */
        Handler<NetSocket> handler = socket -> {
            // setTimer内部又是一个事件。虽然是由一个线程执行，但这本质是个异步操作。因此会存在丢失数据的问题
            vertx.setTimer(1, id -> {
                socket.handler(System.out::println).closeHandler(v -> socket.close());
            });
        };
        Handler<AsyncResult<NetServer>> successHandler = r -> System.out.println("Server listening on port " + port);
        Handler<Throwable> throwableHandler = Throwable::printStackTrace;
        vertx.createNetServer().connectHandler(handler).listen(port, host).onComplete(successHandler).onFailure(throwableHandler);
    }

    /**
     * 对tcpServer1的bug解决
     * 学习pause和resume
     */
    private static void tcpServer2() {
        Handler<NetSocket> handler = socket -> {
            socket.pause();//告诉socket暂停从缓存中读取数据。该操作不会影响tcp中数据的传输，数据会持续过来写入到系统缓存。
            vertx.setTimer(1, id -> {
                socket.handler(System.out::println).closeHandler(v -> socket.close());
                socket.resume();//告诉socket开始从缓存中读取数据。pause和resume，可以用于异步注册监听事件时，保证数据不会丢失。
            });
        };
        Handler<AsyncResult<NetServer>> successHandler = r -> System.out.println("Server listening on port " + port);
        Handler<Throwable> throwableHandler = Throwable::printStackTrace;
        vertx.createNetServer().connectHandler(handler).listen(port, host).onComplete(successHandler).onFailure(throwableHandler);
    }


    /**
     * tcp反向代理
     */
    private static void tcpReverse() {
        NetClient netClient = vertx.createNetClient();
        NetServer netServer = vertx.createNetServer();
        Handler<NetSocket> handler = sourceSocket -> {
            sourceSocket.pause();
            netClient.connect(5432, "10.0.0.9").onSuccess(targetSocket -> {
                targetSocket.handler(sourceSocket::write);
                sourceSocket.handler(targetSocket::write);
                sourceSocket.resume();
            });
        };
        Handler<AsyncResult<NetServer>> successHandler = r -> System.out.println("Server listening on port " + port);
        Handler<Throwable> throwableHandler = Throwable::printStackTrace;

        netServer.connectHandler(handler).listen(port, host).onComplete(successHandler).onFailure(throwableHandler);
    }
}

