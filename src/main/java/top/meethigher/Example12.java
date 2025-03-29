package top.meethigher;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;
import top.meethigher.proto.Event;

import java.util.concurrent.locks.LockSupport;

public class Example12 {

    public static void main(String[] args) throws Exception {
        // 首先生成protobuf

        // protoc --java_out=D:/3Develop/1creativity/vertx-examples/src/main/java top/meethigher/proto/origin/Event.proto

        /**
         * 在任意目录下
         * protoc -I=$SRC_DIR --java_out=$DST_DIR $SRC_DIR/addressbook.proto
         * 在当前应用目录下
         * protoc --java_out=${OUTPUT_DIR} path/to/your/proto/file
         */

        Event.MyMessage message = Event.MyMessage.newBuilder().setId("编号").setContent("内容").build();
        byte[] byteArray = message.toByteArray();
        Event.MyMessage myMessage = Event.MyMessage.parseFrom(byteArray);


        Vertx.vertx().createNetServer().connectHandler(netSocket -> {
            netSocket.pause();
            netSocket.handler(buffer -> {
                try {
                    Event.MyMessage t = Event.MyMessage.parseFrom(byteArray);
                    System.out.println("接收<==id: " + t.getId() + " content: " + t.getContent());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            netSocket.resume();
        }).listen(7777);
        Vertx.vertx().createNetClient().connect(7777, "127.0.0.1").onComplete(ar -> {
            if (ar.succeeded()) {
                NetSocket result = ar.result();
                Event.MyMessage m1 = Event.MyMessage.newBuilder().setId("编号").setContent("内容").build();
                System.out.println("发送==>id: " + m1.getId() + " content: " + m1.getContent());
                byte[] ba = m1.toByteArray();
                result.write(Buffer.buffer(ba));
            }
        });

        LockSupport.park();

    }
}
