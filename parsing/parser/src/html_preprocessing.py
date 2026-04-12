from bs4 import BeautifulSoup
from html import unescape
import asyncio
from common.src.s3_storage_connection import S3Storage
from dotenv import load_dotenv
import os

load_dotenv("parser/parser_settings.env") # загружаем .env файл конфигурации

# Максимальное количество одновременно обрабатываемых html файлов
MAX_CONCURRENT_PREPROCESSING = int(os.getenv("MAX_CONCURRENT_PREPROCESSING"))


# WHITELIST формируется из известных аргументов тегов, используемых на различных сайтах или связанных с узкой специализацией сайта
# теги html: https://developer.mozilla.org/ru/docs/Web/HTML/Reference/Elements


# Раскрываем noscript в обычный html контент
def decode_noscript(soup):
    try:
        for noscript in soup.find_all("noscript"):
            # Получаем содержимое тега noscript в виде строки
            raw_str = (noscript.decode_contents() or "").strip()
            if not raw_str:
                continue

            # Проверяем наличие закодированных сущностей '<' и '>'
            if "&lt;" not in raw_str and "&gt;" not in raw_str:
                continue

            # Декодируем сущности "&lt;" и "&gt;" в '<' и '>'
            decoded_str = unescape(raw_str).strip()
            decoded = BeautifulSoup(decoded_str, "lxml")

            # Заменяем содержимое noscript на раскодированные узлы
            noscript.clear()
            nodes = decoded.body.contents if decoded.body else decoded.contents
            for node in list(nodes):
                noscript.append(node)
    except:
        raise Exception("<noscript> tag processing error")


META_NAME_STARTSWITH_WHITELIST = {"citation_", "dc.", "dcterms.", "eprints.", "bepress_citation_", "prism.", }
META_NAME_WHITELIST = {"description", "keywords", "author", "title", "news_keywords", "date", "pubdate", "twitter:title", "twitter:description", "twitter:creator",}
META_PROPERTY_WHITELIST = {"og:title", "og:description", "og:type", "og:site_name", "og:url", "og:locale", "og:updated_time", "article:published_time", "article:modified_time", "article:section", "article:tag", "article:author",}
META_ITEMPROP_WHITELIST = {"name", "description", "datePublished", "datepublished", "dateModified", "datemodified", "author", "headline", "url", "startDate", "startdate", "endDate", "enddate", "location", "organizer",}

def keep_meta(tag):
    content = (tag.get("content") or "").strip()
    if not content:
        return False

    name_l = (tag.get("name") or "").strip().lower()
    prop_l = (tag.get("property") or "").strip().lower()
    itemprop = (tag.get("itemprop") or "").strip()

    if any(name_l.startswith(wl) for wl in META_NAME_STARTSWITH_WHITELIST):
        return True
    if name_l in META_NAME_WHITELIST: 
        return True
    if prop_l in META_PROPERTY_WHITELIST:
        return True
    if itemprop in META_ITEMPROP_WHITELIST:
        return True
    return False


SCRIPT_TYPE_WHITELIST = {"application/ld+json", "application/json"}

def keep_script(tag):
    t = (tag.get("type") or "").strip().lower()

    if t in SCRIPT_TYPE_WHITELIST:
        return True
    return False


CANVAS_ATTRS_WITH_TEXT = {"aria-label", "title", "aria-labelledby", "data-title", "data-label",}

def keep_canvas(tag):
    inner_text = tag.get_text(" ", strip=True)
    if inner_text:
        return True

    for attr in CANVAS_ATTRS_WITH_TEXT:
        val = (tag.get(attr) or "").strip()
        if val:
           return True
    return False


SVG_WHITELIST_COMPONENTS = {"title", "desc", "text", "tspan", "textpath", "metadata", "a", "switch",}

# Удаление всех элементов для отрисовки внутри тега 'svg' (оставляем только текст)
def delete_svg_internal_components(soup):
    try:
        for svg in soup.find_all("svg"):
            for child in list(svg.find_all(True)):
                if child.name not in SVG_WHITELIST_COMPONENTS:
                    child.decompose()
    except:
        raise Exception("<svg> tag processing error")


