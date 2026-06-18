#!/bin/bash

set -e


echo "== Настройка секретов =="
read -p "Введите YANDEXGPT_FOLDER_ID: " YANDEXGPT_FOLDER_ID
read -p "Введите YANDEXGPT_API_KEY: " YANDEXGPT_API_KEY
read -p "Введите GIGACHAT_AUTH_KEY: " GIGACHAT_AUTH_KEY


echo "Добавление LLM api-ключей к модулю parsing-task-orchestrator-microservice"
cp parsing-task-orchestrator-microservice/llm.env.example parsing-task-orchestrator-microservice/llm.env
sed -i "s|<CHANGE_ME_YANDEXGPT_FOLDER_ID>|$YANDEXGPT_FOLDER_ID|" parsing-task-orchestrator-microservice/llm.env
sed -i "s|<CHANGE_ME_YANDEXGPT_API_KEY>|$YANDEXGPT_API_KEY|" parsing-task-orchestrator-microservice/llm.env
sed -i "s|<CHANGE_ME_GIGACHAT_AUTH_KEY>|$GIGACHAT_AUTH_KEY|" parsing-task-orchestrator-microservice/llm.env


echo "Секреты сохранены"
