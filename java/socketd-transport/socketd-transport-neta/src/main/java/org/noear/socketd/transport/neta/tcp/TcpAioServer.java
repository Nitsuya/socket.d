package org.noear.socketd.transport.neta.tcp;

import net.hasor.neta.channel.CobbleSocket;
import net.hasor.neta.channel.NetChannel;
import net.hasor.neta.channel.PipelineFactory;
import net.hasor.neta.channel.SoConfig;
import net.hasor.neta.handler.PipeInitializer;
import net.hasor.neta.handler.PipeListener;
import net.hasor.neta.handler.codec.LimitFrameHandler;
import org.noear.socketd.SocketD;
import org.noear.socketd.transport.core.ChannelSupporter;
import org.noear.socketd.transport.core.Constants;
import org.noear.socketd.transport.core.Frame;
import org.noear.socketd.transport.neta.tcp.impl.FramePipeLayer;
import org.noear.socketd.transport.server.Server;
import org.noear.socketd.transport.server.ServerBase;
import org.noear.socketd.transport.server.ServerConfig;
import org.noear.socketd.utils.StrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author noear
 * @since 2.3
 */
public class TcpAioServer extends ServerBase<TcpAioChannelAssistant> implements ChannelSupporter<NetChannel> {
    private static final Logger log = LoggerFactory.getLogger(TcpAioServer.class);
    private CobbleSocket server;

    public TcpAioServer(ServerConfig config) {
        super(config, new TcpAioChannelAssistant());
    }

    @Override
    public String getTitle() {
        return "tcp/aio/neta 1.0/" + SocketD.version();
    }

    @Override
    public Server start() throws IOException {
        if (isStarted) {
            throw new IllegalStateException("Socket.D server started");
        } else {
            isStarted = true;
        }

        PipelineFactory pipeline = PipeInitializer.builder()
                .nextToDecoder(new LimitFrameHandler(Constants.MAX_SIZE_FRAME))
                .nextTo(new FramePipeLayer(getConfig()))
                .bindReceive((PipeListener<Frame>) (channel, data) -> {
                    ((NetChannel) channel).sendData("echo " + data);
                }).build();

        SoConfig soConfig = new SoConfig();
        server = new CobbleSocket(soConfig);

        if (StrUtils.isEmpty(getConfig().getHost())) {
            server.listen(getConfig().getPort(), pipeline);
        } else {
            server.listen(getConfig().getHost(), getConfig().getPort(), pipeline);
        }

        log.info("Socket.D server started: {server=" + getConfig().getLocalUrl() + "}");

        return this;
    }

    @Override
    public void stop() {
        if (isStarted) {
            isStarted = false;
        } else {
            return;
        }

        try {
            if (server != null) {
                server.shutdown();
            }
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Server stop error", e);
            }
        }
    }
}