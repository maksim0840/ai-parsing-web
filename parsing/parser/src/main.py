from faststream.rabbit import RabbitBroker
from faststream import FastStream
import asyncio
from parser.src.html_parser import HTMLParser, PageComplexity
from parser.src.html_preprocessing import HTMLPreprocessing
import json
from dotenv import load_dotenv
import os

# загружаем .env файл конфигурации
load_dotenv("parser/parser_settings.env")

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
async def habdle_html_parsing(msg: dict):
    print(msg)
    url = msg.get("url")
    html_out_dir = msg.get("html_out_dir")
    images_out_dir = msg.get("images_out_dir")
    download_images = msg.get("download_images", False)
    headers = msg.get("headers", "{}")
    cookies = msg.get("cookies", "{}")
    proxy = msg.get("proxy", "{}")
    page_complexity = msg.get("page_complexity", "DEFAULT")
    additional_page_load_timeout_s = msg.get("additional_page_load_timeout_s", 0)
    
    if (not url): return
    if (not html_out_dir): return
    if (not images_out_dir): return

    try:
        headers_dict = json.loads(headers)
    except Exception as e:
        return
    try:
        cookies_dict = json.loads(cookies)
    except:
        return
    try:
        proxy_dict = json.loads(proxy)
    except:
        return
    
    if (page_complexity == "LIGHT"): page_complexity_enum = PageComplexity.LIGHT.value
    elif (page_complexity == "DEFAULT"): page_complexity_enum = PageComplexity.DEFAULT.value
    elif (page_complexity == "DIFFICULT"): page_complexity_enum = PageComplexity.DIFFICULT.value
    else:
        return
    
    print("start parsing")
    r = await html_parser.download_html_content(
        url=url,
        html_out_dir=html_out_dir,
        images_out_dir=images_out_dir,
        download_images=download_images,
        headers=headers_dict,
        cookies=cookies_dict,
        proxy=proxy_dict,
        settings=page_complexity_enum,
        additional_page_load_timeout_s=additional_page_load_timeout_s
    )
    print(r)



@broker.subscriber(QUEUE_HTML_PREPROCESSING_REQUEST)
async def habdle_html_preprocessing(msg: dict):
    html_path = msg.get("html_path")
    noscript_processing = msg.get("noscript_processing", False)
    link_processing = msg.get("link_processing", False)
    style_processing = msg.get("style_processing", False)
    meta_processing = msg.get("meta_processing", False)
    script_processing = msg.get("script_processing", False)
    canvas_processing = msg.get("canvas_processing", False)
    svg_processing = msg.get("svg_processing", False)
    area_processing = msg.get("area_processing", False)
    img_processing = msg.get("img_processing", False)
    video_processing = msg.get("video_processing", False)
    audio_processing = msg.get("audio_processing", False)
    iframe_processing= msg.get("iframe_processing", False)
    portal_processing = msg.get("portal_processing", False)
    embed_processing = msg.get("embed_processing", False)
    object_processing = msg.get("object_processing", False)
    source_processing = msg.get("source_processing", False)

    if (not html_path): return

    print("start preprocessing")
    r = await html_preprocessing.apply_preprocessing(
        html_path=html_path,
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
    print(r)



async def main():
    await html_parser.start()
    async with broker:
        await app.run()
    await html_parser.stop()

if __name__ == "__main__":
    asyncio.run(main())