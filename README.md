# ai-parsing-web

создаём сети
```
docker network create garage-net
docker network create rabbitmq-net
```

запускаем сервисы
```
cd garage
docker compose -f docker-compose.garage.yaml -p garage up

cd ../parsing-task-orchestrator-microservice
docker compose -f docker-compose.rabbitmq.yaml -p rabbitmq up

cd ../parsing
docker compose -f docker-compose.parser.yaml -p parser up
docker compose -f docker-compose.recognition.yaml -p recognition up
```


paddlepaddle==3.3.0


text_recognition модели
'''
pip install --no-cache-dir paddlepaddle==3.2.2
https://paddle-whl.bj.bcebos.com/stable/cpu/paddlepaddle/paddlepaddle-3.2.2-cp313-cp313-win_amd64.whl


# Скачать архивы
wget -O ./models/PP-OCRv5_mobile_det_infer.tar \
  "https://paddle-model-ecology.bj.bcebos.com/paddlex/official_inference_model/paddle3.0.0/PP-OCRv5_mobile_det_infer.tar"

wget -O ./models/PP-OCRv5_mobile_rec_infer.tar \
  "https://paddle-model-ecology.bj.bcebos.com/paddlex/official_inference_model/paddle3.0.0//PP-OCRv5_mobile_rec_infer.tar"

# Распаковать
tar -xf ./models/PP-OCRv5_mobile_det_infer.tar -C ./models/ppocrv5_mobile_det
tar -xf ./models/PP-OCRv5_mobile_rec_infer.tar -C ./models/ppocrv5_mobile_rec
'''