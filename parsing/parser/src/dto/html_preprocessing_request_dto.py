from dataclasses import dataclass
from common.src.dto.file_info_dto import FileInfoDTO

@dataclass
class HtmlPreprocessingRequestDTO:
    taskId: str
    htmlDocs: list[FileInfoDTO]
    noscriptProcessing: bool
    linkProcessing: bool
    styleProcessing: bool
    metaProcessing: bool
    scriptProcessing: bool
    canvasProcessing: bool
    svgProcessing: bool
    areaProcessing: bool
    imgProcessing: bool
    videoProcessing: bool
    audioProcessing: bool
    iframeProcessing: bool
    portalProcessing: bool
    embedProcessing: bool
    objectProcessing: bool
    sourceProcessing: bool

    @staticmethod
    def from_dict(data: dict):
        task_id = data.get("taskId")
        html_docs = data.get("htmlDocs")
        noscript_processing = data.get("noscriptProcessing", False)
        link_processing = data.get("linkProcessing", False)
        style_processing = data.get("styleProcessing", False)
        meta_processing = data.get("metaProcessing", False)
        script_processing = data.get("scriptProcessing", False)
        canvas_processing = data.get("canvasProcessing", False)
        svg_processing = data.get("svgProcessing", False)
        area_processing = data.get("areaProcessing", False)
        img_processing = data.get("imgProcessing", False)
        video_processing = data.get("videoProcessing", False)
        audio_processing = data.get("audioProcessing", False)
        iframe_processing= data.get("iframeProcessing", False)
        portal_processing = data.get("portalProcessing", False)
        embed_processing = data.get("embedProcessing", False)
        object_processing = data.get("objectProcessing", False)
        source_processing = data.get("sourceProcessing", False)

        if (task_id is None): 
            raise ValueError("Not specified parameter task_id") 
        if (html_docs is None): 
            raise ValueError("Not specified parameter html_docs")
        
        html_docs_dto_list = [FileInfoDTO.from_dict(doc) for doc in html_docs]

        return HtmlPreprocessingRequestDTO(
            taskId=task_id,
            htmlDocs=html_docs_dto_list,
            noscriptProcessing=noscript_processing,
            linkProcessing=link_processing,
            styleProcessing=style_processing,
            metaProcessing=meta_processing,
            scriptProcessing=script_processing,
            canvasProcessing=canvas_processing,
            svgProcessing=svg_processing,
            areaProcessing=area_processing,
            imgProcessing=img_processing,
            videoProcessing=video_processing,
            audioProcessing=audio_processing,
            iframeProcessing=iframe_processing,
            portalProcessing=portal_processing,
            embedProcessing=embed_processing,
            objectProcessing=object_processing,
            sourceProcessing=source_processing
        )