package kr.ac.kopo.waltdev29.aquamate.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 물고기 정보 도메인 엔티티 클래스.
 * data_fish.json 및 MySQL fish 테이블과 매핑.
 */
@Entity
@Table(name = "fish")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Fish {

    /** JSON 키이자 DB PK (goldfish, betta ...) */
    @Id
    @Column(name = "fish_key", length = 50, nullable = false)
    private String key;

    /** 번호 (001, 002 ...) */
    @Column(name = "num", length = 10, unique = true)
    private String num;

    /** 한국어 이름 */
    @Column(name = "name", length = 50, nullable = false)
    private String name;

    /** 기본 정보 HTML 문자열 */
    @Column(name = "basic", columnDefinition = "TEXT")
    private String basic;

    /** 사육 환경 HTML 문자열 */
    @Column(name = "env", columnDefinition = "TEXT")
    private String env;

    /** 품종 설명 */
    @JsonProperty("type")
    @Column(name = "type", columnDefinition = "TEXT")
    private String type;

    /** 사육 방법 */
    @Column(name = "taming", columnDefinition = "TEXT")
    private String taming;

    /** 이미지 경로 (예: images/fish_gold.jpg) */
    @Column(name = "img", length = 255)
    private String img;
}
