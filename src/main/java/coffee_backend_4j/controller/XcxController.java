package coffee_backend_4j.controller;

import coffee_backend_4j.dto.CoffeeDTO;
import coffee_backend_4j.entity.Coffee;
import coffee_backend_4j.mapper.CoffeeMapper;
import coffee_backend_4j.service.CoffeeService;
import coffee_backend_4j.utils.Result;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/xcx/api")
@RequiredArgsConstructor
public class XcxController {

    private final CoffeeMapper coffeeMapper;
    private final CoffeeService coffeeService;

    @GetMapping("/home/banner")
    public Result<List<Map<String, Object>>> getHomeBanner() {
        try {
            LambdaQueryWrapper<Coffee> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Coffee::getIsAvailable, true);
            wrapper.isNotNull(Coffee::getImageUrl);
            wrapper.ne(Coffee::getImageUrl, "");
            wrapper.orderByDesc(Coffee::getCreatedAt);
            wrapper.last("LIMIT 5");
            List<Coffee> coffees = coffeeMapper.selectList(wrapper);

            List<Map<String, Object>> bannerList = new ArrayList<>();
            for (Coffee coffee : coffees) {
                Map<String, Object> banner = new HashMap<>();
                banner.put("id", coffee.getId());
                banner.put("image_url", coffee.getImageUrl());
                banner.put("name", coffee.getName());
                bannerList.add(banner);
            }

            return Result.ok(bannerList);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail("获取轮播图失败");
        }
    }
}
