/**
 * File: MemberSeeder.java
 * Author: system
 * Date: 2026-05-06
 *
 * Seeds 30 test members into mypet_xlong via direct JDBC (commits immediately).
 * Per README #22: inserts only, skips existing mobiles.
 *
 * Run: mvn compile -pl common -q && mvn exec:java -pl common -Dexec.mainClass="app.xinqianmao.com.common.MemberSeeder"
 */
package app.xinqianmao.com.common;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

public final class MemberSeeder {

    private static final String URL = "jdbc:postgresql://127.0.0.1:1800/mypet_xlong";
    private static final String USER = "postgres";
    private static final String PASSWORD = "mypg123abc";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        System.out.println("==== Member Seeder ====");
        try {
            Class.forName("org.postgresql.Driver");

            String[] nicknames = {
                "爱宠达人", "喵星人控", "狗粮专家", "宠物小助手", "毛毛家长",
                "鱼乐无穷", "小鸟依人", "仓鼠大王", "龟龟慢生活", "兔兔快跑",
                "萌宠日记", "猫粮测评师", "汪汪队队长", "铲屎官老张", "遛狗小能手",
                "布偶猫舍", "金毛控", "柯基爱好者", "英短铲屎官", "柴犬小分队",
                "宠食测评", "喵不可言", "汪星来客", "家有懒猫", "爱宠一生",
                "宠物营养师", "小橘猫", "拉布拉多妈", "边境牧羊人", "虎斑猫爸"
            };

            // Read existing mobiles to skip
            Set<String> existing = new HashSet<>();
            try (Connection c = DriverManager.getConnection(URL, USER, PASSWORD);
                 Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery("SELECT mobile FROM t_member")) {
                while (rs.next()) existing.add(rs.getString("mobile"));
            }
            System.out.println("Existing members: " + existing.size());

            // Build member rows
            LocalDateTime now = LocalDateTime.now();
            List<Object[]> members = new ArrayList<>();
            for (int i = 0; i < 30; i++) {
                String mobile = String.format("138%08d", 10000001 + i * 357);
                if (existing.contains(mobile)) {
                    System.out.println("[SKIP] " + mobile + " (already exists)");
                    continue;
                }
                String id = UUID.randomUUID().toString().replace("-", "");
                String nickname = "用户" + nicknames[i];
                String createTime = now.minus((long)(Math.random() * 90), ChronoUnit.DAYS).format(FMT);
                members.add(new Object[]{id, mobile, mobile, nickname, "", createTime});
            }

            // Insert
            String sql = "INSERT INTO t_member (id, account, mobile, nickname, avatar, create_time) "
                    + "VALUES (?, ?, ?, ?, ?, ?::timestamp)";
            int inserted = 0;
            try (Connection c = DriverManager.getConnection(URL, USER, PASSWORD);
                 PreparedStatement ps = c.prepareStatement(sql)) {
                for (Object[] m : members) {
                    ps.setString(1, (String) m[0]);   // id
                    ps.setString(2, (String) m[1]);   // account
                    ps.setString(3, (String) m[2]);   // mobile
                    ps.setString(4, (String) m[3]);   // nickname
                    ps.setString(5, (String) m[4]);   // avatar
                    ps.setString(6, (String) m[5]);   // createTime
                    ps.executeUpdate();
                    inserted++;
                    System.out.println("[OK] " + m[2] + " → " + m[3]);
                }
            }

            // Verify
            try (Connection c = DriverManager.getConnection(URL, USER, PASSWORD);
                 Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM t_member")) {
                rs.next();
                int total = rs.getInt(1);
                System.out.println("==== Done: " + inserted + " inserted, " + total + " total in mypet_xlong ====");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
