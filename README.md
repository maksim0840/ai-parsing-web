# UML Sequence Diagram основного пайплайна работы программы

<img width="3865" height="2915" alt="uml sequence ai-parsing-web drawio (2)" src="https://github.com/user-attachments/assets/83072707-3cde-49b9-b58e-2c450e9e09f7" />

# Микросервисы, их связи и используемые технологии

<img width="3020" height="1425" alt="ai-parsing-web drawio (1)" src="https://github.com/user-attachments/assets/1946da8e-233c-421d-8653-ec8274a4da6e" />

# Экран авторизации

<img width="2560" height="1694" alt="Screenshot 2026-07-20 at 00-25-07 AI parse" src="https://github.com/user-attachments/assets/63f8d36c-4844-4f87-95d3-09ee9d6f9728" />

# Выполнение задачи

<img width="3292" height="1694" alt="Screenshot 2026-07-20 at 00-27-12 AI parse" src="https://github.com/user-attachments/assets/89efe88f-abcc-44e6-ae84-39057d05838f" />

# История предыдущих результатов анализа веб-страниц

<img width="2021" height="1526" alt="Screenshot 2026-07-22 at 00-19-18 AI parse" src="https://github.com/user-attachments/assets/34a79408-38cd-4e83-b170-b51ae7eb7b83" />

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
