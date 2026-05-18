
# Установка зависимостей

Необходимо установить:

- docker
- docker compose
- openssl

Для успешной загрузки образов указать registry-mirrors и proxy:

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

## 1. Создаём docker-сети

```
docker network create garage-net
docker network create rabbitmq-net
docker network create grpc-orchestrator-api-net
docker network create grpc-results-api-net
docker network create grpc-users-api-net
docker network create api-frontend-net
```


## 2. Настройка Garage S3
```
cd ai-parsing-web/garage
```

### 2.1. Генерация секретов
Сгенерируйте значения для `rpc_secret`, `admin_token` и `metrics_token`:
```
openssl rand -hex 32      # rpc_secret
openssl rand -base64 32   # admin_token
openssl rand -base64 32   # metrics_token
```

### 2.2. Настройка конфигов
Укажите сгенерированные значения в конфигурационных файлах:

- `rpc_secret` — в файле `garage.toml`
- `admin_token` — в файлах `garage.toml` и `docker-compose.garage.yaml`
- `metrics_token` — в файле `garage.toml`

Опционально можно изменить период автоматической очистки объектов в файле `custom-lifecycle.json`.

### 2.3. Запуск контейнера Garage
Создайте служебные директории для хранения метаданных и данных Garage:
```
mkdir -p meta data
```

Запустите контейнер Garage:
```
docker compose -f docker-compose.garage.yaml -p garage up -d
```

### 2.4. Инициализация layout
Получите `NODE_ID` текущего узла:
```
docker exec -it garage /garage status
```

Назначьте узел в layout. Вместо `<CHANGE_ME_NODE_ID>` укажите полученный `NODE_ID`:
```
docker exec -it garage /garage layout assign -z dc1 -c 1G <CHANGE_ME_NODE_ID>
docker exec -it garage /garage layout apply --version 1
```

### 2.5. Создание S3-ключа

Создайте S3-ключ для доступа приложения к Garage:
```
docker exec -it garage /garage key create my-app-key
```
После выполнения команды сохраните значения:
- `Key ID` — это `ACCESS_KEY`
- `Secret Key` — это `SECRET_ACCESS_KEY`

### 2.6. Создание бакетов

Создайте основной бакет и бакет с автоматической очисткой:
```
docker exec -it garage /garage bucket create garage-default-bucket
docker exec -it garage /garage bucket create garage-custom-ttl-bucket
```

Создайте основной бакет и бакет с автоматической очисткой:
```
docker exec -it garage /garage bucket allow --read --write --owner garage-default-bucket --key my-app-key
docker exec -it garage /garage bucket allow --read --write --owner garage-custom-ttl-bucket --key my-app-key
```

### 2.7. Настройка TTL-бакета
Примените lifecycle-конфигурацию к бакету `garage-custom-ttl-bucket`.

Перед запуском команды замените:

- `<CHANGE_ME_ACCESS_KEY>` на `ACCESS_KEY`
- `<CHANGE_ME_SECRET_ACCESS_KEY>` на `SECRET_ACCESS_KEY`

Команду нужно выполнить один раз и дождаться её завершения:
```
docker run --rm \
  --network host \
  -e AWS_ACCESS_KEY_ID='<CHANGE_ME_ACCESS_KEY>' \
  -e AWS_SECRET_ACCESS_KEY='<CHANGE_ME_SECRET_ACCESS_KEY>' \
  -e AWS_DEFAULT_REGION='garage' \
  -v "$PWD:/work" \
  -w /work \
  public.ecr.aws/aws-cli/aws-cli \
  s3api put-bucket-lifecycle-configuration \
  --bucket garage-custom-ttl-bucket \
  --lifecycle-configuration file://custom-lifecycle.json \
  --endpoint-url http://127.0.0.1:3900
```

### 2.8. Подключение Garage к сервисам приложения
Укажите `ACCESS_KEY` и `SECRET_ACCESS_KEY` в env-файлах сервисов, которые обращаются к Garage:
  - `ai-parsing-web/parsing/common/s3_settings.env`
  - `ai-parsing-web/parsing-task-orchestrator-microservice/connection.env`
  - `ai-parsing-web/api-gateway-microservice/connection.env`


## 3. Настройка сервиса-оркестратора
```
cd ai-parsing-web/parsing-task-orchestrator-microservice
```

Настройте подключение к LLM-моделям в файле `llm.env`. Необходимо указать API-ключи для доступа к GigaChat и YandexGPT:
- `YANDEXGPT_API_KEY`
- `GIGACHAT_AUTH_KEY`

Опционально можно изменить начальные значения температуры, количества выходных токенов и таймаутов.

Запустите сервис-оркестратор:
```
docker compose -f docker-compose.orchestrator.yaml -p orchestrator up -d
```


## 4. Настройка сервисов парсинга и распознавания текста
```
cd ai-parsing-web/parsing
```

Опционально можно изменить настройки (максимальное количество одновременно обрабатываемых запросов) парсинга и предобработки HTML в файле:
- `parser/parser_settings.env`

Опционально можно изменить настройки (количество запущенных моделей) сервиса распознавания текста в файле:
- `text_recognition/recognition_settings.env`

Запустите сервис парсинга:
```
docker compose -f docker-compose.parser.yaml -p parser up -d
```

Запустите сервис распознавания текста:
```
docker compose -f docker-compose.recognition.yaml -p recognition up -d
```

## 5. Настройка сервиса хранения результатов анализа
```
cd ai-parsing-web/extraction-results-microservice
```

Запустите сервис хранения результатов: 
```
docker compose -f docker-compose.results.yaml -p results up -d
```


## 6. Настройка сервиса хранения пользовательской информации
```
cd ai-parsing-web/users-info-microservice
```

Запустите сервис хранения пользовательской информации:
```
docker compose -f docker-compose.users.yaml -p users up -d
```


## 7. Настройка сервиса-шлюза пользовательских запросов
```
cd ai-parsing-web/api-gateway-microservice
```

Сгенерируйте секретный ключ для подписи JWT-токенов:
```
openssl rand -base64 32
```

Укажите сгенерированное значение `jwt_secret` в файле `security.env`. В этом же файле опционально можно настроить время действия токена без обновления.

Запустите сервис-шлюз:
```
docker compose -f docker-compose.api.yaml -p api up -d
```

## 8. Настройка фронтенда
```
cd ai-parsing-web/frontend
```

Запустите frontend:
```
docker compose -f docker-compose.frontend.yaml -p frontend up -d
```

После запуска frontend будет доступен по адресу:
```
http://localhost:5173
```


# Повторный запуск настроенной среды

```
cd ai-parsing-web

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
