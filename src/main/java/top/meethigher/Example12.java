package top.meethigher;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetSocket;
import top.meethigher.proto.Event;

import java.util.UUID;

public class Example12 {


    /**
     * 在tcp传输中，为了防止粘包/半包问题，需要双方明确定义好消息结构
     * <p>
     * |  4 字节（消息长度） |  2 字节（消息类型） |  变长（消息体）  |
     * |      0x0010      |      0x0001       |   {"id":1, "msg":"Hello"}  |
     * <p>
     * <p>
     * <p>
     * <p>
     * 该枚举用于其中的消息类型，使用占用2字节的short类型存储
     * <p>
     * java中的int支持自动转换进制。0x开头表示十六进制，0b开头表示二进制
     * 如 int a=0xa, 会自动将十六进制的a转为十进制的10
     * 如 int b=0b1111，会自动将二进制1111转为十进制的15
     */
    public enum MessageType {

        ping(0x1),//发送心跳
        pong(0x2),//响应心跳
        auth_req(0x3),//认证请求
        auth_resp(0x4),// 认证响应
        open_80_port_req(0x5),// 开启80端口请求
        open_80_port_resp(0x6),// 开启80端口响应
        error(0x7),//未知消息类型


        ;

        private final short code;

        MessageType(int code) {
            this.code = (short) code;
        }

        public short getCode() {
            return code;
        }

        public static MessageType fromCode(short code) {
            for (MessageType type : values()) {
                if (type.code == code) {
                    return type;
                }
            }
            return null;
        }
    }


    /**
     * pbf基本使用
     */
    public static void init() throws Exception {
        // 首先生成protobuf

        // protoc --java_out=D:/3Develop/1creativity/vertx-examples/src/main/java top/meethigher/proto/origin/Event.proto

        /**
         * 在任意目录下
         * protoc -I=$SRC_DIR --java_out=$DST_DIR $SRC_DIR/addressbook.proto
         * 在当前应用目录下
         * protoc --java_out=${OUTPUT_DIR} path/to/your/proto/file
         *
         * 注意，如果你使用的是javalite，那么在生成时，命令需要更换为如下
         * 参考 https://github.com/protocolbuffers/protobuf/blob/main/java/lite.md
         * protoc --java_out=lite:${OUTPUT_DIR} path/to/your/proto/file
         */

        Event.MyMessage message = Event.MyMessage.newBuilder().setId("编号").setContent("内容").build();
        byte[] byteArray = message.toByteArray();
        Event.MyMessage myMessage = Event.MyMessage.parseFrom(byteArray);
        System.out.println(myMessage.getId() + "--" + myMessage.getContent());
    }


    private static void startServer() {
        Vertx vertx = Vertx.vertx();
        NetServer server = vertx.createNetServer();
        server.connectHandler(socket -> {
            socket.handler(buffer -> {
                handleServerReceivedMessage(socket, buffer);
            });
        });
        server.listen(9000, res -> {
            if (res.succeeded()) {
                System.out.println("Server started on port 9000");
            } else {
                System.out.println("Failed to start server: " + res.cause());
            }
        });
    }

    private static void handleServerReceivedMessage(NetSocket socket, Buffer buffer) {
        short type = buffer.getShort(4);
        Buffer body = buffer.getBuffer(6, buffer.length());

        MessageType messageType = MessageType.fromCode(type);

        switch (messageType) {
            case ping:
                sendResponse(socket, MessageType.pong.code, Buffer.buffer().getBytes());
                break;
            case auth_req:
                try {
                    Event.MyMessage myMessage = Event.MyMessage.parseFrom(body.getBytes());
                    String content = myMessage.getContent();
                    if ("这是个token".equals(content)) {
                        Event.MyMessage message = Event.MyMessage.newBuilder().setId(UUID.randomUUID().toString())
                                .setContent("成功了")
                                .build();
                        sendResponse(socket, MessageType.auth_resp.code, message.toByteArray());
                    } else {
                        Event.MyMessage message = Event.MyMessage.newBuilder().setId(UUID.randomUUID().toString())
                                .setContent("失败了")
                                .build();
                        sendResponse(socket, MessageType.auth_resp.code, message.toByteArray());
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case open_80_port_req:
                System.out.println("客户端要求我开启80端口");
                System.out.println("已开启80端口");
                Event.MyMessage message = Event.MyMessage.newBuilder().setId(UUID.randomUUID().toString())
                        .setContent("已开启80端口")
                        .build();
                sendResponse(socket, MessageType.open_80_port_resp.code, message.toByteArray());
                break;
            default:
                Event.MyMessage message1 = Event.MyMessage.newBuilder().setId(UUID.randomUUID().toString())
                        .setContent("未知的消息类型")
                        .build();
                sendResponse(socket, MessageType.error.code, message1.toByteArray());
        }
    }


    private static void handleClientReceivedMessage(NetSocket socket, Buffer buffer) {
        short type = buffer.getShort(4);
        Buffer body = buffer.getBuffer(6, buffer.length());

        MessageType messageType = MessageType.fromCode(type);
        Event.MyMessage myMessage = null;

        try {
            myMessage = Event.MyMessage.parseFrom(body.getBytes());
        } catch (Exception e) {

        }

        System.out.println(messageType + " : " + myMessage.getContent());
    }

    private static void sendResponse(NetSocket socket, short type, byte[] body) {
        Buffer buffer = Buffer.buffer();
        buffer.appendInt(6 + body.length); // 4字节长度和消息长度
        buffer.appendShort(type); // 2字节消息类型
        buffer.appendBytes(body); // 可变消息体
        socket.write(buffer);
    }

    private static void startClient() {
        Vertx vertx = Vertx.vertx();
        NetClient client = vertx.createNetClient();
        client.connect(9000, "localhost", res -> {
            if (res.succeeded()) {
                NetSocket socket = res.result();
                socket.pause();
                socket.handler(buffer -> {
                    handleClientReceivedMessage(socket, buffer);
                });
                sendRequest(socket, MessageType.ping.code, Buffer.buffer().getBytes());
                sendRequest(socket, MessageType.auth_req.code, Event.MyMessage.newBuilder().setId(UUID.randomUUID().toString()).setContent("hello world").build().toByteArray());
                socket.resume();
            } else {
                System.out.println("Failed to connect to server: " + res.cause());
            }
        });
    }

    private static void sendRequest(NetSocket socket, short type, byte[] body) {
        Buffer buffer = Buffer.buffer();
        buffer.appendInt(6 + body.length);
        buffer.appendShort(type);
        buffer.appendBytes(body);
        socket.write(buffer);
    }

    public static void main(String[] args) {
        startServer();
        startClient();
    }

}
