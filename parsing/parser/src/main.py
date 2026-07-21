from faststream.rabbit import RabbitBroker, RabbitRouter
from faststream import FastStream
import asyncio
from parser.src.html_parser import HTMLParser, PageComplexity
from parser.src.html_preprocessing import HTMLPreprocessing
import json
from dataclasses import asdict
import os
from parser.src.dto.html_parser_request_dto import HtmlParserRequestDTO
from parser.src.dto.html_preprocessing_request_dto import HtmlPreprocessingRequestDTO
from parser.src.dto.html_parser_response_dto import HtmlParserResponseDTO
from parser.src.dto.html_preprocessing_response_dto import HtmlPreprocessingResponseDTO

# from dotenv import load_dotenv
# load_dotenv("parser/parser_settings.env") # загружаем .env файл конфигурации

QUEUE_HTML_PARSING_REQUEST = os.getenv("QUEUE_HTML_PARSING_REQUEST")
QUEUE_HTML_PARSING_RESPONSE = os.getenv("QUEUE_HTML_PARSING_RESPONSE")
QUEUE_HTML_PREPROCESSING_REQUEST = os.getenv("QUEUE_HTML_PREPROCESSING_REQUEST")
QUEUE_HTML_PREPROCESSING_RESPONSE = os.getenv("QUEUE_HTML_PREPROCESSING_RESPONSE")

RABBITMQ_USERNAME = os.getenv("RABBITMQ_USERNAME")
RABBITMQ_PASSWORD = os.getenv("RABBITMQ_PASSWORD")
RABBITMQ_IP = os.getenv("RABBITMQ_IP")
RABBITMQ_PORT = os.getenv("RABBITMQ_PORT")


broker = RabbitBroker(
    f"amqp://{RABBITMQ_USERNAME}:{RABBITMQ_PASSWORD}@{RABBITMQ_IP}:{RABBITMQ_PORT}/",
    timeout=5.0,
    fail_fast=True,
    reconnect_interval=5.0,
)
app = FastStream(broker)


html_parser = HTMLParser()
html_preprocessing = HTMLPreprocessing()



@broker.subscriber(QUEUE_HTML_PARSING_REQUEST)
async def handle_html_parsing(msg: str):
    msg = json.loads(msg)
    task_id = msg.get("taskId")
    try:
        request = HtmlParserRequestDTO.from_dict(msg)
        
        r = await html_parser.download_html_content(
            url=request.url,
            html_out_dir=request.htmlOutDir,
            images_out_dir=request.imagesOutDir,
            download_images=request.downloadImages,
            headers=request.headers,
            cookies=request.cookies,
            proxy=request.proxy,
            settings=request.pageComplexity,
            additional_page_load_timeout_s=request.additionalPageLoadTimeoutS
        )
        response = HtmlParserResponseDTO(taskId=task_id, success=True, message="", htmlDocs=r["htmlDocs"], images=r["images"])
        await send_html_parsing_response(response)
    except Exception as e:
        response = HtmlParserResponseDTO(taskId=task_id, success=False, message=f"[html_parser service] {str(e)}", htmlDocs=[], images=[])
        await send_html_parsing_response(response)



@broker.subscriber(QUEUE_HTML_PREPROCESSING_REQUEST)
async def handle_html_preprocessing(msg: str):
    msg = json.loads(msg)
    task_id = msg.get("taskId")
    try:
        request = HtmlPreprocessingRequestDTO.from_dict(msg)

        r = await html_preprocessing.apply_preprocessing(
            html_docs=request.htmlDocs,
            noscript_processing=request.noscriptProcessing,
            link_processing=request.linkProcessing,
            style_processing=request.styleProcessing,
            meta_processing=request.metaProcessing,
            script_processing=request.scriptProcessing,
            canvas_processing=request.canvasProcessing,
            svg_processing=request.svgProcessing,
            area_processing=request.areaProcessing,
            img_processing=request.imgProcessing,
            video_processing=request.videoProcessing,
            audio_processing=request.audioProcessing,
            iframe_processing=request.iframeProcessing,
            portal_processing=request.portalProcessing,
            embed_processing=request.embedProcessing,
            object_processing=request.objectProcessing,
            source_processing=request.sourceProcessing
        )
        response = HtmlPreprocessingResponseDTO(taskId=task_id, success=True, message="", htmlDocs=r["htmlDocs"])
        await send_html_preprocessing_response(response)
    except Exception as e:
        response = HtmlPreprocessingResponseDTO(taskId=task_id, success=False, message=f"[html_preprocessing service] {str(e)}", htmlDocs=[])
        await send_html_preprocessing_response(response)



async def send_html_parsing_response(response: HtmlParserResponseDTO):
    json_string = json.dumps(asdict(response), ensure_ascii=False)
    await broker.publish(json_string, queue=QUEUE_HTML_PARSING_RESPONSE)

async def send_html_preprocessing_response(response: HtmlPreprocessingResponseDTO):
    json_string = json.dumps(asdict(response), ensure_ascii=False)
    await broker.publish(json_string, queue=QUEUE_HTML_PREPROCESSING_RESPONSE)


async def main():
    await html_parser.start()
    async with broker:
        await app.run()
    await html_parser.stop()

if __name__ == "__main__":
    asyncio.run(main())