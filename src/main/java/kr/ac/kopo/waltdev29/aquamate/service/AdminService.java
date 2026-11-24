package kr.ac.kopo.waltdev29.aquamate.service;

import kr.ac.kopo.waltdev29.aquamate.domain.Fish;
import kr.ac.kopo.waltdev29.aquamate.domain.Role;
import kr.ac.kopo.waltdev29.aquamate.domain.User;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 관리자 서비스 인터페이스.
 */
public interface AdminService {

    // ─── 대시보드 통계 ───
    Map<String, Object> getDashboardStats();

    // ─── 회원 관리 ───
    List<User> getUsers(String query);
    void updateUserRole(String userId, Role role, String currentUserId);
    boolean resetUserPassword(String userId, String newPassword);
    void deleteUser(String userId, String currentUserId);

    // ─── 물고기 도감 관리 ───
    List<Fish> getAllFishes();
    Optional<Fish> getFishByKey(String key);
    String getNextFishNum();
    Fish saveFish(Fish fish, org.springframework.web.multipart.MultipartFile imageFile);
    boolean deleteFish(String key);
}
