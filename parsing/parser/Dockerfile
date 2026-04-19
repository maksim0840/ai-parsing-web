FROM python:3.12.3-slim
WORKDIR /app

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

COPY parser/requirements.txt /app/parser/requirements.txt
RUN pip install --no-cache-dir -r /app/parser/requirements.txt \
 && python -m playwright install --with-deps chromium

COPY parser ./parser
COPY common ./common

CMD ["python3", "-m", "parser.src.main"]
