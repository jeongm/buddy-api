package com.buddy.buddyapi.domain.member.dto;


public record UpdatePasswordRequest (
        String currentPassword,
        String newPassword
) {
}
