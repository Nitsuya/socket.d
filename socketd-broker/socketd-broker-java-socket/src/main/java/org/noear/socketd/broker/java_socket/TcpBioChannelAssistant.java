package org.noear.socketd.broker.java_socket;

import org.noear.socketd.protocol.ChannelAssistant;
import org.noear.socketd.protocol.CodecByteBuffer;
import org.noear.socketd.protocol.Frame;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

/**
 * Tcp-Bio 交换器实现（它没法固定接口，但可以固定输出目录）
 *
 * @author noear
 * @since 2.0
 */
public class TcpBioChannelAssistant implements ChannelAssistant<Socket> {
    private CodecByteBuffer codec = new CodecByteBuffer();

    @Override
    public void write(Socket source, Frame frame) throws IOException {
       OutputStream output = source.getOutputStream();
       output.write(codec.encode(frame).array());
       output.flush();
    }

    @Override
    public boolean isValid(Socket target) {
        return target.isConnected();
    }

    @Override
    public void close(Socket target) throws IOException {
        target.close();
    }

    @Override
    public InetSocketAddress getRemoteAddress(Socket target) {
        return (InetSocketAddress)target.getRemoteSocketAddress();
    }

    @Override
    public InetSocketAddress getLocalAddress(Socket target) {
        return (InetSocketAddress)target.getLocalSocketAddress();
    }

    public Frame read(Socket source) throws IOException {
        InputStream input = source.getInputStream();
        if (input == null) {
            return null;
        }

        byte[] lenBts = new byte[4];
        if (input.read(lenBts) < -1) {
            return null;
        }

        int len = bytesToInt32(lenBts);

        if (len == 0) {
            return null;
        }

        ByteBuffer buffer = ByteBuffer.allocate(len);
        buffer.putInt(len);

        int bufSize = 512;
        byte[] buf = new byte[bufSize];

        int readSize = 0;

        while (true) {
            if (buffer.remaining() > bufSize) {
                readSize = bufSize;
            } else {
                readSize = buffer.remaining();
            }

            if ((readSize = input.read(buf, 0, readSize)) > 0) {
                buffer.put(buf, 0, readSize);
            } else {
                break;
            }
        }

        buffer.flip();

        return codec.decode(buffer);
    }

    private static int bytesToInt32(byte[] bytes) {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            int shift = (3 - i) * 8;
            value += (bytes[i] & 0xFF) << shift;
        }
        return value;
    }
}