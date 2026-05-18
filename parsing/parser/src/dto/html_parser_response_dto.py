from dataclasses import dataclass

from common.src.dto.file_info_dto import FileInfoDTO

@dataclass
class HtmlParserResponseDTO:
    taskId: str
    success: bool
    message: str
    htmlDocs: list[FileInfoDTO]
    images: list[FileInfoDTO]