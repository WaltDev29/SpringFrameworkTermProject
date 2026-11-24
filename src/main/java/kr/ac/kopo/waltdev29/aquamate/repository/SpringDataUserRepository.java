package kr.ac.kopo.waltdev29.aquamate.repository;

import kr.ac.kopo.waltdev29.aquamate.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA 기반 사용자 Repository 인터페이스.
 */
@Repository
public interface SpringDataUserRepository extends JpaRepository<User, String> {

    boolean existsByEmail(String email);

    boolean existsByPhoneNum(String phoneNum);

    long countByRole(kr.ac.kopo.waltdev29.aquamate.domain.Role role);

    @Query(value = "SELECT COUNT(*) FROM user_bookmark", nativeQuery = true)
    long countTotalBookmarks();
}
