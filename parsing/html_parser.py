
from dataclasses import dataclass
from enum import Enum
import asyncio
from playwright.async_api import async_playwright
import os
import re
import hashlib
import time
import shutil

# Максимальное количество одновременно обрабатываемых страниц (~ создаваемых контекстов)
MAX_PLAYWRIGHT_CONTEXTS_CONCURRENCY = 3
# Максимальное количество одновременно сохраняемых картинок со всех страниц (защита системы от перегрузки)
MAX_IMG_SAVE_CONCURRENCY_GLOBAL = 20


@dataclass(kw_only=True)
class ParseTimeSettings:
    dom_content_loaded_timeout_ms: int  # таймаут для загрузки обязательных блокирующих скриптов
    network_idle_timeout_ms: int        # таймаут для ождания остановки входящих сетевых запросов
    sleep_before_scroll_s: float        # ожидание перед началом скролла страницы
    page_scroll_timeout_s: int          # таймаут для скролла страницы
    scrol_sleep_time_s: float           # задержка между попытками скролла страницы
    scrol_max_stable_rounds: int        # максимальное количество попыток скролла без изменения высоты страницы перед остановкой


# Доступные сценарии параметров для парсинга страницы в зависимости от её сложности
class PageComplexity(Enum):
    LIGHT = ParseTimeSettings(
        dom_content_loaded_timeout_ms=20_000,
        network_idle_timeout_ms=5_000,
        sleep_before_scroll_s=0.5,
        page_scroll_timeout_s=5,
        scrol_sleep_time_s=0.25,
        scrol_max_stable_rounds=5
    )
    DEFAULT = ParseTimeSettings(
        dom_content_loaded_timeout_ms=30_000,
        network_idle_timeout_ms=5_000,
        sleep_before_scroll_s=1,
        page_scroll_timeout_s=15,
        scrol_sleep_time_s=0.5,
        scrol_max_stable_rounds=10
    )
    DIFFICULT = ParseTimeSettings(
        dom_content_loaded_timeout_ms=60_000,
        network_idle_timeout_ms=10_000,
        sleep_before_scroll_s=2,
        page_scroll_timeout_s=40,
        scrol_sleep_time_s=0.5,
        scrol_max_stable_rounds=20
    )


class HTMLParser:
    def __init__(self):
        self.playwright = None
        self.browser = None
        self.contexts_sem = asyncio.Semaphore(MAX_PLAYWRIGHT_CONTEXTS_CONCURRENCY)
        self.img_save_tasks = asyncio.Queue()
        self.img_save_workers = []

    async def start(self):
        await self.stop()
        # Запускаем браузер playwright
        self.playwright = await async_playwright().start()
        launch_kwargs = {
            "channel": "chrome",
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
    async def download_html_content(self, url, download_images=True,                                          # основные параметры
                                    headers={}, cookies={}, proxy={},                                         # заголовки
                                    html_out_path="out.html", images_out_dir="images",                        # выходные файлы
                                    settings=PageComplexity.DEFAULT.value, additional_page_load_timeout_s=0): # задержки
        if (self.browser is None):
            return {"success": False, "message": "closed browser"} 

        async with self.contexts_sem:
            loop = asyncio.get_running_loop()
            img_task_futures = [] # объекты-результаты выполнения функции по скачиванию изображения
            closing = False

            # Настраиваем заголовки
            context_kwargs = {}
            if (headers): 
                context_kwargs["extra_http_headers"] = headers
            if (cookies): 
                context_kwargs["storage_state"] = {
                    "cookies": cookies
                }
            if (proxy):
                context_kwargs["proxy"] = {
                    "server": f"http://{proxy['ip']}:{proxy['port']}",
                }
                if (proxy.get("username")): context_kwargs["proxy"]["username"] = proxy["username"]
                if (proxy.get("password")): context_kwargs["proxy"]["password"] = proxy["userpasswordname"]
            try:
                context = await self.browser.new_context(**context_kwargs)
            except Exception as e:
                return {"success": False, "message": str(e)}

            page= None
            try:
                # Открываем страницу
                page = await context.new_page()

                # Создаём и отправляем задачу на скачивание изображения воркерам
                async def on_response(resp):
                    if closing:
                        return
                    future = loop.create_future()
                    img_task_futures.append(future)
                    await self.img_save_tasks.put((resp, images_out_dir, future))

                # Подписываемся на все сетевые ответы страницы + создаём дирректорию под изображения
                if (download_images):
                    await asyncio.to_thread(HTMLParser.create_and_clear_dir, images_out_dir)
                    page.on("response", on_response)

                # Переходим по url и ожидаем подгрузки контента
                await page.goto(url, wait_until="domcontentloaded", timeout=settings.dom_content_loaded_timeout_ms)
                await asyncio.sleep(settings.sleep_before_scroll_s)
                await HTMLParser.auto_scroll(page, settings)
                try:
                    await page.wait_for_load_state("networkidle", timeout=settings.network_idle_timeout_ms)
                except: pass
                await asyncio.sleep(additional_page_load_timeout_s) # дополнительное пользовательское ожидание

                # Сохраняем html код страницы
                html = await page.content()
                await asyncio.to_thread(HTMLParser.write_file, html_out_path, html)
            except Exception as e:
                await context.close()
                return {"success": False, "message": str(e)}
            finally:
                # Останавливаем поиск изображений
                closing = True
                if page and download_images:
                    try: page.remove_listener("response", on_response)
                    except: pass
                
                # Дожидаемся завершения задач
                await asyncio.gather(*img_task_futures, return_exceptions=True)

                await context.close()
            return {"success": True, "message": "OK"} 


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
                body = await response.body()
                img_ext = HTMLParser.get_img_extension(content_type, url)
                img_name = HTMLParser.get_img_name(url)
                img_path = os.path.join(img_dir, img_name + img_ext)
                await asyncio.to_thread(HTMLParser.write_file, img_path, body)
                future.set_result(img_path)
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

    # Формируем имя картинки, как хэш от его url
    @staticmethod
    def get_img_name(url):
        return hashlib.sha1(url.encode("utf-8")).hexdigest()[:16]
    
    # Сохранить в файл
    @staticmethod
    def write_file(path, data):
        if isinstance(data, (bytes, bytearray, memoryview)):
            with open(path, "wb") as f:
                f.write(data)
        else:
            with open(path, "w", encoding="utf-8") as f:
                f.write(data)
    
    # Очистка и создание новой дирректирии
    @staticmethod
    def create_and_clear_dir(dir):
        shutil.rmtree(dir, ignore_errors=True)
        os.makedirs(dir, exist_ok=True)


async def main():
    url = "https://impulse.t1.ru/"
    # url = "https://вэбцентр.рф/playground/tpost/ik7pp010g1-festival-finansovoi-gramotnosti-i-predpr"
    user_agent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36"
    
    html_parser = HTMLParser()
    await html_parser.start()

    try:
        r = await html_parser.download_html_content(
            url=url, 
            headers={"User-Agent": user_agent}, 
            additional_page_load_timeout_s=3,
            settings=PageComplexity.DEFAULT.value
        )
        print(r["success"], r["message"])
    finally:
        await html_parser.stop()


if __name__ == "__main__":
    asyncio.run(main())