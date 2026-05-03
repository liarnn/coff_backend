package coffee_backend_4j.controller;

import coffee_backend_4j.service.CartService;
import coffee_backend_4j.utils.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping
    public Result<Void> addToCart(@RequestBody Map<String, Object> request) {
        Integer coffeeVariantsId = (Integer) request.get("coffee_variants_id");
        Integer quantity = (Integer) request.get("quantity");
        cartService.addToCart(coffeeVariantsId, quantity);
        return Result.ok();
    }

    @GetMapping("/{page}/{size}")
    public Result<Result.PageData<Map<String, Object>>> getCart(
            @PathVariable Integer page,
            @PathVariable Integer size) {
        return Result.ok(cartService.getCart(page, size));
    }

    @PutMapping
    public Result<Map<String, Object>> updateCartQuantity(@RequestBody Map<String, Object> request) {
        Integer status = (Integer) request.get("status");
        Integer coffeeVariantsId = (Integer) request.get("coffee_variants_id");
        Integer quantity = (Integer) request.get("quantity");
        return Result.ok(cartService.updateCartQuantity(status, coffeeVariantsId, quantity));
    }

    @DeleteMapping
    public Result<Map<String, Object>> deleteCartItem(@RequestBody Map<String, Object> request) {
        Integer coffeeVariantsId = (Integer) request.get("coffee_variants_id");
        return Result.ok(cartService.deleteCartItem(coffeeVariantsId));
    }

    @DeleteMapping("/clear")
    public Result<Void> clearCart() {
        cartService.clearCart();
        return Result.ok();
    }
}
