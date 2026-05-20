#!/bin/bash

set -e


echo "ВНИМАНИЕ!"
echo "Этот скрипт полностью сбросит установленные секреты."
echo "Конфигурационных файлы будут удалены в модулях: parsing-task-orchestrator-microservice."
read -p "Продолжить? (yes/no): " CONFIRM
if [ "$CONFIRM" != "yes" ]; then
  echo "Операция отменена."
  exit 0
fi


echo "== Удаление сгенерированных конфигурационных файлов =="
rm -f parsing-task-orchestrator-microservice/llm.env


echo
echo "Настройки секретов сброшены."
