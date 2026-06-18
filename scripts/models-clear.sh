#!/bin/bash

set -e


echo "ВНИМАНИЕ!"
echo "Этот скрипт полностью удалит установленные моедли распознования текста."
echo "Файлы будут удалены в модулях: parsing/text_recognition."
read -p "Продолжить? (yes/no): " CONFIRM
if [ "$CONFIRM" != "yes" ]; then
  echo "Операция отменена."
  exit 0
fi


echo "== Удаление моделей распознования текста =="
rm -rf ./parsing/text_recognition/models/PP-OCRv5_mobile_det_infer
rm -rf ./parsing/text_recognition/models/eslav_PP-OCRv5_mobile_rec_infer


echo
echo "Модели успешно удалены."