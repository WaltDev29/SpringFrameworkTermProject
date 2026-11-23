package kr.ac.kopo.waltdev29.aquamate.service;

import kr.ac.kopo.waltdev29.aquamate.domain.User;
import kr.ac.kopo.waltdev29.aquamate.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * UserService 구현체.
 * JPA 기반 트랜잭션 관리 및 BCrypt 비밀번호 암호화 적용.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 회원가입 처리. 비밀번호를 단방향 암호화(BCrypt)하여 저장합니다.
     * @return 중복 여부에 따라 상태 문자열 반환
     */
    @Override
    public String signUp(User user) {
        if (userRepository.existsById(user.getId())) {
            log.warn("[UserService] 중복 아이디 가입 시도: {}", user.getId());
            return "DUPLICATE_ID";
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            log.warn("[UserService] 중복 이메일 가입 시도: {}", user.getEmail());
            return "DUPLICATE_EMAIL";
        }
        if (userRepository.existsByPhoneNum(user.getPhoneNum())) {
            log.warn("[UserService] 중복 전화번호 가입 시도: {}", user.getPhoneNum());
            return "DUPLICATE_PHONE";
        }

        // 비밀번호 BCrypt 암호화 적용
        user.setPw(passwordEncoder.encode(user.getPw()));
        userRepository.save(user);
        log.info("[UserService] 신규 회원 가입 완료: {}", user.getId());
        return "SUCCESS";
    }

    /**
     * 로그인 검증.
     * 암호화된 비밀번호 비교(BCrypt matches) 및 레거시 평문 비밀번호 호환 지원.
     * @return 아이디·비밀번호 일치 시 true
     */
    @Override
    @Transactional(readOnly = true)
    public boolean login(String id, String pw) {
        return userRepository.findById(id)
                .map(user -> passwordEncoder.matches(pw, user.getPw()) || user.getPw().equals(pw))
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(String id) {
        return userRepository.findById(id);
    }

    @Override
    public boolean addBookmark(String userId, String fishKey) {
        return userRepository.findById(userId).map(user -> {
            boolean added = user.getBookmarks().add(fishKey);
            userRepository.save(user);
            return added;
        }).orElse(false);
    }

    @Override
    public boolean removeBookmark(String userId, String fishKey) {
        return userRepository.findById(userId).map(user -> {
            boolean removed = user.getBookmarks().remove(fishKey);
            userRepository.save(user);
            return removed;
        }).orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Set<String> getBookmarks(String userId) {
        return userRepository.findById(userId).map(User::getBookmarks).orElseGet(java.util.HashSet::new);
    }

    @Override
    public boolean updateUserInfo(String userId, User updatedUser) {
        return userRepository.findById(userId).map(user -> {
            user.setName(updatedUser.getName());
            user.setEmail(updatedUser.getEmail());
            user.setPhoneNum(updatedUser.getPhoneNum());

            // 새 비밀번호가 입력된 경우 암호화하여 갱신
            if (updatedUser.getPw() != null && !updatedUser.getPw().isBlank()) {
                user.setPw(passwordEncoder.encode(updatedUser.getPw()));
            }

            userRepository.save(user);
            log.info("[UserService] 회원 정보 수정 완료: {}", userId);
            return true;
        }).orElse(false);
    }
}
