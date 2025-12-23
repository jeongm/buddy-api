package com.buddy.buddyapi.dto.request;


import lombok.Getter;

@Getter
public class UpdatePasswordRequest {

    private String currentPassword;
    private String newPassword;

}
