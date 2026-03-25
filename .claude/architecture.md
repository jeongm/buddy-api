# Architecture

Buddy 프로젝트의 도메인 설계 원칙 및 계층 아키텍처.
자주 바뀌지 않는 내용을 위주로 기록됨

---

## 1. 전체 구조

```
Controller  →  Service  →  Repository  →  DB / Redis / External API
     ↑               ↑
  Request DTO    Entity ↔ Response DTO
```

- **Controller**: HTTP 요청/응답 변환만 담당. 비즈니스 로직 금지.
- **Service**: 모든 비즈니스 로직 + 트랜잭션 경계. DTO ↔ Entity 변환.
- **Repository**: DB 접근만 담당. JPA / QueryDSL.
- **Entity**: 상태 변경은 도메인 메서드만. Setter 금지.

---

## 2. 도메인 관계도

```
Member
  ├── BuddyCharacter (N:1, nullable)
  ├── NotificationSetting (1:1)
  ├── OauthAccount (1:N)
  ├── MemberInsight (1:1)
  └── ChatSession (1:N)
         ├── BuddyCharacter (N:1)
         ├── ChatMessage (1:N)
         └── Diary (1:1, nullable)
                └── DiaryTag (1:N) → Tag (N:1)
```

### 핵심 연관관계 정책
| 관계 | 삭제 정책 | 이유 |
|------|-----------|------|
| Member 삭제 → ChatSession | CASCADE | 회원 데이터 완전 삭제 |
| Member 삭제 → Diary | CASCADE | 회원 데이터 완전 삭제 |
| ChatSession 삭제 → Diary.session_id | SET NULL | 일기는 세션 없어도 보존 |
| ChatSession 삭제 → ChatMessage | CASCADE | 메시지는 세션 종속 |
| Member 삭제 → BuddyCharacter FK | SET NULL | 캐릭터는 마스터 데이터 |
| Diary 삭제 → DiaryTag | CASCADE | 중간 테이블 정리 |

---

## 3. 인증/보안 플로우

```
Client
  │
  ├─[POST /api/v1/auth/login]──→ JWT 발급 (Access 1h + Refresh 14d)
  │
  └─[모든 인증 API 요청]
       │
       ↓
  JwtAuthenticationFilter
       │  Header: "Bearer {accessToken}"
       ↓
  JwtTokenProvider.validateToken()
       │
       ↓
  SecurityContextHolder.setAuthentication()
       │
       ↓
  Controller (@AuthenticationPrincipal CustomUserDetails)
```

- Access Token 만료 시 → `T002 EXPIRED_TOKEN` (401)
- Refresh Token으로 재발급: `POST /api/v1/auth/refresh`
- OAuth2 플로우: `auth/component/` 하위 provider별 verifier 처리

---

## 4. Redis 캐시 전략

```
Key   : chat:context:{sessionId}
Type  : List (최근 10개 메시지 유지)
TTL   : 24시간 (세션 활동 시 갱신)
크기  : 최근 10개 메시지 유지 (AI 컨텍스트 윈도우 절약)
```

- 세션 종료(`is_ended = true`) 시 Redis 캐시 삭제
- AI 호출 전 Redis → 없으면 DB 폴백

---

## 6. 스케줄러

| 스케줄러 | 주기 | 역할 |
|----------|------|------|
| `ChatRetentionScheduler` | 매시간 | 10시간 미활동 세션에 소멸 경고 FCM 발송 |
| `DailyPushScheduler` | 매일 지정 시간 | 데일리 안부 알림 발송 |
| `WeeklyInsightScheduler` | 매주 월요일 | 전주 일기 분석 → MemberInsight 업데이트 |

---

## 7. 외부 서비스 연동

| 서비스 | 도메인 | 용도 |
|--------|--------|------|
| OpenAI API | `ai/` | 대화 응답 + 일기 생성 + 주간 칭호 |
| Cloudinary | `diary/` | 일기 이미지 업로드/관리 |
| Firebase FCM | `global/infra/` | 푸시 알림 |
| Gmail SMTP | `auth/` | 이메일 인증 코드 발송 |

**외부 서비스는 항상 인터페이스로 추상화** → 테스트 시 Mock 교체 가능하게 유지.

---

