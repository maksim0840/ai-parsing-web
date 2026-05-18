from faststream.rabbit import RabbitBroker
from faststream import FastStream
import asyncio
from text_recognition.src.text_recognition import TextRecognition
import os
import json
from dataclasses import asdict
from text_recognition.src.dto.text_recognition_request_dto import TextRecognitionRequestDTO
from text_recognition.src.dto.text_recognition_response_dto import TextRecognitionResponseDTO

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
    msg = json.loads(msg)
    task_id = msg.get("taskId")
    try:
        request = TextRecognitionRequestDTO.from_dict(msg)
        print("request:", request)

        r = await text_recognition_model.run_ocr(
            images=request.images
        )
        response = TextRecognitionResponseDTO(taskId=task_id, success=True, message="", images=r["images"])
        await send_text_recognition_response(response)
        print("response:", response)
    except Exception as e:
        response = TextRecognitionResponseDTO(taskId=task_id, success=False, message=f"[text_recognition service] {str(e)}", images=[])
        await send_text_recognition_response(response)
        print("response:", response)


async def send_text_recognition_response(response: TextRecognitionResponseDTO):
    json_string = json.dumps(asdict(response), ensure_ascii=False)
    await broker.publish(json_string, queue=QUEUE_TEXT_RECOGNITION_RESPONSE)



async def main():
    async with broker:
        await app.run()

if __name__ == "__main__":
    asyncio.run(main())