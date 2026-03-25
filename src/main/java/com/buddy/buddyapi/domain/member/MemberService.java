package com.buddy.buddyapi.domain.member;

import com.buddy.buddyapi.domain.auth.dto.SignUpRequest;
import com.buddy.buddyapi.domain.character.BuddyCharacter;
import com.buddy.buddyapi.domain.character.BuddyCharacterService;
import com.buddy.buddyapi.domain.member.dto.*;
import com.buddy.buddyapi.domain.member.event.MemberWithdrawEvent;
import com.buddy.buddyapi.global.exception.BaseException;
import com.buddy.buddyapi.global.exception.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    private final BuddyCharacterService characterService;
    private final NotificationSettingService notificationSettingService;

    private final ApplicationEventPublisher eventPublisher;

    /**
     * 일반(이메일) 회원가입을 처리하고 새로운 회원을 생성합니다.
     * 반드시 호출자의 트랜잭션 내에서 실행되어야 합니다. (MANDATORY)
     *
     * @param request         회원가입 요청 DTO
     * @param encodedPassword 암호화된 비밀번호
     * @return 영속 상태의 Member 엔티티 (PK 보장)
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Member registerLocalMember(SignUpRequest request, String encodedPassword) {

        checkEmailDuplicate(request.email());

        String tempNickname = generateDefaultNickname(request.email());

        Member newMember = Member.builder()
                .email(request.email())
                .password(encodedPassword)
                .nickname(tempNickname)
                .buddyCharacter(null)
                .build();

        return memberRepository.save(newMember);
    }

    /**
     * 신규 소셜 로그인 유저를 데이터베이스에 저장합니다.
     * 반드시 호출자의 트랜잭션 내에서 실행되어야 합니다. (MANDATORY)
     *
     * @param email 소셜 제공자로부터 전달받은 이메일
     * @param name  소셜 제공자로부터 전달받은 닉네임
     * @return 영속 상태의 Member 엔티티 (PK 보장)
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Member registerSocialMember(String email, String name) {

        String nickname = (name != null && !name.isBlank()) ? name : generateDefaultNickname(email);

        return memberRepository.save(
                Member.builder()
                        .email(email)
                        .nickname(nickname)
                        .build()
        );
    }

    /**
     * [온보딩 완료] 회원의 초기 설정(닉네임, 캐릭터, 알림 동의)을 단일 트랜잭션으로 처리합니다.
     * Member와 NotificationSetting 변경을 하나의 트랜잭션으로 묶어 원자성을 보장합니다.
     *
     * @param memberId 온보딩을 완료할 회원의 고유 식별자
     * @param request  온보딩 요청 DTO (닉네임, 캐릭터ID, 캐릭터 별명, 야간 알림 동의 여부)
     * @throws BaseException 회원 또는 캐릭터를 찾을 수 없을 경우 발생
     */
    @Transactional
    public void completeOnboarding(Long memberId, CompleteOnboardingRequest request) {
        Member member = getMemberById(memberId);
        BuddyCharacter myCharacter = characterService.getCharacter(request.characterId());

        member.updateNickname(request.nickname());
        member.changeCharacter(myCharacter);
        member.updateCharacterNickname(request.characterName());

        notificationSettingService.updateOnboardingSettings(memberId, request.isNightAgreed());

    }

    /**
     * Email로 회원을 정보를 가져옵니다.
     * @param email 조회할 회원의 고유 식별자
     * @return 조회한 회원 엔티티 (Member)
     */
    @Transactional(readOnly = true)
    public Member getMemberByEmail(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new BaseException(ResultCode.USER_NOT_FOUND));
    }


    /**
     * [조회] 이메일로 회원을 찾습니다. (회원이 없을 수도 있는 로직용)
     */
    @Transactional(readOnly = true)
    public Optional<Member> findMemberByEmail(String email) {
        return memberRepository.findByEmail(email);
    }

    /**
     * PK로 회원을 정보를 가져옵니다.
     * @param memberId 조회할 회원의 고유 식별자
     * @return 조회한 회원 엔티티 (Member)
     */
    @Transactional(readOnly = true)
    public Member getMemberById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BaseException(ResultCode.USER_NOT_FOUND));
    }

    /**
     * [조회] PK로 회원과 설정된 캐릭터 정보를 함께 가져옵니다.
     */
    @Transactional(readOnly = true)
    public Member getMemberWithCharacter(Long memberId) {
        return memberRepository.findByIdWithCharacter(memberId)
                .orElseThrow(() -> new BaseException(ResultCode.USER_NOT_FOUND));
    }

    /**
     * 회원의 닉네임을 변경합니다.
     * @param memberId 변경할 회원의 고유 식별자
     * @param request 새로운 닉네임 정보를 담은 DTO
     * @return 변경된 닉네임 결과 DTO
     * @throws BaseException 해당 회원이 존재하지 않을 경우 발생
     */
    @Transactional
    public UpdateNicknameResponse updateNickName(Long memberId, UpdateNicknameRequest request) {

        Member member = getMemberById(memberId);

        member.updateNickname(request.nickname());

        return new UpdateNicknameResponse(member.getNickname());
    }

    /**
     * 회원의 비밀번호를 변경합니다.
     *
     * @param memberId 비밀번호를 변경할 회원의 고유 식별자
     * @param currentPassword 현재비밀번호
     * @param newPassword 새 비밀번호를 담은 DTO
     * @throws BaseException 기존 비밀번호가 일치하지 않거나 유저가 없을 경우 발생
     */
    @Transactional
    public void updateMemberPassword(Long memberId, String currentPassword, String newPassword) {
        Member member = getMemberById(memberId);

        if (!passwordEncoder.matches(currentPassword, member.getPassword())) {
            throw new BaseException(ResultCode.CURRENT_PASSWORD_MISMATCH);
        }

        member.updatePassword(passwordEncoder.encode(newPassword));
    }

    /**
     * 현재 비밀번호가 일치하는지 검증합니다. (비밀번호 변경 외 독립 검증이 필요한 경우에만 사용)
     *
     * @param memberId    검증할 회원의 고유 식별자
     * @param rawPassword 검증할 평문 비밀번호
     * @throws BaseException 비밀번호가 일치하지 않을 경우 발생
     */
    @Transactional(readOnly = true)
    public void verifyPassword(Long memberId, String rawPassword) {
        Member member = getMemberById(memberId);

        if (!passwordEncoder.matches(rawPassword, member.getPassword())) {
            throw new BaseException(ResultCode.CURRENT_PASSWORD_MISMATCH);
        }
    }

    /**
     * 내 정보(상세 프로필)를 조회합니다.
     *
     * @param memberId 조회할 회원의 고유 식별자
     * @return 회원의 이메일, 닉네임, 캐릭터 정보 등을 포함한 DTO
     * @throws BaseException 해당 회원이 존재하지 않을 경우 발생
     */
    @Transactional(readOnly = true)
    public MemberResponse getUserDetails(Long memberId) {
        Member member = memberRepository.findByIdWithCharacter(memberId)
                .orElseThrow(() -> new BaseException(ResultCode.USER_NOT_FOUND));
        return MemberResponse.from(member);
    }

    /**
     * 회원이 사용할 버디 캐릭터를 변경합니다.
     *
     * @param memberId 현재 로그인한 회원 정보
     * @param request    변경하고자 하는 캐릭터의 식별자가 담긴 DTO
     * @throws BaseException 존재하지 않는 캐릭터 ID이거나 회원을 찾을 수 없을 경우 발생
     */
    @Transactional
    public void changeMyCharacter(Long memberId, ChangeCharacterRequest request) {

        Member member = getMemberById(memberId);

        //캐릭터는 가짜 프록시 객체로 가져옴 (SELECT 발생 안 함)
        // DB에 가지 않고, id값만 가진 껍데기 객체를 만듭니다.
        BuddyCharacter newCharacter = characterService.getCharacterProxy(request.characterId());

        member.changeCharacter(newCharacter);

    }

    /**
     * 현재 사용 중인 버디 캐릭터의 별명(애칭)을 변경합니다.
     *
     * @param memberId  현재 로그인한 회원 정보
     * @param newName 새로 설정할 캐릭터의 별명
     * @throws BaseException 회원을 찾을 수 없을 경우 발생
     */
    @Transactional
    public void updateCharacterNickname(Long memberId, String newName) {
        Member member = getMemberById(memberId);
        member.updateCharacterNickname(newName);
    }

    /**
     * [회원 탈퇴] DB에서 회원을 삭제하고 외부 자원 정리를 예약합니다.
     *
     * <pre>
     * [트랜잭션 내부 - 즉시 실행]
     *   1. DiaryService  : Cloudinary 삭제 대상 URL 수집 → DiaryImagesCleanupEvent 예약
     *   2. OauthService  : 소셜 계정 정보 스냅샷 수집   → SocialUnlinkEvent 예약
     *   3. memberRepository.deleteById() → DB @OnDelete CASCADE가 모든 자식 정리
     *      (ChatSession, ChatMessage, Diary, DiaryTag, NotificationSetting, OauthAccount, MemberInsight)
     *
     * [트랜잭션 커밋 후 - AFTER_COMMIT]
     *   4. AuthService   : Redis Refresh Token 삭제
     *   5. ImageService  : Cloudinary 이미지 일괄 삭제
     *   6. OauthService  : 소셜 연동 해제 API 호출
     * </pre>
     *
     * @param memberId 탈퇴할 회원의 식별자
     */
    @Transactional
    public void deleteMember(Long memberId) {
        eventPublisher.publishEvent(new MemberWithdrawEvent(memberId));
        memberRepository.deleteById(memberId);
    }

    /**
     * 이메일 중복 여부를 검증합니다.
     * 회원가입 요청 전 이메일 사전 검증 및 가입 처리 시 내부적으로 호출됩니다.
     *
     * @param email 중복 여부를 확인할 이메일 주소
     * @throws BaseException 이미 존재하는 이메일인 경우 {@link ResultCode#EMAIL_DUPLICATED} 발생
     */
    @Transactional(readOnly = true)
    public void checkEmailDuplicate(String email) {
        if(memberRepository.existsByEmail(email)) {
            throw new BaseException(ResultCode.EMAIL_DUPLICATED);
        }
    }

    /**
     * FCM 푸시 알림 토큰을 갱신합니다.
     * 앱 재설치 또는 토큰 만료 시 프론트엔드에서 새 토큰을 전달합니다.
     *
     * @param memberId  토큰을 갱신할 회원의 고유 식별자
     * @param pushToken 새로 발급된 FCM 푸시 토큰
     * @throws BaseException 회원을 찾을 수 없을 경우 발생
     */
    @Transactional
    public void updatePushToken(Long memberId, String pushToken) {
        Member member = getMemberById(memberId);

        member.updatePushToken(pushToken);
    }


    /**
     * 임시 닉네임, 이메일 앞자리를 따거나, 랜덤 문자열로 임시 닉네임을 만듭니다.
     */
    private String generateDefaultNickname(String email) {
        if (email != null && email.contains("@")) {
            String prefix = email.substring(0, email.indexOf("@"));
            return prefix.length() > 10 ? prefix.substring(0, 10) : prefix;
        }
        // 이메일도 없다면 최후의 수단 (임의의 UUID 부여)
        return "Buddy_" + java.util.UUID.randomUUID().toString().substring(0, 6);
    }
}
