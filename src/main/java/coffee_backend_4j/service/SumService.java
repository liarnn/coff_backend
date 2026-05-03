package coffee_backend_4j.service;

import coffee_backend_4j.dto.DashboardResponseDTO;
import jakarta.servlet.http.HttpServletResponse;

public interface SumService {
    DashboardResponseDTO getDashboard();
    void updateDashboardStat();
    void exportDashboard(HttpServletResponse response);
}
