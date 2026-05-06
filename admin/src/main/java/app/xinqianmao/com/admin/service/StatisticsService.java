/**
 * File: StatisticsService.java
 * Author: system
 * Date: 2026-05-05
 */
package app.xinqianmao.com.admin.service;

import app.xinqianmao.com.admin.common.pojo.OrderTrendResponse;
import app.xinqianmao.com.admin.common.pojo.ProductTopResponse;
import app.xinqianmao.com.admin.dao.ProductMapper;
import app.xinqianmao.com.common.utils.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final JdbcTemplate jdbcTemplate;
    private final ProductMapper productMapper;

    /**
     * Get order trend (volume and amount) grouped by day or month.
     */
    public OrderTrendResponse getOrderTrend(String startDate, String endDate) {
        // Default: last 7 days
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now(DateTimeUtil.ZONE_BEIJING);
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : end.minusDays(6);

        long daysBetween = ChronoUnit.DAYS.between(start, end);
        boolean byMonth = daysBetween > 31;

        String dateFormat = byMonth ? "YYYY-MM" : "YYYY-MM-DD";
        String orderByFormat = byMonth ? "YYYY-MM" : "YYYY-MM-DD";
        String sql = "SELECT TO_CHAR(o.create_time AT TIME ZONE 'Asia/Shanghai', '" + dateFormat + "') AS date_key, "
                + "COUNT(*) AS order_count, COALESCE(SUM(o.actual_pay_money), 0) AS total_amount "
                + "FROM t_order o "
                + "WHERE o.create_time AT TIME ZONE 'Asia/Shanghai' >= ?::date "
                + "AND o.create_time AT TIME ZONE 'Asia/Shanghai' < (?::date + INTERVAL '1 day') "
                + "GROUP BY date_key ORDER BY date_key";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, start.toString(), end.toString());

        OrderTrendResponse r = new OrderTrendResponse();
        List<OrderTrendResponse.TrendPoint> points = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            OrderTrendResponse.TrendPoint p = new OrderTrendResponse.TrendPoint();
            p.setDateKey((String) row.get("date_key"));
            p.setOrderCount(((Number) row.get("order_count")).longValue());
            p.setTotalAmount((BigDecimal) row.get("total_amount"));
            points.add(p);
        }
        r.setPoints(points);
        return r;
    }

    /**
     * Get top 20 products by sales volume in the given date range.
     */
    public ProductTopResponse getProductTop(String startDate, String endDate) {
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now(DateTimeUtil.ZONE_BEIJING);
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : end.minusDays(6);

        String sql = "SELECT ops.product_id, p.name AS product_name, SUM(ops.inventory) AS total_sales "
                + "FROM t_order_product_skus ops "
                + "JOIN t_order o ON ops.order_id = o.id "
                + "JOIN t_product p ON ops.product_id = p.id "
                + "WHERE o.create_time AT TIME ZONE 'Asia/Shanghai' >= ?::date "
                + "AND o.create_time AT TIME ZONE 'Asia/Shanghai' < (?::date + INTERVAL '1 day') "
                + "GROUP BY ops.product_id, p.name "
                + "ORDER BY total_sales DESC LIMIT 20";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, start.toString(), end.toString());

        ProductTopResponse r = new ProductTopResponse();
        List<ProductTopResponse.TopItem> items = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            ProductTopResponse.TopItem item = new ProductTopResponse.TopItem();
            item.setProductId((String) row.get("product_id"));
            item.setProductName((String) row.get("product_name"));
            item.setTotalSales(((Number) row.get("total_sales")).longValue());
            items.add(item);
        }
        r.setItems(items);
        return r;
    }
}
