
# Установка зависимостей

Необходимо установить:

- docker
- docker compose
- openssl
- git
- git-lfs
- wget
- tar

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

## 2. Создание docker-сетей

Запустите скрипт создания docker-сетей для взаимодействия микросервисов между собой

```bash
docker network create garage-net
docker network create rabbitmq-net
docker network create grpc-orchestrator-api-net
docker network create grpc-results-api-net
docker network create grpc-users-api-net
docker network create api-frontend-net
```

## 3. Настройка Garage S3

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

## 4. Генерация рандомных значений

Запустите скрипт генерации случайных значений для `.env`-файлов модулей:

```bash
./scripts/rand-env-generate.sh
```

Скрипт генерирует внутренние секреты приложения, такие как секретный ключ для JWT.

## 5. Установка секретов

Запустите скрипт ручной настройки внешних секретов:

```bash
./scripts/secrets-setup.sh
```

Во время выполнения скрипт запросит значения следующих переменных:

`YANDEXGPT_FOLDER_ID` — ID папки каталога YandexGPT;
`YANDEXGPT_API_KEY` — API-ключ для доступа к YandexGPT;
`GIGACHAT_AUTH_KEY` — API-ключ для доступа к GigaChat.

После ввода значений скрипт создаст или обновит необходимые `.env`-файлы сервисов.

## 6. Установка моделей распознования текста

Запустите скрипт для скачивания моделей распознования текста

```bash
./scripts/models-download.sh
```

В папке `parsing/text_recognition/models` появятся две директории: `PP-OCRv5_mobile_det_infer` (модель детекции текста) и `eslav_PP-OCRv5_mobile_rec_infer` (модель распознавания кириллицы).

## 7. Настройка сервиса-оркестратора

Опционально можно изменить начальные параметры работы с LLM-моделями:

```bash
nano parsing-task-orchestrator-microservice/llm.env
```

В этом файле можно настроить:

- начальную температуру;
- количество выходных токенов;
- таймауты запросов к LLM-моделям.

## 8. Настройка сервисов парсинга и распознавания текста

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

## 9. Настройка сервиса-шлюза пользовательских запросов

Опционально можно изменить время действия JWT-токенов и их обновления:

```bash
nano api-gateway-microservice/security.env
```



# Запуск

```bash
docker compose -f garage/docker-compose.garage.yaml -p garage --project-directory garage up

docker compose -f parsing-task-orchestrator-microservice/docker-compose.orchestrator.yaml -p orchestrator --project-directory parsing-task-orchestrator-microservice up

docker compose -f parsing/docker-compose.parser.yaml -p parser --project-directory parsing up
docker compose -f parsing/docker-compose.recognition.yaml -p recognition --project-directory parsing up

docker compose -f extraction-results-microservice/docker-compose.results.yaml -p results --project-directory extraction-results-microservice up

docker compose -f users-info-microservice/docker-compose.users.yaml -p users --project-directory users-info-microservice up

docker compose -f api-gateway-microservice/docker-compose.api.yaml -p api --project-directory api-gateway-microservice up

docker compose -f frontend/docker-compose.frontend.yaml -p frontend --project-directory frontend up
```


После запуска frontend будет доступен по адресу:
```
http://localhost:5173
```