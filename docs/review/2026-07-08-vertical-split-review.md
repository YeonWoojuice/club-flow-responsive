# 기능(수직) 단위 분업 전환 검토 보고서

작성일: 2026-07-08 · 상태: **승인 대기 (아직 아무 파일도 수정하지 않음)**

## 요약

프론트와 백엔드가 도구별로 분업됐음에도 도메인 용어·API 경로·필드명은 잘 맞아 있음. 실제 문제는 코드보다 **계약의 단일 소스 부재**(타입 수동 중복)와 **규칙 문서의 도구 종속**(frontend-rules가 "claude sonnet용"으로 커밋됨, AGENTS.md 부재)에 있음. 기능 단위 전환의 핵심 작업은 폴더 대이동이 아니라 이 두 가지 해소임.

---

## 1. 컨벤션 분열 진단

### 1-1. 로드(조회) 경로에서 서버 에러 메시지 무시 — 중간

`docs/development/frontend-rules.md` §5는 "서버 오류의 `message`가 있으면 사용자에게 표시"를 요구. 쓰기 요청은 이를 지키지만(`requestError instanceof ApiError ? requestError.message : ...`), **모든 조회 경로는 고정 문구로 삼킴**:

- `frontend/src/pages/GenerationPage.tsx:37,47`
- `frontend/src/pages/ApplicationListPage.tsx:48`
- `frontend/src/pages/ApplicationDetailPage.tsx:25`
- `frontend/src/pages/MemberListPage.tsx:60`
- `frontend/src/pages/ClubListPage.tsx:19`
- `frontend/src/pages/ManualApplicationPage.tsx:38`

백엔드는 이미 사용자용 한국어 메시지를 정성껏 만들고 있음(`GlobalExceptionHandler`, 각 DTO validation 메시지). **권장: 백엔드 메시지 우선 스타일로 통일.** 근거 — 에러 문구의 단일 소스가 백엔드가 되어 프론트·백 중복 관리가 사라지고, 기능 단위 작업 시 에러 문구를 한 곳에서만 고치면 됨.

### 1-2. 에러 `code` 필드가 계약에만 있고 사용처 없음 — 낮음

백엔드는 `{code, message}`를 반환하고 `frontend/src/api/http.ts`의 `ApiError`도 `code`를 담지만, 어떤 페이지도 `code`로 분기하지 않음(HTTP status로만 분기). 반쪽짜리 계약. **권장: 당장 제거보다 "status로만 분기, code는 로깅용"이라고 규칙에 명시**하거나, `CONFLICT` 등 code 기반 분기로 통일. 두 도구가 각자 절반씩 구현한 전형적 흔적.

### 1-3. 네이밍: `ClubRole` vs `ClubStaffRole` — 낮음

- `frontend/src/types/club.ts` → `ClubRole`
- `backend/.../club/ClubStaffRole.java`, `docs/product/data-model.md` → `ClubStaffRole` / `club_staffs.role`

**권장: 백엔드(데이터 모델) 명칭으로 통일.** 도메인 모델 문서가 이미 `club_staffs` 기준. 또한 FE `Club` 타입은 실제로 Club+Staff 합성인 `ClubResponse`에 해당하므로 이름이 실체를 감춤(2-1의 타입 생성 도입 시 자동 해결).

### 1-4. 네이밍: 요청 타입 접미사 `*Input`(FE) vs `*Request`(BE) — 낮음

`CreateClubInput`/`CreateClubRequest`, `ManualApplicationInput`/`ManualApplicationRequest`, `CurrentUser`/`CurrentUserResponse`. 대응 규칙이 문서화되어 있지 않아 기능 단위 작업자가 매번 매핑을 추측해야 함. **권장: OpenAPI 생성 타입(§2-1) 도입으로 접미사 논쟁 자체를 제거.** 수동 유지 시엔 BE 접미사(`Request`/`Response`)로 통일 — 계약의 원본이 백엔드이므로.

### 1-5. 페이지 내 데이터 로딩 패턴 자체 분열 — 중간

