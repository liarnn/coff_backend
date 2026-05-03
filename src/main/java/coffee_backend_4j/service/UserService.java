package coffee_backend_4j.service;

import coffee_backend_4j.dto.LoginRequest;
import coffee_backend_4j.dto.LoginResponse;

public interface UserService {

    void sendCaptcha(String phone);

    LoginResponse login(LoginRequest request);

    void logout();
}
