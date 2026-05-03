package coffee_backend_4j.controller;

import coffee_backend_4j.dto.LoginRequest;
import coffee_backend_4j.dto.LoginResponse;
import coffee_backend_4j.service.UserService;
import coffee_backend_4j.utils.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/captcha")
    public Result<Void> sendCaptcha(@RequestBody LoginRequest request) {
        log.info("发送验证码: " + request.getPhone());
        userService.sendCaptcha(request.getPhone());
        return Result.ok();
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        return Result.ok(userService.login(request));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        userService.logout();
        return Result.ok();
    }
}
