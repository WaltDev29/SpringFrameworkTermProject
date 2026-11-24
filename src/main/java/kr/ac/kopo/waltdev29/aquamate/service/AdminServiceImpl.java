package kr.ac.kopo.waltdev29.aquamate.service;

import jakarta.persistence.EntityManager;
import kr.ac.kopo.waltdev29.aquamate.domain.Fish;
import kr.ac.kopo.waltdev29.aquamate.domain.Role;
import kr.ac.kopo.waltdev29.aquamate.domain.User;
import kr.ac.kopo.waltdev29.aquamate.repository.SpringDataFishRepository;
import kr.ac.kopo.waltdev29.aquamate.repository.SpringDataUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 관리자 서비스 구현체.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdminServiceImpl implements AdminService {

    private final SpringDataUserRepository userRepository;
    private final SpringDataFishRepository fishRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("userCount", userRepository.count());
        stats.put("fishCount", fishRepository.count());
        stats.put("bookmarkCount", userRepository.countTotalBookmarks());
        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getUsers(String query) {
        List<User> allUsers = userRepository.findAll();
        if (query == null || query.isBlank()) {
            return allUsers;
        }
        String q = query.toLowerCase().trim();
        return allUsers.stream()
                .filter(u -> (u.getId() != null && u.getId().toLowerCase().contains(q)) ||
                             (u.getName() != null && u.getName().toLowerCase().contains(q)) ||
                             (u.getEmail() != null && u.getEmail().toLowerCase().contains(q)))
                .collect(Collectors.toList());
    }

    @Override
    public void updateUserRole(String userId, Role role, String currentUserId) {
        if (userId.equals(currentUserId) && role != Role.ROLE_ADMIN) {
            throw new IllegalArgumentException("자기 자신의 관리자 권한은 회수할 수 없습니다.");
        }
        if ("admin".equalsIgnoreCase(userId) && role != Role.ROLE_ADMIN) {
            throw new IllegalArgumentException("기본 관리자(admin) 계정의 관리자 권한은 박탈할 수 없습니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 관리자에서 일반회원으로 강등하는 경우, 최소 1명 이상의 관리자 유지 검증
        if (user.getRole() == Role.ROLE_ADMIN && role != Role.ROLE_ADMIN) {
            long adminCount = userRepository.countByRole(Role.ROLE_ADMIN);
            if (adminCount <= 1) {
                throw new IllegalStateException("관리자는 최소 1명 이상 존재해야 하므로 권한을 낮출 수 없습니다.");
            }
        }

        user.setRole(role);
        userRepository.save(user);
        log.info("[AdminService] 회원 권한 변경 완료 - userId: {}, newRole: {}", userId, role);
    }

    @Override
    public boolean resetUserPassword(String userId, String newPassword) {
        return userRepository.findById(userId).map(user -> {
            user.setPw(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            log.info("[AdminService] 회원 비밀번호 초기화 완료 - userId: {}", userId);
            return true;
        }).orElse(false);
    }

    @Override
    public void deleteUser(String userId, String currentUserId) {
        if (userId.equals(currentUserId)) {
            throw new IllegalArgumentException("자기 자신 계정은 삭제할 수 없습니다.");
        }
        if ("admin".equalsIgnoreCase(userId)) {
            throw new IllegalArgumentException("기본 관리자(admin) 계정은 삭제할 수 없습니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        if (user.getRole() == Role.ROLE_ADMIN) {
            long adminCount = userRepository.countByRole(Role.ROLE_ADMIN);
            if (adminCount <= 1) {
                throw new IllegalStateException("관리자는 최소 1명 이상 존재해야 하므로 마지막 관리자를 삭제할 수 없습니다.");
            }
        }

        userRepository.delete(user);
        log.info("[AdminService] 회원 강제 탈퇴 완료 - userId: {}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Fish> getAllFishes() {
        return fishRepository.findAllByOrderByNumAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Fish> getFishByKey(String key) {
        return fishRepository.findById(key);
    }

    @Override
    @Transactional(readOnly = true)
    public String getNextFishNum() {
        return fishRepository.getNextFishNum();
    }

    @Override
    public Fish saveFish(Fish fish, org.springframework.web.multipart.MultipartFile imageFile) {
        Optional<Fish> existingOpt = fishRepository.findById(fish.getKey());
        String oldImg = existingOpt.map(Fish::getImg).orElse(null);

        // 이미지 파일이 업로드된 경우 처리
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                java.nio.file.Path uploadDir = java.nio.file.Paths.get("src/main/resources/static/images");
                if (!java.nio.file.Files.exists(uploadDir)) {
                    java.nio.file.Files.createDirectories(uploadDir);
                }

                String orig = imageFile.getOriginalFilename();
                String ext = (orig != null && orig.contains(".")) ? orig.substring(orig.lastIndexOf(".")) : ".jpg";
                String savedName = "fish_" + fish.getKey() + "_" + System.currentTimeMillis() + ext;
                java.nio.file.Path targetPath = uploadDir.resolve(savedName);

                // 소스 디렉토리에 복사
                java.nio.file.Files.copy(imageFile.getInputStream(), targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                // 빌드 디렉토리에도 실시간 반영 복사
                java.nio.file.Path buildDir = java.nio.file.Paths.get("build/resources/main/static/images");
                if (java.nio.file.Files.exists(buildDir)) {
                    java.nio.file.Files.copy(targetPath, buildDir.resolve(savedName), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }

                // 기존 이미지가 존재하고 신규 이미지로 대체되는 경우, 기존 이미지 파일 삭제
                if (oldImg != null && !oldImg.isBlank()) {
                    deleteImageFile(oldImg);
                }

                fish.setImg("images/" + savedName);
                log.info("[AdminService] 물고기 신규 이미지 파일 저장 완료: {}", targetPath);
            } catch (Exception e) {
                log.error("[AdminService] 이미지 파일 업로드 실패: {}", e.getMessage(), e);
            }
        } else {
            // 새 이미지가 업로드되지 않은 경우 기존 이미지 경로 유지
            if ((fish.getImg() == null || fish.getImg().isBlank()) && oldImg != null) {
                fish.setImg(oldImg);
            }
        }

        Fish saved = fishRepository.save(fish);
        log.info("[AdminService] 물고기 데이터 저장 완료 - key: {}, name: {}, num: {}", fish.getKey(), fish.getName(), fish.getNum());
        return saved;
    }

    @Override
    public boolean deleteFish(String key) {
        return fishRepository.findById(key).map(fish -> {
            // 물고기 대표 이미지 서버 파일 삭제
            deleteImageFile(fish.getImg());

            // 연관된 user_bookmark 매핑 레코드 선제적 삭제
            entityManager.createNativeQuery("DELETE FROM user_bookmark WHERE fish_key = :key")
                    .setParameter("key", key)
                    .executeUpdate();

            fishRepository.delete(fish);
            log.info("[AdminService] 물고기 데이터 및 이미지 파일 삭제 완료 - key: {}", key);
            return true;
        }).orElse(false);
    }

    /**
     * 서버 파일시스템에서 이미지 파일을 안전하게 삭제합니다.
     */
    private void deleteImageFile(String imgPath) {
        if (imgPath == null || imgPath.isBlank()) {
            return;
        }

        String normalized = imgPath.trim();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.startsWith("aquamate/")) {
            normalized = normalized.substring("aquamate/".length());
        }

        // 시스템 필수 아이콘 보호 (오삭제 방지)
        List<String> protectedIcons = List.of(
                "logo.png", "logo2.png", "user.png", "tank.png", "heart.png", "book.png",
                "bookmark.png", "filter.png", "email.png", "github.png", "helpBtn.png",
                "help_fish.png", "info_dic.png", "info_match.png", "match_heart.png",
                "sendBtn.png", "Star.png", "star_yellow.png"
        );
        for (String icon : protectedIcons) {
            if (normalized.endsWith(icon)) {
                log.info("[AdminService] 시스템 보호 아이콘은 삭제 대상에서 제외합니다: {}", normalized);
                return;
            }
        }

        try {
            java.nio.file.Path srcPath = java.nio.file.Paths.get("src/main/resources/static").resolve(normalized);
            if (java.nio.file.Files.exists(srcPath)) {
                java.nio.file.Files.delete(srcPath);
                log.info("[AdminService] 소스 디렉토리 이미지 삭제 완료: {}", srcPath);
            }

            java.nio.file.Path buildPath = java.nio.file.Paths.get("build/resources/main/static").resolve(normalized);
            if (java.nio.file.Files.exists(buildPath)) {
                java.nio.file.Files.delete(buildPath);
                log.info("[AdminService] 빌드 디렉토리 이미지 삭제 완료: {}", buildPath);
            }
        } catch (Exception e) {
            log.warn("[AdminService] 이미지 파일 삭제 중 예외 발생 (무시 가능): {}", e.getMessage());
        }
    }
}
