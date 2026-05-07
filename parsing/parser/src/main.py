from faststream.rabbit import RabbitBroker, RabbitRouter
from faststream import FastStream
import asyncio
from parser.src.html_parser import HTMLParser, PageComplexity
from parser.src.html_preprocessing import HTMLPreprocessing
import json
import os

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
    url = msg.get("url")
    html_out_dir = msg.get("htmlOutDir")
    images_out_dir = msg.get("imagesOutDir")
    download_images = msg.get("downloadImages", False)
    headers = msg.get("headers", {})
    cookies = msg.get("cookies", {})
    proxy = msg.get("proxy", {})
    page_complexity = msg.get("pageComplexity", "DEFAULT")
    additional_page_load_timeout_s = msg.get("additionalPageLoadTimeoutS", 0)
    
    if (not task_id):
        await send_html_parsing_response({"taskId": task_id, "success": False, "message": "Not specified parameter 'task_id' for parsing"})
        print({"taskId": task_id, "success": False, "message": "Not specified parameter 'task_id' for parsing"})
        return
    if (not url): 
        await send_html_parsing_response({"taskId": task_id, "success": False, "message": "Not specified parameter 'url' for parsing"})
        print({"taskId": task_id, "success": False, "message": "Not specified parameter 'url' for parsing"})
        return
    if (not html_out_dir): 
        await send_html_parsing_response({"taskId": task_id, "success": False, "message": "Not specified parameter 'html_out_dir' for parsing"})
        print({"taskId": task_id, "success": False, "message": "Not specified parameter 'html_out_dir' for parsing"})
        return
    if (not images_out_dir): 
        await send_html_parsing_response({"taskId": task_id, "success": False, "message": "Not specified parameter 'images_out_dir' for parsing"})
        print({"taskId": task_id, "success": False, "message": "Not specified parameter 'images_out_dir' for parsing"})
        return
    
    if (page_complexity == "LIGHT"): page_complexity_enum = PageComplexity.LIGHT.value
    elif (page_complexity == "DEFAULT"): page_complexity_enum = PageComplexity.DEFAULT.value
    elif (page_complexity == "DIFFICULT"): page_complexity_enum = PageComplexity.DIFFICULT.value
    else:
        await send_html_parsing_response({"taskId": task_id, "success": False, "message": "Unknown page complexity type"})
        print({"taskId": task_id, "success": False, "message": "Unknown page complexity type"})
        return
    
    try:
        r = await html_parser.download_html_content(
            url=url,
            html_out_dir=html_out_dir,
            images_out_dir=images_out_dir,
            download_images=download_images,
            headers=headers,
            cookies=cookies,
            proxy=proxy,
            settings=page_complexity_enum,
            additional_page_load_timeout_s=additional_page_load_timeout_s
        )
        await send_html_parsing_response({"taskId": task_id, "success": True, "message": "OK", "htmlPath": r["htmlPath"], "imagePaths": r["imagePaths"]})
        print({"taskId": task_id, "success": True, "message": "OK", "htmlPath": r["htmlPath"], "imagePaths": r["imagePaths"]})
    except Exception as e:
        await send_html_parsing_response({"taskId": task_id, "success": False, "message": str(e)})
        print({"taskId": task_id, "success": False, "message": str(e)})



@broker.subscriber(QUEUE_HTML_PREPROCESSING_REQUEST)
async def handle_html_preprocessing(msg: str):
    msg = json.loads(msg)

    task_id = msg.get("taskId")
    html_paths = msg.get("htmlPaths")
    noscript_processing = msg.get("noscriptProcessing", False)
    link_processing = msg.get("linkProcessing", False)
    style_processing = msg.get("styleProcessing", False)
    meta_processing = msg.get("metaProcessing", False)
    script_processing = msg.get("scriptProcessing", False)
    canvas_processing = msg.get("canvasProcessing", False)
    svg_processing = msg.get("svgProcessing", False)
    area_processing = msg.get("areaProcessing", False)
    img_processing = msg.get("imgProcessing", False)
    video_processing = msg.get("videoProcessing", False)
    audio_processing = msg.get("audioProcessing", False)
    iframe_processing= msg.get("iframeProcessing", False)
    portal_processing = msg.get("portalProcessing", False)
    embed_processing = msg.get("embedProcessing", False)
    object_processing = msg.get("objectProcessing", False)
    source_processing = msg.get("sourceProcessing", False)

    if (not task_id): 
        await send_html_preprocessing_response({"taskId": task_id, "success": False, "message": "Not specified parameter 'task_id' for preprocessing"})
        print({"taskId": task_id, "success": False, "message": "Not specified parameter 'task_id' for preprocessing"})
        return
    if (not html_paths): 
        await send_html_preprocessing_response({"taskId": task_id, "success": False, "message": "Not specified parameter 'html_paths' for preprocessing"})
        print({"taskId": task_id, "success": False, "message": "Not specified parameter 'html_paths' for preprocessing"})
        return

    try:
        r = await html_preprocessing.apply_preprocessing(
            html_paths=html_paths,
            noscript_processing=noscript_processing,
            link_processing=link_processing,
            style_processing=style_processing,
            meta_processing=meta_processing,
            script_processing=script_processing,
            canvas_processing=canvas_processing,
            svg_processing=svg_processing,
            area_processing=area_processing,
            img_processing=img_processing,
            video_processing=video_processing,
            audio_processing=audio_processing,
            iframe_processing=iframe_processing,
            portal_processing=portal_processing,
            embed_processing=embed_processing,
            object_processing=object_processing,
            source_processing=source_processing
        )
        await send_html_preprocessing_response({"taskId": task_id, "success": True, "message": "OK", "htmlPaths": r["htmlPaths"]})
        print({"taskId": task_id, "success": True, "message": "OK", "htmlPaths": r["htmlPaths"]})
    except Exception as e:
        await send_html_preprocessing_response({"taskId": task_id, "success": False, "message": str(e)})
        print({"taskId": task_id, "success": False, "message": str(e)})



async def send_html_parsing_response(response):
    await broker.publish(response, queue=QUEUE_HTML_PARSING_RESPONSE)

async def send_html_preprocessing_response(response):
    await broker.publish(response, queue=QUEUE_HTML_PREPROCESSING_RESPONSE)


async def main():
    await html_parser.start()
    async with broker:
        await app.run()
    await html_parser.stop()

if __name__ == "__main__":
    asyncio.run(main())