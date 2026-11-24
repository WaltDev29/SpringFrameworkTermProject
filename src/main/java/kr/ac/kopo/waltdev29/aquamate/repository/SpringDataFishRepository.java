package kr.ac.kopo.waltdev29.aquamate.repository;

import kr.ac.kopo.waltdev29.aquamate.domain.Fish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA 기반 물고기 도감 Repository 인터페이스.
 */
@Repository
public interface SpringDataFishRepository extends JpaRepository<Fish, String> {

    /** 도감 번호(num) 오름차순으로 전체 물고기 목록 조회 */
    List<Fish> findAllByOrderByNumAsc();

    /**
     * 다음 도감 번호 자동 채번 (최대 번호 + 1, 3자리 포맷팅)
     */
    default String getNextFishNum() {
        return findAll().stream()
                .map(Fish::getNum)
                .filter(java.util.Objects::nonNull)
                .map(String::trim)
                .filter(s -> s.matches("\\d+"))
                .mapToInt(Integer::parseInt)
                .max()
                .stream()
                .mapToObj(max -> String.format("%03d", max + 1))
                .findFirst()
                .orElse("001");
    }
}
