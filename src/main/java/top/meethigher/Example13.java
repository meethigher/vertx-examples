package top.meethigher;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.parsetools.RecordParser;
import top.meethigher.example13.ProtoParser;

import java.util.concurrent.ThreadLocalRandom;

public class Example13 {

    public static void main(String[] args) {
        diyRecord();
    }

    /**
     * 使用换行符作为分隔符，表示消息的结尾
     * 示例消息内容
     * <pre>
     * [...\n][...\n]
     * </pre>
     */
    public static void lineBasedRecord() {
        Handler<Buffer> output = h -> {
            System.out.println(h.toString());
        };
        final RecordParser parser = RecordParser.newDelimited("\n", output);

        // parser.handle(Buffer.buffer("HELLO\nHOW ARE Y"));
        // parser.handle(Buffer.buffer("OU?\nI AM"));
        // parser.handle(Buffer.buffer("DOING OK"));
        // parser.handle(Buffer.buffer("\n"));

        Buffer buffer = Buffer.buffer("Hello!\nHow are you?\nI am fine! Thank you!\n");
        // 模拟网络传输中的粘包半包问题
        int tStart = 0, tEnd = 0;
        for (int i = 0; i < buffer.length(); i++) {
            // 前闭后开
            tEnd = ThreadLocalRandom.current().nextInt(tStart + 1, buffer.length() + 1);
            if (tEnd > buffer.length()) {
                tEnd = buffer.length();
            }
            // 前闭后开
            Buffer tb = buffer.getBuffer(tStart, tEnd);
            parser.handle(tb);
            tStart = tEnd;

            if (tStart >= buffer.length()) {
                break;
            }
        }
    }


    /**
     * 定长消息结构。假如我固定长度为3（一个中文刚好字节占3）。示例消息内容
     * 你好啊世界
     */
    public static void fixedLengthRecord() {
        RecordParser parser = RecordParser.newFixed(3, b -> {
            System.out.println(b);
        });
        parser.handle(Buffer.buffer("你好啊世界"));
    }


    /**
     * 自定义消息结构。示例消息内容
     * <pre>
     * [4字节长度+2字节类型+protobuf变长消息体][4字节长度+2字节类型+protobuf变长消息体]
     * </pre>
     */
    public static void diyRecord() {
        ProtoParser parser = new ProtoParser(b -> {
            short type = b.getShort(0);
            Buffer buffer = b.getBuffer(2, b.length());
            System.out.println("消息类型=" + type + ", 消息内容=" + buffer.toString());
        });

        // 模拟发送的消息
        Buffer buffer = Buffer.buffer();
        Buffer body = Buffer.buffer("你好，世界！");
        // 网络传输都使用大端
        // 模拟第一条消息
        buffer.appendInt(4 + 2 + body.length());
        buffer.appendShort((short) 1);
        buffer.appendBytes(body.getBytes());
        // 模拟第二条消息
        Buffer body1 = Buffer.buffer("hello, world! ");
        buffer.appendInt(4 + 2 + body1.length());
        buffer.appendShort((short) 2);
        buffer.appendBytes(body1.getBytes());
        buffer.appendBytes(body1.getBytes());


        // 模拟网络传输中的粘包半包问题
        int tStart = 0, tEnd = 0;
        for (int i = 0; i < buffer.length(); i++) {
            // 前闭后开
            tEnd = ThreadLocalRandom.current().nextInt(tStart + 1, buffer.length() + 1);
            if (tEnd > buffer.length()) {
                tEnd = buffer.length();
            }
            // 前闭后开
            Buffer tb = buffer.getBuffer(tStart, tEnd);
            parser.handle(tb);
            tStart = tEnd;

            if (tStart >= buffer.length()) {
                break;
            }
        }

    }
}
