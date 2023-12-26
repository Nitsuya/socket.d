import {CodecUtils} from "./CodecUtils";
import {EntityMetas,Flags} from "./Constants";
import {SocketD} from "../../SocketD";

export interface Entity {
    metaString(): string;

    metaMap(): URLSearchParams;

    meta(name: string): string;

    data(): ArrayBuffer;

    dataAsString(): string;

    dataSize(): number;
}

export interface Reply extends Entity {
    isEnd(): boolean
}


export class EntityDefault implements Entity {
    private _metaMap: URLSearchParams
    private _data: ArrayBuffer;

    constructor() {
        this._metaMap = null;
        this._data = null;
    }


    metaStringSet(metaString: string): EntityDefault {
        this._metaMap = new URLSearchParams(metaString);
        return this;
    }

    metaMapSet(map): EntityDefault {
        for (let name of map.prototype) {
            this.metaMap().set(name, map[name]);
        }
        return this;
    }

    metaSet(name: string, value: string): EntityDefault {
        this.metaMap().set(name, value);
        return this;
    }

    dataSet(data: ArrayBuffer): EntityDefault {
        this._data = data;
        return this;
    }

    metaString(): string {
        return this.metaMap().toString();
    }

    metaMap(): URLSearchParams {
        if (this._metaMap == null) {
            this._metaMap = new URLSearchParams();
        }

        return this._metaMap;
    }

    meta(name: string): string {
        return this.metaMap().get(name);
    }

    data(): ArrayBuffer {
        return this._data;
    }

    dataAsString(): string {
        throw new Error("Method not implemented.");
    }

    dataSize(): number {
        return this._data.byteLength;
    }
}

export class StringEntity extends EntityDefault implements Entity{
    constructor(data: string) {
        super();
        const dataBuf = CodecUtils.strToBuf(data);
        this.dataSet(dataBuf);
    }
}

export interface Message extends Entity {
    isRequest(): boolean;

    isSubscribe(): boolean;

    sid(): string;

    event(): string;

    entity(): Entity;
}

export interface MessageInternal extends Message, Entity, Reply{

}

export class MessageDefault implements MessageInternal {
    _flag: number;
    _sid: string;
    _event: string;
    _entity: Entity;

    constructor(flag: number, sid: string, event: string, entity: Entity) {
        this._flag = flag;
        this._sid = sid;
        this._event = event;
        this._entity = entity;
    }

    isRequest(): boolean {
        return this._flag == Flags.Request;
    }

    isSubscribe(): boolean {
        return this._flag == Flags.Subscribe;
    }

    isEnd(): boolean {
        return this._flag == Flags.ReplyEnd;
    }

    sid(): string {
        return this._sid;
    }

    event(): string {
        return this._event;
    }

    entity(): Entity {
        return this._entity;
    }

    metaString(): string {
        return this._entity.metaString();
    }

    metaMap(): URLSearchParams {
        return this._entity.metaMap();
    }

    meta(name: string): string {
        return this._entity.meta(name);
    }

    data(): ArrayBuffer {
        return this._entity.data();
    }

    dataAsString(): string {
        return this._entity.dataAsString();
    }

    dataSize(): number {
        return this._entity.dataSize();
    }
}

export class Frame {
    _flag: number;
    _message: MessageInternal;

    constructor(flag: number, message: MessageInternal) {
        this._flag = flag;
        this._message = message;
    }

    getFlag(): number {
        return this._flag;
    }

    getMessage(): MessageInternal {
        return this._message;
    }
}


/**
 * 帧工厂
 *
 * @author noear
 * @since 2.0
 * */
export class Frames {
    /**
     * 构建连接帧
     *
     * @param sid 流Id
     * @param url 连接地址
     */
    static connectFrame(sid: string, url: string): Frame {
        let entity = new EntityDefault();
        //添加框架版本号
        entity.metaSet(EntityMetas.META_SOCKETD_VERSION, SocketD.protocolVersion());
        return new Frame(Flags.Connect, new MessageDefault(Flags.Connect, sid, url, entity));
    }

    /**
     * 构建连接确认帧
     *
     * @param connectMessage 连接消息
     */
    static connackFrame(connectMessage: Message): Frame {
        let entity = new EntityDefault();
        //添加框架版本号
        entity.metaSet(EntityMetas.META_SOCKETD_VERSION, SocketD.protocolVersion());
        return new Frame(Flags.Connack, new MessageDefault(Flags.Connack, connectMessage.sid(), connectMessage.event(), entity));
    }

    /**
     * 构建 ping 帧
     */
    static pingFrame(): Frame {
        return new Frame(Flags.Ping, null);
    }

    /**
     * 构建 pong 帧
     */
    static pongFrame(): Frame {
        return new Frame(Flags.Pong, null);
    }

    /**
     * 构建关闭帧（一般用不到）
     */
    static closeFrame(): Frame {
        return new Frame(Flags.Close, null);
    }

    /**
     * 构建告警帧（一般用不到）
     */
    static alarmFrame(from: Message, alarm: string): Frame {
        let message;

        if (from != null) {
            let entity = new StringEntity(alarm).metaStringSet(from.metaString());
            message = new MessageDefault(Flags.Alarm, from.sid(), from.event(), entity)
        } else {
            let entity = new StringEntity(alarm);
            message = new MessageDefault(Flags.Alarm, '', '', entity);
        }

        return new Frame(Flags.Alarm, message);
    }
}