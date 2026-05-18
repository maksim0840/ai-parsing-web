# ai-parsing-web

# Установка необходимых зависимостей
- установке docker, docker compose

- добавляем docker registry mirror
```
sudo nano /etc/docker/daemon.json
```

- указать registry-mirrors и proxy для загрузки образов
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

# Первый запуск


1. Создаём docker-сети

```
docker network create garage-net
docker network create rabbitmq-net
docker network create grpc-orchestrator-api-net
docker network create grpc-results-api-net
docker network create grpc-users-api-net
```


2. Настройка S3 хранилища Garage
```
cd ai-parsing-web/garage
```

- генерируем ключи
```
openssl rand -hex 32		# rpc_secret
openssl rand -base64 32		# admin_token
openssl rand -base64 32		# metrics_token
```

- настраиваем конфиги
    - указать ключ rpc_secret в файлах: garage.toml 
    - указать ключ admin_token в файлах: garage.toml, docker-compose.garage.yaml 
    - указать ключ metrics_token в файлах: garage.toml
    - (опцианально) изменить период для автоматической очистки бакетов в файле custom-lifecycle.json

- запуск
```
# служебные папки
mkdir -p meta data

# запуск контейнера
docker compose -f docker-compose.garage.yaml -p garage up -d
```

- получить NODE_ID
```
docker exec -it garage /garage status
```

- назначить layout (указать свой NODE_ID)
```
docker exec -it garage /garage layout assign -z dc1 -c 1G <CHANGE_ME_NODE_ID>
docker exec -it garage /garage layout apply --version 1
```

- создать ключ и получить Key ID (ACCESS_KEY) и Secret Key (SECRET_ACCESS_KEY)
```
docker exec -it garage /garage key create my-app-key
```

- создать бакеты
```
docker exec -it garage /garage bucket create garage-default-bucket
docker exec -it garage /garage bucket create garage-custom-ttl-bucket
docker exec -it garage /garage bucket allow --read --write --owner garage-default-bucket --key my-app-key
docker exec -it garage /garage bucket allow --read --write --owner garage-custom-ttl-bucket --key my-app-key
```

- настроить ttl на бакете (указать свои ACCESS_KEY и SECRET_ACCESS_KEY)
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

- настроить подключение к garage в других сервисах (указать свои ACCESS_KEY и SECRET_ACCESS_KEY)
  - ai-parsing-web/parsing/common/s3_settings.env
  - ai-parsing-web/parsing-task-orchestrator-microservice/connection.env
  - ai-parsing-web/api-gateway-microservice/connection.env


3. Настройка сервиса-оркестратора (parsing-task-orchestrator)
```
cd ai-parsing-web/parsing-task-orchestrator-microservice
```

- настроить взаимодействие с LLM-моделями в файле llm.env
  - указать API-ключи для доступа к GigaChat и YandexGPT (YANDEXGPT_API_KEY и GIGACHAT_AUTH_KEY)
  - (опционально) указать начальную температуру, количество выходных токенов и таймауты

- запустить сервис
```
docker compose -f docker-compose.orchestrator.yaml -p orchestrator up -d
```


4. Настройка сервисов парсинга и распознования (parsing)
```
cd ai-parsing-web/parsing
```

- (опционально) настроить максимальное количество обрабатываемых запросов при парсинге и предобработке в файле parser/parser_settings.env

- (опционально) настроить количество запущенных моделей распознования текста в файле recognition_settings.env

- запустить сервисы
```
docker compose -f docker-compose.parser.yaml -p parser up -d
docker compose -f docker-compose.recognition.yaml -p recognition up -d
```

5. Настройка сервиса хранения результатов анализа
```
cd ai-parsing-web/extraction-results-microservice
```

- запустить сервисы
```
docker compose -f docker-compose.results.yaml -p results up -d
```


6. Настройка сервиса хранения пользовательской информации
```
cd ai-parsing-web/users-info-microservice
```

- запустить сервисы
```
docker compose -f docker-compose.users.yaml -p users up -d
```


7. Настройка сервиса-шлюза пользовательских запросов
```
cd ai-parsing-web/api-gateway-microservice
```

- генерируем ключи
```
openssl rand -base64 32		# jwt_secret
```

- настраиваем секретный ключ JWT (jwt_secret) и время работы токена без обновления в файле security.env

- запустить сервисы
```
docker compose -f docker-compose.api.yaml -p api up -d
```

8. Настройка фронтенда
```
cd ai-parsing-web/frontend
```


# Повторный запуск настроенной среды

```
cd ai-parsing-web

cd garage
docker compose -f docker-compose.garage.yaml -p garage up

cd ../parsing-task-orchestrator-microservice
docker compose -f docker-compose.orchestrator.yaml -p orchestrator up

cd ../parsing
docker compose -f docker-compose.parser.yaml -p parser up
docker compose -f docker-compose.recognition.yaml -p recognition up

cd ../extraction-results-microservice
docker compose -f docker-compose.results.yaml -p results up

cd ../users-info-microservice
docker compose -f docker-compose.users.yaml -p users up

cd ../api-gateway-microservice
docker compose -f docker-compose.api.yaml -p api up

cd ../frontend
npm run dev
```
