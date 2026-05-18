/**
 * Restore image URLs: convert 36-char dashed UUIDs back to 32-char (file names on disk).
 * Run: mvn exec:java -pl common -Dexec.mainClass="app.xinqianmao.com.common.FixImageUrls"
 */
package app.xinqianmao.com.common;

import java.sql.*;

public final class FixImageUrls {
    static final String U = "jdbc:postgresql://127.0.0.1:1800/";

    public static void main(String[] a) throws Exception {
        Class.forName("org.postgresql.Driver");
        for (String db : new String[]{"mypet_xlong"}) {
            System.out.println("=== " + db + " ===");
            try (Connection c = DriverManager.getConnection(U + db, "postgres", "mypg123abc"); Statement s = c.createStatement()) {
                // Regex: 36-char dashed UUID → 32-char plain hex (reverse STEP 0.6)
                String pattern = "([a-f0-9]{8})-([a-f0-9]{4})-([a-f0-9]{4})-([a-f0-9]{4})-([a-f0-9]{12})";

                // t_product: picture, main_pictures[], detail
                s.execute("UPDATE t_product SET picture = regexp_replace(picture, '" + pattern + "', '\\1\\2\\3\\4\\5') WHERE picture ~ '[a-f0-9]{8}-'");
                s.execute("UPDATE t_product SET main_pictures = (SELECT array_agg(regexp_replace(v, '" + pattern + "', '\\1\\2\\3\\4\\5')) FROM unnest(main_pictures) v) WHERE EXISTS (SELECT 1 FROM unnest(main_pictures) v WHERE v ~ '[a-f0-9]{8}-')");
                s.execute("UPDATE t_product SET detail = regexp_replace(detail, '" + pattern + "', '\\1\\2\\3\\4\\5', 'g') WHERE detail ~ '[a-f0-9]{8}-'");
                System.out.println("  t_product OK");

                // t_product_sku: picture
                s.execute("UPDATE t_product_sku SET picture = regexp_replace(picture, '" + pattern + "', '\\1\\2\\3\\4\\5') WHERE picture ~ '[a-f0-9]{8}-'");
                System.out.println("  t_product_sku OK");

                // t_product_brand: brand_logo
                s.execute("UPDATE t_product_brand SET brand_logo = regexp_replace(brand_logo, '" + pattern + "', '\\1\\2\\3\\4\\5') WHERE brand_logo ~ '[a-f0-9]{8}-'");
                System.out.println("  t_product_brand OK");

                // t_product_category: picture
                s.execute("UPDATE t_product_category SET picture = regexp_replace(picture, '" + pattern + "', '\\1\\2\\3\\4\\5') WHERE picture ~ '[a-f0-9]{8}-'");
                System.out.println("  t_product_category OK");

                // t_shop: logo, banners
                s.execute("UPDATE t_shop SET logo = regexp_replace(logo, '" + pattern + "', '\\1\\2\\3\\4\\5') WHERE logo ~ '[a-f0-9]{8}-'");
                s.execute("UPDATE t_shop SET banners = regexp_replace(banners::text, '" + pattern + "', '\\1\\2\\3\\4\\5', 'g')::jsonb WHERE banners::text ~ '[a-f0-9]{8}-'");
                System.out.println("  t_shop OK");
            }
        }
        System.out.println("Done.");
    }
}
