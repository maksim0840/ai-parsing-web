#!/bin/bash

set -e


echo "== Генерация секретов =="
JWT_SECRET=$(openssl rand -base64 32)


echo "Добавляем security-ключи к модулю api-gateway-microservice"
cp api-gateway-microservice/security.env.example api-gateway-microservice/security.env
sed -i "s|<CHANGE_ME_JWT_SECRET>|$JWT_SECRET|" api-gateway-microservice/security.env


echo "Настройки секретов успешно применены."
