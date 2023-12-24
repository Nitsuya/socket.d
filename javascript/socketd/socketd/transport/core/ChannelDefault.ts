import {Processor} from "./Processor";
import {ChannelAssistant} from "./ChannelAssistant";
import {StreamBase, StreamManger} from "./Stream";
import {Session, SessionDefault} from "./Session";
import {ChannelSupporter} from "./ChannelSupporter";
import {Config} from "./Config";
import {Frame, Frames, MessageDefault} from "./Message";
import {EntityMetas} from "./Constants";
import {ChannelBase, ChannelInternal} from "./Channel";

export class ChannelDefault<S> extends ChannelBase implements ChannelInternal {
    _source: S;
    //处理器
    _processor: Processor;
    //助理
    _assistant: ChannelAssistant<S>;
    //流管理器
    _streamManger: StreamManger;
    //会话（懒加载）
    _session:Session;

    constructor(source: S, supporter: ChannelSupporter<S>) {
        super(supporter.config());
        this._source = source;
        this._processor = supporter.processor();
        this._assistant = supporter.assistant();
        this._streamManger = supporter.config().getStreamManger();
    }

    isValid() {
        return this.isClosed() == 0 && this._assistant.isValid(this._source);
    }

    config(): Config {
        return this._config;
    }

    sendPing() {
        this.send(Frames.pingFrame(), null);
    }

    sendPong() {
        this.send(Frames.pongFrame(), null);
    }

    send(frame: Frame, stream: StreamBase) {

        if (this.getConfig().clientMode()) {
            console.debug("C-SEN:{}", frame);
        } else {
            console.debug("S-SEN:{}", frame);
        }


        if (frame.getMessage() != null) {
            let message = frame.getMessage();

            //注册流接收器
            if (stream != null) {
                this._streamManger.addStream(message.sid(), stream);
            }

            //如果有实体（尝试分片）
            if (message.entity() != null) {
                //确保用完自动关闭

                if (message.dataSize() > this.getConfig().getFragmentSize()) {
                    message.metaMap().set(EntityMetas.META_DATA_LENGTH, message.dataSize().toString());

                    //满足分片条件
                    let fragmentIndex = 0;
                    while (true) {
                        //获取分片
                        fragmentIndex++;
                        let fragmentEntity = this.getConfig().getFragmentHandler().nextFragment(this, fragmentIndex, message);

                        if (fragmentEntity != null) {
                            //主要是 sid 和 entity
                            let fragmentFrame = new Frame(frame.getFlag(), new MessageDefault(
                                frame.getFlag(),
                                message.sid(),
                                '',
                                fragmentEntity));

                            this._assistant.write(this._source, fragmentFrame);
                        } else {
                            //没有分片，说明发完了
                            return;
                        }
                    }
                } else {
                    //不满足分片条件，直接发
                    this._assistant.write(this._source, frame);
                    return;
                }

            }
        }

        this._assistant.write(this._source, frame);
    }

    getSession(): Session {
        if (this._session == null) {
            this._session = new SessionDefault(this);
        }

        return this._session;
    }

    setSession(session: Session) {
        this._session = session;
    }

    close(code) {
        console.debug("{} channel will be closed, sessionId={}",
            this.getConfig().getRoleName(),
            this.getSession().sessionId());

        try {
            super.close(code);
            this._assistant.close(this._source);
        } catch (e) {
            console.warn("{} channel close error, sessionId={}",
                this.getConfig().getRoleName(),
                this.getSession().sessionId(), e);
        }
    }
}
