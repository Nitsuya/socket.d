package org.noear.socketd.transport.netty.udp;

import io.netty.buffer.ByteBuf;
import org.noear.socketd.transport.core.buffer.ByteBufferReader;
import org.noear.socketd.transport.core.buffer.ByteBufferWriter;
import org.noear.socketd.transport.netty.udp.impl.DatagramTagert;
import org.noear.socketd.transport.core.ChannelAssistant;
import org.noear.socketd.transport.core.Config;
import org.noear.socketd.transport.core.Frame;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

/**
 * Udp-Nio 通道助理实现
 *
 * @author noear
 * @since 2.0
 */
public class UdpNioChannelAssistant implements ChannelAssistant<DatagramTagert> {
    private Config config;
    public UdpNioChannelAssistant(Config config){
        this.config = config;
    }
    @Override
    public void write(DatagramTagert target, Frame frame) throws IOException {
        ByteBufferWriter writer = config.getCodec().write(frame, i -> new ByteBufferWriter(ByteBuffer.allocate(i)));

        target.send(writer.getBuffer().array());
    }

    public Frame read(ByteBuf inBuf) throws Exception {
        if (inBuf.readableBytes() < Integer.BYTES) {
            return null;
        }

        inBuf.markReaderIndex();
        int len = inBuf.readInt();

        if (inBuf.readableBytes() < (len - Integer.BYTES)) {
            inBuf.resetReaderIndex();
            return null;
        }

        byte[] bytes = new byte[len - Integer.BYTES];
        inBuf.readBytes(bytes);

        ByteBuffer byteBuffer = ByteBuffer.allocate(len);
        byteBuffer.putInt(len);
        byteBuffer.put(bytes);
        byteBuffer.flip();

        return config.getCodec().read(new ByteBufferReader(byteBuffer));
    }

    @Override
    public boolean isValid(DatagramTagert target) {
        return true;
    }

    @Override
    public void close(DatagramTagert target) throws IOException {
        target.close();
    }

    @Override
    public InetSocketAddress getRemoteAddress(DatagramTagert target) {
        return target.getRemoteAddress();
    }

    @Override
    public InetSocketAddress getLocalAddress(DatagramTagert target) {
        return target.getLocalAddress();
    }
}