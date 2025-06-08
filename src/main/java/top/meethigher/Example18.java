package top.meethigher;

import io.vertx.core.Vertx;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.meethigher.example18.DiyPipe;

public class Example18 {
    private static final Logger log = LoggerFactory.getLogger(Example18.class);

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();

        /**
         * set http_proxy=http://127.0.0.1:7777
         * set https_proxy=http://127.0.0.1:7777
         * curl -v http://meethigher.top/whoami
         */
        final String host = "127.0.0.1";
        final int port = 1081;

        NetServer netServer = vertx.createNetServer();
        NetClient netClient = vertx.createNetClient();
        netServer.connectHandler(src -> {
            src.pause();
            netClient.connect(port, host).onSuccess(dst -> {
                dst.pause();
                new DiyPipe<>(src).to(dst);
                new DiyPipe<>(dst).to(src);
            });
        }).listen(7777).onFailure(e -> System.exit(1));
    }
}
