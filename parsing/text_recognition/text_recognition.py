from paddleocr import PaddleOCR
'''
pip install --no-cache-dir paddlepaddle==3.2.2!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
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
ocr = PaddleOCR(
  # модель детекции текста (обнаружение области с текстом на картинке)
  text_detection_model_name="PP-OCRv5_mobile_det",
  text_detection_model_dir="./models/PP-OCRv5_mobile_det_infer",
  # модель распознования текста (поддерживает русский + английский + цифры)
  text_recognition_model_name="eslav_PP-OCRv5_mobile_rec",
  text_recognition_model_dir="./models/eslav_PP-OCRv5_mobile_rec_infer",

  use_doc_orientation_classify=False,
  use_doc_unwarping=False,
  use_textline_orientation=False
)

result = ocr.predict("../images/f7ad7fb29d6d2a34.jpg")
for res in result:
  print("\n".join(res["rec_texts"]))
