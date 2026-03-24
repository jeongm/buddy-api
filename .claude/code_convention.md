# Code Convention

Buddy 프로젝트의 코딩 스타일 및 네이밍 규칙.
모든 코드는 이 문서의 규칙을 따른다.

---

## 1. 네이밍 컨벤션


### 클래스/메서드/변수
```
클래스명   : PascalCase        (e.g. ChatSessionService)
메서드명   : camelCase         (e.g. createChatSession)
변수명     : camelCase         (e.g. chatSession)
상수       : UPPER_SNAKE_CASE  (e.g. MAX_RETRY_COUNT)
패키지     : 소문자 단수        (e.g. com.buddy.buddyapi.domain.chat)
```

### DTO
| 용도 | 네이밍 규칙 | 예시 |
|------|------------|------|
| 요청 DTO | `{동사}{명사}Request` | `CreateDiaryRequest`, `SendMessageRequest` |
| 응답 DTO | `{명사}Response` | `DiaryResponse`, `ChatMessageResponse` |
| 목록 응답 | `{명사}ListResponse` | `DiaryListResponse` |
| 내부 전달 | `{명사}Dto` | `DiaryDto` (Service ↔ Service 간) |

> **`Dto` suffix는 내부 전달용 전용이다.** 외부 응답으로 나가는 객체에 `Dto`를 붙이지 않는다.


### 메서드 접두사
| 접두사 | 의미 | 반환 타입 | 예시 |
|--------|------|-----------|------|
| `create` | 생성 및 저장 | Response DTO | `createDiary()` |
| `get` | 단건 조회 — 없으면 예외 | Response DTO | `getDiary()` |
| `find` | 단건 조회 — 없으면 Optional | `Optional<T>` | `findDiaryByDate()` |
| `get{복수}` | 목록 조회 | `List<T>` | `getDiaries()`, `getMonthlyDiaries()` |
| `update` | 수정 | Response DTO | `updateDiary()` |
| `delete` | 삭제 | `void` | `deleteDiary()` |
| `save` | upsert 성격 (있으면 수정, 없으면 생성) | Response DTO | `saveDraft()` |

> **`get` vs `find` 구분은 필수다.**
> `get`은 호출하는 쪽이 "반드시 있다"고 가정 → 없으면 예외.
> `find`는 호출하는 쪽이 "없을 수도 있다"고 가정 → Optional 반환.
 
---

## 2. Lombok 사용 규칙

```java
// ✅ Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // JPA 기본 생성자
@Entity
public class Diary { ... }

// ✅ DTO (불변 객체 권장)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiaryResponse { ... }

// ❌ 금지
@Setter          // Entity에 Setter 금지 (도메인 메서드로 상태 변경)
@Data            // equals/hashCode 오버라이드 문제로 금지
@ToString        // 연관관계 포함 시 무한루프 위험, 필요시 필드 명시
```

---

## 3. Javadoc 주석 규칙

**모든 public 메서드**에 Javadoc 필수 작성.

```java
/**
 * 채팅 세션 ID로 일기를 생성한다.
 * 세션에 메시지가 없거나 이미 일기가 존재하면 예외를 던진다.
 *
 * @param memberId  요청 회원 PK
 * @param sessionId 대상 채팅 세션 PK
 * @return 생성된 일기의 응답 DTO
 * @throws BaseException SESSION_NOT_FOUND   - 세션이 존재하지 않을 때
 * @throws BaseException EMPTY_CHAT_HISTORY  - 대화 내역이 없을 때
 */
public DiaryResponse createDiaryFromSession(Long memberId, Long sessionId) { ... }
```

---

## 4. 예외 처리 규칙

`GlobalExceptionHandler` + `ResultCode` + `BaseException` 조합을 반드시 사용.

```java
// ✅ 올바른 예외 처리
Member member = memberRepository.findById(memberId)
        .orElseThrow(() -> new BaseException(ResultCode.USER_NOT_FOUND));

// ❌ 금지: 직접 RuntimeException 던지기
throw new RuntimeException("사용자를 찾을 수 없습니다.");

// ❌ 금지: 커스텀 메시지 없이 NPE 유발
Member member = memberRepository.findById(memberId).get();
```

새로운 에러 코드 추가 시 `ResultCode.java` enum에만 추가한다.
도메인 접두사: `M`(회원), `D`(일기), `C`(채팅), `A`(AI), `T`(토큰), `N`(알림), `G`(공통)

---

## 5. API 응답 규칙

모든 응답은 `ApiResponse<T>` 래퍼로 반환한다.

```java
// 조회 (200)
return ResponseEntity.ok(ApiResponse.ok(response));

// 생성 (201)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));

// 삭제 등 데이터 없는 성공 (200)
        return ResponseEntity.ok(ApiResponse.ok());

// ❌ Entity 직접 반환 금지
        return ResponseEntity.ok(diary);
```

---

## 6. 트랜잭션 규칙

```java
// Service 기본 원칙
@Transactional(readOnly = true)  // 조회 메서드 기본값
public DiaryResponse getDiary(...) { ... }

@Transactional                   // 쓰기 작업
public DiaryResponse createDiary(...) { ... }
```

---

## 7. 성능 규칙

- **N+1 금지**: 연관 엔티티 조회 시 Fetch Join 또는 `@EntityGraph` 사용
- **복잡한 동적 쿼리**: QueryDSL 사용 (`DiaryRepositoryCustom` 패턴 참고)
- **페이징**: `Pageable` 파라미터 활용, 커서 기반 페이징 권장
- **대용량 목록**: `List` 대신 `Slice` 또는 `Page` 반환

```java
// ✅ Fetch Join으로 N+1 방지
@Query("SELECT d FROM Diary d JOIN FETCH d.member WHERE d.diaryId = :id")
Optional<Diary> findByIdWithMember(@Param("id") Long id);

// ✅ 복잡한 동적 쿼리는 QueryDSL 사용
// DiaryRepositoryCustom / DiaryRepositoryImpl 패턴 참고
```

---

## 8. 테스트 규칙

```
Controller  : @WebMvcTest + MockMvc + @WithMockUser
Service     : 순수 단위 테스트 (Spring 컨텍스트 없음)
Repository  : @DataJpaTest + H2
```

- 외부 API (OpenAI, FCM, Cloudinary, Gmail) 는 항상 Mock 처리.
- Given-When-Then 구조 필수.
- 테스트 클래스명: `{대상클래스}Test` (e.g. `DiaryServiceTest`)
- 테스트 메서드명: `{메서드명}_{시나리오}` (e.g. `createDiary_Success`, `createDiary_SessionNotFound`)