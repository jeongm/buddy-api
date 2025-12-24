package com.buddy.buddyapi.dto.request;


public record UpdatePasswordRequest (
        String currentPassword,
        String newPassword
) {
}
