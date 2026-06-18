from dataclasses import dataclass

from common.src.enum.file_type import FileType

@dataclass
class FileInfoDTO:
    filePath: str
    fileName: str
    fileType: FileType
    sizeBytes: int
    description: str
    valid: bool
    errorMessage: str

    @staticmethod
    def from_dict(data: dict):
        file_path = data.get("filePath")
        file_name = data.get("fileName", "")
        file_type = data.get("fileType")
        size_bytes = data.get("sizeBytes", 0)
        description = data.get("description", "")
        valid = data.get("valid")
        error_message = data.get("errorMessage", "")

        if (file_path is None):
            raise ValueError("Not specified parameter file_path")
        if (file_type is None): 
            raise ValueError("Not specified parameter file_type")
        if (valid is None):
            raise ValueError("Not specified parameter valid")
        
        if (file_type == "HTML"): file_type_enum = FileType.HTML
        elif (file_type == "IMG"): file_type_enum = FileType.IMG
        else:
            raise ValueError("Unknown file type")

        return FileInfoDTO(
            filePath=file_path,
            fileName=file_name,
            fileType=file_type_enum,
            sizeBytes=size_bytes,
            description=description,
            valid=valid,
            errorMessage=error_message
        )