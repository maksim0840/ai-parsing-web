docker compose -f docker-compose.garage.yml -p garage up


# узнать NODE_ID
docker exec -it garage /garage status

docker exec -it garage /garage layout assign -z dc1 -c 1G <NEW_NODE_ID>
docker exec -it garage /garage layout apply --version 1


# создать ключ и получить Key ID (access key) и Secret key (secret access key)
docker exec -it garage /garage key create my-app-key


# создаём бакеты
docker exec -it garage /garage bucket create garage-default-bucket
docker exec -it garage /garage bucket create garage-ttl-7d-bucket
docker exec -it garage /garage bucket allow --read --write --owner garage-default-bucket --key my-app-key
docker exec -it garage /garage bucket allow --read --write --owner garage-ttl-7d-bucket --key my-app-key

# задаём ttl на бакет garage-ttl-7d-bucket (правила из файла lifecycle-7d.json)
docker run --rm \
  --network host \
  -e AWS_ACCESS_KEY_ID='ТВОЙ_ACCESS_KEY' \
  -e AWS_SECRET_ACCESS_KEY='ТВОЙ_SECRET_KEY' \
  -e AWS_DEFAULT_REGION='garage' \
  -v "$PWD:/work" \
  -w /work \
  public.ecr.aws/aws-cli/aws-cli \
  s3api put-bucket-lifecycle-configuration \
  --bucket garage-ttl-7d-bucket \
  --lifecycle-configuration file://lifecycle-7d.json \
  --endpoint-url http://127.0.0.1:3900


