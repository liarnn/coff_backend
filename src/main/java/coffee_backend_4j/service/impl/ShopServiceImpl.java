package coffee_backend_4j.service.impl;

import coffee_backend_4j.dto.ShopSettingDTO;
import coffee_backend_4j.exception.BusinessException;
import coffee_backend_4j.service.OssService;
import coffee_backend_4j.service.ShopService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ShopServiceImpl implements ShopService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OssService ossService;

    private static final String SHOP_SETTING_KEY = "shop:setting";
    private static final String DEFAULT_SHOP_NAME = "咖啡小店";
    private static final String DEFAULT_BUSINESS_HOURS = "09:00 - 22:00";

    private Map<String, Object> loadShopSettingFromFile() {
        try {
            Path path = Paths.get("static", "shop_setting.json");
            if (Files.exists(path)) {
                String content = Files.readString(path);
                @SuppressWarnings("unchecked")
                Map<String, Object> setting = objectMapper.readValue(content, Map.class);
                if (setting.get("banners") == null) {
                    setting.put("banners", new ArrayList<>());
                }
                return setting;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Map<String, Object> defaultSetting = new HashMap<>();
        defaultSetting.put("name", DEFAULT_SHOP_NAME);
        defaultSetting.put("business_hours", DEFAULT_BUSINESS_HOURS);
        defaultSetting.put("address", "");
        defaultSetting.put("phone", "");
        defaultSetting.put("notice", "欢迎光临~");
        defaultSetting.put("banners", new ArrayList<>());
        return defaultSetting;
    }

    private boolean saveShopSettingToFile(Map<String, Object> setting) {
        try {
            Path path = Paths.get("static", "shop_setting.json");
            Files.createDirectories(path.getParent());
            String content = objectMapper.writeValueAsString(setting);
            Files.writeString(path, content);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Map<String, Object> getShopSetting() {
        try {
            String cached = redisTemplate.opsForValue().get(SHOP_SETTING_KEY);
            if (cached != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> setting = objectMapper.readValue(cached, Map.class);
                return setting;
            }
            Map<String, Object> setting = loadShopSettingFromFile();
            redisTemplate.opsForValue().set(SHOP_SETTING_KEY, objectMapper.writeValueAsString(setting), 1, TimeUnit.HOURS);
            return setting;
        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessException("获取店铺设置失败");
        }
    }

    @Override
    public Map<String, Object> updateShopSetting(ShopSettingDTO shopSettingDTO) {
        try {
            Map<String, Object> currentSetting = loadShopSettingFromFile();
            if (shopSettingDTO.getName() != null) {
                currentSetting.put("name", shopSettingDTO.getName());
            }
            if (shopSettingDTO.getBusiness_hours() != null) {
                currentSetting.put("business_hours", shopSettingDTO.getBusiness_hours());
            }
            if (shopSettingDTO.getAddress() != null) {
                currentSetting.put("address", shopSettingDTO.getAddress());
            }
            if (shopSettingDTO.getPhone() != null) {
                currentSetting.put("phone", shopSettingDTO.getPhone());
            }
            if (shopSettingDTO.getNotice() != null) {
                currentSetting.put("notice", shopSettingDTO.getNotice());
            }
            if (shopSettingDTO.getBanners() != null) {
                currentSetting.put("banners", shopSettingDTO.getBanners());
            }
            if (saveShopSettingToFile(currentSetting)) {
                redisTemplate.opsForValue().set(SHOP_SETTING_KEY, objectMapper.writeValueAsString(currentSetting), 1, TimeUnit.HOURS);
                return currentSetting;
            } else {
                throw new BusinessException("保存店铺设置失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessException("更新店铺设置失败");
        }
    }

    @Override
    public Map<String, String> uploadBanner(MultipartFile file) {
        try {
            String imageUrl = ossService.uploadFile(file);
            Map<String, String> result = new HashMap<>();
            result.put("image_url", imageUrl);
            return result;
        } catch (Exception e) {
            throw new BusinessException("上传轮播图失败");
        }
    }

    @Override
    public Map<String, Object> saveBanners(List<Map<String, Object>> banners) {
        try {
            if (banners.size() > 5) {
                throw new BusinessException("最多只能添加5个轮播图");
            }
            Map<String, Object> currentSetting = loadShopSettingFromFile();
            currentSetting.put("banners", banners);
            if (saveShopSettingToFile(currentSetting)) {
                redisTemplate.opsForValue().set(SHOP_SETTING_KEY, objectMapper.writeValueAsString(currentSetting), 1, TimeUnit.HOURS);
                return currentSetting;
            } else {
                throw new BusinessException("保存轮播图失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessException("保存轮播图失败");
        }
    }

    @Override
    public Map<String, Object> saveBasicInfo(ShopSettingDTO shopSettingDTO) {
        try {
            Map<String, Object> currentSetting = loadShopSettingFromFile();
            if (shopSettingDTO.getName() != null) {
                currentSetting.put("name", shopSettingDTO.getName());
            }
            if (shopSettingDTO.getBusiness_hours() != null) {
                currentSetting.put("business_hours", shopSettingDTO.getBusiness_hours());
            }
            if (shopSettingDTO.getAddress() != null) {
                currentSetting.put("address", shopSettingDTO.getAddress());
            }
            if (shopSettingDTO.getPhone() != null) {
                currentSetting.put("phone", shopSettingDTO.getPhone());
            }
            if (shopSettingDTO.getNotice() != null) {
                currentSetting.put("notice", shopSettingDTO.getNotice());
            }
            if (saveShopSettingToFile(currentSetting)) {
                redisTemplate.opsForValue().set(SHOP_SETTING_KEY, objectMapper.writeValueAsString(currentSetting), 1, TimeUnit.HOURS);
                return currentSetting;
            } else {
                throw new BusinessException("保存基本信息失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessException("保存基本信息失败");
        }
    }

    @Override
    public Map<String, Object> addBanner(Map<String, Object> banner) {
        try {
            String imageUrl = (String) banner.get("image_url");
            if (imageUrl == null || imageUrl.isEmpty()) {
                throw new BusinessException("图片URL不能为空");
            }
            Map<String, Object> currentSetting = loadShopSettingFromFile();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> banners = (List<Map<String, Object>>) currentSetting.getOrDefault("banners", new ArrayList<>());
            if (banners.size() >= 5) {
                throw new BusinessException("最多只能添加5个轮播图");
            }
            banners.add(banner);
            currentSetting.put("banners", banners);
            if (saveShopSettingToFile(currentSetting)) {
                redisTemplate.opsForValue().set(SHOP_SETTING_KEY, objectMapper.writeValueAsString(currentSetting), 1, TimeUnit.HOURS);
                return currentSetting;
            } else {
                throw new BusinessException("保存轮播图失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessException("添加轮播图失败");
        }
    }

    @Override
    public Map<String, Object> updateBanner(Integer index, Map<String, Object> banner) {
        try {
            Map<String, Object> currentSetting = loadShopSettingFromFile();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> banners = (List<Map<String, Object>>) currentSetting.getOrDefault("banners", new ArrayList<>());
            if (index < 0 || index >= banners.size()) {
                throw new BusinessException("轮播图索引无效");
            }
            Map<String, Object> existingBanner = banners.get(index);
            if (banner.containsKey("image_url")) {
                existingBanner.put("image_url", banner.get("image_url"));
            }
            if (banner.containsKey("coffee_id")) {
                existingBanner.put("coffee_id", banner.get("coffee_id"));
            }
            if (saveShopSettingToFile(currentSetting)) {
                redisTemplate.opsForValue().set(SHOP_SETTING_KEY, objectMapper.writeValueAsString(currentSetting), 1, TimeUnit.HOURS);
                return currentSetting;
            } else {
                throw new BusinessException("更新轮播图失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessException("更新轮播图失败");
        }
    }

    @Override
    public Map<String, Object> deleteBanner(Integer index) {
        try {
            Map<String, Object> currentSetting = loadShopSettingFromFile();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> banners = (List<Map<String, Object>>) currentSetting.getOrDefault("banners", new ArrayList<>());
            if (index < 0 || index >= banners.size()) {
                throw new BusinessException("轮播图索引无效");
            }
            banners.remove(index);
            if (saveShopSettingToFile(currentSetting)) {
                redisTemplate.opsForValue().set(SHOP_SETTING_KEY, objectMapper.writeValueAsString(currentSetting), 1, TimeUnit.HOURS);
                return currentSetting;
            } else {
                throw new BusinessException("删除轮播图失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessException("删除轮播图失败");
        }
    }
}
