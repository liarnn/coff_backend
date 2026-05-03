package coffee_backend_4j.interceptor;

import coffee_backend_4j.utils.JwtUtil;
import coffee_backend_4j.utils.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String uri = request.getRequestURI();
        if (("GET".equalsIgnoreCase(request.getMethod()) && "/shop/setting".equals(uri))
                || ("POST".equalsIgnoreCase(request.getMethod()) && "/shop/banner/upload".equals(uri))) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未登录或登陆过期...\"}");
            System.out.println("1未登录或登陆过期...");
            return false;
        }

        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未登录或登陆过期...\"}");
            System.out.println("2未登录或登陆过期...");
            return false;
        }

        Integer userId = jwtUtil.getUserIdFromToken(token);
        String redisToken = redisTemplate.opsForValue().get("login:user:" + userId);
        if (redisToken == null || !redisToken.equals(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未登录或登陆过期...\"}");
            System.out.println("3未登录或登陆过期...");
            return false;
        }

        UserContext.setUserId(userId);
        redisTemplate.opsForValue().set("login:user:" + userId, token, 3600, TimeUnit.SECONDS);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserContext.clear();
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}
