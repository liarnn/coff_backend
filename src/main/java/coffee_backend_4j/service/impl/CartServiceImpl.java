package coffee_backend_4j.service.impl;

import coffee_backend_4j.dto.CoffeeDTO;
import coffee_backend_4j.entity.CartItem;
import coffee_backend_4j.exception.BusinessException;
import coffee_backend_4j.mapper.CartItemMapper;
import coffee_backend_4j.service.CartService;
import coffee_backend_4j.service.CoffeeService;
import coffee_backend_4j.utils.UserContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartItemMapper cartItemMapper;
    private final CoffeeService coffeeService;

    @Override
    @Transactional
    public void addToCart(Integer coffeeVariantsId, Integer quantity) {
        Integer userId = UserContext.getUserId();
        if (userId == null || coffeeVariantsId == null || quantity == null) {
            throw new BusinessException("参数错误：用户ID/规格ID/数量不能为空");
        }

        Map<String, Object> variant = coffeeService.getVariantById(coffeeVariantsId);

        cartItemMapper.delete(
            new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getCoffeeVariantsId, coffeeVariantsId)
        );

        try {
            CartItem cartItem = new CartItem();
            cartItem.setUserId(userId);
            cartItem.setCoffeeId((Integer) variant.get("coffee_id"));
            cartItem.setCoffeeVariantsId(coffeeVariantsId);
            cartItem.setQuantity(quantity);
            cartItem.setStatus(1);
            cartItem.setCreatedAt(LocalDateTime.now());
            cartItem.setUpdatedAt(LocalDateTime.now());
            cartItemMapper.insert(cartItem);
        } catch (Exception e) {
            System.out.println("添加购物车失败：" + e.getMessage());
            throw new BusinessException("添加购物车失败，请重试");
        }
    }

    @Override
    public coffee_backend_4j.utils.Result.PageData<Map<String, Object>> getCart(Integer page, Integer size) {
        Integer userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }
        List<CartItem> allCartItems = cartItemMapper.selectList(
            new LambdaQueryWrapper<CartItem>().eq(CartItem::getUserId, userId)
        );

        Page<CartItem> pageParam = new Page<>(page, size);
        IPage<CartItem> cartPage = cartItemMapper.selectPage(pageParam, 
            new LambdaQueryWrapper<CartItem>().eq(CartItem::getUserId, userId)
        );

        double total = 0;

        for (CartItem item : allCartItems) {
            if (item.getStatus() == 1) {
                try {
                    Map<String, Object> variant = coffeeService.getVariantById(item.getCoffeeVariantsId());
                    Double price = (Double) variant.get("price");
                    total += price * item.getQuantity();
                } catch (Exception e) {
                    // 忽略单条商品规格异常，不影响整体返回
                }
            }
        }

        List<Map<String, Object>> cartList = cartPage.getRecords().stream()
                .map(item -> {
                    Map<String, Object> itemMap = new HashMap<>();
                    itemMap.put("id", item.getId());
                    itemMap.put("user_id", item.getUserId());
                    itemMap.put("coffee_id", item.getCoffeeId());
                    itemMap.put("coffee_variants_id", item.getCoffeeVariantsId());
                    itemMap.put("quantity", item.getQuantity());
                    itemMap.put("status", item.getStatus());

                    try {
                        Map<String, Object> variant = coffeeService.getVariantById(item.getCoffeeVariantsId());
                        itemMap.put("coffee_variants", variant);
                        itemMap.put("cup_size", variant.get("cup_size"));
                        itemMap.put("price", variant.get("price"));
                    } catch (Exception e) {
                        System.out.println("获取规格信息失败: " + e.getMessage());
                    }

                    try {
                        CoffeeDTO coffee = coffeeService.getCoffeeById(item.getCoffeeId());
                        itemMap.put("coffee_name", coffee.getName());
                        itemMap.put("image_url", coffee.getImage_url());
                        itemMap.put("category", coffee.getCategory());
                    } catch (Exception e) {
                        System.out.println("鑾峰彇鍜栧暋淇℃伅澶辫触: " + e.getMessage());
                    }

                    return itemMap;
                })
                .toList();

        Map<String, Object> data = new HashMap<>();
        data.put("cart", cartList);
        data.put("total", total);

        coffee_backend_4j.utils.Result.PageData<Map<String, Object>> pageData =
                new coffee_backend_4j.utils.Result.PageData<>(data, page, size, cartPage.getTotal());
        return pageData;
    }

    @Override
    @Transactional
    public Map<String, Object> updateCartQuantity(Integer status, Integer coffeeVariantsId, Integer quantity) {
        Integer userId = UserContext.getUserId();
        if (userId == null || quantity == null || coffeeVariantsId == null || quantity <= 0) {
            throw new BusinessException("参数错误：用户ID/商品ID/状态不能为空，数量必须大于0");
        }

        CartItem cartItem = cartItemMapper.selectOne(
            new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getCoffeeVariantsId, coffeeVariantsId)
        );

        if (cartItem == null) {
            throw new BusinessException("购物车中无该商品，无法更新");
        }

        cartItem.setQuantity(quantity);
        cartItem.setStatus(status);
        cartItem.setUpdatedAt(LocalDateTime.now());

        try {
            cartItemMapper.updateById(cartItem);
            return convertCartItemToPythonShape(cartItem);
        } catch (Exception e) {
            System.out.println("更新数量失败：" + e.getMessage());
            throw new BusinessException("更新商品数量失败，请重试");
        }
    }

    @Override
    @Transactional
    public Map<String, Object> deleteCartItem(Integer coffeeVariantsId) {
        Integer userId = UserContext.getUserId();
        if (userId == null || coffeeVariantsId == null) {
            throw new BusinessException("参数错误：用户ID/商品ID不能为空");
        }

        cartItemMapper.delete(
            new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getCoffeeVariantsId, coffeeVariantsId)
        );

        Map<String, Object> data = new HashMap<>();
        data.put("coffee_id", coffeeVariantsId);
        return data;
    }

    @Override
    @Transactional
    public void clearCart() {
        Integer userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }

        try {
            cartItemMapper.delete(
                new LambdaQueryWrapper<CartItem>().eq(CartItem::getUserId, userId)
            );
        } catch (Exception e) {
            System.out.println("清空购物车失败：" + e.getMessage());
            throw new BusinessException("清空购物车失败，请重试");
        }
    }

    private Map<String, Object> convertCartItemToPythonShape(CartItem item) {
        Map<String, Object> itemMap = new HashMap<>();
        itemMap.put("id", item.getId());
        itemMap.put("user_id", item.getUserId());
        itemMap.put("coffee_id", item.getCoffeeId());
        itemMap.put("quantity", item.getQuantity());
        itemMap.put("coffee_variants_id", item.getCoffeeVariantsId());
        itemMap.put("status", item.getStatus());
        return itemMap;
    }
}