`frontend/src/pages/GenerationPage.tsx`에 같은 목록 로딩이 두 벌 존재: `load()`(라인 32, 언마운트 가드 없음)와 `useEffect` 내 인라인(라인 42, `active` 플래그 가드 있음). 다른 페이지들은 가드 없음(`MemberListPage.tsx:58` 등). 도구 분업과 무관한 FE 내부 분열이지만, 기능 단위 전환 전에 "목록 로딩 표준 훅/패턴" 하나를 정해두지 않으면 기능별 작업자마다 또 갈라짐. **권장: `useEffect`+가드 패턴(또는 공용 훅)으로 통일.**

### 1-6. 되돌릴 수 없는 동작에 확인 절차 없음 — 중간

frontend-rules §7 "학기 종료, 지원자 합격·불합격·취소 … 확인 절차를 둔다"가 규칙인데, `grep confirm` 결과 0건. `GenerationPage.tsx`의 `handleClose`(학기 종료), `ApplicationDetailPage.tsx`의 `handleStatus`(최종 상태 변경) 모두 즉시 실행. 규칙과 구현의 괴리 — 규칙 문서가 특정 도구용 지시문으로 작성되다 보니 구현 검증 없이 규칙만 앞서간 사례. **권장: 구현을 규칙에 맞추거나, 규칙에서 해당 조항을 MVP 이후로 명시 이동.**

### 1-7. 문서 문체·구조 분열 — 낮음

`frontend-rules.md`(236줄, 합니다체, §0 지시 우선순위 있음, 커밋 메시지 "claude sonnet용 frontend 규칙으로 수정")와 `backend-rules.md`(97줄, 한다체, 우선순위 없음). 내용 충돌은 아니지만 문서가 "도구에게 주는 지시문"으로 각각 진화한 증거. **권장: §3의 통합안에서 두 문서 구조를 동일 골격(기준 파일 / 규칙 / 금지 / 검증)으로 정렬.**

---

## 2. 경계면(API 계약) 점검

먼저 좋은 소식: 엔드포인트 경로, 필드명, enum 값을 전수 대조한 결과 **현재 실제 어긋난 지점은 거의 없음.** 프론트 5개 API 모듈의 경로가 백엔드 4개 컨트롤러 + Security 필터(logout)와 모두 일치하고, 응답 필드도 일치함.

### 2-1. 타입 이중 선언, 단일 소스 부재 — 높음

- `frontend/src/types/{application,auth,club,generation,member}.ts` (수동 작성)
- `backend/.../*/dto/*.java` 13개 record + enum 7종 (`ApplicationStatus`, `GenerationStatus`, `ClubStaffStatus` 등)
- `frontend/src/api/http.ts:1` — `ErrorResponse`가 백엔드 `common/ErrorResponse.java`와 별도 3중 선언

같은 계약이 두세 곳에 손으로 쓰여 있고, `backend-rules.md`는 "계약의 기준은 `/v3/api-docs`"라 선언했지만 프론트는 이를 전혀 소비하지 않음. 지금까지 안 어긋난 건 두 도구가 성실했기 때문이지 구조 덕이 아님 — 기능 단위 전환 후 한 사람이 양쪽을 고치기 시작하면 가장 먼저 깨질 지점.

**제안 (단일 소스 고정):**
1. springdoc이 이미 있으므로 `openapi-typescript`로 `/v3/api-docs` → `frontend/src/types/api.gen.ts` 생성 (`npm run gen:api` 스크립트).
2. 기존 `src/types/*.ts`는 생성 타입의 재export로 축소 → import 경로 변경 없이 이행.
3. 한계 명시: Security 필터 처리 엔드포인트(`POST /api/auth/logout`, `/oauth2/**`)는 OpenAPI에 안 나오므로 `docs/auth/auth_flow.md`를 해당 부분의 공식 문서로 유지(backend-rules에 이미 이 규칙 있음).
4. 검증: CI 또는 로컬 스크립트에서 생성 결과 diff가 있으면 실패 처리.

### 2-2. `phone` 필수 여부 불일치 — 중간

