package com.buddy.buddyapi.controller;

import com.buddy.buddyapi.dto.common.ApiResponse;
import com.buddy.buddyapi.dto.request.ChatRequest;
import com.buddy.buddyapi.dto.response.ChatResponse;
import com.buddy.buddyapi.entity.Member;
import com.buddy.buddyapi.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Chat", description = "채팅 API")
@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;

    @Operation(summary = "메시지 전송", description = "사용자가 메시지를 보내고 AI의 답변을 받습니다.")
    @PostMapping
    public ApiResponse<ChatResponse> sendMessage(
            @AuthenticationPrincipal Member member,
            @RequestBody ChatRequest request) {
        return ApiResponse.success(chatService.sendMessage(member, request));
    }

    @Operation(summary = "대화 내역 조회", description = "특정 세션의 모든 대화 내역을 조회합니다.")
    @GetMapping("/{sessionId}")
    public ApiResponse<List<ChatResponse>> getChatHistory(
            @AuthenticationPrincipal Member member,
            @PathVariable Long sessionId
    ) {
        return ApiResponse.success(chatService.getChatHistory(member,sessionId));
    }

    @Operation(summary = "대화 세션 종료", description = "대화를 종료하고 해당 세션을 일기 생성 가능 상태로 변경합니다.")
    @PatchMapping("/{sessionId}/end")
    public ApiResponse<String> endSession(
            @AuthenticationPrincipal Member member,
            @PathVariable Long sessionId) {
        chatService.endChatSession(member, sessionId);
        return ApiResponse.success("대화가 성공적으로 종료되었습니다. 일기를 확인해보세요!");
    }
}
