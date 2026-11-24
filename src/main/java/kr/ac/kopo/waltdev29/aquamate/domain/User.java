package kr.ac.kopo.waltdev29.aquamate.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import kr.ac.kopo.waltdev29.aquamate.validator.Password;
import kr.ac.kopo.waltdev29.aquamate.validator.Phone;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 사용자 계정 도메인 엔티티 클래스.
 * 프론트엔드 signUp.html 폼 필드 및 MySQL users 테이블과 매핑.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @NotBlank(message = "아이디를 입력해주세요.")
    @Size(max = 15, message = "아이디는 최대 15자입니다.")
    @Column(name = "id", length = 50, nullable = false)
    private String id;

    @NotBlank(message = "이름을 입력해주세요.")
    @Size(min = 2, max = 30, message = "이름은 2~30자 사이여야 합니다.")
    @Column(name = "name", length = 50, nullable = false)
    private String name;

    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @Size(max = 80)
    @Column(name = "email", length = 100, nullable = false, unique = true)
    private String email;

    @NotBlank(message = "전화번호를 입력해주세요.")
    @Phone
    @Column(name = "phone_num", length = 20, nullable = false, unique = true)
    private String phoneNum;

    @NotBlank(message = "비밀번호를 입력해주세요.")
    @Password
    @Column(name = "password", length = 255, nullable = false)
    private String pw;

    // 비밀번호 확인용 (DB 컬럼 생성 제외, 폼 검증 목적)
    @Transient
    private String pwCheck;

    // 사용자 권한 구분 (기본값: ROLE_USER)
    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 20, nullable = false)
    private Role role = Role.ROLE_USER;

    // 사용자 북마크 목록 (user_bookmark 매핑 테이블로 분리 저장)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "user_bookmark",
            joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id")
    )
    @Column(name = "fish_key", length = 50)
    private java.util.Set<String> bookmarks = new java.util.HashSet<>();
}
