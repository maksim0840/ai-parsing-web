from dataclasses import dataclass

from common.src.dto.file_info_dto import FileInfoDTO

@dataclass
class TextRecognitionRequestDTO:
    taskId: str
    images: list[FileInfoDTO]

    @staticmethod
    def from_dict(data: dict):
        task_id = data.get("taskId")
        images = data.get("images")
        
        if (task_id is None): 
            raise ValueError("Not specified parameter task_id")
        if (images is None): 
            raise ValueError("Not specified parameter images")

        images_dto_list = [FileInfoDTO.from_dict(img) for img in images]

        return TextRecognitionRequestDTO(
            taskId=task_id,
            images=images_dto_list,
        )