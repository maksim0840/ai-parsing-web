#!/bin/bash

set -e

GARAGE_CONTAINER_NAME="garage"
GARAGE_PROJECT_NAME="garage"
GARAGE_COMPOSE_FILE="docker-compose.garage.yaml"

APP_KEY_NAME="my-app-key"
DEFAULT_BUCKET="garage-default-bucket"
TTL_BUCKET="garage-custom-ttl-bucket"
LIFECYCLE_FILE="custom-lifecycle.json"


cd garage


echo "== Генерация секретов =="
RPC_SECRET=$(openssl rand -hex 32)
ADMIN_TOKEN=$(openssl rand -base64 32)
METRICS_TOKEN=$(openssl rand -base64 32)


echo "== Настройка конфига =="
cp garage.toml.example garage.toml
sed -i "s|<CHANGE_ME_RPC_SECRET>|$RPC_SECRET|" garage.toml
sed -i "s|<CHANGE_ME_ADMIN_TOKEN>|$ADMIN_TOKEN|" garage.toml
sed -i "s|<CHANGE_ME_METRICS_TOKEN>|$METRICS_TOKEN|" garage.toml


echo "== Создание служебных директорий =="
mkdir -p meta data


echo "== Запуск Garage =="
docker compose -f "$GARAGE_COMPOSE_FILE" -p "$GARAGE_PROJECT_NAME" up -d


echo "== Ожидание запуска Garage =="
for i in {1..30}; do
  if docker exec "$GARAGE_CONTAINER_NAME" /garage status >/dev/null 2>&1; then
    echo "Garage запущен."
    break
  fi
  echo "Ожидание Garage... попытка $i"
  sleep 2
done


echo "== Получение NODE_ID =="
GARAGE_STATUS=$(docker exec "$GARAGE_CONTAINER_NAME" /garage status)
NODE_ID=$(echo "$GARAGE_STATUS" | grep -E '^[0-9a-f]+[[:space:]]' | awk '{print $1}' | head -n 1)
if [ -z "$NODE_ID" ]; then
  echo "Не удалось автоматически определить NODE_ID."
  echo "Вывод команды /garage status:"
  echo "$GARAGE_STATUS"
  exit 1
fi


echo "== Назначение layout =="
ASSIGN_OUTPUT=$(docker exec "$GARAGE_CONTAINER_NAME" /garage layout assign -z dc1 -c 1G "$NODE_ID" || true)
APPLY_VERSION=$(echo "$ASSIGN_OUTPUT" | grep -Eo 'layout apply --version [0-9]+' | grep -Eo '[0-9]+' | tail -n 1)
if [ -z "$APPLY_VERSION" ]; then
  APPLY_VERSION="1"
fi


echo "== Применение layout version $APPLY_VERSION =="
docker exec "$GARAGE_CONTAINER_NAME" /garage layout apply --version "$APPLY_VERSION" || true


echo "== Создание S3-ключа =="
KEY_OUTPUT=$(docker exec "$GARAGE_CONTAINER_NAME" /garage key create "$APP_KEY_NAME")
ACCESS_KEY=$(echo "$KEY_OUTPUT" | grep -Ei 'Key ID|Access key' | grep -Eo 'GK[a-zA-Z0-9]+' | head -n 1)
SECRET_ACCESS_KEY=$(echo "$KEY_OUTPUT" | grep -Ei 'Secret' | awk '{print $NF}' | head -n 1)
if [ -z "$ACCESS_KEY" ] || [ -z "$SECRET_ACCESS_KEY" ]; then
  echo "Не удалось автоматически извлечь ACCESS_KEY или SECRET_ACCESS_KEY."
  echo "Вывод команды key create:"
  echo "$KEY_OUTPUT"
  exit 1
fi


echo "== Сохранение S3-ключей в файл garage-s3.env =="
cat > garage-s3.env <<EOF
ACCESS_KEY=$ACCESS_KEY
SECRET_ACCESS_KEY=$SECRET_ACCESS_KEY
EOF


echo "== Создание бакетов =="
docker exec "$GARAGE_CONTAINER_NAME" /garage bucket create "$DEFAULT_BUCKET" || true
docker exec "$GARAGE_CONTAINER_NAME" /garage bucket create "$TTL_BUCKET" || true


echo "== Выдача прав на бакеты =="
docker exec "$GARAGE_CONTAINER_NAME" /garage bucket allow --read --write --owner "$DEFAULT_BUCKET" --key "$APP_KEY_NAME" || true
docker exec "$GARAGE_CONTAINER_NAME" /garage bucket allow --read --write --owner "$TTL_BUCKET" --key "$APP_KEY_NAME" || true


echo "== Настройка TTL для бакета $TTL_BUCKET =="
docker run --rm \
  --network garage-net \
  -e AWS_ACCESS_KEY_ID="$ACCESS_KEY" \
  -e AWS_SECRET_ACCESS_KEY="$SECRET_ACCESS_KEY" \
  -e AWS_DEFAULT_REGION="garage" \
  -v "$PWD:/work" \
  -w /work \
  amazon/aws-cli \
  s3api put-bucket-lifecycle-configuration \
  --bucket "$TTL_BUCKET" \
  --lifecycle-configuration "file://$LIFECYCLE_FILE" \
  --endpoint-url http://garage:3900


cd ..


echo "Добавление s3-переменных к модулю parsing"
cp parsing/common/s3_settings.env.example parsing/common/s3_settings.env
sed -i "s|<CHANGE_ME_ACCESS_KEY>|$ACCESS_KEY|" parsing/common/s3_settings.env
sed -i "s|<CHANGE_ME_SECRET_ACCESS_KEY>|$SECRET_ACCESS_KEY|" parsing/common/s3_settings.env


echo "Добавление s3-переменных к модулю parsing-task-orchestrator-microservice"
cp parsing-task-orchestrator-microservice/s3_settings.env.example parsing-task-orchestrator-microservice/s3_settings.env
sed -i "s|<CHANGE_ME_ACCESS_KEY>|$ACCESS_KEY|" parsing-task-orchestrator-microservice/s3_settings.env
sed -i "s|<CHANGE_ME_SECRET_ACCESS_KEY>|$SECRET_ACCESS_KEY|" parsing-task-orchestrator-microservice/s3_settings.env


echo "Добавление s3-переменных к модулю api-gateway-microservice"
cp api-gateway-microservice/s3_settings.env.example api-gateway-microservice/s3_settings.env
sed -i "s|<CHANGE_ME_ACCESS_KEY>|$ACCESS_KEY|" api-gateway-microservice/s3_settings.env
sed -i "s|<CHANGE_ME_SECRET_ACCESS_KEY>|$SECRET_ACCESS_KEY|" api-gateway-microservice/s3_settings.env


echo "== Garage успешно настроен =="
echo "S3-настройки Garage успешно применены."
