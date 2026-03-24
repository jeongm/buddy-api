# CLAUDE.md

Buddy 프로젝트 AI 어시스턴트 가이드.
상세 규칙은 아래 파일들을 참고한다.

---

## 참고 문서

- **[code_convention.md](.claude/code_convention.md)** — 네이밍, Lombok, Javadoc, 예외처리, API 응답 규칙
- **[workflow.md](.claude/workflow.md)** — 브랜치 전략, 커밋 컨벤션

---

## 핵심 설계 및 코딩 원칙

### 1. SOLID 원칙 & Clean Code
- 유지보수성과 가독성이 뛰어난 최적의 코드를 작성한다.
- 메서드는 하나의 책임만 가진다. 길어지면 private 메서드로 분리한다.
- 매직 넘버/문자열은 상수로 추출한다.

### 2. 완벽한 계층 분리
- **Controller**: HTTP 요청/응답 변환만 담당. 비즈니스 로직 작성 금지.
- **Service**: 모든 비즈니스 로직과 트랜잭션 경계. DTO ↔ Entity 변환.
- **Repository**: DB 접근만 담당. 비즈니스 로직 작성 금지.
- **Entity**: 외부로 직접 노출 금지. 반드시 DTO로 변환 후 반환.

### 3. 성능 최적화
- N+1 문제 방지: 연관 엔티티 조회 시 Fetch Join 또는 `@EntityGraph` 사용.
- 불필요한 DB 쿼리 최소화: 같은 데이터를 두 번 조회하지 않는다.
- 복잡한 동적 쿼리는 QueryDSL 사용 (`DiaryRepositoryCustom` 패턴 참고).
- 페이징 처리 시 `Pageable` 또는 커서 기반 페이징 사용.

---

## 코드 작성 규칙

### Javadoc 필수
모든 public 메서드에 Javadoc 작성. 파라미터, 반환값, 예외 명시.

```java
/**
 * 채팅 세션 ID로 일기를 생성한다.
 *
 * @param memberId  요청 회원 PK
 * @param sessionId 대상 채팅 세션 PK
 * @return 생성된 일기 응답 DTO
 * @throws BaseException SESSION_NOT_FOUND  - 세션이 존재하지 않을 때
 * @throws BaseException EMPTY_CHAT_HISTORY - 대화 내역이 없을 때
 */
```

### 예외 처리
`BaseException + ResultCode` 조합만 사용. 직접 `RuntimeException` 던지기 금지.

```java
// ✅
.orElseThrow(() -> new BaseException(ResultCode.USER_NOT_FOUND));
 
// ❌
throw new RuntimeException("사용자를 찾을 수 없습니다.");
```

### Lombok
- Entity: `@Getter`, `@NoArgsConstructor(access = PROTECTED)` 사용.
- `@Setter`, `@Data` 금지

---

## 코드 리뷰 규칙

코드 리뷰 요청 시 아래 순서와 형식을 반드시 따른다.

### 1. 버그 탐지 우선
리뷰 시 버그 가능성, 예외 처리 누락, NPE 위험 등 위험한 부분을 가장 먼저 짚는다.

### 2. Before / After 비교
코드 개선 시 반드시 수정 전/후를 함께 보여준다.

```java
// Before
Member member = memberRepository.findById(memberId).get(); // NPE 위험
 
// After
Member member = memberRepository.findById(memberId)
        .orElseThrow(() -> new BaseException(ResultCode.USER_NOT_FOUND));
```

### 3. 피드백 형식
각 지적 사항은 아래 형식으로 작성한다.

```
[문제점] 어떤 문제가 있는지
[개선 이유] 왜 이 방식으로 수정해야 하는지 (원칙, 성능 등 근거)
[기대 효과] 개선 후 어떤 부분이 좋아지는지
```

---

## Stack

- **Spring Boot 3.4** + **Java 17**
- **MariaDB** (prod) / **H2** (dev/test)
- **Redis** — 채팅 컨텍스트 캐싱 (최근 10개 메시지)
- **QueryDSL** — 복잡한 동적 쿼리
- **Flyway** — DB 마이그레이션 (prod only)
- **OpenAI API** — 대화/일기 생성/주간 칭호
- **Firebase FCM** — 푸시 알림
- **Cloudinary** — 이미지 스토리지

---

## Commands

```bash
# Run application (dev profile uses H2 in-memory DB)
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=DiaryServiceTest

# Run a specific test method
./mvnw test -Dtest=DiaryServiceTest#saveDiary_Success

# Build JAR (skip tests)
./mvnw clean package -DskipTests
```
Swagger UI: `http://localhost:8080/swagger-ui.html`
