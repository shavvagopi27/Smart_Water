#!/usr/bin/env bash
# Runs the Spring Boot backend on your machine against a local PostgreSQL (see README "Running PostgreSQL").
set -euo pipefail
export DB_URL="${DB_URL:-jdbc:postgresql://localhost:5432/smartwater}"
export DB_USERNAME="${DB_USERNAME:-smartwater}"
export DB_PASSWORD="${DB_PASSWORD:-local_demo_db_password}"          # local demo value
export JWT_SECRET="${JWT_SECRET:-local-demo-only-jwt-secret-please-change-0123456789}"   # local demo value
export SEED_DEMO_DATA="${SEED_DEMO_DATA:-true}"
cd "$(dirname "$0")/../backend"
mvn spring-boot:run
