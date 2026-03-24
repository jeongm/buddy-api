# Workflow

Buddy 프로젝트의 브랜치 전략 및 커밋 컨벤션.

---

## 1. 브랜치 전략 (GitHub Flow 기반)

```
main
└── develop
    ├── feature/{작업명}
    ├── fix/{작업명}
    ├── refactor/{작업명}
    └── hotfix/{작업명}
```

| 브랜치 | 용도 |
|--------|------|
| `main` | 프로덕션 배포. 직접 push 금지. |
| `develop` | 통합 개발 브랜치. feature 머지 대상. |
| `feature/{작업명}` | 새 기능 개발 |
| `fix/{작업명}` | 버그 수정 |
| `refactor/{작업명}` | 리팩토링 (기능 변경 없음) |
| `hotfix/{작업명}` | 프로덕션 긴급 수정 |

### 브랜치 네이밍
```bash
feature/chat-send-message
feature/diary-ai-generate
fix/jwt-expired-token
refactor/diary-service
hotfix/chat-npe
```

---

## 2. 커밋 컨벤션

```
{타입}: {제목} (50자 이내)

{본문 - 선택사항}
- 무엇을, 왜 변경했는지 설명
- 줄당 72자 이내

{푸터 - 선택사항}
Closes #이슈번호
```

### 커밋 타입
| 타입 | 의미 | 예시 |
|------|------|------|
| `feat` | 새 기능 추가 | `feat: 채팅 메시지 전송 API 구현` |
| `fix` | 버그 수정 | `fix: 만료된 세션 조회 시 NPE 수정` |
| `refactor` | 리팩토링 | `refactor: DiaryService 메서드 분리` |
| `test` | 테스트 추가/수정 | `test: DiaryService 단위 테스트 추가` |
| `docs` | 문서 수정 | `docs: API 명세 주석 보완` |
| `chore` | 빌드/설정 변경 | `chore: Redis 설정 추가` |
| `perf` | 성능 개선 | `perf: 일기 목록 쿼리 Fetch Join 적용` |

### 커밋 시점 가이드
```
✅ 커밋해야 할 시점
- 하나의 기능/수정이 완전히 동작할 때
- 테스트가 통과했을 때
- 컴파일 에러가 없을 때

❌ 커밋하면 안 되는 시점
- 빌드가 깨진 상태
- TODO/임시 코드가 남아있는 상태
- 민감 정보(API Key, 비밀번호)가 포함된 상태
```
