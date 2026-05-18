package app.xinqianmao.com.common.migrate;
import java.sql.*;

public final class UpdateLocalDb {
    public static void main(String[] a) throws Exception {
        Class.forName("org.postgresql.Driver");
        try (Connection c = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:1800/mypet_config", "postgres", "mypg123abc");
             Statement s = c.createStatement()) {

            // Check current values
            ResultSet rs = s.executeQuery("SELECT host, port, \"user\", password FROM c_database_instance");
            rs.next();
            System.out.println("Before: host=" + rs.getString(1) + " port=" + rs.getInt(2) + " user=" + rs.getString(3));

            // Update to local
            String localPwd = "rNQ/rOH0fCKXGFtML1I6cQ=="; // encrypted mypg123abc from init-config.sql
            s.execute("UPDATE c_database_instance SET host='127.0.0.1', port=1800, \"user\"='postgres', password='" + localPwd + "'");

            rs = s.executeQuery("SELECT host, port, \"user\", password FROM c_database_instance");
            rs.next();
            System.out.println("After:  host=" + rs.getString(1) + " port=" + rs.getInt(2) + " user=" + rs.getString(3));
            System.out.println("Done.");
        }
    }
}
