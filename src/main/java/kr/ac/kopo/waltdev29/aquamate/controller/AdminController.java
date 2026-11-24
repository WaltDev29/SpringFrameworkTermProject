package kr.ac.kopo.waltdev29.aquamate.controller;

import kr.ac.kopo.waltdev29.aquamate.domain.Fish;
import kr.ac.kopo.waltdev29.aquamate.domain.Role;
import kr.ac.kopo.waltdev29.aquamate.domain.User;
import kr.ac.kopo.waltdev29.aquamate.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 관리자 전용 웹 및 REST Controller.
 * 인터셉터(AdminAuthInterceptor)를 통해 ROLE_ADMIN 권한이 확인된 요청만 인입됩니다.
 */
@Slf4j
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // ─────────────────────────────────────────────
    // 웹 페이지 매핑
    // ─────────────────────────────────────────────

    /** GET /admin - 관리자 메인 대시보드 */
    @GetMapping({"", "/"})
    public String dashboard(Model model) {
        model.addAttribute("stats", adminService.getDashboardStats());
        model.addAttribute("activeMenu", "dashboard");
        return "admin/dashboard";
    }

    /** GET /admin/users - 회원 관리 페이지 */
    @GetMapping("/users")
    public String userList(@RequestParam(required = false) String q, Model model) {
        List<User> users = adminService.getUsers(q);
        model.addAttribute("users", users);
        model.addAttribute("query", q);
        model.addAttribute("activeMenu", "users");
        return "admin/users";
    }

    /** GET /admin/fish - 물고기 도감 관리 페이지 */
    @GetMapping("/fish")
    public String fishList(Model model) {
        List<Fish> fishList = adminService.getAllFishes();
        model.addAttribute("fishList", fishList);
        model.addAttribute("activeMenu", "fish");
        return "admin/fish";
    }

    // ─────────────────────────────────────────────
    // 회원 관리 REST API
    // ─────────────────────────────────────────────

    /** POST /admin/api/users/{id}/role - 회원 권한 변경 */
    @PostMapping("/api/users/{id}/role")
    @ResponseBody
    public ResponseEntity<?> updateUserRole(@PathVariable String id,
                                           @RequestParam Role role,
                                           jakarta.servlet.http.HttpSession session) {
        String currentUserId = (String) session.getAttribute("userId");
        try {
            adminService.updateUserRole(id, role, currentUserId);
            return ResponseEntity.ok(Map.of("message", "권한이 변경되었습니다."));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** POST /admin/api/users/{id}/reset-password - 회원 비밀번호 초기화 */
    @PostMapping("/api/users/{id}/reset-password")
    @ResponseBody
    public ResponseEntity<?> resetUserPassword(@PathVariable String id, @RequestParam String newPassword) {
        if (newPassword == null || newPassword.isBlank() || newPassword.length() < 8) {
            return ResponseEntity.badRequest().body(Map.of("message", "비밀번호는 최소 8자 이상이어야 합니다."));
        }
        boolean success = adminService.resetUserPassword(id, newPassword);
        return success ? ResponseEntity.ok(Map.of("message", "비밀번호가 성공적으로 초기화되었습니다."))
                       : ResponseEntity.badRequest().body(Map.of("message", "회원을 찾을 수 없습니다."));
    }

    /** DELETE /admin/api/users/{id} - 회원 삭제 */
    @DeleteMapping("/api/users/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteUser(@PathVariable String id,
                                       jakarta.servlet.http.HttpSession session) {
        String currentUserId = (String) session.getAttribute("userId");
        try {
            adminService.deleteUser(id, currentUserId);
            return ResponseEntity.ok(Map.of("message", "회원이 삭제되었습니다."));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────
    // 물고기 관리 REST API
    // ─────────────────────────────────────────────

    /** GET /admin/api/fish/next-num - 다음 도감 번호 자동 채번 조회 */
    @GetMapping("/api/fish/next-num")
    @ResponseBody
    public ResponseEntity<?> getNextFishNum() {
        return ResponseEntity.ok(Map.of("nextNum", adminService.getNextFishNum()));
    }

    /** GET /admin/api/fish/{key} - 물고기 단건 상세 조회 (수정 모달용) */
    @GetMapping("/api/fish/{key}")
    @ResponseBody
    public ResponseEntity<?> getFish(@PathVariable String key) {
        return adminService.getFishByKey(key)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** POST /admin/api/fish - 물고기 등록 또는 수정 (멀티파트 폼 및 파일 첨부 지원) */
    @PostMapping(value = "/api/fish", consumes = {"multipart/form-data"})
    @ResponseBody
    public ResponseEntity<?> saveFishMultipart(
            @ModelAttribute Fish fish,
            @RequestParam(value = "imageFile", required = false) org.springframework.web.multipart.MultipartFile imageFile) {
        if (fish.getKey() == null || fish.getKey().isBlank() ||
            fish.getName() == null || fish.getName().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "물고기 키와 이름은 필수 항목입니다."));
        }
        if (fish.getNum() == null || fish.getNum().isBlank()) {
            fish.setNum(adminService.getNextFishNum());
        }
        Fish saved = adminService.saveFish(fish, imageFile);
        return ResponseEntity.ok(saved);
    }

    /** POST /admin/api/fish - 물고기 등록 또는 수정 (JSON 페이로드 지원 - 캐시 호환) */
    @PostMapping(value = "/api/fish", consumes = {"application/json"})
    @ResponseBody
    public ResponseEntity<?> saveFishJson(@RequestBody Fish fish) {
        if (fish.getKey() == null || fish.getKey().isBlank() ||
            fish.getName() == null || fish.getName().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "물고기 키와 이름은 필수 항목입니다."));
        }
        if (fish.getNum() == null || fish.getNum().isBlank()) {
            fish.setNum(adminService.getNextFishNum());
        }
        Fish saved = adminService.saveFish(fish, null);
        return ResponseEntity.ok(saved);
    }

    /** DELETE /admin/api/fish/{key} - 물고기 삭제 */
    @DeleteMapping("/api/fish/{key}")
    @ResponseBody
    public ResponseEntity<?> deleteFish(@PathVariable String key) {
        boolean success = adminService.deleteFish(key);
        return success ? ResponseEntity.ok(Map.of("message", "물고기 데이터가 삭제되었습니다."))
                       : ResponseEntity.badRequest().body(Map.of("message", "해당 물고기를 찾을 수 없습니다."));
    }
}
