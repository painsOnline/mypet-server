/**
 * File: UuidGenerator.java
 * Author: system
 * Date: 2026-05-16
 */
package app.xinqianmao.com.common.dao;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Custom ID generator that produces standard 36-char UUIDs with dashes.
 * Replaces MyBatis-Plus ASSIGN_UUID which strips dashes (32 chars).
 */
@Component
public class UuidGenerator implements IdentifierGenerator {

    @Override
    public Number nextId(Object entity) {
        return null; // not used for string IDs
    }

    @Override
    public String nextUUID(Object entity) {
        return UUID.randomUUID().toString();
    }
}
