package com.buddy.buddyapi.global.security;

import com.buddy.buddyapi.domain.character.BuddyCharacter;
import com.buddy.buddyapi.domain.member.Member;
import lombok.Builder;

import java.util.Map;

@Builder
public record OAuthAttributes(
        Map<String, Object> attributes,      // 소셜에서 받은 원본 데이터
        String registrationId,
        String nameAttributeKey,   //"sub"나 "id" 같은 키 이름을 저장
        String oauthId,            // OAuth2 로그인 진행 시 키가 되는 필드값 (PK)//
        String name,
        String email,
        String picture
) {
    /**
     * registrationId(google, naver 등)를 보고 어떤 플랫폼인지 판단하여
     * 알맞은 객체를 생성해주는 팩토리 메서드입니다.
     */
    public static OAuthAttributes of(String registrationId, String userNameAttributeName, Map<String, Object> attributes) {
        return switch (registrationId.toLowerCase()) {
            case "naver" -> ofNaver(registrationId, "id", attributes);
            case "kakao" -> ofKakao(registrationId, "id", attributes);
            case "google" -> ofGoogle(registrationId, userNameAttributeName, attributes);
            default -> throw new IllegalArgumentException("지원하지 않는 소셜 로그인 플랫폼입니다: " + registrationId);
        };
    }

    private static OAuthAttributes ofGoogle(String registrationId, String userNameAttributeName, Map<String, Object> attributes) {

        return OAuthAttributes.builder()
                .registrationId(registrationId)
                .nameAttributeKey(userNameAttributeName)
                .oauthId((String) attributes.get(userNameAttributeName))
                .name((String) attributes.get("name"))
                .email((String) attributes.get("email"))
//                .picture((String) attributes.get("picture"))
                .attributes(attributes)
                .build();
    }

    private static OAuthAttributes ofNaver(String registrationId, String userNameAttributeName, Map<String, Object> attributes) {
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");

        return OAuthAttributes.builder()
                .registrationId(registrationId)
                .nameAttributeKey(userNameAttributeName)
                .oauthId((String) response.get(userNameAttributeName))
                .name((String) response.get("name"))
                .email((String) response.get("email"))
//                .picture((String) response.get("profile_image"))
                .attributes(attributes)
                .build();
    }

    private static OAuthAttributes ofKakao(String registrationId, String userNameAttributeName, Map<String, Object> attributes) {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

        return OAuthAttributes.builder()
                .registrationId(registrationId)
                .nameAttributeKey(userNameAttributeName)
                .oauthId(String.valueOf(attributes.get(userNameAttributeName))) // 카카오는 id가 Long인 경우가 많아 변환 필요
                .name((String) profile.get("nickname"))
                .email((String) kakaoAccount.get("email"))
//                .picture((String) profile.get("thumbnail_image_url"))
                .attributes(attributes)
                .build();
    }

    /**
     * 처음 가입하는 시점에 Member 엔티티를 생성합니다.
     * 이때, 캐릭터는 필수이므로 파라미터로 기본 캐릭터를 전달받습니다.
     */
    public Member toEntity() {
        return Member.builder()
                .email(email)
                .nickname(getSafeName())
                .build();
    }

    private String getSafeName() {
        if (name != null && !name.isBlank()) {
            return name;
        }

        return (email != null && email.contains("@")) ? email.split("@")[0] : "User";
    }


}