- FE 요청: `frontend/src/types/application.ts` `ManualApplicationInput.phone: string` (필수)
- BE 요청: `ManualApplicationRequest.phone` — `@Size`만 있고 `@NotBlank` 없음 (선택)
- 응답/DB: `phone: string | null`, `persons.phone NULL 허용`

계약상 선택 필드를 프론트 타입이 필수로 강제. 빈 문자열 `""` 제출 시 DB에 `null`이 아닌 `""`가 저장될 수 있어 데이터 일관성도 애매해짐(이 부분은 `PersonService.findOrCreate`의 정규화 여부를 추가 확인 필요 — 의심 지점으로 남김). **권장: BE(선택) 기준으로 FE 타입을 `phone?: string`으로 맞추고, 빈 문자열→null 정규화 위치를 한 곳으로 결정.**

### 2-3. 상태 전이 규칙 3중 구현 — 중간

지원 상태 전이 규칙이 세 곳에 각각 존재:
- `backend/.../application/Application.java:72` `changeStatus()` (강제 주체)
- `frontend/src/pages/ApplicationDetailPage.tsx:42` `isTerminal` 하드코딩 배열
- `docs/product/requirements.md` "상태 정책" 절 (산문)

정책 변경 시 세 곳을 고쳐야 하고, 실제로 FE의 `isTerminal`은 BE `ApplicationStatus.isTerminal()`의 복제본. **권장: requirements.md를 정책의 원본으로, BE를 유일한 강제 주체로 명시하고, FE는 UI 편의용 파생임을 주석으로 표기.** (응답에 `allowedTransitions`를 포함하는 방안은 MVP엔 과함 — 후보로만 기록.)

### 2-4. 어긋나지 않았음을 확인한 항목 (기록용)

경로 전체 일치, `ClubResponse` ↔ `Club` 필드 일치, `CsrfTokenResponse{headerName,token}` ↔ `http.ts` CSRF 처리 일치, 403 시 CSRF 1회 재시도 규칙 ↔ `http.ts:78` 구현 일치, logout 204 ↔ FE `apiRequest<void>` 처리 일치.

---

## 3. 규칙 파일 통합

### 3-1. 현황 — 높음

- **AGENTS.md 없음.** Codex는 관례상 AGENTS.md를 읽는데 진입점이 없어, 백엔드 규칙은 CLAUDE.md/docs에 느슨하게 얹혀 있고 frontend-rules는 "claude sonnet용"으로 별도 진화(커밋 330446c). 도구별 생태계 분열의 뿌리.
- **기술 스택이 4곳에 중복**: `CLAUDE.md`, `README.md`, `docs/development/frontend-rules.md` §1, `docs/development/backend-rules.md` 기술 기준 표. 이미 표기가 어긋남 — CLAUDE.md "Spring Boot 4" vs backend-rules "4.0.7"; CLAUDE.md는 ESLint 10·Vitest 4 버전 명시, frontend-rules는 버전 없음.
- **CLAUDE.md 정보 부패**: 레포 구조에 `club_manage.pen`만 있으나 실제 루트엔 더 최신인 `club_flow_v2.pen`(7/6)과 정체불명 `club_project` 파일 존재. "UI 변경 시 club_manage.pen 우선" 원칙(CLAUDE.md AI 원칙 4)이 어느 pen 파일 기준인지 불명확하고, frontend-rules는 pen 파일을 아예 언급하지 않음 → 두 문서 간 실질 충돌.

### 3-2. 통합안

```
AGENTS.md                ← 단일 소스 (신규)
├── 프로젝트 개요·도메인 용어 (현 CLAUDE.md에서 이동)
├── 레포 구조 (pen 파일 최신화 포함)
├── 기술 스택 표 (유일본 — 다른 문서는 링크만)
├── 공통 작업 원칙 (영역 경계, 도메인 용어, 마이그레이션 불변 등)
├── 영역 규칙 링크 → docs/development/*.md (유지)
└── 검증 명령 (루트에서 FE+BE 전체)

CLAUDE.md                ← 축소: "@AGENTS.md를 따른다" + Claude 전용 예외만
README.md                ← 사람용 소개만, 스택 표는 AGENTS.md 링크
docs/development/*.md    ← 도구 중립 문체로 정렬, "claude sonnet용" 표현 제거
```

