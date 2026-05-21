package app.xinqianmao.com.tenant;
import java.sql.*;
public class QuickVerify {
    public static void main(String[] args) throws Exception {
        Class.forName("org.postgresql.Driver");
        String url = "jdbc:postgresql://127.0.0.1:1800/";
        String u = "postgres", p = "mypg123abc";
        try (Connection c = DriverManager.getConnection(url + "mypet_config", u, p); Statement s = c.createStatement()) {
            ResultSet rs = s.executeQuery("SELECT account FROM t_admin");
            System.out.println("=== t_admin in config ===");
            while (rs.next()) System.out.println("  account: " + rs.getString(1));
            rs = s.executeQuery("SELECT migration_name, status FROM c_migration_log ORDER BY id");
            System.out.println("=== c_migration_log ===");
            while (rs.next()) System.out.println("  " + rs.getString(1) + " -> " + rs.getString(2));
        }
        try (Connection c = DriverManager.getConnection(url + "mypet_empty", u, p); Statement s = c.createStatement()) {
            ResultSet rs = s.executeQuery("SELECT table_name FROM information_schema.tables WHERE table_schema='public' AND table_name LIKE 'c_admin_login%' ORDER BY table_name");
            System.out.println("=== empty security tables ===");
            while (rs.next()) System.out.println("  " + rs.getString(1));
        }
        try (Connection c = DriverManager.getConnection(url + "mypet_xlong", u, p); Statement s = c.createStatement()) {
            ResultSet rs = s.executeQuery("SELECT table_name FROM information_schema.tables WHERE table_schema='public' AND table_name LIKE 'c_admin_login%' ORDER BY table_name");
            System.out.println("=== xlong security tables ===");
            while (rs.next()) System.out.println("  " + rs.getString(1));
        }
        System.out.println("DONE - all verified");
    }
}
