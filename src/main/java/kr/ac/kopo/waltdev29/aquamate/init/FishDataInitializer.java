package kr.ac.kopo.waltdev29.aquamate.init;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.ac.kopo.waltdev29.aquamate.domain.Fish;
import kr.ac.kopo.waltdev29.aquamate.repository.SpringDataFishRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 물고기 도감 데이터 자동 초기화 컴포넌트.
 * 애플리케이션 기동 시 MySQL fish 테이블이 비어있다면
 * classpath:data/data_fish.json 을 읽어 DB에 자동으로 시딩(Seeding)합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FishDataInitializer implements ApplicationRunner {

    private final SpringDataFishRepository fishRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        long currentCount = fishRepository.count();
        if (currentCount > 0) {
            log.info("[FishDataInitializer] 기존 물고기 데이터가 DB에 존재합니다 (총 {}종). 시딩을 건너뜁니다.", currentCount);
            return;
        }

        log.info("[FishDataInitializer] fish 테이블이 비어있어 data_fish.json 기반 초기 데이터 시딩을 시작합니다.");
        ObjectMapper mapper = new ObjectMapper();
        try {
            ClassPathResource resource = new ClassPathResource("data/data_fish.json");
            try (InputStream is = resource.getInputStream()) {
                Map<String, Map<String, String>> rawMap =
                        mapper.readValue(is, new TypeReference<Map<String, Map<String, String>>>() {});

                List<Fish> fishList = new ArrayList<>();
                rawMap.forEach((key, val) -> {
                    Fish fish = new Fish();
                    fish.setKey(key);
                    fish.setNum(val.get("num"));
                    fish.setName(val.get("name"));
                    fish.setBasic(val.get("basic"));
                    fish.setEnv(val.get("env"));
                    fish.setType(val.get("type"));
                    fish.setTaming(val.get("taming"));
                    fish.setImg(val.get("img"));
                    fishList.add(fish);
                });

                fishRepository.saveAll(fishList);
                log.info("[FishDataInitializer] MySQL 데이터베이스에 물고기 데이터 총 {}종 시딩 완료", fishList.size());
            }
        } catch (Exception e) {
            log.error("[FishDataInitializer] 물고기 데이터 초기화 중 오류 발생: {}", e.getMessage(), e);
        }
    }
}
