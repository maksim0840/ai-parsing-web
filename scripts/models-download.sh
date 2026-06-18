
#!/bin/bash
set -e

echo "== Установка моделей =="

echo "Устоновка модели детекции PP-OCRv5_mobile_det_infer"
wget -O ./parsing/text_recognition/models/PP-OCRv5_mobile_det_infer.tar \
  "https://paddle-model-ecology.bj.bcebos.com/paddlex/official_inference_model/paddle3.0.0/PP-OCRv5_mobile_det_infer.tar"
tar -xf ./parsing/text_recognition/models/PP-OCRv5_mobile_det_infer.tar -C ./parsing/text_recognition/models/
rm ./parsing/text_recognition/models/PP-OCRv5_mobile_det_infer.tar

echo "Устоновка модели распознования eslav_PP-OCRv5_mobile_rec_infer"
git clone https://huggingface.co/PaddlePaddle/eslav_PP-OCRv5_mobile_rec \
  ./parsing/text_recognition/models/eslav_PP-OCRv5_mobile_rec_infer

echo "Установка моделей успешно завершена."