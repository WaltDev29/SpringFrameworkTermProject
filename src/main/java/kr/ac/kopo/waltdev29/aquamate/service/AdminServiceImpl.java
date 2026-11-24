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

                java.nio.file.Files.copy(imageFile.getInputStream(), targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                fish.setImg("images/" + savedName);
                log.info("[AdminService] 물고기 이미지 파일 저장 완료: {}", targetPath);
            } catch (Exception e) {
                log.error("[AdminService] 이미지 파일 업로드 실패: {}", e.getMessage(), e);
            }
        }

        Fish saved = fishRepository.save(fish);
        log.info("[AdminService] 물고기 데이터 저장 완료 - key: {}, name: {}, num: {}", fish.getKey(), fish.getName(), fish.getNum());
        return saved;
    }

    @Override
    public boolean deleteFish(String key) {
        return fishRepository.findById(key).map(fish -> {
            // 연관된 user_bookmark 매핑 레코드 선제적 삭제
            entityManager.createNativeQuery("DELETE FROM user_bookmark WHERE fish_key = :key")
                    .setParameter("key", key)
                    .executeUpdate();

            fishRepository.delete(fish);
            log.info("[AdminService] 물고기 데이터 삭제 완료 - key: {}", key);
            return true;
        }).orElse(false);
    }
}