원리: 도구가 무엇이든 같은 문서를 읽게 하면 규칙이 도구별로 진화할 통로가 닫힘. CLAUDE.md는 import 참조(`@AGENTS.md`)만 남겨 중복 0을 유지.

### 3-3. 통합 시 정리할 충돌 목록

| 충돌 | 위치 | 해소안 |
|---|---|---|
| Spring Boot 버전 표기 | CLAUDE.md vs backend-rules.md | AGENTS.md 한 곳에만 버전 명시 |
| 디자인 파일 기준 | CLAUDE.md(club_manage.pen) vs 실제(club_flow_v2.pen) vs frontend-rules(무언급) | 사용자 확인 후 최신 pen을 기준으로 명시 |
| 지시 우선순위 체계 | frontend-rules §0에만 존재 | AGENTS.md 공통 절로 승격 |
| `club_project` 루트 파일 | 어느 문서에도 없음 | 정체 확인 후 문서화 또는 정리 |

---

## 4. 구조 개선 제안

### 4-1. 기능 단위 작업이 어려운 지점

한 기능(예: "학기 종료") 수정 시 건드리는 파일: `backend/.../generation/*`(응집 좋음 — BE는 이미 도메인 패키지) + FE에서는 `types/generation.ts` + `api/generations.ts` + `pages/GenerationPage.tsx` 3개 레이어 폴더로 분산. 다만 **FE 전체가 파일 43개 규모라 지금 `features/` 폴더로 재구성하는 건 과잉일 수 있음** — 의심 포인트로, §2-1(타입 생성)만 도입해도 "types 수동 동기화" 고통은 사라지므로 폴더 이동의 실익을 먼저 재평가할 것을 권함.

검증 절차도 이원화되어 있음: `./gradlew test` 와 `npm run lint && npm test && npm run build`를 각각 수동 실행. 기능 단위 작업자는 항상 양쪽을 돌려야 하므로 루트 단일 명령이 필요.

### 4-2. 마이그레이션 계획 (우선순위순)

| 순위 | 작업 | 심각도 대응 | 규모 |
|---|---|---|---|
| 1 | AGENTS.md 신설 + CLAUDE.md 참조화 + 스택 표 단일화 + pen 파일 기준 확정 (§3) | 높음 | 문서만, 반나절 |
| 2 | OpenAPI → TS 타입 생성 파이프라인 (§2-1) | 높음 | FE 빌드 스크립트 + 타입 재export |
| 3 | FE 에러 처리 통일: 조회 경로도 `ApiError.message` 우선, `code` 취급 규칙 명문화 (§1-1, 1-2) | 중간 | 페이지 6곳 소규모 수정 |
| 4 | `phone` 선택 필드 정합 + 상태 전이 원본 명시 (§2-2, 2-3) | 중간 | 타입 1곳 + 문서 |
| 5 | 확인 절차(confirm) 구현 또는 규칙 유예 명시, 로딩 패턴 표준화 (§1-5, 1-6) | 중간 | UI 소규모 |
| 6 | 루트 통합 검증 스크립트 (FE+BE 한 명령) | 낮음 | 스크립트 1개 |
| 7 | 네이밍 정렬(`ClubStaffRole` 등) — 2번 완료 시 자동 해소되는지 먼저 확인 | 낮음 | 조건부 |
| 보류 | FE `features/` 폴더 재구성 | 낮음 | 실익 재평가 후 |

### 스스로 의심한 지점 (검토 시 함께 판단해 주세요)

1. 실행 검증 없이 정적 분석만 수행 — `PersonService`의 phone 빈 문자열 처리(§2-2)는 코드 미확인 추정 포함.
2. OpenAPI 타입 생성은 백엔드 서버 기동이 전제(`/v3/api-docs`) — 빌드 시 서버 없이 스펙을 뽑는 `springdoc-openapi-gradle-plugin` 도입 여부는 별도 결정 필요.
3. `club_flow_v2.pen`이 정말 최신 기준 디자인인지는 파일 날짜로만 추정 — 사용자 확인 필요.
