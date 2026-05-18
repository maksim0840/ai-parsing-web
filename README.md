# ai-parsing-web

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
cd ai-parsing-web/garage

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
  -e AWS_ACCESS_KEY_ID='ТВОЙ_ACCESS_KEY' \
  -e AWS_SECRET_ACCESS_KEY='ТВОЙ_SECRET_KEY' \
  -e AWS_DEFAULT_REGION='garage' \
  -v "$PWD:/work" \
  -w /work \
  public.ecr.aws/aws-cli/aws-cli \
  s3api put-bucket-lifecycle-configuration \
  --bucket garage-custom-ttl-bucket \
  --lifecycle-configuration file://custom-lifecycle.json \
  --endpoint-url http://127.0.0.1:3900
```

# Повторный запуск настроенной среды
запускаем сервисы
```
cd garage
docker compose -f docker-compose.garage.yaml -p garage up

cd parsing-task-orchestrator-microservice
docker compose -f docker-compose.orchestrator.yaml -p orchestrator up

cd parsing
docker compose -f docker-compose.parser.yaml -p parser up
docker compose -f docker-compose.recognition.yaml -p recognition up

cd extraction-results-microservice
docker compose -f docker-compose.results.yaml -p results up

cd users-info-microservice
docker compose -f docker-compose.users.yaml -p users up

cd api-gateway-microservice
docker compose -f docker-compose.api.yaml -p api up

cd frontend
npm run dev
```
