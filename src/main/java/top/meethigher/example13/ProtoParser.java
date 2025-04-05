package top.meethigher.example13;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;

/**
 * 自定义消息结构解析器
 * [4字节长度+2字节类型+protobuf变长消息体]
 */
public class ProtoParser implements Handler<Buffer> {

    private Buffer buf = Buffer.buffer();

    /**
     * 预设长度起始位置
     */
    private final int lengthFieldOffset = 0;

    /**
     * 预设长度占用的字节数
     */
    private final int lengthFieldLength = 4;
    /**
     * 消息类型起始位置
     */
    private final int typeFieldOffset = 4;

    /**
     * 消息类型占用的字节数
     */
    private final int typeFieldLength = 2;

    /**
     * 消息体起始位置
     */
    private final int bodyFieldOffset = 6;

    private final Handler<Buffer> outputHandler;

    public ProtoParser(Handler<Buffer> outputHandler) {
        this.outputHandler = outputHandler;
    }

    @Override
    public void handle(Buffer buffer) {
        buf.appendBuffer(buffer);
        if (buf.length() < lengthFieldLength) {
            return;
        } else {
            int totalLength = buf.getInt(lengthFieldOffset);
            if (buf.length() < totalLength) {
                return;
            } else {
                outputHandler.handle(buf.getBuffer(4, totalLength));
                buf = buf.getBuffer(totalLength, buf.length());
            }
        }

    }
}
