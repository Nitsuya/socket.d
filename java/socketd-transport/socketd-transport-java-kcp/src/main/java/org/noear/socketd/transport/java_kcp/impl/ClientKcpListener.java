package org.noear.socketd.transport.java_kcp.impl;

import io.netty.buffer.ByteBuf;
import kcp.KcpListener;
import kcp.Ukcp;
import org.noear.socketd.transport.client.ClientHandshakeResult;
import org.noear.socketd.transport.core.Channel;
import org.noear.socketd.transport.core.ChannelInternal;
import org.noear.socketd.transport.core.Flags;
import org.noear.socketd.transport.core.Frame;
import org.noear.socketd.transport.core.buffer.BufferReader;
import org.noear.socketd.transport.core.internal.ChannelDefault;
import org.noear.socketd.transport.java_kcp.KcpNioClient;

import java.util.concurrent.CompletableFuture;

/**
 * @author noear
 * @since 2.1
 */
public class ClientKcpListener implements KcpListener {
    private final KcpNioClient client;
    private final CompletableFuture<ClientHandshakeResult> handshakeFuture = new CompletableFuture<>();

    public CompletableFuture<ClientHandshakeResult> getHandshakeFuture() {
        return handshakeFuture;
    }

    public ClientKcpListener(KcpNioClient client){
        this.client = client;
    }

    @Override
    public void onConnected(Ukcp ukcp) {
        ChannelInternal channel = new ChannelDefault<>(ukcp, client);
        ukcp.user().setCache(channel);

        //开始握手
        try {
            channel.sendConnect(client.config().getUrl());
        } catch (Throwable e) {
            channel.onError(e);
        }
    }

    @Override
    public void handleReceive(ByteBuf byteBuf, Ukcp ukcp) {
        BufferReader reader = new NettyBufferReader(byteBuf);
        Frame frame = client.config().getCodec().read(reader);
        if (frame == null) {
            return;
        }

        ChannelInternal channel = ukcp.user().getCache();

        try {
            if (frame.getFlag() == Flags.Connack) {
                channel.onOpenFuture().whenComplete((r, e) -> {
                    handshakeFuture.complete(new ClientHandshakeResult(channel, e));
                });
            }

            client.processor().onReceive(channel, frame);
        } catch (Throwable e) {
            client.processor().onError(channel, e);

            //说明握手失败了
            handshakeFuture.complete(new ClientHandshakeResult(channel, e));
        }
    }

    @Override
    public void handleException(Throwable throwable, Ukcp ukcp) {
        ChannelInternal channel = ukcp.user().getCache();
        client.processor().onError(channel, throwable);
    }

    @Override
    public void handleClose(Ukcp ukcp) {
        ChannelInternal channel = ukcp.user().getCache();
        client.processor().onClose(channel);
    }
}