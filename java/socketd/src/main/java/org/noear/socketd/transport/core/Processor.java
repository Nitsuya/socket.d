package org.noear.socketd.transport.core;

import org.noear.socketd.transport.stream.StreamInternal;

import java.io.IOException;

/**
 * 协议处理器
 *
 * @author noear
 * @since 2.0
 */
public interface Processor {
    /**
     * 设置监听器
     */
    void setListener(Listener listener);

    /**
     * 接收处理
     */
    void onReceive(ChannelInternal channel, Frame frame);

    /**
     * 打开时
     *
     * @param channel 通道
     */
    void onOpen(ChannelInternal channel);

    /**
     * 收到消息时
     *
     * @param channel 通道
     * @param frame   帧
     */
    void onMessage(ChannelInternal channel, Frame frame);

    /**
     * 收到签复时
     *
     * @param channel 通道
     * @param frame   帧
     * @param stream  流
     */
    void onReply(ChannelInternal channel, Frame frame, StreamInternal stream);


    /**
     * 发送时
     *
     * @param channel          通道
     * @param frame            帧
     * @param channelAssistant 通道助理
     * @param target           发送目标
     */
    <S> void onSend(ChannelInternal channel, Frame frame, ChannelAssistant<S> channelAssistant, S target) throws IOException;

    /**
     * 关闭时
     *
     * @param channel 通道
     */
    void onClose(ChannelInternal channel);

    /**
     * 出错时
     *
     * @param channel 通道
     * @param error   错误信息
     */
    void onError(ChannelInternal channel, Throwable error);

    /**
     * 执行关闭通知
     *
     * @param channel 通道
     */
    void doCloseNotice(ChannelInternal channel);
}