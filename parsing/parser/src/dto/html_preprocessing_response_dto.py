from dataclasses import dataclass
from common.src.dto.file_info_dto import FileInfoDTO
import json

@dataclass
class HtmlPreprocessingResponseDTO:
    taskId: str
    success: bool
    message: str
    htmlDocs: list[FileInfoDTO]