package org.noear.socketd.transport.smartsocket.tcp.impl;

import org.noear.socketd.exception.SocketDSizeLimitException;
import org.noear.socketd.transport.core.ChannelSupporter;
import org.noear.socketd.transport.core.Constants;
import org.noear.socketd.transport.core.Frame;
import org.noear.socketd.transport.core.codec.ByteBufferCodecReader;

import org.smartboot.socket.Protocol;
import org.smartboot.socket.extension.decoder.FixedLengthFrameDecoder;
import org.smartboot.socket.transport.AioSession;

import java.nio.ByteBuffer;

/**
 * @author noear
 * @since 2.1
 */
public class FrameProtocol implements Protocol<Frame> {
    private final ChannelSupporter<AioSession> channelSupporter;

    public FrameProtocol(ChannelSupporter<AioSession> channelSupporter) {
        this.channelSupporter = channelSupporter;
    }


    @Override
    public Frame decode(ByteBuffer buffer, AioSession aioSession) {
        ChannelDefaultEx channel = aioSession.getAttachment();
        FixedLengthFrameDecoder decoder = channel.getDecoder();

        if (decoder == null) {
            if (buffer.remaining() < Integer.BYTES) {
                return null;
            } else {
                buffer.mark();
                int frameLength = buffer.getInt();

                if(frameLength > Constants.MAX_SIZE_FRAME) {
                    //超过限制大小
                    throw new SocketDSizeLimitException("Adjusted frame length exceeds " + Constants.MAX_SIZE_FRAME + ": " + frameLength + " - discarded");
                }

                decoder = new FixedLengthFrameDecoder(frameLength);
                buffer.reset();
                channel.setDecoder(decoder);
            }
        }

        if (decoder.decode(buffer) == false) {
            return null;
        } else {
            channel.setDecoder(null);
            buffer = decoder.getBuffer();
        }

        return channelSupporter.getConfig().getCodec().read(new ByteBufferCodecReader(buffer));
    }
}
