package top.meethigher;

import io.vertx.core.Vertx;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.net.SocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.LockSupport;

public class Example20 {
    private static final Logger log = LoggerFactory.getLogger(Example20.class);

    private static Vertx vertx = Vertx.vertx();

    public static void main(String[] args) {

        /**
         * dns服务反代
         * nslookup meethigher.top 127.0.0.1
         */
        udpReverseServer("0.0.0.0", 53,
                "119.29.29.29", 53);

        /**
         * 时钟服务反代
         * w32tm /stripchart /computer:127.0.0.1 /dataonly /samples:1
         */
        udpReverseServer("0.0.0.0", 123,
                "time.windows.com", 123);

        LockSupport.park();
    }


    public static void udpReverseServer(String host, int port,
                                        String targetHost, int targetPort) {
        DatagramSocket dst = vertx.createDatagramSocket();
        vertx.createDatagramSocket().listen(port, host)
                .onFailure(e -> {
                    e.printStackTrace();
                    System.exit(1);
                })
                .onSuccess(src -> {
                    src.handler(srcPk -> {
                        SocketAddress sender = srcPk.sender();
                        dst.handler(dstPk -> {
                            src.send(dstPk.data(), sender.port(), sender.host())
                                    .onSuccess(v -> {
                                        log.debug("target {} -- {} pipe to source {} -- {} succeeded",
                                                targetHost + ":" + targetPort, dst.localAddress(), src.localAddress(), srcPk.sender());
                                    }).onFailure(e -> {
                                        log.error("target {} -- {} pipe to source {} -- {} failed",
                                                targetHost + ":" + targetPort, dst.localAddress(), src.localAddress(), srcPk.sender(), e);
                                    });
                        });
                        dst.send(srcPk.data(), targetPort, targetHost)
                                .onSuccess(v -> {
                                    log.debug("source {} -- {} pipe to target {} -- {} succeeded",
                                            srcPk.sender(), src.localAddress(), dst.localAddress(), targetHost + ":" + targetPort);
                                }).onFailure(e -> {
                                    log.error("source {} -- {} pipe to target {} -- {} failed",
                                            srcPk.sender(), src.localAddress(), dst.localAddress(), targetHost + ":" + targetPort, e);
                                });
                    });
                });
    }
}
