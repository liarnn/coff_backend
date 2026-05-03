package coffee_backend_4j.entity;

import coffee_backend_4j.enums.UserRole;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("users")
public class User {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("username")
    private String username;

    @TableField("phone")
    private String phone;

    @TableField("email")
    private String email;

    @TableField("hashed_password")
    private String hashedPassword;

    @TableField("full_name")
    private String fullName;

    @TableField("role")
    private UserRole role = UserRole.customer;

    @TableField("is_active")
    private Boolean isActive = true;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
