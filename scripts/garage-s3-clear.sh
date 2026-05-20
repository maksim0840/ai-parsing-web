#!/bin/bash

set -e

COMPOSE_FILE="docker-compose.garage.yaml"
PROJECT_NAME="garage"


cd garage


echo "ВНИМАНИЕ!"
echo "Этот скрипт полностью сбросит Garage"
echo "Все бакеты, ключи и загруженные файлы будут потеряны."
read -p "Продолжить? (yes/no): " CONFIRM
if [ "$CONFIRM" != "yes" ]; then
  echo "Операция отменена."
  exit 0
fi


echo "== Остановка и удаление контейнеров Garage =="
if [ -f "$COMPOSE_FILE" ]; then
  docker compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" down -v
else
  echo "Файл $COMPOSE_FILE не найден. Пропускаю docker compose down."
fi


echo "== Удаление служебных директорий Garage =="
sudo rm -rf meta data


cd ..


echo "== Удаление сгенерированных конфигурационных файлов =="
rm -f garage/garage.toml
rm -f garage/garage-s3.env
rm -f parsing/common/s3_settings.env
rm -f parsing-task-orchestrator-microservice/s3_settings.env
rm -f api-gateway-microservice/s3_settings.env


echo "== Проверка оставшихся контейнеров Garage =="
docker ps -a | grep garage || true


echo
echo "Garage сброшен."
echo "S3-переменные модулей сброшены."
