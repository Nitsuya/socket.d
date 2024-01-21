import asyncio

from abc import ABC
from typing import Optional

from socketd.transport.core.impl.SessionBase import SessionBase
from socketd.transport.core.Channel import Channel
from socketd.transport.core.HandshakeDefault import HandshakeDefault
from socketd.transport.core.Entity import Entity
from socketd.transport.core.Message import Message
from socketd.transport.core.Frame import Frame
from socketd.transport.core.Costants import Flag
from socketd.transport.core.entity.MessageDefault import MessageDefault
from socketd.exception.SocketdExecption import SocketdException
from socketd.transport.stream.RequestStream import RequestStream
from socketd.transport.stream.SendStream import SendStream

from socketd.transport.stream import SubscribeStream

from loguru import logger


class SessionDefault(SessionBase, ABC):

    def __init__(self, channel: Channel):
        super().__init__(channel)
        self.__path_new = None

    def is_valid(self) -> bool:
        return self._channel.is_valid()

    def get_remote_address(self) -> str:
        return self._channel.get_remote_address()

    def get_local_address(self) -> str:
        return self._channel.get_local_address()

    def get_handshake(self) -> HandshakeDefault:
        return self._channel.get_handshake()

    def send_ping(self):
        # 取消 await(让它异步运行)
        self._channel.send_ping()

    def send(self, topic: str, content: Entity) -> SendStream:
        message = MessageDefault().set_sid(self.generate_id()).set_event(topic).set_entity(content)

        stream: SendStream = SendStream(message.get_sid())
        #取消 await(让它异步运行) //就不会挡住 stream 的输出
        self._channel.send(Frame(Flag.Message, message), stream)
        return stream

    def send_and_request(self, event: str, content: Entity,
                               timeout: int = 100) -> RequestStream:

        if timeout < 100:
            timeout = self._channel.get_config().get_reply_timeout()
        message = MessageDefault().set_sid(self.generate_id()).set_event(event).set_entity(content)
        try:
            stream = RequestStream(message.get_sid(), timeout)
            #取消 await(让它异步运行)
            self._channel.send(Frame(Flag.Request, message),
                                     stream)
            return stream
        # 这几行的异常处理，参考 java 移到 RequestStream::__await__
        # except asyncio.TimeoutError as e:
        #     if self._channel.is_valid():
        #         raise SocketdException(f"Request reply timeout>{timeout} "
        #                                f"sessionId={self._channel.get_session().get_session_id()} "
        #                                f"event={event} sid={message.get_sid()}")
        #     else:
        #         raise SocketdException(
        #             f"This channel is closed sessionId={self._channel.get_session().get_session_id()} "
        #             f"event={event} sid={message.get_sid()} {str(e)}")
        except Exception as e:
            raise e

    def send_and_subscribe(self, event: str, content: Entity, timeout: int = 0):
        message = MessageDefault().set_sid(self.generate_id()).set_event(event).set_entity(content)
        stream = SubscribeStream(message.get_sid(), timeout)
        # 取消 await(让它异步运行)
        self._channel.send(Frame(Flag.Subscribe, message), stream)
        return stream

    async def reply(self, from_msg: Message, content: Entity):
        await self._channel.send(Frame(Flag.Reply,
                                       MessageDefault()
                                       .set_sid(from_msg.get_sid())
                                       .set_event(from_msg.get_event())
                                       .set_entity(content)), None)

    async def reply_end(self, from_msg: Message, content: Entity):
        await self._channel.send(Frame(Flag.ReplyEnd,
                                       MessageDefault()
                                       .set_sid(from_msg.get_sid())
                                       .set_event(from_msg.get_event())
                                       .set_entity(content)), None)

    async def reconnect(self):
        await self._channel.reconnect()

    async def close(self):
        if self._channel.is_valid():
            try:
                await self._channel.send_close()
            except Exception as e:
                logger.warning(f" {self._channel.get_config().get_role_name()} channel send_close error {e}")
        await self._channel.close()

    def get_param(self, name: str):
        return self.get_handshake().get_param(name)

    def pathNew(self, path: str):
        self.__path_new = path

    def path(self) -> Optional[str]:
        if path_new := self.__path_new:
            return path_new
        return self.get_handshake().get_uri().__str__()