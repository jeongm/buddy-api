package com.buddy.buddyapi.domain.chat;

import com.buddy.buddyapi.domain.chat.dto.ChatRequest;
import com.buddy.buddyapi.domain.ai.dto.OpenAiRequest;
import com.buddy.buddyapi.domain.chat.dto.ChatResponse;
import com.buddy.buddyapi.domain.character.BuddyCharacter;
import com.buddy.buddyapi.domain.member.Member;
import com.buddy.buddyapi.global.aspect.Timer;
import com.buddy.buddyapi.global.exception.BaseException;
import com.buddy.buddyapi.global.exception.ResultCode;
import com.buddy.buddyapi.domain.character.BuddyCharacterRepository;
import com.buddy.buddyapi.domain.member.MemberRepository;
import com.buddy.buddyapi.domain.ai.AiPrompt;
import com.buddy.buddyapi.domain.ai.AiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {
    private final MemberRepository memberRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final BuddyCharacterRepository buddyCharacterRepository;
    private final AiService aiService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final String CHAT_KEY_PREFIX = "chat:history:";

    /**
     * 버디(AI 캐릭터)에게 메시지를 전송하고 응답을 받습니다.
     * @param memberSeq  현재 로그인한 회원 정보
     * @param request 전송할 메시지 내용 및 세션 ID가 담긴 DTO
     * @return AI의 응답 메시지와 세션 ID를 포함한 응답 DTO
     */
    @Timer
    @Transactional
    public ChatResponse sendMessage(Long memberSeq, ChatRequest request) {

        // 1. 세션 조회 또는 생성 (세션 ID가 없거나 종료된 세션이면 새로 생성)
        ChatSession session = getOrCreateSession(memberSeq, request.sessionId());

        // 2. 사용자 메시지 저장
        saveMessage(session, SenderRole.USER, request.content());

        // 3. AI 답변 생성
        String aiContent = generateAiResponse(session, request.content());

        // 4. AI 메시지 저장(DB)
        ChatMessage aiMessage = saveMessage(session, SenderRole.ASSISTANT, aiContent);

        // 5. Redis에 대화내용 저장
        saveContextToRedis(session.getSessionSeq(), request.content(), aiContent);

        return ChatResponse.from(aiMessage, session.getSessionSeq());

    }

    /**
     * AI 서비스를 호출하여 캐릭터의 성격이 반영된 답변을 생성합니다.
     * * @param session     현재 대화 세션 (캐릭터 정보 포함)
     *
     * @param userContent 사용자가 입력한 메시지 내용
     * @return AI가 생성한 답변 문자열
     */
    private String generateAiResponse(ChatSession session, String userContent) {
        // OpenAI API 연동 지점
        // 1. 조립할 리스트 생성
        List<OpenAiRequest.Message> fullMessages = new ArrayList<>();

        String characterName = session.getMember().getCharacterNickname();
        String characterPersonality = session.getBuddyCharacter().getPersonality();


        fullMessages.add(new OpenAiRequest.Message("system",
                String.format(AiPrompt.CHAT_SYSTEM_PROMPT, characterPersonality,characterName)));

        // Redis에서 과거 대화 가져오기
        fullMessages.addAll(getContextFromRedis(session.getSessionSeq()));

        // 현재 사용자의 질문 추가
        fullMessages.add(new OpenAiRequest.Message("user", userContent));


        return aiService.getChatResponse(fullMessages);
    }

    /**
     * Redis에서 저장된 JSON 대화 내역을 객체 리스트로 변환
     * @param sessionSeq 현재 대화 세션
     * @return Redis에서 저장된 JSON 대화 내역 ~10개
     */
    private List<OpenAiRequest.Message> getContextFromRedis(Long sessionSeq) {
        String key = CHAT_KEY_PREFIX + sessionSeq;
        List<String> jsonHistory = redisTemplate.opsForList().range(key, 0, -1);
        List<OpenAiRequest.Message> history = new ArrayList<>();

        if(jsonHistory != null) {
            for(String json : jsonHistory) {
                try {
                    history.add(objectMapper.readValue(json, OpenAiRequest.Message.class));
                } catch (Exception e) {
                    log.error("Redis 메시지 파싱 에러 : {}", e.getMessage());
                }
            }
        }

        return history;
    }

    /**
     * 대화 내역을 JSON으로 변환하여 Redis에 저장하고, 관리(Trim/Expire)
     * @param sessionSeq 현재 대화 세션
     * @param userMessage 현재 유저 메시지
     * @param aiMessage 응답 메시지
     */
    private void saveContextToRedis(Long sessionSeq, String userMessage, String aiMessage) {
        String key = CHAT_KEY_PREFIX + sessionSeq;

        try{
            String userJson = objectMapper.writeValueAsString(new OpenAiRequest.Message("user", userMessage));
            String aiJson = objectMapper.writeValueAsString(new OpenAiRequest.Message("assistant", aiMessage));

            redisTemplate.opsForList().rightPush(key, userJson);
            redisTemplate.opsForList().rightPush(key, aiJson);

            // 최신 10개(사용자5개, AI5개)만 유지
            redisTemplate.opsForList().trim(key, -10,-1);
            // TODO 1시간동안 대화 없으면 자동 삭제
//            redisTemplate.expire(key, Duration.ofHours(1));
        }catch (Exception e) {
            log.error("Redis 저장 중 에러 발생: {}", e.getMessage());

        }
    }

    /**
     * 기존 대화 세션을 조회하거나, 없을 경우 새로운 세션을 생성합니다.
     * @param memberSeq    현재 로그인한 회원 정보
     * @param sessionId 조회할 세션의 고유 식별자 (null 가능)
     * @return 활성화된 대화 세션 엔티티
     */
    private ChatSession getOrCreateSession(Long memberSeq, Long sessionId) {

        Member member = memberRepository.findByIdWithCharacter(memberSeq)
                .orElseThrow(() -> new BaseException(ResultCode.USER_NOT_FOUND));

        if(sessionId != null) {
            return chatSessionRepository.findBySessionSeqAndMember_MemberSeq(sessionId, memberSeq)
                    .filter(s -> !s.isEnded()) // 종료되지 않은 세션만 사용
                    .orElseGet(() -> createNewSession(member));
        }
        return createNewSession(member);
    }


    /**
     * 회원이 설정한 캐릭터 정보를 바탕으로 새로운 대화 세션을 생성합니다.
     * * @param member 세션을 생성할 회원 정보
     * @return 저장된 새로운 대화 세션 엔티티
     * @throws BaseException 설정된 캐릭터 정보를 찾을 수 없을 경우 발생
     */
    private ChatSession createNewSession(Member member) {

        Long characterSeq = member.getBuddyCharacter().getCharacterSeq();

        BuddyCharacter character = buddyCharacterRepository.findById(characterSeq)
                .orElseThrow(() -> new BaseException(ResultCode.CHARACTER_NOT_FOUND));

        // 사용자가 현재 설정한 캐릭터를 가져와서 세션 생성
        ChatSession session = ChatSession.builder()
                .member(member)
                .buddyCharacter(character)
                .build();

        return  chatSessionRepository.save(session);
    }

    /**
     * 대화 메시지(사용자 또는 AI)를 DB에 저장합니다.
     * * @param session 메시지가 속한 대화 세션
     * @param role    메시지 발신자 역할 (USER 또는 ASSISTANT)
     * @param content 메시지 본문 내용
     * @return 저장된 메시지 엔티티
     */
    private ChatMessage saveMessage(ChatSession session, SenderRole role, String content) {
        ChatMessage message = ChatMessage.builder()
                .chatSession(session)
                .role(role)
                .content(content)
                .build();
        return chatMessageRepository.save(message);
    }


    /**
     * 특정 세션의 이전 대화 기록을 조회합니다.
     * * @param member    현재 로그인한 회원 정보
     * @param sessionId 조회할 세션의 고유 식별자
     * @return 과거 메시지 내역 리스트 (최신순)
     * @throws BaseException 해당 세션이 존재하지 않거나 본인 세션이 아닐 경우 발생
     */
    public List<ChatResponse> getChatHistory(Long memberSeq, Long sessionId) {

        // 내 세션인지 검증함께
        ChatSession session = chatSessionRepository.findBySessionSeqAndMember_MemberSeq(sessionId, memberSeq)
                .orElseThrow(() -> new BaseException(ResultCode.SESSION_NOT_FOUND));

        // 메시지 목록을 과거순으로 조회하여 DTO로 변환
        return chatMessageRepository.findAllByChatSessionOrderByCreatedAtDesc(session)
                .stream()
                .map(msg -> ChatResponse.from(msg, sessionId))
                .toList();
    }

    /**
     * 진행 중인 대화 세션을 종료 상태로 변경합니다.
     * * @param member    현재 로그인한 회원 정보
     * @param sessionId 종료할 세션의 고유 식별자
     * @throws BaseException 해당 세션이 존재하지 않거나 본인 세션이 아닐 경우 발생
     */
    @Transactional
    public void endChatSession(Long memberSeq, Long sessionId){

        ChatSession session = chatSessionRepository.findBySessionSeqAndMember_MemberSeq(sessionId, memberSeq)
                .orElseThrow(() -> new BaseException(ResultCode.SESSION_NOT_FOUND));

        // 2. 이미 종료된 세션인지 체크 (선택 사항)
        if (session.isEnded()) {
            throw new BaseException(ResultCode.SESSION_ALREADY_ENDED);
        }

        session.endSession();

        chatSessionRepository.save(session);
    }



}
