package app.xinqianmao.com.common.migrate;
import java.sql.*;

public final class CheckProps {
    public static void main(String[] a) throws Exception {
        Class.forName("org.postgresql.Driver");
        try (Connection c = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:1800/mypet_xlong", "postgres", "mypg123abc");
             Statement s = c.createStatement()) {
            // Count null value_ids
            ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM t_product_properties WHERE value_id IS NULL");
            rs.next(); System.out.println("value_id IS NULL: " + rs.getInt(1));

            rs = s.executeQuery("SELECT COUNT(*) FROM t_product_properties WHERE value_id IS NOT NULL");
            rs.next(); System.out.println("value_id IS NOT NULL: " + rs.getInt(1));

            // Sample null ones
            rs = s.executeQuery("SELECT pp.specs_id, pp.value_name, ps.input_type, ps.name " +
                "FROM t_product_properties pp JOIN t_product_specs ps ON pp.specs_id = ps.id " +
                "WHERE pp.value_id IS NULL LIMIT 10");
            System.out.println("\n=== Sample NULL value_id ===");
            while (rs.next()) System.out.println("  specs_id=" + rs.getString(1).trim() + " value_name=" + rs.getString(2) + " input_type=" + rs.getInt(3) + " spec_name=" + rs.getString(4));

            // Check type mismatch
            rs = s.executeQuery("SELECT udt_name FROM information_schema.columns WHERE table_name='t_product_properties' AND column_name='specs_id'");
            rs.next(); System.out.println("\nt_product_properties.specs_id type: " + rs.getString(1));
            rs = s.executeQuery("SELECT udt_name FROM information_schema.columns WHERE table_name='t_product_specs_value' AND column_name='specs_id'");
            rs.next(); System.out.println("t_product_specs_value.specs_id type: " + rs.getString(1));
            rs = s.executeQuery("SELECT udt_name FROM information_schema.columns WHERE table_name='t_product_specs' AND column_name='id'");
            rs.next(); System.out.println("t_product_specs.id type: " + rs.getString(1));
        }
    }
}
