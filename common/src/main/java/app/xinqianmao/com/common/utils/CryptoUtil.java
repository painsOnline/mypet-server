/**
 * File: CryptoUtil.java
 * Author: system
 * Date: 2026-05-10
 */
package app.xinqianmao.com.common.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES symmetric encryption utility for sensitive config values (e.g. DB passwords).
 * Uses the JWT secret as the encryption key (hashed to 16 bytes).
 */
public final class CryptoUtil {

    private static final String ALGORITHM = "AES";

    private CryptoUtil() {}

    /**
     * Encrypt plaintext with the given key, returns Base64-encoded ciphertext.
     */
    public static String encrypt(String plaintext, String key) {
        try {
            SecretKeySpec keySpec = deriveKey(key);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * Decrypt Base64-encoded ciphertext with the given key.
     */
    public static String decrypt(String ciphertext, String key) {
        try {
            SecretKeySpec keySpec = deriveKey(key);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(ciphertext));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }

    private static SecretKeySpec deriveKey(String key) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha.digest(key.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(Arrays.copyOf(keyBytes, 16), ALGORITHM);
        } catch (Exception e) {
            throw new RuntimeException("Key derivation failed", e);
        }
    }
}
