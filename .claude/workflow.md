# Workflow

Branch strategy and commit conventions for the Buddy project.
---

## 1. Branch Strategy

```
main
└── develop
    ├── feature/{task}
    ├── fix/{task}
    ├── refactor/{task}
    └── hotfix/{task}
```

| Branch | Purpose |
|--------|---------|
| `main` | Production deployment. Direct push is forbidden. |
| `develop` | Integration branch. All feature branches merge here. |
| `feature/{task}` | New feature development |
| `fix/{task}` | Bug fix |
| `refactor/{task}` | Refactoring (no functional change) |
| `hotfix/{task}` | Urgent production fix |

### Branch Naming
```
feature/chat-send-message
feature/diary-ai-generate
fix/jwt-expired-token
refactor/diary-service
hotfix/chat-npe
```

---

## 2. Commit Message Format

```
{type}: {subject} (50자 이내)
 
{body - optional}
- 무엇을, 왜 변경했는지 설명
- 줄당 72자 이내
 
{footer - optional}
Closes #이슈번호
```

### Commit type
| Type | Meaning | Example |
|------|---------|---------|
| `feat` | New feature | `feat: 채팅 메시지 전송 API 구현` |
| `fix` | Bug fix | `fix: 만료된 세션 조회 시 NPE 수정` |
| `refactor` | Refactoring | `refactor: DiaryService 메서드 분리` |
| `test` | Add or update tests | `test: DiaryService 단위 테스트 추가` |
| `docs` | Documentation update | `docs: API 명세 주석 보완` |
| `chore` | Build or config change | `chore: Redis 설정 추가` |
| `perf` | Performance improvement | `perf: 일기 목록 쿼리 Fetch Join 적용` |

## 3. Commit Timing

```
✅ Commit when
- A single feature or fix is fully working
- No compile errors
- Tests pass
 
❌ Do not commit when
- Build is broken
- TODO or temporary code remains
- Sensitive info (API keys, passwords) is included
```
