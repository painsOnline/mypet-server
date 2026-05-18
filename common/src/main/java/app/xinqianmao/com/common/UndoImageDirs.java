/**
 * Undo FixImageDirs: rename 36-char dirs back to 32-char.
 * Run: mvn exec:java -pl common -Dexec.mainClass="app.xinqianmao.com.common.UndoImageDirs"
 */
package app.xinqianmao.com.common;

import java.nio.file.*;
import java.util.regex.*;

public final class UndoImageDirs {
    private static final Pattern UUID36 = Pattern.compile("^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$");

    public static void main(String[] args) throws Exception {
        String base = args.length > 0 ? args[0] : "F:/MyWorkspace/project/mypet/uploads";
        Path products = Paths.get(base, "xlong", "products");
        if (!Files.isDirectory(products)) { System.out.println("Not found: " + products); return; }
        try (var dirs = Files.list(products)) {
            dirs.filter(Files::isDirectory).forEach(dir -> {
                String name = dir.getFileName().toString();
                if (UUID36.matcher(name).matches()) {
                    String oldName = name.replace("-", "");
                    try {
                        Files.move(dir, products.resolve(oldName));
                        System.out.println("OK: " + name + " -> " + oldName);
                    } catch (Exception e) {
                        System.err.println("FAIL: " + name + ": " + e.getMessage());
                    }
                }
            });
        }
        System.out.println("Done.");
    }
}
