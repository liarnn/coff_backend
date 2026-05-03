package coffee_backend_4j.service;

import coffee_backend_4j.dto.CoffeeDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface CoffeeService {

    List<Map<String, String>> getAllCategories();

    Map<String, CoffeeDTO> getCoffeeList(String category);

    CoffeeDTO createCoffee(CoffeeDTO coffeeDTO);

    CoffeeDTO getCoffeeById(Integer coffeeId);

    CoffeeDTO updateCoffee(Integer coffeeId, CoffeeDTO coffeeDTO);

    void deleteCoffee(Integer coffeeId);

    Map<String, Object> getVariantById(Integer variantId);

    Map<String, String> uploadImage(MultipartFile file);

    Map<String, Object> createVariant(Integer coffeeId, Map<String, Object> variantData);

    Map<String, Object> updateVariant(Integer variantId, Map<String, Object> variantData);

    void deleteVariant(Integer variantId);
}
