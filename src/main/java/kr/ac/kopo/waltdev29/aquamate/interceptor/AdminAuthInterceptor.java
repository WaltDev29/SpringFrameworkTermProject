package kr.ac.kopo.waltdev29.aquamate.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import kr.ac.kopo.waltdev29.aquamate.domain.Role;
import kr.ac.kopo.waltdev29.aquamate.repository.SpringDataUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 관리자 페이지 접근 권한 검증 인터셉터.
 * /admin/** 경로 접근 시 세션 로그인 상태 및 ROLE_ADMIN 권한을 검증합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAuthInterceptor implements HandlerInterceptor {

    private final SpringDataUserRepository userRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        String contextPath = request.getContextPath();

        if (session == null || session.getAttribute("userId") == null) {
            log.warn("[AdminAuthInterceptor] 미인증 사용자의 관리자 페이지 접근 시도: {}", request.getRequestURI());
            response.sendRedirect(contextPath + "/login");
            return false;
        }

        String userId = (String) session.getAttribute("userId");
        String userRole = (String) session.getAttribute("userRole");

        // 세션에 권한 정보가 없으면 DB에서 재확인
        if (userRole == null) {
            userRole = userRepository.findById(userId)
                    .map(u -> u.getRole() != null ? u.getRole().name() : Role.ROLE_USER.name())
                    .orElse(Role.ROLE_USER.name());
            session.setAttribute("userRole", userRole);
        }

        if (!Role.ROLE_ADMIN.name().equals(userRole)) {
            log.warn("[AdminAuthInterceptor] 권한 없는 사용자의 관리자 페이지 접근 차단 - userId: {}, role: {}", userId, userRole);
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "관리자 권한이 필요합니다.");
            return false;
        }

        return true;
    }
}