AREA_WHITELIST_ATTRS = {"href", "alt", "title",}

# Очистить теги 'area' внутри тега 'map' от лишней информации
def clear_area(soup):
    try:
        for mp in list(soup.find_all("map")):
            for area in list(mp.find_all("area")):
                any_useful_attr = False
                for attr in list(area.attrs.keys()):
                    if attr in AREA_WHITELIST_ATTRS and (area.get(attr) or "").strip():
                        any_useful_attr = True
                    else:
                        del area.attrs[attr]
                if not any_useful_attr:
                    area.decompose()
    except:
        raise Exception("<area> tag processing error")


# Очистить тег 'img' от встроенных данных и лишних аттрибутов
def clear_img(soup):
    try:
        for img in list(soup.find_all("img")):
            src_text = (img.get("src") or "").strip().lower()
            if src_text.startswith("data:"):
                del img.attrs["src"]
            if "srcset" in img.attrs:
                del img.attrs["srcset"]
            if "sizes" in img.attrs:
                del img.attrs["sizes"]
    except:
        raise Exception("<img> tag processing error")

# Очистить тег 'video' от встроенных данных
def clear_video(soup):
    try:
        for video in list(soup.find_all("video")):
            src_text = (video.get("src") or "").strip().lower()
            if src_text.startswith("data:"):
                del video.attrs["src"]
            poster_text = (video.get("poster") or "").strip().lower()
            if poster_text.startswith("data:"):
                del video.attrs["poster"]
            
            for source in list(video.find_all("source")):
                src_text = (source.get("src") or "").strip().lower()
                if src_text.startswith("data:"):
                    del source.attrs["src"]
    except:
        raise Exception("<video> tag processing error")


# Очистить тег 'audio' от встроенных данных
def clear_audio(soup):
    try:
        for audio in list(soup.find_all("audio")):
            src_text = (audio.get("src") or "").strip().lower()
            if src_text.startswith("data:"):
                del audio.attrs["src"]

            for source in list(audio.find_all("source")):
                src_text = (source.get("src") or "").strip().lower()
                if src_text.startswith("data:"):
                    del source.attrs["src"]
    except:
        raise Exception("<audio> tag processing error")


def clear_iframe(soup):
    try:
        for el in list(soup.find_all("iframe")):
            src_text = (el.get("src") or "").strip().lower()
            if src_text.startswith("data:"):
                del el.attrs["src"]
            if "srcdoc" in el.attrs:
                del el.attrs["srcdoc"]
    except:
        raise Exception("<iframe> tag processing error")


def clear_portal(soup):
    try:
        for el in list(soup.find_all("portal")):
            src_text = (el.get("src") or "").strip().lower()
            if src_text.startswith("data:"):
                del el.attrs["src"]
            if "srcdoc" in el.attrs:
                del el.attrs["srcdoc"]
    except:
        raise Exception("<portal> tag processing error")


def clear_embed(soup):
    try:
        for el in list(soup.find_all("embed")):
            src_text = (el.get("src") or "").strip().lower()
            if src_text.startswith("data:"):
                del el.attrs["src"]
    except:
        raise Exception("<embed> tag processing error")


def clear_object(soup):
    try:
        for el in list(soup.find_all("object")):
            data_text = (el.get("data") or "").strip().lower()
            if data_text.startswith("data:"):
                del el.attrs["data"]
    except:
        raise Exception("<object> tag processing error")


def _is_data_uri(v: str | None) -> bool:
    return isinstance(v, str) and v.strip().lower().startswith("data:")

def _clean_srcset(srcset: str) -> str:
    parts = [p.strip() for p in srcset.split(",") if p.strip()]
    kept = []
    for p in parts:
        url = p.split()[0].strip()
        if not _is_data_uri(url):
            kept.append(p)
    return ", ".join(kept)

