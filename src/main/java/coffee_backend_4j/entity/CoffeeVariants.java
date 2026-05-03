package coffee_backend_4j.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("coffee_variants")
public class CoffeeVariants {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("coffee_id")
    private Integer coffeeId;

    @TableField("cup_size")
    private String cupSize = "S";

    @TableField("price")
    private Double price;

    @TableField("stock")
    private Integer stock = 0;
}
