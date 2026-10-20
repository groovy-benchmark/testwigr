#!/bin/bash
# scripts/start-test-db.sh

echo "Starting test MongoDB container..."
docker compose -f ../docker/docker-compose-test.yml down

echo "Test MongoDB is down!"
