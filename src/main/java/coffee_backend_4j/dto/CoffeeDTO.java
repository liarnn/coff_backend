package coffee_backend_4j.dto;

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
public class CoffeeDTO {
    private Integer id;
    private String name;
    private String category;
    private String description;

    @JsonProperty("image_url")
    @JsonAlias("imageUrl")
    private String image_url;

    private List<CoffeeVariantsDTO> variants;

    @JsonProperty("is_available")
    @JsonAlias("isAvailable")
    private Boolean is_available = true;

    @JsonProperty("created_at")
    @JsonAlias("createdAt")
    private LocalDateTime created_at;
}
