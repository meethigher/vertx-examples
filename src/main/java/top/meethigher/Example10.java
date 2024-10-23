package top.meethigher;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetSocket;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class Example10 {

    private static final Vertx vertx = Vertx.vertx();

    public static void main(String[] args) {
        MemoryMonitor memoryMonitor = new MemoryMonitor();
        memoryMonitor.start();
        log.info("start {}", memoryMonitor.convertBytes(memoryMonitor.getUsedMemory()));
        vertx.setPeriodic(1000, v -> {
            log.info("cur {}", memoryMonitor.convertBytes(memoryMonitor.getUsedMemory()));
        });
        String property = System.getProperty("tcpServer", "false");
        if (property.equals("true")) {
            tcpServer();
        } else {
            tcpClient();
        }
    }

    /**
     * 当tcp向socket传输数据时，如果对面消费不过来或者接收不过来，那么数据会存在发送方的本地发送缓冲区（也称为发送队列，在vertx的socket中即writeQueue）中。
     * 若对方一直不消费，那么就会出现本地内存崩了的情况。因此在开发过程中，要利用好相关api，如下
     * ```
     *     ws.drainHandler(v -> src.resume());
     *     src.handler(item -> {
     *       ws.write(item, this::handleWriteResult);
     *       if (ws.writeQueueFull()) {
     *         src.pause();
     *       }
     *     });
     * ```
     */
    private static void tcpClient() {
        NetClient netClient = vertx.createNetClient();
        netClient.connect(234, "10.0.0.9").onSuccess(socket -> {
            log.info("client connected");
            socket.drainHandler(t->{
                log.info("空了，继续写");
                vertx.setPeriodic(100, id -> {
                    byte[] bytes = new byte[10 * 1024 * 1024];
                    socket.write(Buffer.buffer(bytes)).onSuccess(v -> {
                        log.info("成功");
                    }).onFailure(e -> {
                        log.info("失败");
                    });
                    if(socket.writeQueueFull()) {
                        log.info("满了，暂停写");
                        vertx.cancelTimer(id);
                    }
                });
            });
        });
    }

    private static void tcpServer() {
        NetServer netServer = vertx.createNetServer();
        Handler<NetSocket> connectHandler = netSocket -> {
            netSocket.pause();
            netSocket.closeHandler(t -> {
                log.info("remote socket closed");
            });
            netSocket.handler(buffer -> {
            });
            vertx.setTimer(30000, id -> {
                netSocket.resume();
            });
        };

        netServer.connectHandler(connectHandler).listen(234).onSuccess(t -> {
            log.info("server started");
        });
    }
}
