package coffee_backend_4j.dto;

import coffee_backend_4j.enums.UserRole;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Integer id;
    private String username;
    private String phone;
    private String email;

    @JsonProperty("full_name")
    @JsonAlias("fullName")
    private String fullName;

    @JsonProperty("hashed_password")
    @JsonAlias("hashedPassword")
    private String hashedPassword;

    private UserRole role = UserRole.customer;

    @JsonProperty("is_active")
    @JsonAlias("isActive")
    private Boolean isActive = true;

    @JsonProperty("created_at")
    @JsonAlias("createdAt")
    private LocalDateTime createdAt;

    private String code;
}
