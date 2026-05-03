package coffee_backend_4j.service.impl;

import coffee_backend_4j.dto.LoginRequest;
import coffee_backend_4j.dto.LoginResponse;
import coffee_backend_4j.dto.UserDTO;
import coffee_backend_4j.entity.User;
import coffee_backend_4j.enums.UserRole;
import coffee_backend_4j.exception.BusinessException;
import coffee_backend_4j.mapper.UserMapper;
import coffee_backend_4j.service.UserService;
import coffee_backend_4j.utils.JwtUtil;
import coffee_backend_4j.utils.UserContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, String> redisTemplate;
    private final Random random = new Random();

    @Override
    public void sendCaptcha(String phone) {
        if (!isValidPhone(phone)) {
            throw new BusinessException("手机号码格式错误");
        }

        String redisKey = "valid_code:" + phone;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
            throw new BusinessException("验证码已发送，请稍等");
        }

        String validCode = String.format("%06d", random.nextInt(1000000));
        redisTemplate.opsForValue().set(redisKey, validCode, 60, TimeUnit.SECONDS);

        System.out.println("\n" + "=".repeat(30));
        System.out.println("【模拟短信中心】");
        System.out.println("发送至: " + phone);
        System.out.println("验证码: " + validCode);
        System.out.println("有效期: 60秒");
        System.out.println("=".repeat(30) + "\n");
        return;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        String phone = request.getPhone();
        String code = request.getCode();

        if (!isValidPhone(phone)) {
            throw new BusinessException("手机号码格式错误");
        }

        String redisKey = "valid_code:" + phone;
        String validCode = redisTemplate.opsForValue().get(redisKey);
        if (validCode == null) {
            throw new BusinessException("验证码失效或已过期");
        }
        if (!validCode.equals(code)) {
            throw new BusinessException("验证码错误");
        }

        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getPhone, phone)
        );

        if (user == null) {
            user = new User();
            user.setPhone(phone);
            user.setUsername(UUID.nameUUIDFromBytes(phone.getBytes()).toString().substring(0, 8));
            user.setRole(UserRole.customer);
            user.setIsActive(true);
            user.setHashedPassword("qidian_7.");
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.insert(user);
        }

        String token = jwtUtil.generateToken(user.getId());
        redisTemplate.opsForValue().set("login:user:" + user.getId(), token, 3600, TimeUnit.SECONDS);

        UserDTO userDTO = convertToDTO(user);
        LoginResponse response = new LoginResponse(token, userDTO);

        return response;
    }

    @Override
    public void logout() {
        Integer userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }
        try {
            redisTemplate.delete("login:user:" + userId);
        } catch (Exception e) {
            throw new BusinessException("用户登录状态异常...");
        }
        return;
    }

    private boolean isValidPhone(String phone) {
        return phone != null && phone.matches("^1[3-9]\\d{9}$");
    }

    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setPhone(user.getPhone());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setRole(user.getRole());
        dto.setIsActive(user.getIsActive());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }
}
