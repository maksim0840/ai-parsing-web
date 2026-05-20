
# Установка зависимостей

Необходимо установить:

- docker
- docker compose
- openssl
- git

Для успешной загрузки образов указать registry-mirrors и proxy для docker:

```
sudo nano /etc/docker/daemon.json
```

```                       
{
  "registry-mirrors": [
    "https://cr.yandex/mirror/",
    "https://dockerhub.timeweb.cloud/",
    "https://dockerhub1.beget.com/"
  ],
  "max-concurrent-downloads": 1,
  "proxies": {
    "http-proxy": "http://USERNAME:PASSWORD@PROXY_HOST:PROXY_PORT",
    "https-proxy": "http://USERNAME:PASSWORD@PROXY_HOST:PROXY_PORT",
    "no-proxy": "localhost,127.0.0.1,::1"
  }
}
```

# Первый запуск и настройка сервисов

## 1. Загрузка проекта из репозитория

Склонируйте репозиторий и перейдите в директорию проекта:

```bash
git clone https://github.com/maksim0840/ai-parsing-web
cd ai-parsing-web
```

## 2. Настройка Garage S3

Опционально можно изменить время автоматического удаления файлов из бакета `garage-custom-ttl-bucket`:

```bash
nano garage/custom-lifecycle.json
```

Запустите скрипт первичной настройки Garage S3:

```bash
./scripts/garage-s3-init.sh
```

Скрипт выполняет:

- генерацию секретов Garage;
- создание служебных конфигурационных файлов;
- запуск и инициализацию Garage;
- создание S3-бакетов;
- создание S3-ключей доступа;
- настройку TTL для бакета `garage-custom-ttl-bucket`;
- подключение сервисов приложения к S3-хранилищу.

## 3. Генерация рандомных значений

Запустите скрипт генерации случайных значений для `.env`-файлов модулей:

```bash
./scripts/rand-env-generate.sh
```

Скрипт генерирует внутренние секреты приложения, такие как секретный ключ для JWT.

## 4. Установка секретов

Запустите скрипт ручной настройки внешних секретов:

```bash
./scripts/secrets-setup.sh
```

Во время выполнения скрипт запросит значения следующих переменных:

`YANDEXGPT_API_KEY` — API-ключ для доступа к YandexGPT;
`GIGACHAT_AUTH_KEY` — API-ключ для доступа к GigaChat.

После ввода значений скрипт создаст или обновит необходимые `.env`-файлы сервисов.

## 5. Настройка сервиса-оркестратора

Опционально можно изменить начальные параметры работы с LLM-моделями:

```bash
nano parsing-task-orchestrator-microservice/llm.env
```

В этом файле можно настроить:

- начальную температуру;
- количество выходных токенов;
- таймауты запросов к LLM-моделям.

## 6. Настройка сервисов парсинга и распознавания текста

Опционально можно изменить настройки парсинга и предобработки HTML:

```bash
nano parsing/parser/parser_settings.env
```

В этом файле можно настроить максимальное количество одновременно обрабатываемых запросов.

Опционально можно изменить настройки сервиса распознавания текста:

```bash
nano parsing/text_recognition/recognition_settings.env
```

В этом файле можно настроить количество запущенных моделей распознавания текста.

## 7. Настройка сервиса-шлюза пользовательских запросов

Опционально можно изменить время действия JWT-токена без обновления:

```bash
api-gateway-microservice/security.env
```


# Запуск

```bash
cd garage
docker compose -f docker-compose.garage.yaml -p garage up -d

cd ../parsing-task-orchestrator-microservice
docker compose -f docker-compose.orchestrator.yaml -p orchestrator up -d

cd ../parsing
docker compose -f docker-compose.parser.yaml -p parser up -d
docker compose -f docker-compose.recognition.yaml -p recognition up -d

cd ../extraction-results-microservice
docker compose -f docker-compose.results.yaml -p results up -d

cd ../users-info-microservice
docker compose -f docker-compose.users.yaml -p users up -d

cd ../api-gateway-microservice
docker compose -f docker-compose.api.yaml -p api up -d

cd ../frontend
docker compose -f docker-compose.frontend.yaml -p frontend up -d
```


После запуска frontend будет доступен по адресу:
```
http://localhost:5173
```