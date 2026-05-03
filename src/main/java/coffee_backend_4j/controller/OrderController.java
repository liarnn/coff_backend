
package coffee_backend_4j.controller;

import coffee_backend_4j.dto.CreateOrderRequest;
import coffee_backend_4j.dto.OrderDTO;
import coffee_backend_4j.service.OrderService;
import coffee_backend_4j.utils.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/user")
    public Result<Map<String, Object>> createOrder(@RequestBody CreateOrderRequest request) {
        return Result.ok(orderService.createOrder(request));
    }

    @GetMapping("/detail/{orderNumber}")
    public Result<OrderDTO> getOrderDetail(@PathVariable String orderNumber) {
        return Result.ok(orderService.getOrderByOrderNo(orderNumber));
    }

    @GetMapping({"/status", "/status/{page}/{size}", "/status/{page}/{size}/{pathStatus}"})
    public Result<Map<String, Object>> getOrders(
            @PathVariable(required = false) Integer page,
            @PathVariable(required = false) Integer size,
            @PathVariable(required = false) String pathStatus,
            @RequestParam(name = "page", required = false) Integer queryPage,
            @RequestParam(name = "size", required = false) Integer querySize,
            @RequestParam(required = false) String status) {
        if (queryPage != null) page = queryPage;
        if (querySize != null) size = querySize;
        if (page == null) page = 1;
        if (size == null) size = 10;
        return Result.ok(orderService.getOrdersByStatus(page, size, pathStatus != null ? pathStatus : status));
    }

    @GetMapping("/user_id/{userId}")
    public Result<Map<String, Object>> getOrdersByUserId(
            @PathVariable Integer userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.ok(orderService.getOrdersByUserId(userId, page, size));
    }

    @DeleteMapping
    public Result<Void> deleteOrder(@RequestBody Map<String, String> request) {
        String orderNumber = request.getOrDefault("order_no", request.get("orderNumber"));
        orderService.deleteOrder(orderNumber);
        return Result.ok();
    }

    @PostMapping("/pay/{orderNumber}")
    public Result<Void> payOrder(@PathVariable String orderNumber, @RequestBody Map<String, String> request) {
        String paymentMethod = request.getOrDefault("payment_method", "wechat");
        orderService.payOrder(orderNumber, paymentMethod);
        return Result.ok();
    }

    @PostMapping("/confirm/{orderNumber}")
    public Result<Void> confirmOrder(@PathVariable String orderNumber) {
        orderService.confirmOrder(orderNumber);
        return Result.ok();
    }

    @PutMapping
    public Result<Void> updateOrder(@RequestBody OrderDTO orderDTO) {
        orderService.updateOrder(orderDTO);
        return Result.ok();
    }
}
