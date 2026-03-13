from faststream.rabbit import RabbitBroker
from faststream import FastStream
import asyncio
from text_recognition import TextRecognition

QUEUE_TEXT_RECOGNITION = "text_recognition"

RABBITMQ_USER = "admin"
RABBITMQ_PASS = "admin123"
RABBITMQ_IP = "localhost"
RABBITMQ_PORT = "5672"


broker = RabbitBroker(
    f"amqp://{RABBITMQ_USER}:{RABBITMQ_PASS}@{RABBITMQ_IP}:{RABBITMQ_PORT}/",
    timeout=5.0,
    fail_fast=True,
    reconnect_interval=5.0,
)
app = FastStream(broker)


text_recognition_model = TextRecognition()
ocr_semaphore = asyncio.Semaphore(1) # один запрос за раз на один instance модели

@broker.subscriber(QUEUE_TEXT_RECOGNITION)
async def habdle_text_recognition(data: str):
    async with ocr_semaphore:
        text = await asyncio.to_thread(text_recognition_model.run_ocr, "../images/f7ad7fb29d6d2a34.jpg")
        print("data:", data)
        print("text:", text)


async def main():
    async with broker:
        await app.run()

if __name__ == "__main__":
    asyncio.run(main())