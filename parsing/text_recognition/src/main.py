from faststream.rabbit import RabbitBroker
from faststream import FastStream
import asyncio
from text_recognition.src.text_recognition import TextRecognition
from dotenv import load_dotenv
import os

load_dotenv("text_recognition/recognition_settings.env") # загружаем .env файл конфигурации

QUEUE_TEXT_RECOGNITION_REQUEST = os.getenv("QUEUE_TEXT_RECOGNITION_REQUEST")
QUEUE_TEXT_RECOGNITION_RESPONSE = os.getenv("QUEUE_TEXT_RECOGNITION_RESPONSE")

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


text_recognition_model = TextRecognition()


@broker.subscriber(QUEUE_TEXT_RECOGNITION_REQUEST)
async def habdle_text_recognition(msg: dict):
    images_dir = msg.get("images_dir")
    
    if (not images_dir): 
        await send_text_recognition_response({"success": False, "message": "Not specified parameter 'images_dir' for text recognition", "response": {}})
        return
    
    r = await text_recognition_model.run_ocr(images_dir=images_dir)
    await send_text_recognition_response(r) 


async def send_text_recognition_response(response):
    await broker.publish(response, queue=QUEUE_TEXT_RECOGNITION_RESPONSE)


async def main():
    async with broker:
        await app.run()

if __name__ == "__main__":
    asyncio.run(main())