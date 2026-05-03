package coffee_backend_4j.dto;

import coffee_backend_4j.enums.OrderStatus;
import coffee_backend_4j.enums.PaymentMethod;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {
    private Integer id;

    @JsonProperty("order_number")
    @JsonAlias("orderNumber")
    private String orderNumber;

    @JsonProperty("dine_in_no")
    @JsonAlias("dineInNo")
    private String dineInNo;

    @JsonProperty("user_id")
    @JsonAlias("userId")
    private Integer userId;

    @JsonProperty("total_amount")
    @JsonAlias("totalAmount")
    private Double totalAmount;

    private OrderStatus status = OrderStatus.pending;

    @JsonProperty("payment_method")
    @JsonAlias("paymentMethod")
    private PaymentMethod paymentMethod;

    @JsonProperty("payment_time")
    @JsonAlias("paymentTime")
    private LocalDateTime paymentTime;

    @JsonProperty("completed_time")
    @JsonAlias("completedTime")
    private LocalDateTime completedTime;

    private String notes;

    @JsonProperty("address_id")
    @JsonAlias("addressId")
    private Integer addressId;

    @JsonProperty("is_deleted")
    @JsonAlias("isDeleted")
    private Integer isDeleted = 0;

    @JsonProperty("created_at")
    @JsonAlias("createdAt")
    private LocalDateTime createdAt;

    private List<OrderItemDTO> items;
}
