package kr.ac.kopo.waltdev29.aquamate.init;

import kr.ac.kopo.waltdev29.aquamate.domain.Role;
import kr.ac.kopo.waltdev29.aquamate.domain.User;
import kr.ac.kopo.waltdev29.aquamate.repository.SpringDataUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기본 관리자 계정(admin) 자동 생성 컴포넌트.
 * 시스템 시작 시 admin 계정이 없으면 안전하게 생성합니다.
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class AdminAccountInitializer implements ApplicationRunner {

    private final SpringDataUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsById("admin")) {
            log.info("[AdminAccountInitializer] admin 계정이 이미 존재합니다. 생성을 건너뜁니다.");
            return;
        }

        User admin = new User();
        admin.setId("admin");
        admin.setName("관리자");
        admin.setEmail("admin@aquamate.com");
        admin.setPhoneNum("010-0000-0000");
        admin.setPw(passwordEncoder.encode("Admin1234!"));
        admin.setRole(Role.ROLE_ADMIN);

        userRepository.save(admin);
        log.info("[AdminAccountInitializer] 기본 관리자 계정(admin)이 성공적으로 생성되었습니다.");
    }
}
