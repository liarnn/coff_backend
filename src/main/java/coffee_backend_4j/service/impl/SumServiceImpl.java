package coffee_backend_4j.service.impl;

import coffee_backend_4j.dto.DashboardResponseDTO;
import coffee_backend_4j.dto.SalesItemDTO;
import coffee_backend_4j.entity.Coffee;
import coffee_backend_4j.entity.DashboardStat;
import coffee_backend_4j.entity.Order;
import coffee_backend_4j.entity.OrderItem;
import coffee_backend_4j.enums.OrderStatus;
import coffee_backend_4j.mapper.CoffeeMapper;
import coffee_backend_4j.mapper.DashboardStatMapper;
import coffee_backend_4j.mapper.OrderItemMapper;
import coffee_backend_4j.mapper.OrderMapper;
import coffee_backend_4j.service.SumService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SumServiceImpl implements SumService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final CoffeeMapper coffeeMapper;
    private final DashboardStatMapper dashboardStatMapper;

    @Override
    public DashboardResponseDTO getDashboard() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();

        // 1. 今日数据
        List<Order> todayOrders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, OrderStatus.completed)
                .ge(Order::getPaymentTime, startOfDay)
                .lt(Order::getPaymentTime, endOfDay));

        double todayRevenue = todayOrders.stream().mapToDouble(Order::getTotalAmount).sum();
        int todayOrdersCount = todayOrders.size();

        // 获取今日销量前7
        List<SalesItemDTO> todaySalesTop7 = getSalesTop7(startOfDay, endOfDay);
        String todayBestDrink = todaySalesTop7.isEmpty() ? "无" : todaySalesTop7.get(0).getName();

        // 在售商品数
        int onSaleCount = Math.toIntExact(coffeeMapper.selectCount(
                new LambdaQueryWrapper<Coffee>().eq(Coffee::getIsAvailable, true)));

        // 2. 历史数据
        DashboardStat stat = dashboardStatMapper.selectOne(new LambdaQueryWrapper<DashboardStat>().eq(DashboardStat::getId, 1));
        if (stat == null) {
            updateDashboardStat();
            stat = dashboardStatMapper.selectOne(new LambdaQueryWrapper<DashboardStat>().eq(DashboardStat::getId, 1));
        }

        // 3. 历史销量前7
        List<SalesItemDTO> historySalesTop7 = getSalesTop7(null, null);

        DashboardResponseDTO response = new DashboardResponseDTO();
        response.setToday_orders(todayOrdersCount);
        response.setToday_revenue(todayRevenue);
        response.setOn_sale_count(onSaleCount);
        response.setToday_best_drink(todayBestDrink);
        response.setTotal_orders(stat != null ? stat.getTotalOrders() : 0);
        response.setTotal_revenue(stat != null ? stat.getTotalRevenue() : 0.0);
        response.setTotal_best_drink(stat != null ? stat.getBestDrinkName() : "无");
        response.setToday_sales_top7(todaySalesTop7);
        response.setHistory_sales_top7(historySalesTop7);

        return response;
    }

    private List<SalesItemDTO> getSalesTop7(LocalDateTime startTime, LocalDateTime endTime) {
        // 获取所有完成的订单ID
        LambdaQueryWrapper<Order> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(Order::getStatus, OrderStatus.completed);
        if (startTime != null) {
            orderWrapper.ge(Order::getPaymentTime, startTime);
        }
        if (endTime != null) {
            orderWrapper.lt(Order::getPaymentTime, endTime);
        }
        List<Order> orders = orderMapper.selectList(orderWrapper);
        List<Integer> orderIds = orders.stream().map(Order::getId).collect(Collectors.toList());

        if (orderIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取所有相关订单项
        List<OrderItem> orderItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderId, orderIds));

        // 按咖啡ID统计销量
        Map<Integer, Integer> coffeeSalesMap = new HashMap<>();
        for (OrderItem item : orderItems) {
            coffeeSalesMap.merge(item.getCoffeeId(), item.getQuantity(), Integer::sum);
        }

        // 获取所有咖啡信息
        List<Integer> coffeeIds = new ArrayList<>(coffeeSalesMap.keySet());
        List<Coffee> coffees = coffeeIds.isEmpty() ? new ArrayList<>() :
                coffeeMapper.selectList(new LambdaQueryWrapper<Coffee>().in(Coffee::getId, coffeeIds));
        Map<Integer, Coffee> coffeeMap = coffees.stream()
                .collect(Collectors.toMap(Coffee::getId, c -> c));

        // 构建结果
        List<SalesItemDTO> result = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : coffeeSalesMap.entrySet()) {
            Coffee coffee = coffeeMap.get(entry.getKey());
            if (coffee != null) {
                SalesItemDTO item = new SalesItemDTO();
                item.setName(coffee.getName());
                item.setValue(entry.getValue());
                result.add(item);
            }
        }

        // 按销量排序，取前7
        result.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        return result.stream().limit(7).collect(Collectors.toList());
    }

    @Override
    @Scheduled(cron = "0 0 2 * * ?")
    public void updateDashboardStat() {
        // 历史总销售额和订单数
        List<Order> completedOrders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>().eq(Order::getStatus, OrderStatus.completed));
        double totalRevenue = completedOrders.stream().mapToDouble(Order::getTotalAmount).sum();
        int totalOrders = completedOrders.size();

        // 历史销量第一饮品
        List<SalesItemDTO> historySalesTop7 = getSalesTop7(null, null);
        String bestDrinkName = historySalesTop7.isEmpty() ? "无" : historySalesTop7.get(0).getName();
        int bestDrinkCount = historySalesTop7.isEmpty() ? 0 : historySalesTop7.get(0).getValue();

        DashboardStat stat = new DashboardStat();
        stat.setId(1);
        stat.setTotalRevenue(totalRevenue);
        stat.setTotalOrders(totalOrders);
        stat.setBestDrinkName(bestDrinkName);
        stat.setBestDrinkCount(bestDrinkCount);
        stat.setUpdatedAt(LocalDateTime.now());

        LambdaQueryWrapper<DashboardStat> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DashboardStat::getId, 1);
        DashboardStat existing = dashboardStatMapper.selectOne(wrapper);
        if (existing == null) {
            dashboardStatMapper.insert(stat);
        } else {
            stat.setId(existing.getId());
            dashboardStatMapper.updateById(stat);
        }

        return;
    }

    @Override
    public void exportDashboard(HttpServletResponse response) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("营业额统计");

            // 创建样式
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            int rowNum = 0;

            // 标题行
            Row titleRow = sheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("咖啡小店营业额统计表");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

            // 导出时间行
            Row timeRow = sheet.createRow(rowNum++);
            Cell timeCell = timeRow.createCell(0);
            timeCell.setCellValue("导出时间：" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss")));
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 3));

            // 空行
            rowNum++;

            // 今日数据
            LocalDate today = LocalDate.now();
            LocalDateTime startOfDay = today.atStartOfDay();
            LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();

            List<Order> todayOrders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                    .eq(Order::getStatus, OrderStatus.completed)
                    .ge(Order::getPaymentTime, startOfDay)
                    .lt(Order::getPaymentTime, endOfDay));
            double todayRevenue = todayOrders.stream().mapToDouble(Order::getTotalAmount).sum();
            int todayOrdersCount = todayOrders.size();

            // 历史数据
            DashboardStat stat = dashboardStatMapper.selectOne(new LambdaQueryWrapper<DashboardStat>().eq(DashboardStat::getId, 1));
            if (stat == null) {
                updateDashboardStat();
                stat = dashboardStatMapper.selectOne(new LambdaQueryWrapper<DashboardStat>().eq(DashboardStat::getId, 1));
            }

            // 今日最佳饮品
            List<SalesItemDTO> todaySalesTop7 = getSalesTop7(startOfDay, endOfDay);
            String todayBestDrink = todaySalesTop7.isEmpty() ? "无" : todaySalesTop7.get(0).getName();

            // 数据行
            Row dataRow1 = sheet.createRow(rowNum++);
            Cell cell1 = dataRow1.createCell(0);
            cell1.setCellValue("今日销售额");
            cell1.setCellStyle(dataStyle);
            Cell cell2 = dataRow1.createCell(1);
            cell2.setCellValue("¥" + String.format("%.2f", todayRevenue));
            cell2.setCellStyle(dataStyle);
            Cell cell3 = dataRow1.createCell(2);
            cell3.setCellValue("历史总销售额");
            cell3.setCellStyle(dataStyle);
            Cell cell4 = dataRow1.createCell(3);
            cell4.setCellValue("¥" + String.format("%.2f", stat != null ? stat.getTotalRevenue() : 0.0));
            cell4.setCellStyle(dataStyle);

            Row dataRow2 = sheet.createRow(rowNum++);
            cell1 = dataRow2.createCell(0);
            cell1.setCellValue("今日订单数");
            cell1.setCellStyle(dataStyle);
            cell2 = dataRow2.createCell(1);
            cell2.setCellValue(todayOrdersCount);
            cell2.setCellStyle(dataStyle);
            cell3 = dataRow2.createCell(2);
            cell3.setCellValue("历史订单总数");
            cell3.setCellStyle(dataStyle);
            cell4 = dataRow2.createCell(3);
            cell4.setCellValue(stat != null ? stat.getTotalOrders() : 0);
            cell4.setCellStyle(dataStyle);

            Row dataRow3 = sheet.createRow(rowNum++);
            cell1 = dataRow3.createCell(0);
            cell1.setCellValue("今日最佳饮品");
            cell1.setCellStyle(dataStyle);
            cell2 = dataRow3.createCell(1);
            cell2.setCellValue(todayBestDrink);
            cell2.setCellStyle(dataStyle);
            cell3 = dataRow3.createCell(2);
            cell3.setCellValue("历史最佳饮品");
            cell3.setCellStyle(dataStyle);
            cell4 = dataRow3.createCell(3);
            cell4.setCellValue(stat != null ? stat.getBestDrinkName() : "无");
            cell4.setCellStyle(dataStyle);

            // 空行
            rowNum++;

            // 今日销量前7标题
            Row todayTopTitleRow = sheet.createRow(rowNum++);
            Cell todayTopTitleCell = todayTopTitleRow.createCell(0);
            todayTopTitleCell.setCellValue("今日销量前7名");
            todayTopTitleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 3));

            // 今日销量前7表头
            Row todayTopHeaderRow = sheet.createRow(rowNum++);
            String[] todayHeaders = {"排名", "饮品名称", "销量", "占比"};
            for (int i = 0; i < todayHeaders.length; i++) {
                Cell cell = todayTopHeaderRow.createCell(i);
                cell.setCellValue(todayHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            // 今日销量前7数据
            int totalTodaySales = todaySalesTop7.stream().mapToInt(SalesItemDTO::getValue).sum();
            for (int i = 0; i < todaySalesTop7.size(); i++) {
                SalesItemDTO item = todaySalesTop7.get(i);
                Row row = sheet.createRow(rowNum++);
                Cell rankCell = row.createCell(0);
                rankCell.setCellValue(i + 1);
                rankCell.setCellStyle(dataStyle);
                Cell nameCell = row.createCell(1);
                nameCell.setCellValue(item.getName());
                nameCell.setCellStyle(dataStyle);
                Cell salesCell = row.createCell(2);
                salesCell.setCellValue(item.getValue());
                salesCell.setCellStyle(dataStyle);
                Cell percentCell = row.createCell(3);
                String percent = totalTodaySales > 0 ? String.format("%.1f%%", (double) item.getValue() / totalTodaySales * 100) : "0%";
                percentCell.setCellValue(percent);
                percentCell.setCellStyle(dataStyle);
            }

            // 空行
            rowNum++;

            // 历史销量前7标题
            List<SalesItemDTO> historySalesTop7 = getSalesTop7(null, null);
            Row historyTopTitleRow = sheet.createRow(rowNum++);
            Cell historyTopTitleCell = historyTopTitleRow.createCell(0);
            historyTopTitleCell.setCellValue("历史销量前7名");
            historyTopTitleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 3));

            // 历史销量前7表头
            Row historyTopHeaderRow = sheet.createRow(rowNum++);
            for (int i = 0; i < todayHeaders.length; i++) {
                Cell cell = historyTopHeaderRow.createCell(i);
                cell.setCellValue(todayHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            // 历史销量前7数据
            int totalHistorySales = historySalesTop7.stream().mapToInt(SalesItemDTO::getValue).sum();
            for (int i = 0; i < historySalesTop7.size(); i++) {
                SalesItemDTO item = historySalesTop7.get(i);
                Row row = sheet.createRow(rowNum++);
                Cell rankCell = row.createCell(0);
                rankCell.setCellValue(i + 1);
                rankCell.setCellStyle(dataStyle);
                Cell nameCell = row.createCell(1);
                nameCell.setCellValue(item.getName());
                nameCell.setCellStyle(dataStyle);
                Cell salesCell = row.createCell(2);
                salesCell.setCellValue(item.getValue());
                salesCell.setCellStyle(dataStyle);
                Cell percentCell = row.createCell(3);
                String percent = totalHistorySales > 0 ? String.format("%.1f%%", (double) item.getValue() / totalHistorySales * 100) : "0%";
                percentCell.setCellValue(percent);
                percentCell.setCellStyle(dataStyle);
            }

            // 调整列宽
            for (int i = 0; i < 4; i++) {
                sheet.autoSizeColumn(i);
            }

            // 设置响应
            String fileName = "营业额统计_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

            workbook.write(response.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("微软雅黑");
        font.setFontHeightInPoints((short) 16);
        font.setBold(true);
        font.setColor(IndexedColors.BROWN.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("微软雅黑");
        font.setFontHeightInPoints((short) 14);
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.BROWN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("微软雅黑");
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
}
