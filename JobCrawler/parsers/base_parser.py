from abc import ABC, abstractmethod

from models.job import RawJobData
from sources.base import JobStub


class BaseParser(ABC):
    @abstractmethod
    def parse_list(self, html: str, base_url: str) -> list[JobStub]:
        raise NotImplementedError

    @abstractmethod
    def parse_detail(self, html: str, stub: JobStub) -> RawJobData:
        raise NotImplementedError
