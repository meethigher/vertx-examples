package top.meethigher;

import io.vertx.core.Vertx;
import io.vertx.core.Promise;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.VertxOptions;

public class ClusteredApp extends AbstractVerticle {

    public static void main(String[] args) {
        // Start clustered Vert.x instance
        Vertx.clusteredVertx(new VertxOptions(), res -> {
            if (res.succeeded()) {
                Vertx vertx = res.result();
                vertx.deployVerticle(new ClusteredApp());
            } else {
                System.out.println("Failed to create clustered Vert.x instance: " + res.cause());
            }
        });
    }

    @Override
    public void start(Promise<Void> startPromise) {
        System.out.println("Clustered Verticle started!");
        startPromise.complete();
    }
}
