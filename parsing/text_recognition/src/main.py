from faststream.rabbit import RabbitBroker
from faststream import FastStream
import asyncio
from text_recognition.src.text_recognition import TextRecognition
import os
import json

# from dotenv import load_dotenv
# load_dotenv("text_recognition/recognition_settings.env") # загружаем .env файл конфигурации

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
async def handle_text_recognition(msg: str):
    print("request:", msg)
    msg = json.loads(msg)

    task_id = msg.get("taskId")
    image_paths = msg.get("imagePaths")
    
    if (task_id is None): 
        await send_text_recognition_response({"taskId": task_id, "success": False, "message": "Not specified parameter task_id for text recognition"})
        print({"taskId": task_id, "success": False, "message": "Not specified parameter task_id for text recognition"})
        return
    if (image_paths is None): 
        await send_text_recognition_response({"taskId": task_id, "success": False, "message": "Not specified parameter image_paths for text recognition"})
        print({"taskId": task_id, "success": False, "message": "Not specified parameter image_paths for text recognition"})
        return
    try:
        r = await text_recognition_model.run_ocr(image_paths=image_paths)
        await send_text_recognition_response({"taskId": task_id, "success": True, "message": "OK", "textByImage": r["textByImage"]})
        print({"taskId": task_id, "success": True, "message": "OK", "textByImage": r["textByImage"]})
    except Exception as e:
        await send_text_recognition_response({"taskId": task_id, "success": False, "message": str(e)})
        print({"taskId": task_id, "success": False, "message": str(e)})

async def send_text_recognition_response(response):
    await broker.publish(response, queue=QUEUE_TEXT_RECOGNITION_RESPONSE)


async def main():
    async with broker:
        await app.run()

if __name__ == "__main__":
    asyncio.run(main())