package org.noear.socketd.transport.java_kcp.impl;

import io.netty.buffer.ByteBuf;
import kcp.KcpListener;
import kcp.Ukcp;
import org.noear.socketd.transport.core.Channel;
import org.noear.socketd.transport.core.Frame;
import org.noear.socketd.transport.core.buffer.BufferReader;
import org.noear.socketd.transport.core.internal.ChannelDefault;
import org.noear.socketd.transport.java_kcp.KcpNioServer;

/**
 * @author noear
 * @since 2.1
 */
public class ServerKcpListener implements KcpListener {
    private final KcpNioServer server;

    public ServerKcpListener(KcpNioServer server) {
        this.server = server;
    }

    @Override
    public void onConnected(Ukcp ukcp) {
        Channel channel = new ChannelDefault<>(ukcp, server);
        ukcp.user().setCache(channel);
    }

    @Override
    public void handleReceive(ByteBuf byteBuf, Ukcp ukcp) {
        BufferReader reader = new NettyBufferReader(byteBuf);
        Frame frame = server.config().getCodec().read(reader);
        if (frame == null) {
            return;
        }

        Channel channel = ukcp.user().getCache();

        try {
            server.processor().onReceive(channel, frame);
        } catch (Throwable e) {
            server.processor().onError(channel, e);
        }
    }

    @Override
    public void handleException(Throwable throwable, Ukcp ukcp) {
        Channel channel = ukcp.user().getCache();
        server.processor().onError(channel, throwable);
    }

    @Override
    public void handleClose(Ukcp ukcp) {
        Channel channel = ukcp.user().getCache();
        server.processor().onClose(channel);
    }
}