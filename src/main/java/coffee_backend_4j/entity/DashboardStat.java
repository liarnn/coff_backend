package coffee_backend_4j.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dashboard_stat")
public class DashboardStat {

    @TableId
    private Integer id;

    @TableField("total_revenue")
    private Double totalRevenue;

    @TableField("total_orders")
    private Integer totalOrders;

    @TableField("best_drink_name")
    private String bestDrinkName;

    @TableField("best_drink_count")
    private Integer bestDrinkCount;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
