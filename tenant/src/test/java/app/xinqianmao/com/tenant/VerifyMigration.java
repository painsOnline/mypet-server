package app.xinqianmao.com.tenant;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import javax.sql.DataSource;
import java.sql.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = app.xinqianmao.com.tenant.web.TenantApplication.class,
    properties = {"spring.profiles.active=dev","mypet.db.host=127.0.0.1","mypet.db.port=1800","mypet.db.user=postgres","mypet.db.password=mypg123abc"})
class VerifyMigration {
    @Autowired @Qualifier("tenantConfigDataSource") DataSource configDs;
    @Autowired @Qualifier("tenantTemplateDataSource") DataSource templateDs;

    @Test void verifyConfig() throws Exception {
        try (Connection c = configDs.getConnection(); Statement s = c.createStatement()) {
            // t_admin table
            ResultSet rs = s.executeQuery("SELECT account FROM t_admin");
            int count = 0;
            while (rs.next()) { 
                String acct = rs.getString(1);
                assertNotNull(acct);
                count++;
            }
            assertTrue(count > 0, "t_admin must have at least 1 admin, found: " + count);
            
            // c_migration_log
            rs = s.executeQuery("SELECT migration_name, status FROM c_migration_log ORDER BY id");
            int migCount = 0;
            boolean hasConfig002 = false, has003 = false;
            while (rs.next()) { 
                String name = rs.getString(1);
                if ("config_002_tenant_admin.sql".equals(name)) hasConfig002 = true;
                if ("003_admin_security_tables.sql".equals(name)) has003 = true;
                migCount++;
            }
            assertTrue(hasConfig002, "config_002_tenant_admin.sql must be in c_migration_log");
            assertTrue(has003, "003_admin_security_tables.sql must be in c_migration_log");
            assertTrue(migCount >= 3, "c_migration_log must have >=3 entries, found: " + migCount);
        }
    }
    @Test void verifyEmpty() throws Exception {
        try (Connection c = templateDs.getConnection(); Statement s = c.createStatement()) {
            ResultSet rs = s.executeQuery("SELECT table_name FROM information_schema.tables WHERE table_schema='public' AND (table_name='c_admin_login_error_log' OR table_name='c_admin_login_lock') ORDER BY table_name");
            int count = 0;
            while (rs.next()) count++;
            assertEquals(2, count, "empty DB must have both security tables");
        }
    }
    @Test void verifyXlong() throws Exception {
        String url = "jdbc:postgresql://127.0.0.1:1800/mypet_xlong";
        try (Connection c = DriverManager.getConnection(url, "postgres", "mypg123abc"); Statement s = c.createStatement()) {
            ResultSet rs = s.executeQuery("SELECT table_name FROM information_schema.tables WHERE table_schema='public' AND (table_name='c_admin_login_error_log' OR table_name='c_admin_login_lock') ORDER BY table_name");
            int count = 0;
            while (rs.next()) count++;
            assertEquals(2, count, "xlong DB must have both security tables");
        }
    }
}
