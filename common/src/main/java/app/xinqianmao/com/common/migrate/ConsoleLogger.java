/**
 * File: ConsoleLogger.java
 * Author: system
 * Date: 2026-05-18
 *
 * Simple stdout/stderr logger for command-line use and tests.
 */
package app.xinqianmao.com.common.migrate;

public class ConsoleLogger implements MigrationLogger {
    public void info(String msg)  { System.out.println(msg); }
    public void warn(String msg)  { System.out.println("[WARN] " + msg); }
    public void error(String msg) { System.err.println("[ERROR] " + msg); }
}
