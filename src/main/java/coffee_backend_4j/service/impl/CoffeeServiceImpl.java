package coffee_backend_4j.service.impl;

import coffee_backend_4j.dto.CoffeeDTO;
import coffee_backend_4j.dto.CoffeeVariantsDTO;
import coffee_backend_4j.entity.Coffee;
import coffee_backend_4j.entity.CoffeeVariants;
import coffee_backend_4j.exception.BusinessException;
import coffee_backend_4j.mapper.CoffeeMapper;
import coffee_backend_4j.mapper.CoffeeVariantsMapper;
import coffee_backend_4j.service.CoffeeService;
import coffee_backend_4j.service.OssService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CoffeeServiceImpl implements CoffeeService {

    private final CoffeeMapper coffeeMapper;
    private final CoffeeVariantsMapper coffeeVariantsMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final OssService ossService;

    @Override
    public List<Map<String, String>> getAllCategories() {
        List<Coffee> coffees = coffeeMapper.selectList(
                new LambdaQueryWrapper<Coffee>().eq(Coffee::getIsAvailable, true)
        );
        List<String> categories = coffees.stream()
            .map(Coffee::getCategory)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());

        return categories.stream()
                .map(cat -> {
                    Map<String, String> map = new HashMap<>();
                    map.put("value", cat);
                    map.put("label", cat);
                    return map;
                })
                .sorted(Comparator.comparing(m -> m.get("value")))
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, CoffeeDTO> getCoffeeList(String category) {
        // 先尝试从缓存获取
        try {
            String cacheKey = "coffee:list:available" + (category != null ? ":" + category : "");
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                try {
                    return (Map<String, CoffeeDTO>) cached;
                } catch (Exception e) {
                    // 类型转换失败，继续从数据库查询
                }
            }
        } catch (Exception e) {
            // 缓存操作失败，继续从数据库查询
        }

        // 从数据库查询
        LambdaQueryWrapper<Coffee> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Coffee::getIsAvailable, true);
        if (category != null && !category.isEmpty()) {
            wrapper.eq(Coffee::getCategory, category);
        }
        List<Coffee> coffees = coffeeMapper.selectList(wrapper);
        Map<String, CoffeeDTO> coffeeMap = new HashMap<>();
        for (Coffee coffee : coffees) {
            coffeeMap.put(String.valueOf(coffee.getId()), convertToDTO(coffee));
        }

        // 写入缓存
        try {
            String cacheKey = "coffee:list:available" + (category != null ? ":" + category : "");
            redisTemplate.opsForValue().set(cacheKey, coffeeMap, 3600, TimeUnit.SECONDS);
        } catch (Exception e) {
            // 缓存失败不影响返回数据
        }

        return coffeeMap;
    }

    @Override
    @Transactional
    public CoffeeDTO createCoffee(CoffeeDTO coffeeDTO) {
        Coffee coffee = new Coffee();
        coffee.setName(coffeeDTO.getName());
        coffee.setCategory(coffeeDTO.getCategory());
        coffee.setDescription(coffeeDTO.getDescription());
        coffee.setImageUrl(coffeeDTO.getImage_url());
        coffee.setIsAvailable(coffeeDTO.getIs_available() != null ? coffeeDTO.getIs_available() : true);
        coffee.setCreatedAt(LocalDateTime.now());
        coffee.setUpdatedAt(LocalDateTime.now());

        coffeeMapper.insert(coffee);

        if (coffeeDTO.getVariants() != null) {
            for (CoffeeVariantsDTO variantDTO : coffeeDTO.getVariants()) {
                CoffeeVariants variant = new CoffeeVariants();
                variant.setCoffeeId(coffee.getId());
                variant.setCupSize(variantDTO.getCup_size() != null ? variantDTO.getCup_size() : "S");
                variant.setPrice(variantDTO.getPrice());
                variant.setStock(variantDTO.getStock() != null ? variantDTO.getStock() : 0);
                coffeeVariantsMapper.insert(variant);
            }
        }

        // 清除缓存
        try {
            redisTemplate.delete("coffee:list:available");
            Set<String> keys = redisTemplate.keys("coffee:list:available:*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            // 缓存操作失败不影响返回数据
        }

        return convertToDTO(coffeeMapper.selectById(coffee.getId()));
    }

    @Override
    public CoffeeDTO getCoffeeById(Integer coffeeId) {
        Coffee coffee = coffeeMapper.selectById(coffeeId);
        if (coffee == null) {
            throw new BusinessException("咖啡不存在");
        }
        try {
            initCategoryHash();
        } catch (Exception e) {
            // 缓存失败不影响返回数据
        }
        return convertToDTO(coffee);
    }

    @Override
    @Transactional
    public CoffeeDTO updateCoffee(Integer coffeeId, CoffeeDTO coffeeDTO) {
        Coffee coffee = coffeeMapper.selectById(coffeeId);
        if (coffee == null) {
            throw new BusinessException("咖啡不存在");
        }

        if (coffeeDTO.getName() != null) coffee.setName(coffeeDTO.getName());
        if (coffeeDTO.getCategory() != null) coffee.setCategory(coffeeDTO.getCategory());
        if (coffeeDTO.getDescription() != null) coffee.setDescription(coffeeDTO.getDescription());
        if (coffeeDTO.getImage_url() != null) coffee.setImageUrl(coffeeDTO.getImage_url());
        if (coffeeDTO.getIs_available() != null) coffee.setIsAvailable(coffeeDTO.getIs_available());
        coffee.setUpdatedAt(LocalDateTime.now());

        coffeeMapper.updateById(coffee);

        if (coffeeDTO.getVariants() != null) {
            coffeeVariantsMapper.delete(
                new LambdaQueryWrapper<CoffeeVariants>().eq(CoffeeVariants::getCoffeeId, coffeeId)
            );
            for (CoffeeVariantsDTO variantDTO : coffeeDTO.getVariants()) {
                CoffeeVariants variant = new CoffeeVariants();
                variant.setCoffeeId(coffeeId);
                variant.setCupSize(variantDTO.getCup_size() != null ? variantDTO.getCup_size() : "S");
                variant.setPrice(variantDTO.getPrice());
                variant.setStock(variantDTO.getStock() != null ? variantDTO.getStock() : 0);
                coffeeVariantsMapper.insert(variant);
            }
        }

        // 清除缓存
        try {
            redisTemplate.delete("coffee:list:available");
            Set<String> keys = redisTemplate.keys("coffee:list:available:*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            // 缓存操作失败不影响返回数据
        }

        return convertToDTO(coffeeMapper.selectById(coffeeId));
    }

    @Override
    @Transactional
    public void deleteCoffee(Integer coffeeId) {
        Coffee coffee = coffeeMapper.selectById(coffeeId);
        if (coffee == null) {
            throw new BusinessException("咖啡不存在");
        }
        coffeeMapper.deleteById(coffeeId);

        // 清除缓存
        try {
            redisTemplate.delete("coffee:list:available");
            Set<String> keys = redisTemplate.keys("coffee:list:available:*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            // 缓存操作失败不影响返回数据
        }
    }

    @Override
    public Map<String, Object> getVariantById(Integer variantId) {
        if (variantId == null) {
            throw new BusinessException("参数异常");
        }

        String redisKey = "coffee:variant:" + variantId;
        Object cached = redisTemplate.opsForValue().get(redisKey);
        if (cached != null) {
            try {
                return (Map<String, Object>) cached;
            } catch (Exception e) {
            }
        }

        CoffeeVariants variant = coffeeVariantsMapper.selectById(variantId);
        if (variant == null) {
            throw new BusinessException("规格不存在");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("id", variant.getId());
        data.put("coffee_id", variant.getCoffeeId());
        data.put("cup_size", variant.getCupSize());
        data.put("price", variant.getPrice());

        redisTemplate.opsForValue().set(redisKey, data, 3600, TimeUnit.SECONDS);
        return data;
    }

    @Override
    public Map<String, String> uploadImage(MultipartFile file) {
        try {
            String imageUrl = ossService.uploadFile(file);
            Map<String, String> result = new HashMap<>();
            result.put("url", imageUrl);
            return result;
        } catch (Exception e) {
            throw new BusinessException("上传图片失败");
        }
    }

    @Override
    @Transactional
    public Map<String, Object> createVariant(Integer coffeeId, Map<String, Object> variantData) {
        try {
            CoffeeVariants variant = new CoffeeVariants();
            variant.setCoffeeId(coffeeId);
            variant.setCupSize((String) firstPresent(variantData, "cup_size", "cupSize", "S"));
            variant.setPrice(((Number) variantData.getOrDefault("price", 0.0)).doubleValue());
            variant.setStock(((Number) variantData.getOrDefault("stock", 0)).intValue());
            coffeeVariantsMapper.insert(variant);

            Map<String, Object> result = new HashMap<>();
            result.put("id", variant.getId());
            result.put("coffee_id", variant.getCoffeeId());
            result.put("cup_size", variant.getCupSize());
            result.put("price", variant.getPrice());
            result.put("stock", variant.getStock());
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessException("创建规格失败");
        }
    }

    @Override
    @Transactional
    public Map<String, Object> updateVariant(Integer variantId, Map<String, Object> variantData) {
        try {
            CoffeeVariants variant = coffeeVariantsMapper.selectById(variantId);
            if (variant == null) {
                throw new BusinessException("规格不存在");
            }
            if (variantData.containsKey("cup_size") || variantData.containsKey("cupSize")) {
                variant.setCupSize((String) firstPresent(variantData, "cup_size", "cupSize", variant.getCupSize()));
            }
            if (variantData.containsKey("price")) {
                variant.setPrice(((Number) variantData.get("price")).doubleValue());
            }
            if (variantData.containsKey("stock")) {
                variant.setStock(((Number) variantData.get("stock")).intValue());
            }
            coffeeVariantsMapper.updateById(variant);

            Map<String, Object> result = new HashMap<>();
            result.put("id", variant.getId());
            result.put("coffee_id", variant.getCoffeeId());
            result.put("cup_size", variant.getCupSize());
            result.put("price", variant.getPrice());
            result.put("stock", variant.getStock());
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessException("更新规格失败");
        }
    }

    @Override
    @Transactional
    public void deleteVariant(Integer variantId) {
        try {
            CoffeeVariants variant = coffeeVariantsMapper.selectById(variantId);
            if (variant == null) {
                throw new BusinessException("规格不存在");
            }
            coffeeVariantsMapper.deleteById(variantId);
        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessException("删除规格失败");
        }
    }

    private CoffeeDTO convertToDTO(Coffee coffee) {
        CoffeeDTO dto = new CoffeeDTO();
        dto.setId(coffee.getId());
        dto.setName(coffee.getName());
        dto.setCategory(coffee.getCategory());
        dto.setDescription(coffee.getDescription());
        dto.setImage_url(coffee.getImageUrl());
        dto.setIs_available(coffee.getIsAvailable());
        dto.setCreated_at(coffee.getCreatedAt());

        List<CoffeeVariantsDTO> variants = coffeeVariantsMapper.selectList(
            new LambdaQueryWrapper<CoffeeVariants>().eq(CoffeeVariants::getCoffeeId, coffee.getId())
        ).stream()
                .map(this::convertVariantToDTO)
                .collect(Collectors.toList());
        dto.setVariants(variants);

        return dto;
    }

    private CoffeeVariantsDTO convertVariantToDTO(CoffeeVariants variant) {
        CoffeeVariantsDTO dto = new CoffeeVariantsDTO();
        dto.setId(variant.getId());
        dto.setCoffee_id(variant.getCoffeeId());
        dto.setCup_size(variant.getCupSize());
        dto.setPrice(variant.getPrice());
        dto.setStock(variant.getStock());
        return dto;
    }

    private void initCategoryHash() {
        List<Coffee> coffees = coffeeMapper.selectList(null);
        Map<String, Map<String, Object>> categoryMap = new HashMap<>();

        for (Coffee coffee : coffees) {
            String cat = coffee.getCategory() != null ? coffee.getCategory() : "other";
            categoryMap.computeIfAbsent(cat, k -> new HashMap<>());

            CoffeeDTO coffeeDTO = convertToDTO(coffee);
            Map<String, Object> coffeeMap = new HashMap<>();
            coffeeMap.put("id", coffeeDTO.getId());
            coffeeMap.put("name", coffeeDTO.getName());
            coffeeMap.put("category", coffeeDTO.getCategory());
            coffeeMap.put("description", coffeeDTO.getDescription());
            coffeeMap.put("image_url", coffeeDTO.getImage_url());
            coffeeMap.put("is_available", coffeeDTO.getIs_available());
            coffeeMap.put("created_at", coffeeDTO.getCreated_at());
            coffeeMap.put("variants", coffeeDTO.getVariants());
            categoryMap.get(cat).put(String.valueOf(coffee.getId()), coffeeMap);
        }

        for (Map.Entry<String, Map<String, Object>> entry : categoryMap.entrySet()) {
            String hashKey = "coffee:category:" + entry.getKey();
            redisTemplate.opsForHash().putAll(hashKey, entry.getValue());
            redisTemplate.expire(hashKey, 3600, TimeUnit.SECONDS);
        }
    }

    private Object firstPresent(Map<String, Object> data, String snakeKey, String camelKey, Object defaultValue) {
        if (data.containsKey(snakeKey)) {
            return data.get(snakeKey);
        }
        if (data.containsKey(camelKey)) {
            return data.get(camelKey);
        }
        return defaultValue;
    }
}
