from dataclasses import dataclass
from parser.src.enum.page_complexity import PageComplexity
import json

@dataclass
class HtmlParserRequestDTO:
    taskId: str
    url: str
    htmlOutDir: str
    imagesOutDir: str
    downloadImages: bool
    headers: dict[str]
    cookies: dict[str]
    proxy: dict[str]
    pageComplexity: PageComplexity
    additionalPageLoadTimeoutS: str

    @staticmethod
    def from_dict(data: dict):
        task_id = data.get("taskId")
        url = data.get("url")
        html_out_dir = data.get("htmlOutDir")
        images_out_dir = data.get("imagesOutDir")
        download_images = data.get("downloadImages", False)
        headers = data.get("headers", {})
        cookies = data.get("cookies", {})
        proxy = data.get("proxy", {})
        page_complexity = data.get("pageComplexity", "DEFAULT")
        additional_page_load_timeout_s = data.get("additionalPageLoadTimeoutS", 0)
        
        if (task_id is None):
            raise ValueError("Not specified parameter task_id")
        if (url is None): 
            raise ValueError("Not specified parameter url")
        if (html_out_dir is None): 
            raise ValueError("Not specified parameter html_out_dir")
        if (images_out_dir is None): 
            raise ValueError("Not specified parameter images_out_dir")
        
        if (page_complexity == "LIGHT"): page_complexity_enum = PageComplexity.LIGHT.value
        elif (page_complexity == "DEFAULT"): page_complexity_enum = PageComplexity.DEFAULT.value
        elif (page_complexity == "DIFFICULT"): page_complexity_enum = PageComplexity.DIFFICULT.value
        else:
            raise ValueError("Unknown page complexity type")

        return HtmlParserRequestDTO(
            taskId=task_id,
            url=url,
            htmlOutDir=html_out_dir,
            imagesOutDir=images_out_dir,
            downloadImages=download_images,
            headers=headers,
            cookies=cookies,
            proxy=proxy,
            pageComplexity=page_complexity_enum,
            additionalPageLoadTimeoutS=additional_page_load_timeout_s
        )