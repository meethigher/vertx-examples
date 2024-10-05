package top.meethigher;

import com.hazelcast.config.Config;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
public class Example2 {

    public static void main(String[] args) throws Exception {
        Config config = new Config();
        config.setClusterName("test");
        HazelcastClusterManager clusterManager = new HazelcastClusterManager(config);
        Future<Vertx> vertxFuture = Vertx.builder()
                .with(new VertxOptions())
                .withClusterManager(clusterManager)
                //.build()//构建单节点
                .buildClustered();
        //阻塞直到获取集群中的vertx实例
        Vertx vertx = vertxFuture.toCompletionStage().toCompletableFuture().get();
        vertx.eventBus().consumer("test", t -> {
            log.info(t.body().toString());
        });
        vertx.setTimer(20000, t -> {
            vertx.eventBus().publish("test", "现在时间是:" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        });
    }
}
