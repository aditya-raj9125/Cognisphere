#!/usr/bin/env bash
set -euo pipefail

echo "Starting CogniSphere Backend script..."
cd "$(dirname "$0")/.."

cd backend
chmod +x mvnw

# On CI (GitHub Actions) we should build the package instead of running the app.
if [ "${GITHUB_ACTIONS:-}" = "true" ]; then
  ./mvnw clean package -DskipTests -ntp
else
  ./mvnw spring-boot:run -DskipTests -ntp
fi
