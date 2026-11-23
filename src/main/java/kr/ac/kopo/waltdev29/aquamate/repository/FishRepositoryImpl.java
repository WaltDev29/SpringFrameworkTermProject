package kr.ac.kopo.waltdev29.aquamate.repository;

import kr.ac.kopo.waltdev29.aquamate.domain.Fish;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * MySQL Spring Data JPA 기반의 FishRepository 구현체.
 * DB의 fish 테이블에서 물고기 도감 데이터를 조회.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class FishRepositoryImpl implements FishRepository {

    private final SpringDataFishRepository springDataFishRepository;

    @Override
    public List<Fish> getFishList() {
        return springDataFishRepository.findAllByOrderByNumAsc();
    }

    @Override
    public Optional<Fish> getFishByKey(String key) {
        return springDataFishRepository.findById(key);
    }
}
