
from dataclasses import dataclass
from enum import Enum

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