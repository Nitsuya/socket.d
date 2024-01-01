var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
import { ChannelBase } from "../core/Channel";
import { HeartbeatHandlerDefault } from "../core/HeartbeatHandler";
import { Constants } from "../core/Constants";
import { Asserts } from "../core/Asserts";
import { SocketdChannelException, SocketdException } from "../../exception/SocketdException";
import { RunUtils } from "../../utils/RunUtils";
/**
 * 客户端通道
 *
 * @author noear
 * @since 2.0
 */
export class ClientChannel extends ChannelBase {
    constructor(real, connector) {
        super(real.getConfig());
        this._connector = connector;
        this._real = real;
        if (connector.heartbeatHandler() == null) {
            this._heartbeatHandler = new HeartbeatHandlerDefault(null);
        }
        else {
            this._heartbeatHandler = new HeartbeatHandlerDefault(connector.heartbeatHandler());
        }
        this.initHeartbeat();
    }
    /**
     * 初始化心跳（关闭后，手动重链时也会用到）
     */
    initHeartbeat() {
        if (this._heartbeatScheduledFuture) {
            clearInterval(this._heartbeatScheduledFuture);
        }
        if (this._connector.autoReconnect()) {
            this._heartbeatScheduledFuture = setInterval(() => {
                try {
                    this.heartbeatHandle();
                }
                catch (e) {
                    console.warn("Client channel heartbeat error", e);
                }
            }, this._connector.heartbeatInterval());
        }
    }
    /**
     * 心跳处理
     */
    heartbeatHandle() {
        return __awaiter(this, void 0, void 0, function* () {
            if (this._real != null) {
                //说明握手未成
                if (this._real.getHandshake() == null) {
                    return;
                }
                //手动关闭
                if (this._real.isClosed() == Constants.CLOSE4_USER) {
                    console.debug(`Client channel is closed (pause heartbeat), sessionId=${this.getSession().sessionId()}`);
                    return;
                }
            }
            try {
                yield this.prepareCheck();
                this._heartbeatHandler.heartbeat(this.getSession());
            }
            catch (e) {
                if (e instanceof SocketdException) {
                    throw e;
                }
                if (this._connector.autoReconnect()) {
                    this._real.close(Constants.CLOSE3_ERROR);
                    this._real = null;
                }
                throw new SocketdChannelException(e);
            }
        });
    }
    /**
     * 预备检测
     *
     * @return 是否为新链接
     */
    prepareCheck() {
        return __awaiter(this, void 0, void 0, function* () {
            if (this._real == null || this._real.isValid() == false) {
                this._real = yield this._connector.connect();
                return true;
            }
            else {
                return false;
            }
        });
    }
    /**
     * 是否有效
     */
    isValid() {
        if (this._real == null) {
            return false;
        }
        else {
            return this._real.isValid();
        }
    }
    /**
     * 是否已关闭
     */
    isClosed() {
        if (this._real == null) {
            return 0;
        }
        else {
            return this._real.isClosed();
        }
    }
    /**
     * 发送
     *
     * @param frame  帧
     * @param stream 流（没有则为 null）
     */
    send(frame, stream) {
        return __awaiter(this, void 0, void 0, function* () {
            Asserts.assertClosedByUser(this._real);
            try {
                yield this.prepareCheck();
                this._real.send(frame, stream);
            }
            catch (e) {
                if (this._connector.autoReconnect()) {
                    this._real.close(Constants.CLOSE3_ERROR);
                    this._real = null;
                }
                throw e;
            }
        });
    }
    retrieve(frame) {
        this._real.retrieve(frame);
    }
    reconnect() {
        this.initHeartbeat();
        this.prepareCheck();
    }
    onError(error) {
        this._real.onError(error);
    }
    close(code) {
        RunUtils.runAndTry(() => clearInterval(this._heartbeatScheduledFuture));
        RunUtils.runAndTry(() => this._connector.close());
        if (this._real) {
            RunUtils.runAndTry(() => this._real.close(code));
        }
        super.close(code);
    }
    getSession() {
        return this._real.getSession();
    }
}