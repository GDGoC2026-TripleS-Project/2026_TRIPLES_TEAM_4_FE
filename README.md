# 2026_TRIPLES_TEAM_4_FE
 
---

# UNIMATE (TripleS Team 4 FE) 📅🤝🔔

팀플/스터디에서 **“언제 모일지”를 빠르게 합의**하고, 일정/할 일을 **팀 단위로 운영**할 수 있는 Android 앱입니다.  
**팀 스페이스 + 일정 관리 + 모이기(Timepick) + 찌르기 + 푸시 알림**을 하나의 흐름으로 연결했습니다.

---

## 📱 Demo Flow (핵심 화면 흐름)
1. **Splash** → (JWT 있으면 자동 로그인/동기화)  
2. **Login(소셜 로그인)** → **Profile 생성(최초 1회)** 3. **Team 생성/참가** 4. **Main(하단 탭)** - `Home`: 오늘 할 일 / 주간 요약  
   - `Calendar`: 월간 / 필터링 기능  
   - `Poke`: 팀원 찌르기(리마인드)  
   - `MyPage`: 내 정보 관리  
5. **TeamSpace** → 팀 단위 운영 및 **Timepick(모이기)** 생성/참여

---

## ✨ Key Features

### 1️⃣ 모이기 (Timepick)
- 여러 날짜 선택 + 날짜별 가능 시간 설정
- **30분 단위 슬롯 투표 UI**로 팀원이 가능한 시간을 시각적으로 선택
- 타임픽 생성/참여/확정/편집 플로우를 통한 일정 확정 시스템
- 푸시 알림 클릭 시 **해당 모이기 화면으로 즉시 이동**하여 시간 선택 및 일정 확정 가능

### 2️⃣ 팀 스페이스 (TeamSpace)
- 팀 일정 / 개인 일정 모드 토글 지원
- 선택 날짜 기준 **“일정 없는 팀원” 표시** → 효율적인 회의 시간 선정 지원

### 3️⃣ 일정/할일 UX
- **캘린더**: 팀 필터(Chip) + 개인 일정 토글 기능
- **Private 모드**: 개인 일정 잠금 지원 (팀원에게는 카테고리명으로만 노출)
- **홈 화면**: 오늘 할 일을 팀별로 그룹화하여 **체크리스트** 형태로 제공

### 4️⃣ 푸시 알림 (FCM)
- Android 13+ 알림 권한 대응
- 인앱 알림함 저장 및 표시 시스템 구축
- 알림 페이로드(Payload) 기반 **딥 네비게이션**: 클릭 시 캘린더/모이기/찌르기 등 관련 화면으로 바로 연결

### 5️⃣ 찌르기 (Poke) — 팀 리마인드 기능
- 팀원 선택 후 원클릭 푸시 전송으로 참여 유도
- 상황별 **프리셋 문구(드롭다운)** 지원을 통한 빠른 UX 제공
- 전송 이력 추적: 누가, 언제, 어떤 내용으로 리마인드했는지 확인 가능

---
 
## 📝 개발 규칙(Convention)
 
 
### 🛠 태그 (커밋&PR 공통)
- **feat**: 신규 기능 추가
- **fix**: 버그 수정
- **chore**: 환경 설정, 라이브러리 추가, 폴더 구조 변경 (이슈 번호 생략 가능)
- **docs**: 문서 수정 (README 등)
- **style**: 기능 변경 없는 시각적 스타일 개선 (오타 수정, 들여쓰기 등)
- **refactor**: 코드 리팩토링 (기능 변경 없는 구조 개선)
   
---
  
### ✍️ 작성 양식
- **커밋 메세지 / PR 제목** : 태그명: 기능 내용 (#이슈번호)
- **브랜치명** : 태그명/#이슈번호-기능명
  
---

## 🛠 Tech Stack
- **Language**: Kotlin  
- **UI**: Fragment 기반 (Single Activity) + ViewBinding  
- **Architecture**: Repository / Store 기반 로컬 캐시 + 서버 동기화  
- **Network**: Retrofit2, OkHttp, Gson  
- **Async**: Kotlin Coroutines  
- **Image**: Glide  
- **Push**: Firebase Cloud Messaging(FCM)  
- **Navigation**: Jetpack Navigation Component

---

## 📂 Project Structure
```text
app/
├─ ui/           # Activity, Fragment, Adapter 등 화면 레이어
├─ data/
│  ├─ entity/    # 도메인 모델 (Data Class)
│  ├─ repository/ # 로컬/서버 데이터 처리 로직
│  └─ store/      # JwtStore, NotificationStore 등 로컬 저장소
├─ network/      # Retrofit client, API service, DTO
├─ notification/ # FCM 수신 및 Notification 생성 처리
└─ utils/        # 공통 유틸리티 (이미지 로더, 날짜 변환 등)
