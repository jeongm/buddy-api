# CLAUDE.md

AI assistant guide for the Buddy project.
See the reference documents below for detailed rules.

---

## Reference Documents

- **[code_convention.md](.claude/code_convention.md)** — Naming, Lombok, Javadoc, exception handling, API response rules
- **[workflow.md](.claude/workflow.md)** — Branch strategy, commit conventions

---

## Core Design & Coding Principles

### 1. SOLID & Clean Code
- Write code that is maintainable and readable.
- Each method has a single responsibility. Extract into private methods when a method gets too long.
- Extract magic numbers/strings as constants.

### 2. Strict Layer Separation
- **Controller**: Only handles request/response conversion. No business logic.
- **Service**: All business logic and transaction boundaries. Handles DTO ↔ Entity conversion.
- **Repository**: Only handles DB access. No business logic.
- **Entity**: Never expose directly. Always convert to DTO before returning.

### 3. Performance Optimization
- Prevent N+1: Use Fetch Join or `@EntityGraph` when fetching related entities.
- Minimize unnecessary DB queries: never query the same data twice.
- Use QueryDSL for complex dynamic queries (refer to `DiaryRepositoryCustom` pattern).
- Use `Pageable` or cursor-based pagination for paginated responses.


---

## Code Writing Rules

> All feedback, Javadoc descriptions, and code comments must be written in Korean.
When a code review is requested, follow the order and format below.


### Javadoc Required
Write Javadoc for all public methods. Include params, return value, and exceptions.

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

### Exception Handling
Only use `BaseException + ResultCode`. Never throw `RuntimeException` directly.
```java
// ✅
.orElseThrow(() -> new BaseException(ResultCode.USER_NOT_FOUND));
 
// ❌
throw new RuntimeException("User not found.");
```

### Lombok
- Entity: use `@Getter`, `@NoArgsConstructor(access = PROTECTED)` .
- `@Setter`, `@Data` are forbidden.

---

## Code Review Rules

When a code review is requested, follow the order and format below.

### 1. Bug Detection First
Check for potential bugs, missing exception handling, and NPE risks first.

### 2. Before / After Comparison
Always show before and after when suggesting improvements.

```java
// Before
Member member = memberRepository.findById(memberId).get(); // NPE 위험
 
// After
Member member = memberRepository.findById(memberId)
        .orElseThrow(() -> new BaseException(ResultCode.USER_NOT_FOUND));
```

### 3. Feedback Format
Use the following format for each issue.

```
[Problem] What is wrong with the current code
[Reason] Why it should be changed (principle, performance, etc.)
[Effect] What improves after the change
```

---

## Stack

- **Spring Boot 3.4** + **Java 17**
- **MariaDB** (prod) / **H2** (dev/test)
- **Redis** — chat context caching (last 10 messages per session)
- **QueryDSL** — complex dynamic queries
- **Flyway** — DB migration (prod only)
- **OpenAI API** — chat / diary generation / weekly identity
- **Firebase FCM** — push notifications
- **Cloudinary** — image storage

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
