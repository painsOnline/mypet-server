/**
 * File: FixImageDirs.java
 * Author: system
 * Date: 2026-05-16
 *
 * Rename product image directories from 32-char UUID to 36-char UUID (with dashes).
 * Run: mvn exec:java -pl common -Dexec.mainClass="app.xinqianmao.com.common.FixImageDirs"
 */
package app.xinqianmao.com.common;

import java.nio.file.*;
import java.util.regex.*;

public final class FixImageDirs {
    private static final Pattern UUID32 = Pattern.compile("^([a-f0-9]{8})([a-f0-9]{4})([a-f0-9]{4})([a-f0-9]{4})([a-f0-9]{12})$");

    public static void main(String[] args) throws Exception {
        String base = args.length > 0 ? args[0] : "F:/MyWorkspace/project/mypet/uploads";
        Path root = Paths.get(base);
        if (!Files.isDirectory(root)) { System.out.println("Not found: " + base); return; }

        // Walk products/ directories only (other dirs like banners/logos don't have ID in path)
        Path products = root.resolve("xlong").resolve("products");
        if (!Files.isDirectory(products)) { System.out.println("No products dir: " + products); return; }

        try (var dirs = Files.list(products)) {
            dirs.filter(Files::isDirectory).forEach(dir -> {
                String name = dir.getFileName().toString();
                Matcher m = UUID32.matcher(name);
                if (m.matches()) {
                    String newName = m.group(1) + "-" + m.group(2) + "-" + m.group(3) + "-" + m.group(4) + "-" + m.group(5);
                    Path newDir = products.resolve(newName);
                    try {
                        Files.move(dir, newDir);
                        System.out.println("OK: " + name + " -> " + newName);
                    } catch (Exception e) {
                        System.err.println("FAIL: " + name + " -> " + newName + " : " + e.getMessage());
                    }
                }
            });
        }
        System.out.println("Done.");
    }
}
