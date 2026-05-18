package app.xinqianmao.com.common.migrate;
import java.sql.*;
import java.util.*;

public final class CheckDataIssue {
    public static void main(String[] a) throws Exception {
        Class.forName("org.postgresql.Driver");
        try (Connection c = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:1800/mypet_xlong", "postgres", "mypg123abc");
             Statement s = c.createStatement()) {

            // Count specs by type
            ResultSet rs = s.executeQuery("SELECT input_type, COUNT(*) FROM t_product_specs GROUP BY input_type ORDER BY input_type");
            System.out.println("=== t_product_specs by input_type ===");
            while (rs.next()) System.out.println("  type=" + rs.getInt(1) + ": " + rs.getInt(2) + " rows");

            // Count products
            rs = s.executeQuery("SELECT COUNT(*) FROM t_product WHERE is_delete=0");
            rs.next();
            int productCnt = rs.getInt(1);
            System.out.println("\nt_product (is_delete=0): " + productCnt);

            // Count SKUs
            rs = s.executeQuery("SELECT COUNT(*), COUNT(DISTINCT product_id) FROM t_product_sku WHERE is_delete=0");
            rs.next();
            System.out.println("t_product_sku (is_delete=0): " + rs.getInt(1) + " rows, " + rs.getInt(2) + " distinct products");

            // Type-1 specs with input_options={"默认规格"} or similar
            rs = s.executeQuery("SELECT COUNT(*) FROM t_product_specs WHERE input_type=1");
            rs.next();
            int type1Cnt = rs.getInt(1);
            System.out.println("t_product_specs type=1 count: " + type1Cnt);

            // Products WITHOUT a matching type-1 spec (via product_type)
            rs = s.executeQuery(
                "SELECT COUNT(*) FROM t_product p WHERE NOT EXISTS " +
                "(SELECT 1 FROM t_product_specs ps WHERE ps.product_type = p.product_type AND ps.input_type = 1)");
            rs.next();
            System.out.println("Products WITHOUT type-1 spec in their product_type: " + rs.getInt(1));

            // Show some example mismatches
            rs = s.executeQuery(
                "SELECT p.id, p.name, p.product_type, " +
                "(SELECT COUNT(*) FROM t_product_specs ps WHERE ps.product_type = p.product_type AND ps.input_type = 1) as sku_spec_cnt " +
                "FROM t_product p WHERE is_delete=0 " +
                "AND (SELECT COUNT(*) FROM t_product_specs ps WHERE ps.product_type = p.product_type AND ps.input_type = 1) = 0 " +
                "LIMIT 5");
            System.out.println("\n=== Products with 0 type-1 specs ===");
            while (rs.next()) System.out.println("  " + rs.getString("name") + " (type=" + rs.getString("product_type") + ")");

            // SKU specs JSON sample - what spec names do they use?
            rs = s.executeQuery(
                "SELECT DISTINCT elem->>'name' AS sname, COUNT(*) as cnt " +
                "FROM t_product_sku, jsonb_array_elements(t_product_sku.specs::jsonb) AS elem " +
                "WHERE t_product_sku.is_delete = 0 " +
                "GROUP BY elem->>'name' ORDER BY cnt DESC LIMIT 10");
            System.out.println("\n=== SKU spec names used in t_product_sku.specs ===");
            while (rs.next()) System.out.println("  '" + rs.getString(1) + "': " + rs.getInt(2) + " times");

            // Compare: spec names in t_product_specs vs in SKU JSON
            rs = s.executeQuery(
                "SELECT name FROM t_product_specs WHERE input_type = 1 ORDER BY name");
            System.out.println("\n=== t_product_specs names (type=1) ===");
            while (rs.next()) System.out.println("  '" + rs.getString(1) + "'");
        }
    }
}
