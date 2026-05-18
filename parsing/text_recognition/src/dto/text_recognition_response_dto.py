from dataclasses import dataclass

from common.src.dto.file_info_dto import FileInfoDTO

@dataclass
class TextRecognitionResponseDTO:
    taskId: str
    success: bool
    message: str
    images: list[FileInfoDTO]
