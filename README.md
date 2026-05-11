# ai-parsing-web

создаём сети
```
docker network create garage-net
docker network create rabbitmq-net
docker network create grpc-orchestrator-api-net
docker network create grpc-results-api-net
docker network create grpc-users-api-net
```

запускаем сервисы
```
cd garage
docker compose -f docker-compose.garage.yaml -p garage up

# orchestrator + mongodb_orchestrator + rabbitmq
cd parsing-task-orchestrator-microservice
docker compose -f docker-compose.orchestrator.yaml -p orchestrator up

cd parsing
docker compose -f docker-compose.parser.yaml -p parser up
docker compose -f docker-compose.recognition.yaml -p recognition up

# results + mongodb-results
cd extraction-results-microservice
docker compose -f docker-compose.results.yaml -p results up

# users + postgresql_users
cd users-info-microservice
docker compose -f docker-compose.users.yaml -p users up

cd api-gateway-microservice
docker compose -f docker-compose.api.yaml -p api up

cd frontend
npm run dev
```
