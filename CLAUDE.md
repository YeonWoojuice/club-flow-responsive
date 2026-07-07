# Club Flow — AI 개발 가이드

## 프로젝트 개요

**Club Flow**는 대학 동아리 운영진을 위한 웹 기반 관리 툴입니다.
현재 MVP는 학기별 지원자 수동 등록·심사와 부원 이력 관리를 제공하며, Google Form 연동과 활동 기록은 후속 범위입니다.

---

## 레포지토리 구조

```
club-flow-responsive/
├── frontend/              # React 19 + TypeScript + Vite + Tailwind CSS 4
├── backend/               # Spring Boot 4 + Java 21 + JPA + Flyway + PostgreSQL
├── Infra/
│   └── docker-compose.yml # 로컬 개발용 PostgreSQL 컨테이너
├── club_manage.pen        # Pencil 디자인 파일 (pencil MCP 도구로만 접근)
├── docs/
│   ├── auth/              # Google 로그인과 동아리 진입 흐름
│   ├── product/           # 요구사항과 데이터 모델
│   └── development/       # 프론트엔드·백엔드·인프라 코딩 규칙
└── CLAUDE.md              # 이 파일
```

---

## 핵심 도메인 개념

| 개념 | 설명 |
|------|------|
| **Generation (학기)** | 동아리 활동 단위(예: 26-1 학기). 상태: `ACTIVE` / `CLOSED` |
| **Person** | 이메일로 식별되는 고유 인물. 여러 학기에 걸쳐 재사용됨 |
| **Application** | 특정 학기의 지원 기록. 현재는 수동 등록을 지원하고 Google Form 연동은 보류됨 |
| **GenerationMember** | 특정 인물의 특정 학기 참여 기록 |
| **ClubStaff** | 동아리에 접근할 수 있는 운영진 권한과 승인 상태 |

---

## 기술 스택 요약

### 프론트엔드
- React 19, TypeScript 5.8, Vite 7
- Tailwind CSS 4 (`@tailwindcss/vite` 플러그인 방식)
- React Router 7 기반 URL 라우팅
- 상태 관리 라이브러리 없음 — React Context와 컴포넌트 상태 사용
- ESLint 10, Vitest 4, Testing Library
- 폰트: `font-body` (Inter/Pretendard), `font-data` (Geist Mono/IBM Plex Mono)
- 디자인 토큰: `styles.css`의 CSS Custom Properties (`--navy`, `--text-primary` 등)

### 백엔드
- Spring Boot 4.0.7, Java 21
- Spring Data JPA, Spring Web, Spring Validation, Spring Actuator, springdoc OpenAPI
- Flyway (DB 마이그레이션)
- PostgreSQL (런타임), Testcontainers (테스트)
- 빌드: Gradle (Groovy DSL)
- 패키지 루트: `com.clubflow.backend`

### 인프라
- Docker Compose: PostgreSQL 18 단일 서비스
  - DB: `clubflow`, 유저: `clubflow_user`, 호스트 포트: `15432`, 컨테이너 포트: `5432`
- 프론트엔드 개발 서버: `vite --host 0.0.0.0` (포트 5173)
- 백엔드 개발 서버: `./gradlew bootRun` (포트 8080)

---

## 영역별 규칙

코드를 작성하기 전에 반드시 해당 영역의 규칙 문서를 확인하세요.

- **프론트엔드** → [`docs/development/frontend-rules.md`](docs/development/frontend-rules.md)
- **백엔드** → [`docs/development/backend-rules.md`](docs/development/backend-rules.md)
- **인프라** → [`docs/development/infra-rules.md`](docs/development/infra-rules.md)

---

## 로컬 개발 시작

```bash
# 1. DB 실행
docker compose -f Infra/docker-compose.yml up -d postgres

# 2. 백엔드 실행 (별도 터미널)
cp -n backend/.env.example backend/.env.local  # 최초 1회 후 OAuth 값 입력
(cd backend && ./gradlew bootRun)

# 3. 프론트엔드 실행 (별도 터미널)
(cd frontend && npm run dev)
```

### 변경 검증

```bash
(cd backend && ./gradlew test)
(cd frontend && npm run lint && npm test && npm run build)
```

---

## AI 작업 원칙

1. **영역 경계를 지킨다** — 프론트는 프론트 규칙, 백엔드는 백엔드 규칙만 따른다.
2. **도메인 용어를 유지한다** — 변수명·API 경로·필드명에 실제 모델 용어를 사용한다(`generation`, `application`, `generationMember`, `person`).
3. **현재 패턴을 먼저 파악한다** — 새 기능을 추가하기 전에 기존 코드에서 패턴을 찾아 일관되게 따른다.
4. **UI 변경 시 디자인 파일 우선** — `club_manage.pen`을 pencil MCP로 확인하고 스펙을 맞춘다.
5. **마이그레이션은 되돌릴 수 없다** — Flyway SQL 파일을 한 번 커밋하면 절대 수정하지 않는다.
