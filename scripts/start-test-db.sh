#!/bin/bash
# scripts/start-test-db.sh

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

echo "Starting test MongoDB container..."
# docker compose -f "${SCRIPT_DIR}/../docker/docker-compose-test.yml" up -d
docker compose -f "${SCRIPT_DIR}/../docker/docker-compose-test.yml" up -d mongodb-test

# Wait for MongoDB to be ready
echo "Waiting for MongoDB to be ready..."
sleep 5

echo "Test MongoDB is ready!"
