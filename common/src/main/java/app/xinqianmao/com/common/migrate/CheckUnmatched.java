package app.xinqianmao.com.common.migrate;
import java.sql.*;
import java.util.*;

public final class CheckUnmatched {
    public static void main(String[] a) throws Exception {
        Class.forName("org.postgresql.Driver");
        try (Connection c = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:1800/mypet_xlong", "postgres", "mypg123abc");
             Statement s = c.createStatement()) {

            // Load product_type -> spec names
            Map<String, Set<String>> ptSpecNames = new LinkedHashMap<>();
            ResultSet rs = s.executeQuery(
                "SELECT rel.product_type, ps.name FROM t_product_type_spec_rel rel JOIN t_product_specs ps ON rel.specs_id = ps.id");
            while (rs.next()) {
                ptSpecNames.computeIfAbsent(rs.getString(1), k -> new LinkedHashSet<>()).add(rs.getString(2));
            }

            // Find SKUs with empty specs_new and show what spec names they use vs what's available
            rs = s.executeQuery(
                "SELECT sku.id, sku.product_id, p.product_type, sku.specs::text " +
                "FROM t_product_sku sku LEFT JOIN t_product p ON sku.product_id = p.id " +
                "WHERE sku.specs IS NOT NULL AND sku.specs_new = '[]'::jsonb LIMIT 5");
            System.out.println("=== Unmatched SKUs (specs_new='[]') ===");
            while (rs.next()) {
                String jsonNames = rs.getString("specs");
                String ptype = rs.getString("product_type");
                Set<String> availableNames = ptSpecNames.getOrDefault(ptype, Collections.emptySet());
                System.out.println("SKU=" + rs.getString("id"));
                System.out.println("  product_type=" + ptype);
                System.out.println("  JSON spec names: " + jsonNames);
                System.out.println("  Available for this product_type: " + availableNames);
                System.out.println();
            }
        }
    }
}
