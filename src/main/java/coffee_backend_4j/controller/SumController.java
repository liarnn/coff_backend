package coffee_backend_4j.controller;

import coffee_backend_4j.dto.DashboardResponseDTO;
import coffee_backend_4j.service.SumService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/sum")
@RequiredArgsConstructor
public class SumController {

    private final SumService sumService;

    @GetMapping("/dashboard")
    public DashboardResponseDTO getDashboard() {
        return sumService.getDashboard();
    }

    @PostMapping("/dashboard/update")
    public Map<String, Object> manualUpdate() {
        sumService.updateDashboardStat();
        return Map.of("code", 200, "msg", "更新成功");
    }

    @GetMapping("/export")
    public void exportDashboard(HttpServletResponse response) {
        sumService.exportDashboard(response);
    }
}
