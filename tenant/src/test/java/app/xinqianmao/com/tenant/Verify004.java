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
class Verify004 {
    @Autowired @Qualifier("tenantConfigDataSource") DataSource ds;
    @Test void verify() throws Exception {
        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            ResultSet rs = s.executeQuery("SELECT code, name, is_disable, is_bussiness_open FROM c_tenant ORDER BY code");
            System.out.println("=== c_tenant ===");
            while (rs.next()) System.out.println("  code=" + rs.getString(1) + " name=" + rs.getString(2) 
                + " is_disable=" + rs.getInt(3) + " is_bussiness_open=" + rs.getInt(4));
            rs = s.executeQuery("SELECT data_type FROM information_schema.columns WHERE table_name='c_tenant' AND column_name='is_bussiness_open'");
            assertTrue(rs.next());
            assertEquals("smallint", rs.getString(1));
        }
    }
}
