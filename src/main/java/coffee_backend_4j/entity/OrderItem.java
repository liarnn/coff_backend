package coffee_backend_4j.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("order_items")
public class OrderItem {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("order_id")
    private Integer orderId;

    @TableField("coffee_id")
    private Integer coffeeId;

    @TableField("quantity")
    private Integer quantity = 1;

    @TableField("unit_price")
    private Double unitPrice;

    @TableField("coffee_variants_id")
    private Integer coffeeVariantsId;

    @TableField("subtotal")
    private Double subtotal;
}
