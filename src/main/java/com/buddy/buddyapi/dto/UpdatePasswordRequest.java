package com.buddy.buddyapi.dto;


import lombok.Getter;

@Getter
public class UpdatePasswordRequest {

    private String currentPassword;
    private String newPassword;

}
