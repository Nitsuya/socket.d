package org.noear.socketd.broker.bio.server;

import org.noear.socketd.protocol.Frame;
import org.noear.socketd.protocol.Processor;
import org.noear.socketd.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author noear 2023/10/13 created
 */
public class BioServer implements Server {
    private static final Logger log = LoggerFactory.getLogger(BioServer.class);
    private ServerSocket server;
    private BioServerConfig serverConfig;
    private Thread serverThread;
    private ExecutorService serverExecutor;

    private Processor processor;

    public BioServer(BioServerConfig config){
        this.serverConfig = config;
    }

    @Override
    public void binding(Processor processor) {
        this.processor = processor;
    }

    @Override
    public void start() throws IOException {
        if (serverThread != null) {
            throw new IllegalStateException("Server started");
        }

        if (serverExecutor == null) {
            serverExecutor = Executors.newFixedThreadPool(serverConfig.getCoreThreads());
        }

        if (server == null) {
            server = new ServerSocket(serverConfig.getPort());
        }

        serverThread = new Thread(() -> {
            try {
                while (true) {
                    Socket socket = server.accept();

                    try {
                        BioChannel channel = new BioChannel(socket);

                        serverExecutor.submit(() -> {
                            receive(channel, socket);
                        });
                    } catch (Throwable e) {
                        log.debug("{}", e);
                        close(socket);
                    }
                }
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });

        serverThread.start();
    }

    private void receive(BioChannel channel, Socket socket) {
        while (true) {
            try {
                if (socket.isClosed()) {
                    processor.onClose(channel.getSession());
                    break;
                }

                Frame frame = channel.receive();
                if (frame != null) {
                    processor.onReceive(channel, frame);
                }
            } catch (Throwable ex) {
                processor.onError(channel.getSession(), ex);
            }
        }
    }


    private void close(Socket socket) {
        try {
            socket.close();
        } catch (Throwable e) {
            log.debug("{}", e);
        }
    }

    @Override
    public void stop() throws IOException {
        if (server == null || server.isClosed()) {
            return;
        }

        try {
            server.close();
        } catch (Exception e) {
            log.debug("{}", e);
        }
    }
}