
# Auth / Club Creation Flow

## 목표
Google 로그인 이후 사용자가 ClubFlow에 자동 가입하고,
동아리를 생성하면 자동으로 해당 동아리의 PRESIDENT 권한을 가진다.

## 구현 범위
- Google 로그인
- users 자동 생성/조회
- clubs 생성
- club_staffs 생성
- 로그인 후 접근 가능한 동아리 조회
- 동아리가 없으면 동아리 생성 화면으로 이동
- 동아리가 있으면 대시보드로 이동

## 테이블
### users
- id UUID PK
- google_sub VARCHAR UNIQUE
- email VARCHAR UNIQUE
- name VARCHAR
- phone VARCHAR NULL
- profile_image_url TEXT
- created_at TIMESTAMPTZ
- last_login_at TIMESTAMPTZ

### clubs
- id UUID PK
- name VARCHAR
- description TEXT
- created_by_user_id UUID FK -> users.id
- created_at TIMESTAMPTZ
- updated_at TIMESTAMPTZ

### club_staffs
- id UUID PK
- club_id UUID FK -> clubs.id
- user_id UUID FK -> users.id
- role VARCHAR
- status VARCHAR
- created_at TIMESTAMPTZ
- updated_at TIMESTAMPTZ

## 핵심 정책
- 첫 Google 로그인 시 users에 없으면 자동 생성한다.
- 동아리 생성자는 자동으로 PRESIDENT가 된다.
- club_staffs.status = APPROVED 인 경우만 접근 가능한 동아리로 본다.
- 같은 사용자가 같은 동아리에 중복 등록되지 않도록 UNIQUE(club_id, user_id)를 둔다.