package coffee_backend_4j.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ShopSettingDTO {
    private String name;
    private String business_hours;
    private String address;
    private String phone;
    private String notice;
    private List<Map<String, Object>> banners;
}
