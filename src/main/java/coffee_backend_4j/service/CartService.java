package coffee_backend_4j.service;

import coffee_backend_4j.utils.Result;

import java.util.Map;

public interface CartService {

    void addToCart(Integer coffeeVariantsId, Integer quantity);

    Result.PageData<Map<String, Object>> getCart(Integer page, Integer size);

    Map<String, Object> updateCartQuantity(Integer status, Integer coffeeVariantsId, Integer quantity);

    Map<String, Object> deleteCartItem(Integer coffeeVariantsId);

    void clearCart();
}
