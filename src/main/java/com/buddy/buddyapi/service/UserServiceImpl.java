package com.buddy.buddyapi.service;

import com.buddy.buddyapi.dto.UpdatePasswordRequest;
import com.buddy.buddyapi.dto.UserLoginRequest;
import com.buddy.buddyapi.dto.UserRegisterRequest;
import com.buddy.buddyapi.dto.UserResponse;
import com.buddy.buddyapi.entity.BuddyCharacter;
import com.buddy.buddyapi.entity.User;
import com.buddy.buddyapi.repository.BuddyCharacterRepository;
import com.buddy.buddyapi.repository.OauthAccountRepository;
import com.buddy.buddyapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService{

    private final UserRepository userRepository;
    private final BuddyCharacterRepository characterRepository;
    private final OauthAccountRepository oauthAccountRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 일반회원가입
     * @param request 회원가입 요청 DTO
     * @return
     */
    @Override
    public UserResponse registerUser(UserRegisterRequest request) {
        // 1. 중복 검사 (이메일, 닉네임)
        if(userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용중인 이메일입니다.");
        }

        // 2. 비밀번호 암호화 (BCrypt 등)
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // 3. 초기 캐릭터 조회 및 유효성 검사
        Integer charSeq = request.getCharacterSeq() != null ? request.getCharacterSeq() : 1;

        BuddyCharacter selectedCharacter = characterRepository.findById(charSeq)
                .orElseThrow(()-> new NoSuchElementException("선택된 캐릭터를 찾을 수 없습니다."));

        // 3. Request DTO를 Entity로 변환
        User newUser = User.builder()
                .email(request.getEmail())
                .password(encodedPassword)
                .nickname(request.getNickname())
                .buddyCharacter(selectedCharacter)
                .build();
        // 4. userRepository.save(userEntity)
        User savedUser = userRepository.save(newUser);
        // jpa는 영속성 컨텍스트라서 그냥 newUser그대로 반환해도 되지 않나?

        // 5. 저장된 Entity를 Response DTO로 변환하여 반환
//        return UserResponse.builder()
//                .userSeq(newUser.getUserSeq())
//                .email(newUser.getEmail())
//                .nickname(newUser.getNickname())
//                .characterSeq(newUser.getBuddyCharacter().getCharacterSeq())
//                .characterName(newUser.getBuddyCharacter().getName())
//                .avatarUrl(newUser.getBuddyCharacter().getAvatarUrl())
//                .build();
        return UserResponse.from(savedUser);
    }

    @Override
    public UserResponse localLoginUser(UserLoginRequest request) {
        // 1. userRepository.findByEmail()
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(()-> new IllegalArgumentException("이메일 또는 비밀번호가 일치하지 않습니다."));
        // 2. 암호화된 비밀번호 매칭 검사
        if(!passwordEncoder.matches(request.getPassword(), user.getPassword())){
            throw new IllegalArgumentException("이메일 또는 비밀번호가 일치하지 않습니다.");
        }
        // 3. 성공 시 UserResponse 반환

        return UserResponse.from(user);
    }

    @Override
    public UserResponse getUserDetails(Long userSeq) {
        User user = userRepository.findById(userSeq)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));
        return UserResponse.from(user);
    }

    @Override
    public void updateNickName(String nickname) {
//        if (userRepository.ex)
    }

    @Override
    public void updateUserPassword(UpdatePasswordRequest request) {

    }
}
