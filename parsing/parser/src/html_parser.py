import asyncio
from playwright.async_api import async_playwright
import os
import re
import hashlib
import time
import shutil
from common.src.s3_storage_connection import S3Storage
import os
from parser.src.enum.page_complexity import PageComplexity
from common.src.dto.file_info_dto import FileInfoDTO
from common.src.enum.file_type import FileType
from pathlib import Path

# from dotenv import load_dotenv
# load_dotenv("parser/parser_settings.env") # загружаем .env файл конфигурации

# Максимальное количество одновременно обрабатываемых страниц (~ создаваемых контекстов)
MAX_PLAYWRIGHT_CONTEXTS_CONCURRENCY = int(os.getenv("MAX_PLAYWRIGHT_CONTEXTS_CONCURRENCY"))
# Максимальное количество одновременно сохраняемых картинок со всех страниц (защита системы от перегрузки)
MAX_IMG_SAVE_CONCURRENCY_GLOBAL = int(os.getenv("MAX_IMG_SAVE_CONCURRENCY_GLOBAL"))


class HTMLParser:
    def __init__(self):
        self.playwright = None
        self.browser = None
        self.contexts_sem = asyncio.Semaphore(MAX_PLAYWRIGHT_CONTEXTS_CONCURRENCY)
        self.img_save_tasks = asyncio.Queue()
        self.img_save_workers = []
        self.s3_storage = S3Storage()

    async def start(self):
        await self.stop()
        # Запускаем браузер playwright
        self.playwright = await async_playwright().start()
        launch_kwargs = {
            "headless": True,
            "args": ["--disable-blink-features=AutomationControlled"],
        }
        self.browser = await self.playwright.chromium.launch(**launch_kwargs)

        # Запускаем воркеров для обработки ответов
        self.img_save_workers = [
            asyncio.create_task(self.save_image_from_page_response_worker())
            for i in range(MAX_IMG_SAVE_CONCURRENCY_GLOBAL)
        ]
        
    async def stop(self):
        # Закрываем браузер playwright
        if (self.browser is not None):
            await self.browser.close()
            self.browser = None
        if (self.playwright is not None):
            await self.playwright.stop()
            self.playwright = None

        # Останавливаем воркеров
        if (len(self.img_save_workers) > 0):
            for i in range(MAX_IMG_SAVE_CONCURRENCY_GLOBAL):
                await self.img_save_tasks.put(None) # флаг остановки
            await asyncio.gather(*self.img_save_workers, return_exceptions=True)
            self.img_save_tasks = asyncio.Queue()
            self.img_save_workers = []


    # Скачивание html страницы и контента внутри
    async def download_html_content(
        self,
        url,
        html_out_dir,
        images_out_dir,
        download_images=False,
        headers={},     # словарь {header_name: header_value}
        cookies={},     # словарь {cookie_name: cookie_value}
        proxy={},       # словарь http прокси с ключами {"ip": ..., "port": ..., "username": <optional>, "password": <optional>}
        settings=PageComplexity.DEFAULT.value,
        additional_page_load_timeout_s=0
    ):
        if (self.browser is None):
            raise Exception(f"closed browser")

        async with self.contexts_sem:
            loop = asyncio.get_running_loop()
            img_task_futures = [] # объекты-результаты выполнения функции по скачиванию изображения
            closing = False

            # Настраиваем заголовки
            context_kwargs = {}
            cookies_list = []
            if (headers): 
                context_kwargs["extra_http_headers"] = headers
            if (cookies): 
                cookies_list = [{
                    "name": key,
                    "value": value,
                    "url": url
                } for key, value in cookies.items()]
            if (proxy):
                context_kwargs["proxy"] = {
                    "server": f"http://{proxy['ip']}:{proxy['port']}",
                }
                if (proxy.get("username")): context_kwargs["proxy"]["username"] = proxy["username"]
                if (proxy.get("password")): context_kwargs["proxy"]["password"] = proxy["password"]
            try:
                context = await self.browser.new_context(**context_kwargs)
                await context.add_cookies(cookies_list)
            except Exception as e:
                raise e

            page= None
            try:
                # Создаём и очищаем дирректории для сохранения изобрпажений и html страницы
                # await asyncio.to_thread(HTMLParser.create_and_clear_dir, html_out_dir)
                # await asyncio.to_thread(HTMLParser.create_and_clear_dir, images_out_dir)
                # await self.delete_s3_objects_by_prefix(html_out_dir)
                # await self.delete_s3_objects_by_prefix(images_out_dir)

                # Открываем страницу
                page = await context.new_page()

                # Создаём и отправляем задачу на скачивание изображения воркерам
                async def on_response(resp):
                    if closing:
                        return
                    future = loop.create_future()
                    img_task_futures.append(future)
                    await self.img_save_tasks.put((resp, images_out_dir, future))

                # Подписываемся на все сетевые ответы страницы
                if (download_images):
                    page.on("response", on_response)

                # Переходим по url и ожидаем подгрузки контента
                print("page goto")
                await page.goto(url, wait_until="domcontentloaded", timeout=settings.dom_content_loaded_timeout_ms)
                print("page goto end")
                await asyncio.sleep(settings.sleep_before_scroll_s)
                print("start scroll")
                await HTMLParser.auto_scroll(page, settings)
                print("stop scroll")
                try:
                    await page.wait_for_load_state("networkidle", timeout=settings.network_idle_timeout_ms)
                except: pass
                await asyncio.sleep(additional_page_load_timeout_s) # дополнительное пользовательское ожидание
                print("end timeout")

                # Сохраняем html код страницы
                html = await page.content()
                html_bytes = html.encode("utf-8")
                html_name = HTMLParser.get_hash(url) + ".html"
                html_path = os.path.join(html_out_dir, html_name)
                # await asyncio.to_thread(HTMLParser.write_file_bytes, html_path, html_bytes)
                print("save start")
                await self.save_file_to_s3(html_path, html_bytes)
                print("save stop")
            except Exception as e:
                raise e
            finally:
                # Останавливаем поиск изображений
                closing = True
                if page and download_images:
                    try: page.remove_listener("response", on_response)
                    except: pass
                
                # Дожидаемся завершения задач
                print("gather start:")
                future_task_results = await asyncio.gather(*img_task_futures, return_exceptions=True)
                print("gather END")
                image_info = [x for x in future_task_results if isinstance(x, dict)]
                await context.close()
            return {
                "htmlDocs": [FileInfoDTO(filePath=html_path, fileName=html_name, fileType=FileType.HTML, sizeBytes=len(html_bytes), description="", valid=True, errorMessage="")],
                "images": [
                    FileInfoDTO(filePath=info["path"], fileName=info["name"], fileType=FileType.IMG, sizeBytes=info["size"], description="", valid=True, errorMessage="")
                    for info in image_info
                ]
            }


    # Получаем сетевой ответ веб-страницы и скачиваем его, если это изображение
    async def save_image_from_page_response_worker(self):
        while True:
            task = await self.img_save_tasks.get()
            if (task is None): # флаг остановки
                return
            response, img_dir, future = task

            try:
                # Проверяем, что ответ является изображением
                content_type = (response.headers.get("content-type") or "").lower()
                resource_type = response.request.resource_type.lower()
                # print(f"content_type: {content_type}; resource_type: {resource_type}")
                if (resource_type != "image") and (not content_type.startswith("image/")):
                    future.cancel()
                    continue
                
                # Сохраняем изображение на диск
                url = response.url
                body = await asyncio.wait_for(response.body(), timeout=10)
                img_ext = HTMLParser.get_img_extension(content_type, url)
                img_name = HTMLParser.get_hash(url) + img_ext
                img_path = os.path.join(img_dir, img_name)
                # await asyncio.to_thread(HTMLParser.write_file_bytes, img_path, body)
                await self.save_file_to_s3(img_path, body)
                future.set_result({"path": img_path, "name": img_name, "size": len(body)})
            except Exception as e:
                future.set_exception(e)
                # print(e)


    # Скрол страницы, пока не убедимся, что дошли до конца (или пока не случится timeout)
    @staticmethod
    async def auto_scroll(page, settings=PageComplexity.DEFAULT.value):
        start_time = time.monotonic()
        step_px = await page.evaluate("() => Math.floor(window.innerHeight * 0.9)")
        prev_height = -1
        stable = 0

        while (time.monotonic() - start_time < settings.page_scroll_timeout_s):
            await page.mouse.wheel(0, step_px)
            await asyncio.sleep(settings.scrol_sleep_time_s)

            height = await page.evaluate("() => document.documentElement.scrollHeight")
            if (height == prev_height):
                stable += 1
            else:
                stable = 0
            prev_height = height

            if (stable >= settings.scrol_max_stable_rounds):
                break

    # Определить расширение файла картинки (по content_type или по url)
    @staticmethod
    def get_img_extension(content_type, url):
        ct = (content_type or "").split(";")[0].strip().lower()
        mapping = {
            "image/jpeg": ".jpg",
            "image/png": ".png",
            "image/webp": ".webp",
            "image/gif": ".gif",
            "image/svg+xml": ".svg",
            "image/avif": ".avif",
            "image/bmp": ".bmp",
            "image/x-icon": ".ico",
            "image/vnd.microsoft.icon": ".ico",
        }
        if ct in mapping:
            return mapping[ct]
        
        m = re.search(r"\.(jpg|jpeg|png|webp|gif|svg|avif|bmp|ico)(?:\?|#|$)", url, re.I)
        return f".{m.group(1).lower()}" if m else ".bin"

    # Формируем hash из строки
    @staticmethod
    def get_hash(s):
        return hashlib.sha1(s.encode("utf-8")).hexdigest()[:16]
    
    # Сохранить в файл
    @staticmethod
    def write_file_bytes(path, data):
        with open(path, "wb") as f:
            f.write(data)
    
    # Очистка и создание новой дирректирии
    @staticmethod
    def create_and_clear_dir(dir):
        shutil.rmtree(dir, ignore_errors=True)
        os.makedirs(dir, exist_ok=True)

    # Сохранить файл в s3 хранилище
    async def save_file_to_s3(self, path, data):
        await self.s3_storage.upload_file_bytes(s3_object_key=path, file_bytes=data)

    # Удалить все файлы в s3 хранилище, которые начинаются с префикса prefix
    async def delete_s3_objects_by_prefix(self, prefix):
        obj_keys = await self.s3_storage.get_object_keys_by_prefix(prefix)
        for key in obj_keys:
            await self.s3_storage.delete_file(key)
