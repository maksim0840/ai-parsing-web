from bs4 import BeautifulSoup
from html import unescape

# WHITELIST формируется из известных аргументов тегов, используемых на различных сайтах, конкретном сайте или связанные с узкой специализацией сайта
# теги с информацией https://developer.mozilla.org/ru/docs/Web/HTML/Reference/Elements


# Удаление всех тегов 'link'
def delete_all_link(html):
    soup = BeautifulSoup(html, "lxml")
    for link in soup.find_all("link"):
        link.decompose()

# Удаление всех тегов 'style'
def delete_all_style(html):
    soup = BeautifulSoup(html, "lxml")
    for style in soup.find_all("style"):
        style.decompose()


META_NAME_STARTSWITH_WHITELIST = {"citation_", "dc.", "dcterms.", "eprints.", "bepress_citation_", "prism.", }
META_NAME_WHITELIST = {"description", "keywords", "author", "title", "news_keywords", "date", "pubdate", "twitter:title", "twitter:description", "twitter:creator",}
META_PROPERTY_WHITELIST = {"og:title", "og:description", "og:type", "og:site_name", "og:url", "og:locale", "og:updated_time", "article:published_time", "article:modified_time", "article:section", "article:tag", "article:author",}
META_ITEMPROP_WHITELIST = {"name", "description", "datePublished", "dateModified", "author", "headline", "url", "startDate", "endDate", "location", "organizer",}

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


SCRYPT_TYPE_WHITELIST = {"application/ld+json", "application/json"}

def keep_scrypt(tag):
    t = (tag.get("type") or "").strip().lower()

    if t in SCRYPT_TYPE_WHITELIST:
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

def decode_noscript_html(html):
    soup = BeautifulSoup(html, "lxml")

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

        return str(soup)


SVG_WHITELIST_COMPONENTS = {"title", "desc", "text", "tspan", "textpath", "metadata", "a", "switch",}

# Удаление всех элементов для отрисовки внутри тега 'svg' (оставляем только текст)
def delete_svg_internal_components(html):
    soup = BeautifulSoup(html, "lxml")
    for svg in soup.find_all("svg"):
        for child in list(svg.find_all(True)):
            if child.name not in SVG_WHITELIST_COMPONENTS:
                child.decompose()

AREA_WHITELIST_TAGS = {"href", "alt", "title",}

# Очистить теги 'area' внутри тега 'map' от лишней информации
def clear_area(html):
    soup = BeautifulSoup(html, "lxml")

    for mp in list(soup.find_all("map")):
        for area in list(mp.find_all("area")):
            any_useful_attr = False
            for attr in list(area.attrs.keys()):
                if attr in AREA_WHITELIST_TAGS:
                    any_useful_attr = True
                else:
                    del area.attrs[attr]
            if not any_useful_attr:
                area.decompose()

# Очистить тег 'img' от встроенных данных и лишних аттрибутов
def clear_img(html):
    soup = BeautifulSoup(html, "lxml")
    for img in list(soup.find_all("img")):
        src_text = img.get("src").strip().lower()
        if src_text.startswith("data:"):
            del img.attrs["src"]
        if "srcset" in img.attrs:
            del img.attrs["srcset"]
        if "sizes" in img.attrs:
            del img.attrs["sizes"]

# Очистить тег 'video' от встроенных данных
def clear_video(html):
    soup = BeautifulSoup(html, "lxml")
    for video in list(soup.find_all("video")):
        src_text = video.get("src").strip().lower()
        if src_text.startswith("data:"):
            del video.attrs["src"]
        poster_text = video.get("poster").strip().lower()
        if poster_text.startswith("data:"):
            del video.attrs["poster"]
        
        for source in list(video.find_all("source")):
            src_text = source.get("src").strip().lower()
            if src_text.startswith("data:"):
                del source.attrs["src"]

# Очистить тег 'audio' от встроенных данных
def clear_audio(html):
    soup = BeautifulSoup(html, "lxml")
    for audio in list(soup.find_all("audio")):
        src_text = audio.get("src").strip().lower()
        if src_text.startswith("data:"):
            del audio.attrs["src"]

        for source in list(audio.find_all("source")):
            src_text = source.get("src").strip().lower()
            if src_text.startswith("data:"):
                del source.attrs["src"]

def clear_iframe(html):
    soup = BeautifulSoup(html, "lxml")
    for el in list(soup.find_all("clear")):
        src_text = el.get("src").strip().lower()
        if src_text.startswith("data:"):
            del el.attrs["src"]
        if "srcdoc" in el.attrs:
            del el.attrs["srcdoc"]

def clear_portal(html):
    soup = BeautifulSoup(html, "lxml")
    for el in list(soup.find_all("portal")):
        src_text = el.get("src").strip().lower()
        if src_text.startswith("data:"):
            del el.attrs["src"]
        if "srcdoc" in el.attrs:
            del el.attrs["srcdoc"]

def clear_embed(html):
    soup = BeautifulSoup(html, "lxml")
    for el in list(soup.find_all("embed")):
        src_text = el.get("src").strip().lower()
        if src_text.startswith("data:"):
            del el.attrs["src"]

def clear_object(html):
    soup = BeautifulSoup(html, "lxml")
    for el in list(soup.find_all("object")):
        data_text = el.get("data").strip().lower()
        if data_text.startswith("data:"):
            del el.attrs["data"]






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

# чистим source внутри picture
def clear_source(html):
    soup = BeautifulSoup(html, "lxml")
    for pic in list(soup.find_all("picture")):
        for src in list(pic.find_all("source")):
            if "srcset" in src.attrs:
                cleaned = _clean_srcset(src["srcset"])
                if not cleaned:
                    src.decompose()
                    continue
            # если вдруг src тоже data:
            if _is_data_uri(src.get("src")):
                src.decompose()
                continue







def delete_tag(html, tag_name, keep_tag_func):
    soup = BeautifulSoup(html, "lxml")

    for tag in soup.find_all(tag_name):
        if not keep_tag_func(tag):
            tag.decompose()

    return str(soup)





delete_tag("hhhtttmmmlll", "meta", keep_meta)






NOSCRYPT_BLACKLIST_HINTS = {"doubleclick", "googletagmanager", "analytics", "facebook.com/tr", "metrika", "yandex", "pixel", "adsystem", "adservice",}


1) раскрываем noscript в обычным html контент
3) сделать так что nosrypt всегда фильтруется самым первым (до остальных) 
обработать новый soup по правилам остальных тегов
избавиться от NOSCRYPT_BLACKLIST_HINTS, но он ведь может быть везде?