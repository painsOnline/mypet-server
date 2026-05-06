import java.sql.*;
import java.io.*;
public class CheckDb {
public static void main(String[] a) throws Exception {
Class.forName("org.postgresql.Driver");
StringBuilder sb = new StringBuilder();
String url = "jdbc:postgresql://127.0.0.1:1800/postgres";
try (Connection c = DriverManager.getConnection(url, "postgres", "mypg123abc");
     Statement s = c.createStatement();
     ResultSet rs = s.executeQuery("SELECT datname FROM pg_database WHERE datname LIKE 'mypet%' ORDER BY datname")) {
    sb.append("=== mypet databases ===\n");
    while (rs.next()) sb.append("  ").append(rs.getString(1)).append("\n");
}
try (Connection c = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:1800/mypet_xlong", "postgres", "mypg123abc");
     ResultSet rs = c.getMetaData().getTables(null, null, null, new String[]{"TABLE"})) {
    sb.append("\n=== tables in mypet_xlong ===\n");
    int n = 0;
    while (rs.next()) { sb.append("  ").append(rs.getString("TABLE_NAME")).append("\n"); n++; }
    sb.append("  Total: ").append(n).append("\n");
}
Files.writeString(Paths.get("check-db-result.txt"), sb.toString());
}}
