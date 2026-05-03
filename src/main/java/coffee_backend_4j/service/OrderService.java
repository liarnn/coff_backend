
package coffee_backend_4j.service;

import coffee_backend_4j.dto.CreateOrderRequest;
import coffee_backend_4j.dto.OrderDTO;
import coffee_backend_4j.utils.Result;

import java.util.Map;

public interface OrderService {

    Map<String, Object> createOrder(CreateOrderRequest request);

    OrderDTO getOrderByOrderNo(String orderNumber);

    Map<String, Object> getOrdersByStatus(Integer page, Integer size, String status);

    Map<String, Object> getOrdersByUserId(Integer userId, Integer page, Integer size);

    void deleteOrder(String orderNumber);

    void payOrder(String orderNumber, String paymentMethod);

    void confirmOrder(String orderNumber);

    void updateOrder(OrderDTO orderDTO);
}
