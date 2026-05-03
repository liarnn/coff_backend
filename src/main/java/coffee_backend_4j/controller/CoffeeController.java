package coffee_backend_4j.controller;

import coffee_backend_4j.dto.CoffeeDTO;
import coffee_backend_4j.service.CoffeeService;
import coffee_backend_4j.utils.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/coffee")
@RequiredArgsConstructor
public class CoffeeController {

    private final CoffeeService coffeeService;

    @GetMapping("/categories")
    public Result<List<Map<String, String>>> getCategories() {
        return Result.ok(coffeeService.getAllCategories());
    }

    @GetMapping({"/category", "/category/{category}"})
    public Result<Map<String, CoffeeDTO>> getCoffeeList(@PathVariable(required = false) String category) {
        return Result.ok(coffeeService.getCoffeeList(category));
    }

    @GetMapping("/{coffeeId}")
    public Result<CoffeeDTO> getCoffeeDetail(@PathVariable Integer coffeeId) {
        return Result.ok(coffeeService.getCoffeeById(coffeeId));
    }

    @PostMapping
    public Result<CoffeeDTO> createCoffee(@RequestBody CoffeeDTO coffeeDTO) {
        return Result.ok(coffeeService.createCoffee(coffeeDTO));
    }

    @PutMapping("/{coffeeId}")
    public Result<CoffeeDTO> updateCoffee(@PathVariable Integer coffeeId, @RequestBody CoffeeDTO coffeeDTO) {
        return Result.ok(coffeeService.updateCoffee(coffeeId, coffeeDTO));
    }

    @DeleteMapping("/{coffeeId}")
    public Result<Void> deleteCoffee(@PathVariable Integer coffeeId) {
        coffeeService.deleteCoffee(coffeeId);
        return Result.ok();
    }

    @PostMapping("/upload")
    public Result<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        return Result.ok(coffeeService.uploadImage(file));
    }

    @PostMapping("/{coffeeId}/variant")
    public Result<Map<String, Object>> createVariant(@PathVariable Integer coffeeId, @RequestBody Map<String, Object> variantData) {
        return Result.ok(coffeeService.createVariant(coffeeId, variantData));
    }

    @PutMapping("/variant/{variantId}")
    public Result<Map<String, Object>> updateVariant(@PathVariable Integer variantId, @RequestBody Map<String, Object> variantData) {
        return Result.ok(coffeeService.updateVariant(variantId, variantData));
    }

    @DeleteMapping("/variant/{variantId}")
    public Result<Void> deleteVariant(@PathVariable Integer variantId) {
        coffeeService.deleteVariant(variantId);
        return Result.ok();
    }
}
