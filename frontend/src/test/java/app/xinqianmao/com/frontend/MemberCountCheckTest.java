/**
 * File: MemberCountCheckTest.java
 * Author: system
 * Date: 2026-05-05
 *
 * Quick check: counts members in the database.
 */
package app.xinqianmao.com.frontend;

import app.xinqianmao.com.common.auth.TenantContext;
import app.xinqianmao.com.frontend.common.entity.Member;
import app.xinqianmao.com.frontend.dao.MemberMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MemberCountCheckTest extends BaseFrontendTest {

    @Autowired
    private MemberMapper memberMapper;

    @BeforeAll
    static void setUp() {
        initDatabases();
    }

    @Test
    @Order(1)
    @DisplayName("List all members from DB")
    void listAllMembers() {
        TenantContext.set(TENANT_CODE);
        try {
            List<Member> all = memberMapper.selectList(new LambdaQueryWrapper<Member>().orderByAsc(Member::getCreateTime));
            System.out.println("===== MEMBER COUNT: " + all.size() + " =====");
            for (Member m : all) {
                System.out.println("  " + m.getId() + " | " + m.getMobile() + " | " + m.getNickname() + " | " + m.getCreateTime());
            }
        } finally {
            TenantContext.clear();
        }
    }
}
