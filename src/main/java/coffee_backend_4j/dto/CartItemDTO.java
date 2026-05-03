package coffee_backend_4j.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDTO {
    private Integer id;

    @JsonProperty("user_id")
    @JsonAlias("userId")
    private Integer userId;

    @JsonProperty("coffee_id")
    @JsonAlias("coffeeId")
    private Integer coffeeId;

    private Integer quantity = 1;

    @JsonProperty("coffee_variants_id")
    @JsonAlias("coffeeVariantsId")
    private Integer coffeeVariantsId;

    @JsonProperty("coffee_variants")
    @JsonAlias("coffeeVariants")
    private CoffeeVariantsDTO coffeeVariants;
}
