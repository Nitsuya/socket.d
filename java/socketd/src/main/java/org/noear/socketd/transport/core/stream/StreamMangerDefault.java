package org.noear.socketd.transport.core.stream;

import org.noear.socketd.transport.core.StreamAcceptor;
import org.noear.socketd.transport.core.StreamAcceptorBase;
import org.noear.socketd.transport.core.StreamManger;
import org.noear.socketd.transport.core.Config;
import org.noear.socketd.transport.core.internal.ChannelDefault;
import org.noear.socketd.utils.RunUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 流管理器
 *
 * @author noear
 * @since 1.0
 */
public class StreamMangerDefault implements StreamManger {
    private static Logger log = LoggerFactory.getLogger(ChannelDefault.class);

    //配置
    private final Config config;
    //流接收器字典（管理）
    private final Map<String, StreamAcceptorBase> acceptorMap;

    public StreamMangerDefault(Config config) {
        this.acceptorMap = new ConcurrentHashMap<>();
        this.config = config;
    }

    /**
     * 添加流接收器
     *
     * @param sid      流Id
     * @param acceptor 流接收器
     */
    @Override
    public void addAcceptor(String sid, StreamAcceptorBase acceptor) {
        acceptorMap.put(sid, acceptor);

        //增加流超时处理（做为后备保险）
        if (config.getStreamTimeout() > 0) {
            acceptor.insuranceFuture = RunUtils.delay(() -> {
                acceptorMap.remove(sid);
            }, config.getStreamTimeout());
        }
    }

    /**
     * 获取流接收器
     *
     * @param sid 流Id
     */
    @Override
    public StreamAcceptor getAcceptor(String sid) {
        return acceptorMap.get(sid);
    }

    /**
     * 移除流接收器
     *
     * @param sid 流Id
     */
    @Override
    public void removeAcceptor(String sid) {
        StreamAcceptorBase acceptor = acceptorMap.remove(sid);

        if (acceptor != null) {
            if (acceptor.insuranceFuture != null) {
                acceptor.insuranceFuture.cancel(false);
            }

            if (log.isDebugEnabled()) {
                log.debug("{} acceptor removed, sid={}", config.getRoleName(), sid);
            }
        }
    }
}