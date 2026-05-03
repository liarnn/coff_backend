package coffee_backend_4j.controller;

import coffee_backend_4j.service.CoffeeService;
import coffee_backend_4j.utils.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/category")
@RequiredArgsConstructor
public class CategoryController {

    private final CoffeeService coffeeService;

    @GetMapping
    public Result<List<Map<String, String>>> getCategories() {
        return Result.ok(coffeeService.getAllCategories());
    }
}
