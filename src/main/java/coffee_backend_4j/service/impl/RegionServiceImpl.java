package coffee_backend_4j.service.impl;

import coffee_backend_4j.entity.Region;
import coffee_backend_4j.mapper.RegionMapper;
import coffee_backend_4j.service.RegionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RegionServiceImpl implements RegionService {

    private final RegionMapper regionMapper;

    @Override
    public List<Map<String, Object>> searchRegions(String keyword, Integer limit) {
        LambdaQueryWrapper<Region> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(Region::getName, keyword);
        wrapper.last("LIMIT " + limit);
        return regionMapper.selectList(wrapper).stream()
                .map(this::toRegionMap)
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getRegionTree(String parentCode) {
        List<Region> allRegions = regionMapper.selectList(null);
        Map<String, Map<String, Object>> nodeMap = new HashMap<>();

        for (Region region : allRegions) {
            Map<String, Object> node = toRegionMap(region);
            node.put("children", new ArrayList<Map<String, Object>>());
            nodeMap.put(region.getCode(), node);
        }

        List<Map<String, Object>> rootList = new ArrayList<>();
        for (Region region : allRegions) {
            String pCode = region.getParentCode();
            Map<String, Object> node = nodeMap.get(region.getCode());
            boolean isRoot = pCode == null || pCode.trim().equals("0") || pCode.trim().isEmpty();

            if (isRoot) {
                rootList.add(node);
            } else if (nodeMap.containsKey(pCode)) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> children = (List<Map<String, Object>>) nodeMap.get(pCode).get("children");
                children.add(node);
            }
        }

        if (parentCode == null || parentCode.trim().equals("0") || parentCode.trim().isEmpty()) {
            return rootList;
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> children = nodeMap.containsKey(parentCode)
                ? (List<Map<String, Object>>) nodeMap.get(parentCode).get("children")
                : new ArrayList<>();
        return children;
    }

    @Override
    public Map<String, Object> getRegionDetail(String code) {
        Region region = regionMapper.selectOne(new LambdaQueryWrapper<Region>().eq(Region::getCode, code));
        if (region == null) {
            return new HashMap<>();
        }

        List<String> fullPath = new ArrayList<>();
        String currentCode = code;
        Set<String> visited = new HashSet<>();

        while (currentCode != null && !visited.contains(currentCode)) {
            visited.add(currentCode);
            Region currRegion = regionMapper.selectOne(new LambdaQueryWrapper<Region>().eq(Region::getCode, currentCode));
            if (currRegion == null) {
                break;
            }
            fullPath.add(0, currRegion.getName());
            String pCode = currRegion.getParentCode();
            if (pCode == null || pCode.trim().equals("0")) {
                break;
            }
            currentCode = pCode;
        }

        Map<String, Object> result = toRegionMap(region);
        result.put("full_path", fullPath);
        return result;
    }

    private Map<String, Object> toRegionMap(Region region) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", region.getCode());
        result.put("name", region.getName());
        result.put("parent_code", region.getParentCode());
        return result;
    }
}
