package app.xinqianmao.com.tenant;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import app.xinqianmao.com.tenant.service.MigrationRunnerService;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = app.xinqianmao.com.tenant.web.TenantApplication.class,
    properties = {"spring.profiles.active=dev","mypet.db.host=127.0.0.1","mypet.db.port=1800","mypet.db.user=postgres","mypet.db.password=mypg123abc"})
class RunMigration004 {
    @Autowired MigrationRunnerService runner;
    @Test void run004() {
        Map<String,Object> r = runner.runMigration("config_004_add_is_business_open.sql");
        System.out.println("004 result: " + r);
        assertEquals("success", r.get("status"), "Migration 004 must succeed: " + r.get("result"));
    }
}
