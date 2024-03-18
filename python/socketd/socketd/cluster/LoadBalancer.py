from threading import RLock
from typing import List

from socketd.transport.client.ClientSession import ClientSession
from socketd.transport.utils.StrUtil import StrUtil


class LoadBalancer:
    __roundCounter:int = 0 # 轮环计数器
    __lock:RLock = RLock() # 计数锁

    @staticmethod
    def round_counter_get(self)->int:
        self.__lock.acquire()

        try:
            self.__roundCounter += 1
            if self.__roundCounter > 999_999:
                self.__roundCounter = 0
            return self.__roundCounter
        finally:
            self.__lock.release()


    # 根据 poll 获取任意一个
    @staticmethod
    def get_any_by_poll(self, coll:List[ClientSession]) -> ClientSession:
        return self.get_any(coll, self.round_counter_get())

    # 根据 hash 获取任意一个
    @staticmethod
    def get_any_by_hash(self, coll:List[ClientSession], diversion:str) -> ClientSession:
        return self.get_any(coll, StrUtil.hash_code(diversion))

    # 获取任意一个
    @staticmethod
    def get_any(self, coll:list[ClientSession], random:int) -> ClientSession:
        if coll == None or coll.count() == 0:
            return None
        else:
            sessions:List[ClientSession] = []
            for s in coll:
                if s.is_valid() and not s.is_closing():
                    sessions.extend(s)

            if sessions.count() == 0:
                return None

            if sessions.count() == 1:
                return sessions[0]

            random = abs(random)
            idx = random % sessions.count()
            return sessions[idx]


    # 获取第一个
    @staticmethod
    def get_first(self, coll:List[ClientSession]):
        if coll is None or coll.count() == 0:
            return None
        else:
            for s in coll:
                if s.is_valid() and not s.is_closing():
                    return s

            return None