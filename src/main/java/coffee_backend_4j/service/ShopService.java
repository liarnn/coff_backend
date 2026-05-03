package coffee_backend_4j.service;

import coffee_backend_4j.dto.ShopSettingDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface ShopService {
    Map<String, Object> getShopSetting();
    Map<String, Object> updateShopSetting(ShopSettingDTO shopSettingDTO);
    Map<String, String> uploadBanner(MultipartFile file);
    Map<String, Object> saveBanners(List<Map<String, Object>> banners);
    Map<String, Object> saveBasicInfo(ShopSettingDTO shopSettingDTO);
    Map<String, Object> addBanner(Map<String, Object> banner);
    Map<String, Object> updateBanner(Integer index, Map<String, Object> banner);
    Map<String, Object> deleteBanner(Integer index);
}
