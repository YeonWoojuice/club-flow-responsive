# 인프라 개발 규칙

> 현재 로컬 개발 환경과 `Infra/docker-compose.yml`에 적용합니다.

## 현재 구성

| 구성요소 | 실행 방식 | 포트 |
|---|---|---|
| PostgreSQL 18 | Docker Compose | 호스트 15432 → 컨테이너 5432 |
| Spring Boot | 로컬 JVM | 8080 |
| Vite | 로컬 Node.js | 5173 |

DB만 컨테이너로 실행하고 백엔드와 프론트엔드는 로컬에서 직접 실행합니다.

## Docker Compose

현재 기준:

```yaml
services:
  postgres:
    image: postgres:18
    container_name: clubflow-postgres
    ports:
      - "15432:5432"
    environment:
      POSTGRES_DB: clubflow
      POSTGRES_USER: clubflow_user
      POSTGRES_PASSWORD: clubflow_pass
    volumes:
      - postgres_data:/var/lib/postgresql

volumes:
  postgres_data:
```

- 이미지 버전을 명시하고 `latest`를 사용하지 않습니다.
- 컨테이너 이름은 `clubflow-{서비스명}` 형식을 사용합니다.
- 데이터는 named volume에 저장합니다.
- 로컬 PostgreSQL 5432와 충돌하지 않도록 호스트 15432를 유지합니다.

## 환경변수

백엔드는 `backend/.env.local`을 사용하고 예시는 `backend/.env.example`에 둡니다.

```text
DB_URL=jdbc:postgresql://127.0.0.1:15432/clubflow
DB_USERNAME=clubflow_user
DB_PASSWORD=clubflow_pass
```

- `.env.local`은 커밋하지 않습니다.
- 실제 OAuth Client Secret을 저장소에 기록하지 않습니다.
- 공유할 설정 키가 바뀌면 `.env.example`도 함께 수정합니다.

## 실행

```bash
# DB
docker compose -f Infra/docker-compose.yml up -d postgres

# 백엔드
(cd backend && ./gradlew bootRun)

# 프론트엔드
(cd frontend && npm run dev)
```

DB 준비 상태는 다음으로 확인합니다.

```bash
docker exec clubflow-postgres pg_isready -U clubflow_user -d clubflow
```

## 종료와 초기화

```bash
# 데이터 유지
docker compose -f Infra/docker-compose.yml down

# 로컬 DB 데이터까지 삭제
docker compose -f Infra/docker-compose.yml down -v
```

`down -v`는 로컬 개발 데이터를 모두 삭제하므로 초기화가 필요한 경우에만 실행합니다.

## 변경 검증

```bash
docker compose -f Infra/docker-compose.yml config
(cd backend && ./gradlew test)
(cd frontend && npm run lint && npm test && npm run build)
```

배포용 Dockerfile, CI/CD, AWS 또는 Kubernetes 설정은 실제 구현을 시작할 때 별도 문서로 추가합니다.
