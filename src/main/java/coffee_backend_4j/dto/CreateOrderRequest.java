package coffee_backend_4j.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
    @JsonProperty("coffee_variants_ids")
    @JsonAlias("coffeeVariantsIds")
    private List<Integer> coffeeVariantsIds;

    private List<Integer> quantity;

    @JsonProperty("address_id")
    @JsonAlias("addressId")
    private Integer addressId;

    private String notes;
}
