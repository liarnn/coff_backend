package coffee_backend_4j.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDTO {
    private Integer id;

    @JsonProperty("coffee_id")
    @JsonAlias("coffeeId")
    private Integer coffeeId;

    private Integer quantity = 1;

    @JsonProperty("unit_price")
    @JsonAlias("unitPrice")
    private Double unitPrice;

    private Double subtotal;

    @JsonProperty("coffee_variants_id")
    @JsonAlias("coffeeVariantsId")
    private Integer coffeeVariantsId;

    @JsonProperty("coffee_variants")
    @JsonAlias("coffeeVariants")
    private Map<String, Object> coffeeVariants;

    private Map<String, Object> coffee;
}
