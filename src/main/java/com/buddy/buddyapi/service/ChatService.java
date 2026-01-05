package com.buddy.buddyapi.service;

import com.buddy.buddyapi.domain.SenderRole;
import com.buddy.buddyapi.dto.request.ChatRequest;
import com.buddy.buddyapi.dto.response.ChatResponse;
import com.buddy.buddyapi.dto.response.DiaryPreviewResponse;
import com.buddy.buddyapi.entity.BuddyCharacter;
import com.buddy.buddyapi.entity.ChatMessage;
import com.buddy.buddyapi.entity.ChatSession;
import com.buddy.buddyapi.entity.Member;
import com.buddy.buddyapi.global.exception.BaseException;
import com.buddy.buddyapi.global.exception.ResultCode;
import com.buddy.buddyapi.repository.BuddyCharacterRepository;
import com.buddy.buddyapi.repository.ChatMessageRepository;
import com.buddy.buddyapi.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final BuddyCharacterRepository buddyCharacterRepository;

    @Transactional
    public ChatResponse sendMessage(Member member, ChatRequest request) {
        // 1. 세션 조회 또는 생성 (세션 ID가 없거나 종료된 세션이면 새로 생성)
        ChatSession session = getOrCreateSession(member, request.sessionId());

        // 2. 사용자 메시지 저장
        saveMessage(session, SenderRole.USER, request.content());

        // 3. AI 답변 생성 (지금은 Mock 데이터, 나중에 AI 연동)
        String aiContent = generateAiResponse(session, request.content());

        // 4. AI 메시지 저장
        ChatMessage aiMessage = saveMessage(session, SenderRole.ASSISTANT, aiContent);

        return ChatResponse.from(aiMessage, session.getSessionSeq());

    }

    private ChatSession getOrCreateSession(Member member, Long sessionId) {
        if(sessionId != null) {
            return chatSessionRepository.findBySessionSeqAndMember(sessionId, member)
                    .filter(s -> !s.isEnded()) // 종료되지 않은 세션만 사용
                    .orElseGet(() -> createNewSession(member));
        }
        return createNewSession(member);
    }

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

    private ChatMessage saveMessage(ChatSession session, SenderRole role, String content) {
        ChatMessage message = ChatMessage.builder()
                .chatSession(session)
                .role(role)
                .content(content)
                .build();
        return chatMessageRepository.save(message);
    }

    private String generateAiResponse(ChatSession session, String userContent) {
        // TODO: OpenAI API 연동 지점
        // 지금은 캐릭터 이르을 불러주는 가짜 답변 반환
        String characterName = session.getBuddyCharacter().getName();
        return String.format("안녕! 나는 '%s'야. 네가 '%s'라고 말해줘서 정말 기뻐. 임시 답변생성임",
                characterName, userContent);
    }

    public List<ChatResponse> getChatHistory(Member member, Long sessionId) {
        // 내 세션인지 검증
        ChatSession session = chatSessionRepository.findBySessionSeqAndMember(sessionId, member)
                .orElseThrow(() -> new BaseException(ResultCode.SESSION_NOT_FOUND));

        // 메시지 목록을 과거순으로 조회하여 DTO로 변환
        return chatMessageRepository.findAllByChatSessionOrderByCreatedAtDesc(session)
                .stream()
                .map(msg -> ChatResponse.from(msg, sessionId))
                .toList();
    }

    @Transactional
    public void endChatSession(Member member, Long sessionId){
        ChatSession session = chatSessionRepository.findBySessionSeqAndMember(sessionId, member)
                .orElseThrow(() -> new BaseException(ResultCode.SESSION_NOT_FOUND));

        // 2. 이미 종료된 세션인지 체크 (선택 사항)
//        if (session.isEnded()) {
//            throw new BaseException(ResultCode.SESSION_ALREADY_ENDED);
//            // ResultCode에 SESSION_ALREADY_ENDED 하나 추가해주면 좋습니다.
//        }

        session.endSession();

        chatSessionRepository.save(session);
    }

    @Transactional
    public DiaryPreviewResponse generateDiaryFromChat(Member member, Long sessionId) {
        // 1. 세션 존재 확인
        ChatSession session = chatSessionRepository.findBySessionSeqAndMember(sessionId, member)
                .orElseThrow(() -> new BaseException(ResultCode.SESSION_NOT_FOUND));

        // 2. 해당 세션의 모든 메시지 순서대로 가져오기
        List<ChatMessage> messages = chatMessageRepository.findAllByChatSessionOrderByCreatedAtAsc(session);

        // 3. AI에게 전달할 대화 텍스트 생성
        String fullConversation = messages.stream()
                .map(m -> m.getRole() + ": " + m.getContent())
                .collect(Collectors.joining("\n"));

        // 4. TODO: fullConversation을 OpenAI API에 전달하여 요약/태그 추출
        // 지금은 텍스트가 잘 합쳐졌는지 로그로 확인해봅시다.
        System.out.println("AI에게 보낼 텍스트:\n" + fullConversation);

        return new DiaryPreviewResponse("AI가 요약한 제목", "AI가 작성한 본문", List.of());
    }


}
