
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
MAX_PLAYWRIGHT_CONTEXTS_CONCURRENCY = 5
# Максимальное количество одновременно сохраняемых картинок со всех страниц (защита системы от перегрузки)
MAX_IMG_SAVE_CONCURRENCY_GLOBAL = 20
# Максимальное количество одновременно сохраняемых картинок с одной страницы (чтобы одна страница не забила весь лимит)
MAX_IMG_SAVE_CONCURRENCY_PAGE = 8


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
        self.global_img_sem = asyncio.Semaphore(MAX_IMG_SAVE_CONCURRENCY_GLOBAL)

    async def start(self):
        self.playwright = await async_playwright().start()
        launch_kwargs = {
            "channel": "chrome",
            "headless": True,
            "args": ["--disable-blink-features=AutomationControlled"],
        }
        self.browser = await self.playwright.chromium.launch(**launch_kwargs)
        
    async def stop(self):
        await self.browser.close()
        self.browser = None
        await self.playwright.stop()
        self.playwright = None


    # Скачивание html страницы и контента внутри
    async def download_html_content(self, url, download_images=True,
                                    html_out_path="out.html", images_out_dir="images",
                                    proxy=None, user_agent=None, additional_page_load_timeout_s=0,
                                    settings=PageComplexity.DEFAULT.value):
        if (self.browser is None):
            return {"success": False, "message": "closed browser"} 

        async with self.contexts_sem:
            page_img_sem = asyncio.Semaphore(MAX_IMG_SAVE_CONCURRENCY_PAGE)
            img_tasks = []
            closing = False
            img_urls = set()

            # Настраиваем заголовки
            context_kwargs = {
                "locale": "ru-RU",
                "timezone_id": "Europe/Moscow",
                "viewport": {"width": 1366, "height": 768},
            }
            if (user_agent): 
                context_kwargs["user_agent"] = user_agent
            if (proxy): 
                context_kwargs["proxy"] = {
                    "server": f"http://{proxy['ip']}:{proxy['port']}",
                    "username": proxy["username"],
                    "password": proxy["password"]
                }
            context = await self.browser.new_context(**context_kwargs)

            page= None
            try:
                # Открываем страницу
                page = await context.new_page()

                # Подписываемся на все сетевые ответы страницы для скачивания изображений + создаём дирректорию под изображения
                def on_response(resp):
                    if closing:
                        return
                    img_tasks.append(
                        asyncio.create_task(
                            self.save_image_from_page_response(resp, images_out_dir, page_img_sem, saved_urls=img_urls)
                        )
                    )
                if (download_images):
                    HTMLParser.create_and_clear_dir(images_out_dir)
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
                with open(html_out_path, "w", encoding="utf-8") as f:
                    f.write(html)
            except Exception as e:
                return {"success": False, "message": str(e)}
            finally:
                # Останавливаем поиск изображений
                closing = True
                if page and download_images:
                    try: page.remove_listener("response", on_response)
                    except: pass
                
                # Дожидаемся завершения задач
                if img_tasks:
                    await asyncio.gather(*img_tasks, return_exceptions=True)

                await context.close()
            return {"success": True, "message": "OK"} 


    # Получаем сетевой ответ веб-страницы и скачиваем его, если это изображение
    async def save_image_from_page_response(self, response, img_dir, page_img_sem, saved_urls=None):
        if saved_urls is None:
            saved_urls = set()
        try:
            # Проверяем, что ответ является изображением
            content_type = (response.headers.get("content-type") or "").lower()
            resource_type = response.request.resource_type.lower()
            # print(f"content_type: {content_type}; resource_type: {resource_type}")
            if (resource_type != "image") and (not content_type.startswith("image/")):
                return
            
            # Проверяем наличие данного изображения в числе уже скачанных
            url = response.url
            if url in saved_urls:
                return
            saved_urls.add(url)

            async with page_img_sem:
                async with self.global_img_sem:
                    # Сохраняем изображение на диск
                    body = await response.body()
                    img_ext = HTMLParser.get_img_extension(content_type, url)
                    img_name = HTMLParser.get_img_name(url)
                    img_path = os.path.join(img_dir, img_name + img_ext)
                    with open(img_path, "wb") as f:
                        f.write(body)
        except Exception as e:
            # print(e)
            pass


    # Очистка и создание новой дирректирии
    @staticmethod
    def create_and_clear_dir(dir):
        shutil.rmtree(dir, ignore_errors=True)
        os.makedirs(dir, exist_ok=True)

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



async def main():
    url = "https://impulse.t1.ru/"
    # url = "https://вэбцентр.рф/playground/tpost/ik7pp010g1-festival-finansovoi-gramotnosti-i-predpr"
    user_agent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36"
    
    html_parser = HTMLParser()
    await html_parser.start()

    try:
        r = await html_parser.download_html_content(
            url=url, 
            user_agent=user_agent, 
            additional_page_load_timeout_s=3,
            settings=PageComplexity.DEFAULT.value
        )
        print(r["success"], r["message"])
    finally:
        await html_parser.stop()

if __name__ == "__main__":
    asyncio.run(main())