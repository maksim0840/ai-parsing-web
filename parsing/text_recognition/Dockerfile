FROM python:3.12.3-slim
WORKDIR /app

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

RUN apt-get update && apt-get install -y --no-install-recommends \
    libgomp1 \
    libgl1 \
    libglib2.0-0 \
    libcairo2 \
    libffi-dev \
    python3-dev \
 && rm -rf /var/lib/apt/lists/*
 
COPY text_recognition/requirements.txt /app/text_recognition/requirements.txt
RUN pip install --no-cache-dir -r /app/text_recognition/requirements.txt

COPY text_recognition ./text_recognition
COPY common ./common

CMD ["python3", "-m", "text_recognition.src.main"]
