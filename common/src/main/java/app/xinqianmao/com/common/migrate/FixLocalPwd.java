package app.xinqianmao.com.common.migrate;
import java.sql.*;

public final class FixLocalPwd {
    public static void main(String[] a) throws Exception {
        Class.forName("org.postgresql.Driver");
        String localPwd = "rNQ/rOH0fCKXGFtML1I6cQ=="; // encrypted mypg123abc from init-config.sql
        try (Connection c = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:1800/mypet_config", "postgres", "mypg123abc");
             Statement s = c.createStatement()) {
            s.execute("UPDATE c_database_instance SET password='" + localPwd + "'");
            System.out.println("Password updated to local.");
        }
    }
}
