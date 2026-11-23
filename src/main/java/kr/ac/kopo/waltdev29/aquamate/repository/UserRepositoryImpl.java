package kr.ac.kopo.waltdev29.aquamate.repository;

import kr.ac.kopo.waltdev29.aquamate.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * MySQL Spring Data JPA 기반의 UserRepository 구현체.
 * 상위 서비스 인터페이스 계약을 준수하며 SpringDataUserRepository로 위임.
 */
@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final SpringDataUserRepository springDataUserRepository;

    @Override
    public void save(User user) {
        springDataUserRepository.save(user);
    }

    @Override
    public Optional<User> findById(String id) {
        return springDataUserRepository.findById(id);
    }

    @Override
    public boolean existsById(String id) {
        return springDataUserRepository.existsById(id);
    }

    @Override
    public boolean existsByEmail(String email) {
        return springDataUserRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByPhoneNum(String phoneNum) {
        return springDataUserRepository.existsByPhoneNum(phoneNum);
    }
}
