from paddleocr import PaddleOCR


class TextRecognition:

    def __init__(self):
        self.ocr = PaddleOCR(
            # модель детекции текста (обнаружение области с текстом на картинке)
            text_detection_model_name="PP-OCRv5_mobile_det",
            text_detection_model_dir="./models/PP-OCRv5_mobile_det_infer",
            # модель распознования текста (поддерживает русский + английский + цифры)
            text_recognition_model_name="eslav_PP-OCRv5_mobile_rec",
            text_recognition_model_dir="./models/eslav_PP-OCRv5_mobile_rec_infer",

            use_doc_orientation_classify=False,
            use_doc_unwarping=False,
            use_textline_orientation=False,

            # enable_hpi=True,
            enable_mkldnn=True,      # для CPU
            cpu_threads=2,           # под лимиты контейнера
            # уменьшать изображения до размера большей стороны  <= text_det_limit_side_len
            text_det_limit_side_len=640,
            text_det_limit_type="max" 
        )

    def run_ocr(self, img_path: str):
        text_from_img = ""
        result = self.ocr.predict(img_path)

        for res in result:
            text_from_img += "\n".join(res["rec_texts"])
            text_from_img += "\n"
        return text_from_img
