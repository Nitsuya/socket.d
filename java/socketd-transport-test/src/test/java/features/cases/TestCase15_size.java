package features.cases;

import org.noear.socketd.SocketD;
import org.noear.socketd.exception.SocketdException;
import org.noear.socketd.transport.core.Message;
import org.noear.socketd.transport.core.Session;
import org.noear.socketd.transport.core.SimpleListener;
import org.noear.socketd.transport.core.entity.StringEntity;
import org.noear.socketd.transport.server.Server;
import org.noear.socketd.transport.server.ServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * sendAndRequest() 超时
 *
 * @author noear
 * @since 2.0
 */
public class TestCase15_size extends BaseTestCase {
    private static Logger log = LoggerFactory.getLogger(TestCase15_size.class);
    public TestCase15_size(String schema, int port) {
        super(schema, port);
    }

    private Server server;
    private Session clientSession;

    private AtomicInteger messageCounter = new AtomicInteger();

    @Override
    public void start() throws Exception {
        log.trace("...");

        super.start();
        //server
        server = SocketD.createServer(new ServerConfig(getSchema()).port(getPort()))
                .listen(new SimpleListener() {
                    @Override
                    public void onMessage(Session session, Message message) throws IOException {
                        System.out.println("::" + message);
                        messageCounter.incrementAndGet();

                    }
                })
                .start();

        //休息下，启动可能要等会儿
        Thread.sleep(100);


        //client
        String serverUrl = getSchema() + "://127.0.0.1:" + getPort() + "/path?u=a&p=2";
        clientSession = SocketD.createClient(serverUrl).open();

        StringBuilder meta = new StringBuilder(4000);
        while (meta.length() < 50000){
            meta.append("asdfqjwoefjasdfkqowefijqowefjqowefjoqwiefjqoweifjqowef");
        }

        try {
            clientSession.send("/user/size", new StringEntity("hi").meta("test", meta.toString()));
        }catch (SocketdException e){
            e.printStackTrace();
        }
        clientSession.send("/user/size", new StringEntity("hi").meta("test", "ok"));


        Thread.sleep(1000);
    }

    @Override
    public void stop() throws Exception {
        super.stop();

        if (server != null) {
            server.stop();
        }

        if (clientSession != null) {
            clientSession.close();
        }
    }
}
