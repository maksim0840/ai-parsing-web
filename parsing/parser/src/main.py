from faststream.rabbit import RabbitBroker
from faststream import FastStream
import asyncio
from html_parser import HTMLParser, PageComplexity
from html_preprocessing import HTMLPreprocessing

QUEUE_HTML_PARSING = "html_parsing"
QUEUE_HTML_PREPROCESSING = "html_preprocessing"

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


html_parser = HTMLParser()
html_preprocessing = HTMLPreprocessing()


@broker.subscriber(QUEUE_HTML_PARSING)
async def habdle_text_recognition(data: str):
    url = "https://impulse.t1.ru/"
    # url = "https://вэбцентр.рф/playground/tpost/ik7pp010g1-festival-finansovoi-gramotnosti-i-predpr"
    user_agent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36"
    await html_parser.download_html_content()
    r = await html_parser.download_html_content(
            url=url, 
            headers={"User-Agent": user_agent}, 
            additional_page_load_timeout_s=3,
            settings=PageComplexity.DEFAULT.value
        )
    print(r["success"], r["message"])

@broker.subscriber(QUEUE_HTML_PREPROCESSING)
async def habdle_text_recognition(data: str):
    await html_preprocessing.apply_preprocessing()


async def main():
    await html_parser.start()
    async with broker:
        await app.run()

if __name__ == "__main__":
    asyncio.run(main())