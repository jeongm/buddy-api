package com.buddy.buddyapi.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ResultCode {

    // --- 성공 (S) ---
    SUCCESS(HttpStatus.OK, "S000", "요청 성공"),

    // --- 공통 에러 (G) ---
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "G001", "입력 값이 올바르지 않습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "G002", "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "G003", "인증되지 않은 사용자입니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "G004", "지원하지 않는 HTTP 메서드입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "G500", "서버 내부 에러가 발생했습니다."),
    // 429 Too Many Requests : 도배 방지용
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "G005", "요청 횟수가 초과되었습니다. 잠시 후 다시 시도해주세요."),

    // --- 인증 및 토큰 관련 (T) ---
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "T001", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "T002", "만료된 토큰입니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "T003", "리프레시 토큰이 존재하지 않거나 만료되었습니다."),
    TOKEN_SIGNATURE_ERROR(HttpStatus.UNAUTHORIZED, "T004", "토큰 서명이 올바르지 않습니다."),
    UNSUPPORTED_TOKEN(HttpStatus.UNAUTHORIZED, "T005", "지원되지 않는 토큰 형식입니다."),

    // --- 회원/캐릭터 관련 (M) ---
    EMAIL_DUPLICATED(HttpStatus.BAD_REQUEST, "M001", "이미 존재하는 이메일입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "M002", "존재하지 않는 사용자입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "M003", "이메일 또는 비밀번호가 불일치합니다."),
    CURRENT_PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "M004", "현재 비밀번호가 일치하지 않습니다."),
    INVALID_PASSWORD_FORMAT(HttpStatus.BAD_REQUEST, "M005", "비밀번호 형식이 올바르지 않습니다."),
    CHARACTER_NOT_FOUND(HttpStatus.NOT_FOUND, "M006", "존재하지 않는 캐릭터입니다."),
    ALREADY_SIGNED_UP_EMAIL(HttpStatus.BAD_REQUEST, "M007", "이미 가입된 이메일입니다. 소셜 계정 연동이 필요합니다."),
    ALREADY_LINKED_ACCOUNT(HttpStatus.BAD_REQUEST, "M008", "이미 해당 소셜 플랫폼과 연동된 계정입니다."),

    // --- 일기 관련 (D) ---
    DIARY_NOT_FOUND(HttpStatus.NOT_FOUND, "D001", "존재하지 않는 일기입니다."),
    TAG_NOT_FOUND(HttpStatus.NOT_FOUND, "D002", "태그를 찾을 수 없습니다."),

    // --- 채팅 관련 (C) ---
    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "C001", "특정 세션을 찾을 수 없습니다."),
    SESSION_ALREADY_ENDED(HttpStatus.BAD_REQUEST, "C002", "이미 종료된 세션입니다."),
    EMPTY_CHAT_HISTORY(HttpStatus.NOT_FOUND, "C003", "대화 내역이 없어 일기를 생성할 수 없습니다."),

    // --- AI 서비스 관련 (A) ---
    AI_PARSE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "A001", "AI 응답을 처리하는 중 오류가 발생했습니다.");



    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
