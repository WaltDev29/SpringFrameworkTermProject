package kr.ac.kopo.waltdev29.aquamate.service;

import kr.ac.kopo.waltdev29.aquamate.domain.Fish;
import kr.ac.kopo.waltdev29.aquamate.repository.FishRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * FishService 구현체.
 * Repository 메서드를 위임하는 계층으로 읽기 전용 트랜잭션 적용.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FishServiceImpl implements FishService {

    private final FishRepository fishRepository;

    @Override
    public List<Fish> getFishList() {
        return fishRepository.getFishList();
    }

    @Override
    public Optional<Fish> getFishByKey(String key) {
        return fishRepository.getFishByKey(key);
    }
}
