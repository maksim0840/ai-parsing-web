#!/bin/bash

set -e


echo "ВНИМАНИЕ!"
echo "Этот скрипт полностью сбросит сгенерированные секреты."
echo "Конфигурационных файлы будут удалены в модулях: api-gateway-microservice."
read -p "Продолжить? (yes/no): " CONFIRM
if [ "$CONFIRM" != "yes" ]; then
  echo "Операция отменена."
  exit 0
fi


echo "== Удаление сгенерированных конфигурационных файлов =="
rm -f api-gateway-microservice/security.env


echo
echo "Настройки секретов сброшены."
