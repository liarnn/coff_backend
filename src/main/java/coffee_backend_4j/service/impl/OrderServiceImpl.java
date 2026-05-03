
package coffee_backend_4j.service.impl;

import coffee_backend_4j.dto.CoffeeDTO;
import coffee_backend_4j.dto.CreateOrderRequest;
import coffee_backend_4j.dto.OrderDTO;
import coffee_backend_4j.dto.OrderItemDTO;
import coffee_backend_4j.entity.Order;
import coffee_backend_4j.entity.OrderItem;
import coffee_backend_4j.enums.OrderStatus;
import coffee_backend_4j.enums.PaymentMethod;
import coffee_backend_4j.exception.BusinessException;
import coffee_backend_4j.mapper.OrderItemMapper;
import coffee_backend_4j.mapper.OrderMapper;
import coffee_backend_4j.service.CoffeeService;
import coffee_backend_4j.service.OrderService;
import coffee_backend_4j.utils.SnowflakeIdUtil;
import coffee_backend_4j.utils.UserContext;
import coffee_backend_4j.websocket.CoffeeWebSocketHandler;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final CoffeeService coffeeService;
    private final SnowflakeIdUtil snowflakeIdUtil;
    private final RedisTemplate<String, String> redisTemplate;
    private final CoffeeWebSocketHandler webSocketHandler;

    @Override
    @Transactional
    public Map<String, Object> createOrder(CreateOrderRequest request) {
        Integer userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }
        
        List<Integer> coffeeVariantsIds = request.getCoffeeVariantsIds();
        List<Integer> quantities = request.getQuantity();
        
        // 验证请求参数
        if (coffeeVariantsIds == null || coffeeVariantsIds.isEmpty()) {
            throw new BusinessException("商品不能为空");
        }
        if (quantities == null || quantities.isEmpty()) {
            throw new BusinessException("商品数量不能为空");
        }
        if (coffeeVariantsIds.size() != quantities.size()) {
            throw new BusinessException("商品和数量不匹配");
        }

        // 先获取所有商品规格信息并计算总金额
        List<Map<String, Object>> variantDataList = new java.util.ArrayList<>();
        double total = 0;
        for (int i = 0; i < coffeeVariantsIds.size(); i++) {
            Integer coffeeVariantsId = coffeeVariantsIds.get(i);
            Integer quantity = quantities.get(i);
            
            if (coffeeVariantsId == null) {
                throw new BusinessException("商品规格不能为空");
            }
            if (quantity == null || quantity <= 0) {
                throw new BusinessException("商品数量必须大于0");
            }

            Map<String, Object> variant = coffeeService.getVariantById(coffeeVariantsId);
            if (variant == null) {
                throw new BusinessException("商品规格不存在");
            }
            
            double subtotal = (Double) variant.get("price") * quantity;
            total += subtotal;
            
            // 保存商品规格信息以便后续使用
            Map<String, Object> itemData = new HashMap<>();
            itemData.put("variant", variant);
            itemData.put("quantity", quantity);
            itemData.put("subtotal", subtotal);
            variantDataList.add(itemData);
        }

        String orderNumber = snowflakeIdUtil.generateOrderNo();
        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setUserId(userId);
        order.setStatus(OrderStatus.pending);
        order.setAddressId(request.getAddressId());
        order.setNotes(request.getNotes());
        order.setTotalAmount(total); // 设置总金额
        order.setIsDeleted(0);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        if (request.getAddressId() == null) {
            try {
                String dineInNoStr = redisTemplate.opsForValue().get("dine_in_no");
                Integer dineInNo = dineInNoStr != null ? Integer.parseInt(dineInNoStr) + 1 : 110001;
                if (dineInNo > 61024) {
                    dineInNo = 10002;
                }
                redisTemplate.opsForValue().set("dine_in_no", dineInNo.toString());
                order.setDineInNo(dineInNo.toString());
            } catch (Exception e) {
                System.out.println("获取堂食号失败: " + e.getMessage());
                order.setDineInNo("110001");
                redisTemplate.opsForValue().set("dine_in_no", "110001");
            }
        }

        try {
            orderMapper.insert(order);

            // 插入订单项
            for (int i = 0; i < variantDataList.size(); i++) {
                Map<String, Object> itemData = variantDataList.get(i);
                Map<String, Object> variant = (Map<String, Object>) itemData.get("variant");
                Integer quantity = (Integer) itemData.get("quantity");
                Double subtotal = (Double) itemData.get("subtotal");

                OrderItem orderItem = new OrderItem();
                orderItem.setOrderId(order.getId());
                orderItem.setCoffeeVariantsId((Integer) variant.get("id"));
                orderItem.setCoffeeId((Integer) variant.get("coffee_id"));
                orderItem.setQuantity(quantity);
                orderItem.setUnitPrice((Double) variant.get("price"));
                orderItem.setSubtotal(subtotal);

                orderItemMapper.insert(orderItem);
            }

            String redisKey = "order:pay:" + orderNumber;
            redisTemplate.opsForValue().set(redisKey, "1", 900, TimeUnit.SECONDS);

            Map<String, Object> result = new HashMap<>();
            result.put("order_number", orderNumber);
            result.put("order_id", order.getId());
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            System.out.println("添加订单失败: " + e.getMessage());
            e.printStackTrace();
            throw new BusinessException("创建订单失败，请重试");
        }
    }

    @Override
    public OrderDTO getOrderByOrderNo(String orderNumber) {
        Integer userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }
        try {
            Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderNumber, orderNumber)
            );
            if (order == null) {
                throw new BusinessException("订单不存在");
            }

            if (!order.getUserId().equals(userId) && userId > 100) {
                throw new BusinessException("无权限查看此订单");
            }

            return convertToDTO(order);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            System.out.println("查询订单失败: " + e.getMessage());
            throw new BusinessException("查询订单失败");
        }
    }

    @Override
    public Map<String, Object> getOrdersByStatus(Integer page, Integer size, String status) {
        Integer userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }
        try {
            Page<Order> pageParam = new Page<>(page, size);
            LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.orderByDesc(Order::getCreatedAt);

            OrderStatus statusEnum = null;
            if (status != null && !status.isEmpty()) {
                try {
                    statusEnum = OrderStatus.valueOf(status);
                } catch (Exception e) {
                }
            }

            if (userId > 100) {
                queryWrapper.eq(Order::getUserId, userId).eq(Order::getIsDeleted, 0);
                if (statusEnum != null) {
                    queryWrapper.eq(Order::getStatus, statusEnum);
                }
            } else {
                if (statusEnum != null) {
                    queryWrapper.eq(Order::getStatus, statusEnum);
                }
            }

            IPage<Order> orderPage = orderMapper.selectPage(pageParam, queryWrapper);

            List<OrderDTO> orderDTOs = orderPage.getRecords().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            Map<String, Object> result = new HashMap<>();
            result.put("data", orderDTOs);
            result.put("total", orderPage.getTotal());
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            System.out.println("查询订单失败: " + e.getMessage());
            throw new BusinessException("查询订单失败...");
        }
    }

    @Override
    public Map<String, Object> getOrdersByUserId(Integer userId, Integer page, Integer size) {
        Integer currentUserId = UserContext.getUserId();
        if (currentUserId == null) {
            throw new BusinessException("用户未登录");
        }
        try {
            if (currentUserId > 100) {
                throw new BusinessException("无权限");
            }

            Page<Order> pageParam = new Page<>(page, size);
            LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.orderByDesc(Order::getCreatedAt);
            queryWrapper.eq(Order::getUserId, userId);

            IPage<Order> orderPage = orderMapper.selectPage(pageParam, queryWrapper);

            List<OrderDTO> orderDTOs = orderPage.getRecords().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            Map<String, Object> result = new HashMap<>();
            result.put("data", orderDTOs);
            result.put("total", orderPage.getTotal());
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            System.out.println("查询订单失败: " + e.getMessage());
            throw new BusinessException("查询订单失败...");
        }
    }

    @Override
    @Transactional
    public void deleteOrder(String orderNumber) {
        Integer userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }
        try {
            Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderNumber, orderNumber)
            );
            if (order == null) {
                throw new BusinessException("订单不存在");
            }

            if (!order.getUserId().equals(userId)) {
                throw new BusinessException("无权限删除此订单");
            }

            if (order.getStatus() != OrderStatus.pending) {
                throw new BusinessException("只能删除待支付订单");
            }

            order.setIsDeleted(1);
            orderMapper.updateById(order);
            return;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            System.out.println("删除订单失败: " + e.getMessage());
            throw new BusinessException("删除订单失败");
        }
    }

    @Override
    @Transactional
    public void payOrder(String orderNumber, String paymentMethod) {
        Integer userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }
        try {
            String redisKey = "order:pay:" + orderNumber;
            String redisValue = redisTemplate.opsForValue().get(redisKey);

            if (redisValue == null) {
                throw new BusinessException("订单已过期，请重新下单");
            }

            Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderNumber, orderNumber)
            );
            if (order == null) {
                throw new BusinessException("订单不存在");
            }

            if (!order.getUserId().equals(userId)) {
                throw new BusinessException("无权限支付此订单");
            }

            if (order.getStatus() != OrderStatus.pending) {
                throw new BusinessException("订单状态不正确，无法支付");
            }

            if (order.getTotalAmount() <= 0) {
                throw new BusinessException("订单金额不正确");
            }

            PaymentMethod method;
            try {
                method = PaymentMethod.valueOf(paymentMethod);
            } catch (IllegalArgumentException e) {
                throw new BusinessException("不支持的支付方式");
            }

            order.setStatus(OrderStatus.paid);
            order.setPaymentMethod(method);
            order.setPaymentTime(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());

            orderMapper.updateById(order);
            redisTemplate.delete(redisKey);
            sendNewOrder(order);

            return;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            System.out.println("支付订单失败: " + e.getMessage());
            throw new BusinessException("支付失败，请重试");
        }
    }

    @Override
    @Transactional
    public void confirmOrder(String orderNumber) {
        Integer userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }
        try {
            Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderNumber, orderNumber)
            );
            if (order == null) {
                throw new BusinessException("订单不存在");
            }

            if (!order.getUserId().equals(userId)) {
                throw new BusinessException("无权限确认此订单");
            }

            if (order.getStatus() != OrderStatus.shipped) {
                throw new BusinessException("只能确认已发货的订单");
            }

            order.setStatus(OrderStatus.completed);
            order.setCompletedTime(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());
            orderMapper.updateById(order);
            sendOrderStatusUpdate(order);

            return;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            System.out.println("确认收货失败: " + e.getMessage());
            throw new BusinessException("确认收货失败，请重试");
        }
    }

    @Override
    @Transactional
    public void updateOrder(OrderDTO orderDTO) {
        Integer userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }
        try {
            Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderNumber, orderDTO.getOrderNumber())
            );
            if (order == null) {
                throw new BusinessException("订单不存在");
            }

            if (userId <= 100 && orderDTO.getStatus() != OrderStatus.pending) {
                if (orderDTO.getStatus() == OrderStatus.completed) {
                    order.setStatus(orderDTO.getStatus());
                    order.setCompletedTime(LocalDateTime.now());
                } else {
                    order.setAddressId(orderDTO.getAddressId());
                    order.setStatus(orderDTO.getStatus());
                    order.setNotes(orderDTO.getNotes());
                }
                order.setUpdatedAt(LocalDateTime.now());
                orderMapper.updateById(order);
                sendOrderStatusUpdate(order);
                return;
            }

            if (!order.getUserId().equals(userId)) {
                throw new BusinessException("无权限");
            }

            if (order.getStatus() != OrderStatus.pending) {
                throw new BusinessException("仅可修改待支付订单");
            }

            return;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            System.out.println("更新订单失败: " + e.getMessage());
            throw new BusinessException("更新订单失败");
        }
    }

    private OrderDTO convertToDTO(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setDineInNo(order.getDineInNo());
        dto.setUserId(order.getUserId());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setStatus(order.getStatus());
        dto.setPaymentMethod(order.getPaymentMethod());
        dto.setPaymentTime(order.getPaymentTime());
        dto.setCompletedTime(order.getCompletedTime());
        dto.setNotes(order.getNotes());
        dto.setAddressId(order.getAddressId());
        dto.setIsDeleted(order.getIsDeleted());
        dto.setCreatedAt(order.getCreatedAt());

        List<OrderItem> items = orderItemMapper.selectList(
            new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId())
        );
        List<OrderItemDTO> itemDTOs = items.stream().map(this::convertItemToDTO).collect(Collectors.toList());
        dto.setItems(itemDTOs);

        return dto;
    }

    private void sendNewOrder(Order order) {
        try {
            OrderDTO orderDTO = convertToDTO(order);
            Map<String, Object> merchantMessage = new HashMap<>();
            merchantMessage.put("type", "new_order");
            merchantMessage.put("data", orderDTO);
            merchantMessage.put("message", "新订单 " + order.getOrderNumber() + " 待接单");
            webSocketHandler.broadcast(merchantMessage);
            log.info("订单 {} 支付成功，已向商家推送", order.getOrderNumber());

            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("type", "order_update");
            userMessage.put("data", orderDTO);
            userMessage.put("message", getStatusMessage(order.getStatus()));
            webSocketHandler.sendToClient("user_" + order.getUserId(), userMessage);
            log.info("订单 {} 支付成功，已向用户 {} 推送", order.getOrderNumber(), order.getUserId());
        } catch (Exception e) {
            log.error("WebSocket 推送失败: {}", e.getMessage());
        }
    }

    private void sendOrderStatusUpdate(Order order) {
        try {
            OrderDTO orderDTO = convertToDTO(order);
            Map<String, Object> merchantMessage = new HashMap<>();
            merchantMessage.put("type", "order_status_update");
            merchantMessage.put("data", orderDTO);
            merchantMessage.put("message", "订单 " + order.getOrderNumber() + " 状态已更新为 " + order.getStatus());
            webSocketHandler.broadcast(merchantMessage);
            log.info("订单 {} 状态更新，已向商家推送", order.getOrderNumber());

            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("type", "order_update");
            userMessage.put("data", orderDTO);
            userMessage.put("message", getStatusMessage(order.getStatus()));
            webSocketHandler.sendToClient("user_" + order.getUserId(), userMessage);
            log.info("订单 {} 状态更新，已向用户 {} 推送", order.getOrderNumber(), order.getUserId());
        } catch (Exception e) {
            log.error("WebSocket 推送失败: {}", e.getMessage());
        }
    }

    private String getStatusMessage(OrderStatus status) {
        return switch (status) {
            case paid -> "您的订单已支付成功，等待商家接单";
            case preparing -> "商家已接单，正在制作中";
            case shipped -> "您的订单已发货，请耐心等待";
            case completed -> "您的订单已完成，感谢您的购买";
            case cancelled -> "您的订单已取消";
            default -> "订单状态已更新为 " + status;
        };
    }

    private OrderItemDTO convertItemToDTO(OrderItem item) {
        OrderItemDTO dto = new OrderItemDTO();
        dto.setId(item.getId());
        dto.setCoffeeId(item.getCoffeeId());
        dto.setQuantity(item.getQuantity());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setSubtotal(item.getSubtotal());
        dto.setCoffeeVariantsId(item.getCoffeeVariantsId());

        try {
            Map<String, Object> variantData = coffeeService.getVariantById(item.getCoffeeVariantsId());
            dto.setCoffeeVariants(variantData);
        } catch (Exception e) {
            System.out.println("获取规格信息失败: " + e.getMessage());
        }

        try {
            CoffeeDTO coffeeDTO = coffeeService.getCoffeeById(item.getCoffeeId());
            Map<String, Object> coffeeMap = new HashMap<>();
            coffeeMap.put("id", coffeeDTO.getId());
            coffeeMap.put("name", coffeeDTO.getName());
            coffeeMap.put("category", coffeeDTO.getCategory());
            coffeeMap.put("description", coffeeDTO.getDescription());
            coffeeMap.put("image_url", coffeeDTO.getImage_url());
            coffeeMap.put("is_available", coffeeDTO.getIs_available());
            coffeeMap.put("created_at", coffeeDTO.getCreated_at());
            dto.setCoffee(coffeeMap);
        } catch (Exception e) {
            System.out.println("获取咖啡信息失败: " + e.getMessage());
        }

        return dto;
    }
}
