package coffee_backend_4j.controller;

import coffee_backend_4j.dto.ShopSettingDTO;
import coffee_backend_4j.service.ShopService;
import coffee_backend_4j.utils.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/shop")
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;

    @GetMapping("/setting")
    public Result<Map<String, Object>> getShopSetting() {
        return Result.ok(shopService.getShopSetting());
    }

    @PostMapping("/setting")
    public Result<Map<String, Object>> updateShopSetting(@RequestBody ShopSettingDTO shopSettingDTO) {
        return Result.ok(shopService.updateShopSetting(shopSettingDTO));
    }

    @PostMapping("/banner/upload")
    public Result<Map<String, String>> uploadBanner(@RequestParam("file") MultipartFile file) {
        return Result.ok(shopService.uploadBanner(file));
    }

    @PostMapping("/banners")
    public Result<Map<String, Object>> saveBanners(@RequestBody List<Map<String, Object>> banners) {
        return Result.ok(shopService.saveBanners(banners));
    }

    @PostMapping("/basic")
    public Result<Map<String, Object>> saveBasicInfo(@RequestBody ShopSettingDTO shopSettingDTO) {
        return Result.ok(shopService.saveBasicInfo(shopSettingDTO));
    }

    @PostMapping("/banner")
    public Result<Map<String, Object>> addBanner(@RequestBody Map<String, Object> banner) {
        return Result.ok(shopService.addBanner(banner));
    }

    @PutMapping("/banner/{index}")
    public Result<Map<String, Object>> updateBanner(@PathVariable Integer index, @RequestBody Map<String, Object> banner) {
        return Result.ok(shopService.updateBanner(index, banner));
    }

    @DeleteMapping("/banner/{index}")
    public Result<Map<String, Object>> deleteBanner(@PathVariable Integer index) {
        return Result.ok(shopService.deleteBanner(index));
    }
}
