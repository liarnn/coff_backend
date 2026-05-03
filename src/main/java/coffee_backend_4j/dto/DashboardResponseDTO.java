package coffee_backend_4j.dto;

import lombok.Data;
import java.util.List;

@Data
public class DashboardResponseDTO {
    private Integer today_orders;
    private Double today_revenue;
    private Integer on_sale_count;
    private String today_best_drink;
    private Integer total_orders;
    private Double total_revenue;
    private String total_best_drink;
    private List<SalesItemDTO> today_sales_top7;
    private List<SalesItemDTO> history_sales_top7;
}