# Чистим source внутри picture
def clear_source(soup):
    try:
        for pic in list(soup.find_all("picture")):
            for src in list(pic.find_all("source")):
                if "srcset" in src.attrs:
                    cleaned = _clean_srcset(src["srcset"])
                    if not cleaned:
                        src.decompose()
                        continue
                    src["srcset"] = cleaned
                # если вдруг src тоже data:
                if _is_data_uri(src.get("src") or ""):
                    src.decompose()
                    continue
    except:
        raise Exception("<source> tag processing error")


def delete_tag(soup, tag_name, keep_tag_func=None):
    try:
        for tag in list(soup.find_all(tag_name)):
            if (keep_tag_func is None) or (not keep_tag_func(tag)):
                tag.decompose()
    except:
        raise Exception(f"<{tag_name}> tag processing error")


class HTMLPreprocessing:
    def __init__(self):
        self.sem = asyncio.Semaphore(MAX_CONCURRENT_PREPROCESSING)
        self.s3_storage = S3Storage()
    
    # Обновить html (урезать теги в соотвествии с заданными правилами)
    @staticmethod
    def preprocessing_pipeline(
        html_bytes,
        noscript_processing=False,
        link_processing=False,
        style_processing=False,
        meta_processing=False,
        script_processing=False,
        canvas_processing=False,
        svg_processing=False,
        area_processing=False,
        img_processing=False,
        video_processing=False,
        audio_processing=False,
        iframe_processing=False,
        portal_processing=False,
        embed_processing=False,
        object_processing=False,
        source_processing=False
    ):
        soup = BeautifulSoup(html_bytes, "lxml")

        if (noscript_processing): decode_noscript(soup)
        if (link_processing): delete_tag(soup, "link")
        if (style_processing): delete_tag(soup, "style")
        if (meta_processing): delete_tag(soup, "meta", keep_tag_func=keep_meta)
        if (script_processing): delete_tag(soup, "script", keep_tag_func=keep_script)
        if (canvas_processing): delete_tag(soup, "canvas", keep_tag_func=keep_canvas)
        if (svg_processing): delete_svg_internal_components(soup)
        if (area_processing): clear_area(soup)
        if (img_processing): clear_img(soup)
        if (video_processing): clear_video(soup)
        if (audio_processing): clear_audio(soup)
        if (iframe_processing): clear_iframe(soup)
        if (portal_processing): clear_portal(soup)
        if (embed_processing): clear_embed(soup)
        if (object_processing): clear_object(soup)
        if (source_processing): clear_source(soup)

        return str(soup).encode("utf-8")
    

    @staticmethod
    def read_html_bytes(html_path):
        with open(html_path, "rb") as file:
            return file.read()
    
    @staticmethod
    def write_html_bytes(html_path, html_bytes):
        with open(html_path, "w", encoding="utf-8") as f:
            f.write(html_bytes)

    async def read_html_bytes_from_s3(self, html_path):
        return await self.s3_storage.download_file_bytes(s3_object_key=html_path)

    async def write_html_bytes_to_s3(self, html_path, html_bytes):
        return await self.s3_storage.upload_file_bytes(s3_object_key=html_path, file_bytes=html_bytes)


    async def apply_preprocessing(self, html_path, **kwargs):
        async with self.sem:
            try:
                # Прочитать файл
                # html_bytes = await asyncio.to_thread(HTMLPreprocessing.read_html_bytes, html_path)
                html_bytes = await self.read_html_bytes_from_s3(html_path)

                # Обработать теги
                processed_html_bytes = await asyncio.to_thread(HTMLPreprocessing.preprocessing_pipeline, html_bytes, **kwargs)
                
                # Перезаписать новый html файл с обработанными тегами
                # await asyncio.to_thread(HTMLPreprocessing.write_html_bytes, html_path, processed_html_bytes)
                await self.write_html_bytes_to_s3(html_path, processed_html_bytes)

            except Exception as e:
                return {"success": False, "message": str(e), "response": {}}
            return {"success": True, "message": "OK", "response": {"html_path": html_path}}
