from paddleocr import PaddleOCR
import numpy as np
from io import BytesIO
from PIL import Image
import cairosvg
import asyncio
from pathlib import Path
from common.src.s3_storage_connection import S3Storage
from dotenv import load_dotenv
import os

load_dotenv("text_recognition/recognition_settings.env") # загружаем .env файл конфигурации

# Максимальное количество одновременно обрабатываемых картинок для извлечения текста
MAX_CONCURRENT_TEXT_RECOGNITION = int(os.getenv("MAX_CONCURRENT_TEXT_RECOGNITION"))


class TextRecognition:

    def __init__(self):
        self.ocr = PaddleOCR(
            # модель детекции текста (обнаружение области с текстом на картинке)
            text_detection_model_name="PP-OCRv5_mobile_det",
            text_detection_model_dir="./text_recognition/models/PP-OCRv5_mobile_det_infer",
            # модель распознования текста (поддерживает русский + английский + цифры)
            text_recognition_model_name="eslav_PP-OCRv5_mobile_rec",
            text_recognition_model_dir="./text_recognition/models/eslav_PP-OCRv5_mobile_rec_infer",

            use_doc_orientation_classify=False,
            use_doc_unwarping=False,
            use_textline_orientation=False,

            # enable_hpi=True,
            # enable_mkldnn=True,      # для CPU
            enable_mkldnn=False,
            cpu_threads=2,           # под лимиты контейнера
            # уменьшать изображения до размера большей стороны  <= text_det_limit_side_len
            text_det_limit_side_len=640,
            text_det_limit_type="max" 
        )
        self.sem = asyncio.Semaphore(MAX_CONCURRENT_TEXT_RECOGNITION)
        self.s3_storage = S3Storage()


    def predict_ocr(self, img_path, img_bytes):
        img = TextRecognition.image_bytes_to_numpy(img_path, img_bytes)        
        sub_texts = []
        for res in self.ocr.predict(img):
            sub = "\n".join(t for t in res["rec_texts"] if t)
            sub_texts.append(sub)
        text = "\n".join(sub for sub in sub_texts if sub)
        return text
    
    @staticmethod
    def image_bytes_to_numpy(img_path, img_bytes):
        ext = Path(img_path).suffix.lower()
        if ext == ".svg":
            img_bytes = cairosvg.svg2png(bytestring=img_bytes)

        try:
            with Image.open(BytesIO(img_bytes)) as img:
                return np.array(img.convert("RGB"))
        except Exception as e:
            raise ValueError(f"Failed to decode image bytes")

    @staticmethod
    def get_imgs_path_from_dir(imgs_dir):
        dir_path = Path(imgs_dir)
        files = [str(p) for p in dir_path.iterdir() if p.is_file()]
        return files
    
    @staticmethod
    def get_img_bytes(img_path):
        with open(img_path, "rb") as f:
            img_bytes = f.read()
        return img_bytes
    
    async def get_imgs_path_from_s3(self, imgs_dir):
        return await self.s3_storage.get_object_keys_by_prefix(prefix=imgs_dir)

    async def get_img_bytes_s3(self, img_path):
        return await self.s3_storage.download_file_bytes(s3_object_key=img_path)
    

    async def run_ocr(self, image_paths):
        text_by_image = {}
        async with self.sem:
            # Пути до всех изображений внутри дирректории
            # image_paths = await asyncio.to_thread(TextRecognition.get_imgs_path_from_dir, images_dir)
            # image_paths = await self.get_imgs_path_from_s3(images_dir)

            # Для каждого изображения получаем его байты и прогоняем через модель для получения текста
            for path in image_paths:
                # bytes = await asyncio.to_thread(TextRecognition.get_img_bytes, path)
                bytes = await self.get_img_bytes_s3(path)
                text = await asyncio.to_thread(TextRecognition.predict_ocr, self, path, bytes)
                text_by_image[path] = text

            return {"text_by_image": text_by_image}
