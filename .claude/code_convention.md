# Code Convention

Coding style and naming rules for the Buddy project.
All code must follow the rules in this document.

---

## 1. 네이밍 컨벤션


### Class / Method / Variable
```
Class     : PascalCase        (e.g. ChatSessionService)
Method    : camelCase         (e.g. createChatSession)
Variable  : camelCase         (e.g. chatSession)
Constant  : UPPER_SNAKE_CASE  (e.g. MAX_RETRY_COUNT)
Package   : lowercase singular (e.g. com.buddy.buddyapi.domain.chat)
```

### DTO
| Type | Rule | Example |
|------|------|---------|
| Request DTO | `{Verb}{Noun}Request` | `CreateDiaryRequest`, `SendMessageRequest` |
| Response DTO | `{Noun}Response` | `DiaryResponse`, `ChatMessageResponse` |
| List Response | `{Noun}ListResponse` | `DiaryListResponse` |
| Internal transfer | `{Noun}Dto` | `DiaryDto` (between Services only) |

> **`Dto` suffix is reserved for internal transfer only.** Never use `Dto` on objects returned to the client.


### Method Prefix
| Prefix | Meaning | Return Type | Example |
|--------|---------|-------------|---------|
| `create` | Create and save | Response DTO | `createDiary()` |
| `get` | Single lookup — throws exception if not found | Response DTO | `getDiary()` |
| `find` | Single lookup — returns Optional if not found | `Optional<T>` | `findDiaryByDate()` |
| `get{Plural}` | List lookup | `List<T>` | `getDiaries()`, `getMonthlyDiaries()` |
| `update` | Update | Response DTO | `updateDiary()` |
| `delete` | Delete | `void` | `deleteDiary()` |
| `save` | Upsert (update if exists, create if not) | Response DTO | `saveDraft()` |

> **`get` vs `find` distinction is mandatory.**
> `get` assumes the resource must exist → throws exception if not found.
> `find` assumes the resource may not exist → returns Optional.

 
---

## 2. Lombok Rules

```java
// ✅ Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // required for JPA
@Entity
public class Diary { ... }

// ✅ DTO
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiaryResponse { ... }

// ❌ Forbidden
@Setter    // use domain methods to change entity state
@Data      // causes equals/hashCode override issues
@ToString  // risk of infinite loop with relationships; specify fields explicitly if needed
```

---

## 3. Javadoc Rules

Javadoc is required for all public methods. Include params, return value, and exceptions.

```java
/**
 * 채팅 세션 ID로 일기를 생성한다.
 * 세션에 메시지가 없거나 이미 일기가 존재하면 예외를 던진다.
 *
 * @param memberId  요청 회원 PK
 * @param sessionId 대상 채팅 세션 PK
 * @return 생성된 일기의 응답 DTO
 * @throws BaseException SESSION_NOT_FOUND  - 세션이 존재하지 않을 때
 * @throws BaseException EMPTY_CHAT_HISTORY - 대화 내역이 없을 때
 */
public DiaryResponse createDiaryFromSession(Long memberId, Long sessionId) { ... }
```


---

## 4. Exception Handling Rules

Always use `GlobalExceptionHandler` + `ResultCode` + `BaseException`.

```java
// ✅
Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BaseException(ResultCode.USER_NOT_FOUND));

// ❌ never throw RuntimeException directly
throw new RuntimeException("사용자를 찾을 수 없습니다.");

// ❌ never use .get() — causes NPE
Member member = memberRepository.findById(memberId).get();
```

New error codes are added only to `ResultCode.java` enum.
Domain prefixes: `M`(member), `D`(diary), `C`(chat), `A`(AI), `T`(token), `N`(notification), `G`(global)
 
---

---

## 5. API Response Rules

All responses must use the `ApiResponse<T>` wrapper.

```java
// 200 OK
return ResponseEntity.ok(ApiResponse.ok(response));

// 201 Created
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));

// 200 OK with no data (e.g. delete)
        return ResponseEntity.ok(ApiResponse.ok());

// ❌ never return Entity directly
        return ResponseEntity.ok(diary);
```

---

## 6. Transaction Rules

```java
@Transactional(readOnly = true)  // class-level default for read operations
public class DiaryService {

    @Transactional  // override for write operations only
    public DiaryResponse createDiary(...) { ... }
}
```

---

## 7. Performance Rules

- **No N+1**: use Fetch Join or `@EntityGraph` when fetching related entities.
- **No duplicate queries**: never query the same data twice in one request.
- **Complex dynamic queries**: use QueryDSL (refer to `DiaryRepositoryCustom` pattern).
- **Pagination**: use `Pageable` or cursor-based pagination. Prefer `Slice` or `Page` over `List` for large data.

```java
// ✅ Fetch Join to prevent N+1
@Query("SELECT d FROM Diary d JOIN FETCH d.member WHERE d.diaryId = :id")
Optional<Diary> findByIdWithMember(@Param("id") Long id);
```

---

## 8. Test Rules

```
Controller : @WebMvcTest + MockMvc + @WithMockUser
Service    : plain unit test (no Spring context)
Repository : @DataJpaTest + H2
```

- External APIs (OpenAI, FCM, Cloudinary, Gmail) must always be mocked.
- Follow Given-When-Then structure.
- Test class name: `{TargetClass}Test` (e.g. `DiaryServiceTest`)
- Test method name: `{methodName}_{scenario}` (e.g. `createDiary_Success`, `createDiary_SessionNotFound`)
- Use `@DisplayName` to describe the test in Korean.

```java
@Test
@DisplayName("일기 생성 성공 - 채팅 세션 기반")
void createDiary_Success() { ... }
 
@Test
@DisplayName("일기 생성 실패 - 세션이 존재하지 않을 때")
void createDiary_SessionNotFound() { ... }
```