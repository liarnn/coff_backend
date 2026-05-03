package coffee_backend_4j.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddressDTO {
    private Integer id;

    @JsonProperty("user_id")
    @JsonAlias("userId")
    private Integer userId;

    private String receiver;
    private String phone;
    private String province;
    private String city;
    private String district;

    @JsonProperty("detail_address")
    @JsonAlias("detailAddress")
    private String detailAddress;

    @JsonProperty("is_default")
    @JsonAlias("isDefault")
    private Boolean isDefault = false;
}
