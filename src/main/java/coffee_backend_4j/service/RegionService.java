package coffee_backend_4j.service;

import java.util.List;
import java.util.Map;

public interface RegionService {
    List<Map<String, Object>> searchRegions(String keyword, Integer limit);

    List<Map<String, Object>> getRegionTree(String parentCode);

    Map<String, Object> getRegionDetail(String code);
}
