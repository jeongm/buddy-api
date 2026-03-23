<div align="center">
  [Buddy 로고 및 메인 서비스 소개 배너 이미지 추가 예정 (화이트 & #7C3AED 포인트 컬러 활용)]

  <h1>💜 Buddy (버디)</h1>
  <p><b>"대화하다 보니 일기가 남았다."</b><br>나를 가장 잘 이해하는 AI 친구와의 대화, 그리고 자동으로 기록되는 감정 일기</p>

  <img src="https://img.shields.io/badge/Spring%20Boot-6DB33F?style=flat-square&logo=Spring%20Boot&logoColor=white"/>
  <img src="https://img.shields.io/badge/React%20Native-61DAFB?style=flat-square&logo=React&logoColor=black"/>
  <img src="https://img.shields.io/badge/MySQL-4479A1?style=flat-square&logo=MySQL&logoColor=white"/>
</div>

<br>

## 📖 프로젝트 소개 (About)

**"일기가 목표(Goal)라면, 대화는 수단(Experience)입니다."**

Buddy는 바쁘고 지친 현대인들이 부담 없이 하루를 기록할 수 있도록 돕는 **AI 감정 다이어리 서비스**입니다. 빈 화면에 스스로 일기를 써 내려가는 막막함 대신, 나만의 AI 캐릭터와 가볍게 수다를 떠는 경험(Chat-Centric)을 제공합니다.

유저가 Buddy와 일상적인 대화를 나누고 나면, AI가 대화의 맥락을 분석하여 한 편의 정돈된 일기로 완성해 줍니다. **유저는 그저 친구와 대화하기 위해 앱을 켜고, 차곡차곡 쌓인 나만의 감정 기록들로 인해 서비스에 깊은 애착을 느끼게 됩니다.**

<br>

## ✨ 핵심 기능 (Core Features)

### 1. 🗣️ 나를 반겨주는 AI 친구와의 교감 (The Hook)
[캐릭터가 화면 중앙에서 유저를 반겨주고, 실시간으로 음성/텍스트 대화를 나누는 메인 홈 및 채팅 UI 화면 스크린샷 또는 GIF 추가 예정]

* **음성/텍스트 하이브리드 대화:** 앱을 켜자마자 내 캐릭터가 먼저 "오늘 하루는 어땠어?"라고 말을 건넵니다.
* **실시간 인터랙션:** 단순한 챗봇을 넘어, 사용자의 말에 공감하고 반응하는 캐릭터와의 대화를 통해 자연스럽게 감정을 쏟아낼 수 있습니다.
* **Seamless Context:** 세션이 유지되는 동안 대화의 흐름을 잃지 않고 끊김 없는 소통이 가능합니다.

### 2. 📝 대화가 일기가 되는 마법 (The Lock-in)
[대화 종료 후, AI가 대화 내용을 분석해 일기 초안(제목, 본문, 감정 태그)을 자동으로 생성하고 작성 페이지로 넘어가는 플로우 GIF 추가 예정]

* **맥락 기반 자동 일기 생성 (OpenAI API):** 대화를 종료하면, AI가 오늘 나눈 이야기를 바탕으로 일기의 제목, 본문, 그리고 감정 태그(#일상, #위로 등)를 자동으로 추출하여 초안을 작성합니다.
* **유연한 기록 관리:** AI가 작성해 준 일기를 바탕으로 유저가 직접 내용을 수정하거나 사진을 추가하여 나만의 온전한 기록으로 완성할 수 있습니다.

### 3. 📊 내 마음을 돌아보는 감정 캘린더
[날짜별로 감정 태그가 표시된 캘린더 뷰와 주간/월간 감정 리포트가 보이는 상세 화면 스크린샷 추가 예정]

* **월간 감정 트래커:** 매일 쌓인 일기와 감정 태그를 캘린더 뷰를 통해 한눈에 확인할 수 있습니다.
* **감정 리포트:** 단순히 텍스트가 쌓이는 것을 넘어, 나의 주된 감정 패턴을 시각적으로 확인하며 스스로를 회고할 수 있는 가치를 제공합니다.

<br>

## 🛠 기술 스택 (Tech Stack)

### Backend
* **Framework:** Spring Boot 3.x, Java 17
* **Database:** MySQL 8.0, Spring Data JPA
* **Security:** Spring Security, JWT, OAuth 2.0 (Google, Naver, Kakao)
* **AI Integration:** OpenAI API (GPT-4o)
* **Test:** JUnit5, Mockito (@DataJpaTest, @WebMvcTest, @SpringBootTest)
* **API Docs:** Swagger UI, Notion

### Frontend
* **Client:** React Native, React.js (v18+)
* **State Management:** Zustand
* **HTTP Client:** Axios (인터셉터를 통한 JWT 검증 및 에러 핸들링)
* **Styling:** Tailwind CSS

<br>

## 🏗 시스템 아키텍처 (System Architecture)
[클라이언트(React Native) - 서버(Spring Boot) - 데이터베이스(MySQL) 및 외부 API(OpenAI, OAuth) 간의 전체 통신 흐름을 보여주는 아키텍처 다이어그램 이미지 추가 예정]

* **인증 인가:** OAuth 2.0과 JWT를 활용하여 보안성 높은 로그인 구현 및 토큰 기반(Access/Refresh) API 접근 제어.
* **AI 연동:** 프론트엔드에서 백엔드 서버를 거쳐 OpenAI API와 통신하여 API Key 노출을 방지하고 비즈니스 로직(프롬프트 엔지니어링)을 서버에서 중앙 제어.
* **RESTful API:** 일관된 응답 포맷(`ApiResponse<T>`)과 HTTP 상태 코드를 활용하여 클라이언트와 명확한 통신 규격 확립.
<br>

## 🗄 데이터베이스 설계 (ERD)

[유저(User), 캐릭터(Character), 채팅 세션(Session), 메시지(Message), 일기(Diary) 테이블 간의 1:N, N:M 연관 관계가 명확히 보이는 ERD 캡처 이미지 추가 예정]

* **User & Character:** 유저의 소셜 연동 정보와 선택한 AI 캐릭터 정보를 효율적으로 분리하여 관리.
* **Chat Context:** 대화 세션 단위로 메시지 이력을 관리하여 AI가 이전 대화의 문맥을 기억할 수 있도록 구조화.
* **Diary & Tags:** AI가 생성한 일기와 감정 태그를 정규화하여, 향후 캘린더 리포트 및 감정 통계 쿼리 성능 최적화 고려.
<br>

## 📁 패키지 구조 (Directory Structure)

도메인 주도 설계(DDD) 개념을 차용하여, 각 도메인별로 응집도를 높이고 의존성을 최소화하는 구조를 채택했습니다.

```text
com.buddy.api
 ├─ global              # 공통 예외 처리, 보안, 설정(Config), 응답 포맷 등
 │   ├─ config
 │   ├─ error
 │   └─ security
 ├─ domain              # 핵심 비즈니스 도메인
 │   ├─ user            # 회원 가입, 로그인, OAuth, 캐릭터 설정
 │   ├─ diary           # 일기 CRUD, 캘린더 조회
 │   └─ chat            # 채팅 세션 관리, 메시지 송수신, OpenAI 연동
 ...
```

<br>

## 📜 API 명세 (API Documentation)
프론트엔드와의 원활한 협업을 위해 Swagger UI를 적용하여 API 명세서를 자동화 및 관리하고 있습니다. 모든 API는 커스텀 에러 코드(ResultCode)를 통해 일관된 예외 상황을 클라이언트에 전달합니다.

[Swagger UI로 자동 생성된 전체 API 엔드포인트 목록과 응답 포맷이 잘 보이는 스크린샷 이미지 추가 예정]

- 주요 엔드포인트:

    - POST /api/v1/chat/messages : 메시지 전송 및 AI 응답 수신

    - POST /api/v1/diaries/from-chat : 세션 기반 AI 일기 초안 자동 생성

    - GET /api/v1/calendar/diaries : 월별 일기 기록 및 감정 태그 캘린더 조회


## 로컬 환경 설정
1. `application-dev.yml.example` → `application-dev.yml` 으로 복사
2. 본인 로컬 DB 정보로 수정
3. IntelliJ Run Configuration → Active profiles: `dev`