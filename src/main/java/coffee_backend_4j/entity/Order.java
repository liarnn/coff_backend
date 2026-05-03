package coffee_backend_4j.entity;

import coffee_backend_4j.enums.OrderStatus;
import coffee_backend_4j.enums.PaymentMethod;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("orders")
public class Order {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("dine_in_no")
    private String dineInNo;

    @TableField("order_number")
    private String orderNumber;

    @TableField("user_id")
    private Integer userId;

    @TableField("total_amount")
    private Double totalAmount;

    @TableField("status")
    private OrderStatus status = OrderStatus.pending;

    @TableField("payment_method")
    private PaymentMethod paymentMethod;

    @TableField("payment_time")
    private LocalDateTime paymentTime;

    @TableField("completed_time")
    private LocalDateTime completedTime;

    @TableField("notes")
    private String notes;

    @TableField("address_id")
    private Integer addressId;

    @TableField("is_deleted")
    private Integer isDeleted = 0;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
