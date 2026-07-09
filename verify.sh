#!/usr/bin/env bash
# 기능 단위 작업 후 전체 검증 (AGENTS.md "변경 검증" 참조)
# 사용법: ./verify.sh [backend|frontend]  — 인자 없으면 둘 다 실행
set -euo pipefail
cd "$(dirname "$0")"

target="${1:-all}"

if [[ "$target" == "backend" || "$target" == "all" ]]; then
  echo "▶ backend: ./gradlew test"
  (cd backend && ./gradlew test)
fi

if [[ "$target" == "frontend" || "$target" == "all" ]]; then
  echo "▶ frontend: lint / test / build"
  (cd frontend && npm run lint && npm test && npm run build)
fi

echo "✅ verify 통과 ($target)"
