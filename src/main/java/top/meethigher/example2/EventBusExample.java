//package top.meethigher.example1;
//
//import io.vertx.core.AbstractVerticle;
//import io.vertx.core.Vertx;
//import lombok.extern.slf4j.Slf4j;
//
//@Slf4j
//public class EventBusExample extends AbstractVerticle {
//    public static void main(String[] args) {
//        Vertx vertx = Vertx.vertx();
//        vertx.deployVerticle(new SenderVerticle());
//        vertx.deployVerticle(new ReceiverVerticle());
//    }
//
//    static class SenderVerticle extends AbstractVerticle {
//        @Override
//        public void start() {
//            vertx.setPeriodic(1000, id -> {
//                vertx.eventBus().send("message.address", "Hello from Sender!");
//            });
//        }
//    }
//
//    static class ReceiverVerticle extends AbstractVerticle {
//        @Override
//        public void start() {
//            vertx.eventBus().consumer("message.address", message -> {
//                log.info("Received message: " + message.body());
//            });
//        }
//    }
//}
