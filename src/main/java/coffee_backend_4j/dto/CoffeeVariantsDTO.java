package coffee_backend_4j.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CoffeeVariantsDTO {
    private Integer id;

    @JsonProperty("coffee_id")
    @JsonAlias("coffeeId")
    private Integer coffee_id;

    @JsonProperty("cup_size")
    @JsonAlias("cupSize")
    private String cup_size;

    private Double price;
    private Integer stock;
}
