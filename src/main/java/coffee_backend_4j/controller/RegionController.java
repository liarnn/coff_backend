package coffee_backend_4j.controller;

import coffee_backend_4j.service.RegionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/region")
@RequiredArgsConstructor
public class RegionController {

    private final RegionService regionService;

    @GetMapping("/search")
    public List<Map<String, Object>> searchRegions(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "10") Integer limit) {
        return regionService.searchRegions(keyword, limit);
    }

    @GetMapping("/tree")
    public List<Map<String, Object>> getRegionTree(
            @RequestParam(name = "parent_code", required = false) String parentCode,
            @RequestParam(name = "parentCode", required = false) String parentCodeAlias) {
        return regionService.getRegionTree(parentCode != null ? parentCode : parentCodeAlias);
    }

    @GetMapping("/{code}")
    public Map<String, Object> getRegionDetail(@PathVariable String code) {
        return regionService.getRegionDetail(code);
    }
}
