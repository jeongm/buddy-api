package com.buddy.buddyapi.domain.auth.component;

import com.buddy.buddyapi.global.exception.BaseException;
import com.buddy.buddyapi.global.exception.ResultCode;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Apple Identity Token 검증 컴포넌트.
 * Apple 공개키 서버에서 키 목록을 조회하고 RS256 서명을 검증한다.
 * Google의 GoogleIdTokenVerifier와 동일한 역할을 수행한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppleTokenVerifier {
    private static final String APPLE_PUBLIC_KEYS_URL = "https://appleid.apple.com/auth/keys";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${apple.bundle-id}")
    private String bundleId;

    /**
     * Apple Identity Token을 검증하고 OAuthUserInfo를 반환한다.
     * Google/Kakao/Naver Verifier와 동일한 인터페이스를 유지한다.
     *
     * @param identityToken 프론트에서 Sign in with Apple로 발급받은 Identity Token (JWT)
     * @return 이메일, oauthId(sub)를 담은 OAuthUserInfo
     * @throws BaseException 토큰 검증 실패 시
     */
    public OAuthUserInfo verify(String identityToken) {
        Claims claims = verifyAndParseClaims(identityToken);

        String oauthId = claims.getSubject();
        String email = claims.get("email", String.class);

        return new OAuthUserInfo(email, null, oauthId, null, null);

    }

    /**
     * JWT 헤더의 kid로 Apple 공개키를 찾아 서명을 검증하고 Claims를 반환한다.
     *
     * @param identityToken Apple Identity Token
     * @return 검증된 Claims
     */
    private Claims verifyAndParseClaims(String identityToken) {
        try {
            String kid = extractKid(identityToken);
            PublicKey publicKey = fetchMatchingPublicKey(kid);

            return Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .build()
                    .parseClaimsJws(identityToken)
                    .getBody();

        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("🚨 Apple 토큰 검증 실패: {}", e.getMessage());
            throw new BaseException(ResultCode.INVALID_TOKEN);
        }
    }

    /**
     * JWT 헤더(Base64)를 디코딩해 kid 값을 추출한다.
     *
     * @param token Apple Identity Token
     * @return kid (Key ID)
     */
    private String extractKid(String token) {
        try {
            String headerJson = new String(Base64.getUrlDecoder().decode(token.split("\\.")[0]));
            Map<?, ?> header = objectMapper.readValue(headerJson, Map.class);
            return (String) header.get("kid");
        } catch (Exception e) {
            throw new BaseException(ResultCode.INVALID_TOKEN, "Apple 토큰 헤더 파싱 실패");
        }
    }

    /**
     * Apple 공개키 서버에서 keys 목록을 조회하고, kid가 일치하는 RSA PublicKey를 반환한다.
     *
     * @param kid JWT 헤더에서 추출한 Key ID
     * @return RSA PublicKey
     */
    private PublicKey fetchMatchingPublicKey(String kid) {
        try {
            ApplePublicKeyResponse response = restTemplate.getForObject(
                    APPLE_PUBLIC_KEYS_URL, ApplePublicKeyResponse.class);

            if (response == null || response.keys() == null) {
                throw new BaseException(ResultCode.APPLE_PUBLIC_KEY_FETCH_FAILED);
            }

            ApplePublicKeyResponse.ApplePublicKey matched = response.keys().stream()
                    .filter(k -> kid.equals(k.kid()))
                    .findFirst()
                    .orElseThrow(() -> new BaseException(ResultCode.INVALID_TOKEN, "Apple 공개키 kid 불일치"));

            return buildRsaPublicKey(matched);

        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("Apple 공개키 조회 실패: {}", e.getMessage());
            throw new BaseException(ResultCode.APPLE_PUBLIC_KEY_FETCH_FAILED);
        }
    }



    /**
     * n(modulus), e(exponent) Base64 값으로 RSA PublicKey 객체를 생성한다.
     *
     * @param key Apple 공개키 정보
     * @return RSA PublicKey
     */
    private PublicKey buildRsaPublicKey(ApplePublicKeyResponse.ApplePublicKey key) {
        try {
            BigInteger modulus  = new BigInteger(1, Base64.getUrlDecoder().decode(key.n()));
            BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(key.e()));
            return KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(modulus, exponent));
        } catch (Exception e) {
            throw new BaseException(ResultCode.INVALID_TOKEN, "Apple RSA 공개키 생성 실패");
        }
    }

    // -------------------------------------------------------------------------
    // Apple 공개키 응답 전용 내부 DTO
    // KakaoTokenVerifier의 private record 패턴과 동일
    // -------------------------------------------------------------------------

    private record ApplePublicKeyResponse(
            @JsonProperty("keys") List<ApplePublicKey> keys
    ) {
        private record ApplePublicKey(
                String kty, String kid, String use, String alg,
                String n,
                String e
        ) {
        }
    }

}
