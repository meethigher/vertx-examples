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

//        updSocketPort();

        /**
         * dns服务反代
         * nslookup meethigher.top 127.0.0.1
         */
        udpReverseServer("0.0.0.0", 53,
                "119.29.29.29", 53, 60000);

        /**
         * 时钟服务反代
         * w32tm /stripchart /computer:127.0.0.1 /dataonly /samples:1
         */
        udpReverseServer("0.0.0.0", 123,
                "time.windows.com", 123, 60000);

        LockSupport.park();
    }

    public static void updSocketPort() {
        for (int i = 0; i < 1000; i++) {
            DatagramSocket datagramSocket = vertx.createDatagramSocket();
            datagramSocket.send("halo",53,"119.29.29.29").onSuccess(v->{
                System.out.println(datagramSocket.localAddress());
            });
        }
    }


    public static void udpReverseServer(String host, int port,
                                        String targetHost, int targetPort, long clientTimeout) {
        /**
         * 这里面要考虑一个问题。
         * A和B两个用户，同时请求我这个反代服务，我给转发出去后，获取到了响应，如何知道这个响应是给A的还是给B的？
         *
         * 如果只使用两个socket，一个做server端的src，一个做转发client的dst，无法解决该问题，因为dst的五元组(srcIp-srcPort-dstHost-dstPort-protocol)始终是同一个。
         *
         * 因此只能每次创建一个新的dst
         */
        DatagramSocket src = vertx.createDatagramSocket();
        src.handler(srcPk -> {
            SocketAddress srcSender = srcPk.sender();
            DatagramSocket dst = vertx.createDatagramSocket();
            long id = vertx.setTimer(clientTimeout, tid -> {
                dst.close();
            });
            dst.handler(dstPk -> {
                src.send(dstPk.data(), srcSender.port(), srcSender.host()).onFailure(e -> {
                    log.error("target {} -- {} pipe to source {} -- {} failed",
                            dst.localAddress(), targetHost + ":" + targetPort, src.localAddress(), srcSender);
                }).onSuccess(v -> {
                    log.debug("target {} -- {} pipe to source {} -- {} succeeded",
                            dst.localAddress(), targetHost + ":" + targetPort, src.localAddress(), srcSender);
                });
                vertx.cancelTimer(id);
                dst.close();
            });
            dst.send(srcPk.data(), targetPort, targetHost).onSuccess(v -> {
                log.debug("source {} -- {} pipe to target {} -- {} succeeded",
                        srcSender, host + ":" + port, dst.localAddress(), targetHost + ":" + targetPort);
            }).onFailure(e -> {
                log.error("source {} -- {} pipe to target {} -- {} failed",
                        srcSender, host + ":" + port, dst.localAddress(), targetHost + ":" + targetPort, e);
            });
            dst.send(srcPk.data(), targetPort, targetHost);
        });

        src.listen(port, host).onFailure(e -> {
            e.printStackTrace();
            System.exit(1);
        }).onSuccess(v -> {
            log.info("udp reverse server started on {}:{}", host, port);
        });
    }
}
