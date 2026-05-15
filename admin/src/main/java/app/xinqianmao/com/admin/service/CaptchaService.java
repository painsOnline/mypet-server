package app.xinqianmao.com.admin.service;

import app.xinqianmao.com.common.utils.CryptoUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Captcha image generation with visual anti-AI distortion.
 * Stores answer in memory mapped to encrypted token (5-min expiry).
 */
@Slf4j
@Service
public class CaptchaService {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int WIDTH = 200, HEIGHT = 70;
    private static final int EXPIRE_MIN = 5;
    private static final String SECRET = "mypet-jwt-secret-key-2026-minimum-32chars!!";

    private final ConcurrentMap<String, TokenEntry> tokenStore = new ConcurrentHashMap<>();
    private final Random rnd = new Random();

    private static int clamp(int value) {
        return Math.min(255, Math.max(0, value));
    }

    private record TokenEntry(String answer, long expireAt) {}

    /** Generate captcha image and return {token, base64Image}. */
    public java.util.Map<String, String> generate() {
        int len = 4 + rnd.nextInt(4); // 4-7 chars
        StringBuilder answer = new StringBuilder();
        for (int i = 0; i < len; i++) answer.append(CHARS.charAt(rnd.nextInt(CHARS.length())));
        String ans = answer.toString();

        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Background texture/waves
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int shade = 200 + rnd.nextInt(55);
                img.setRGB(x, y, new Color(clamp(shade), clamp(shade - 10 + rnd.nextInt(20)),
                        clamp(shade - 20 + rnd.nextInt(30))).getRGB());
            }
        }
        // Random wave lines
        g.setColor(new Color(180, 180, 200, 60));
        for (int i = 0; i < 6; i++) {
            int y1 = rnd.nextInt(HEIGHT), y2 = rnd.nextInt(HEIGHT);
            g.drawLine(0, y1, WIDTH, y2);
        }

        // Draw characters with random overlap — advance by 40-70% of char width
        int xPos = 8;
        for (int i = 0; i < ans.length(); i++) {
            char ch = ans.charAt(i);
            Font font = new Font(rnd.nextBoolean() ? "Serif" : "SansSerif",
                    rnd.nextInt(3), 32 + rnd.nextInt(14));
            g.setFont(font);
            g.setColor(new Color(30 + rnd.nextInt(60), 20 + rnd.nextInt(80), 80 + rnd.nextInt(120)));

            int charW = g.getFontMetrics().stringWidth(String.valueOf(ch));
            // Clamp xPos to keep char within image bounds
            if (xPos + charW > WIDTH - 4) xPos = WIDTH - charW - 4;
            if (xPos < 4) xPos = 4;

            AffineTransform at = new AffineTransform();
            double angle = (rnd.nextDouble() - 0.5) * 0.6;
            at.rotate(angle);
            at.translate(xPos, 35 + rnd.nextInt(20));

            g.setTransform(at);
            g.drawString(String.valueOf(ch), 0, 0);
            g.setTransform(new AffineTransform());

            // Normal spacing with occasional slight overlap (~30% chance)
            boolean overlap = rnd.nextInt(10) < 3;
            double ratio = overlap ? 0.55 + rnd.nextDouble() * 0.2   // 55-75% → slight overlap
                                   : 0.82 + rnd.nextDouble() * 0.13; // 82-95% → near-normal spacing
            xPos += (int)(charW * ratio) + 1;
        }

        // Noise dots
        for (int i = 0; i < 80; i++) {
            g.setColor(new Color(rnd.nextInt(255), rnd.nextInt(255), rnd.nextInt(255), 100));
            int nx = rnd.nextInt(WIDTH), ny = rnd.nextInt(HEIGHT);
            g.fillOval(nx, ny, 2 + rnd.nextInt(3), 2 + rnd.nextInt(3));
        }
        // Color blocks for anti-AI
        for (int i = 0; i < 4; i++) {
            g.setColor(new Color(rnd.nextInt(200), rnd.nextInt(200), rnd.nextInt(200), 80));
            g.fillRect(rnd.nextInt(WIDTH - 30), rnd.nextInt(HEIGHT - 15), 10 + rnd.nextInt(30), 5 + rnd.nextInt(15));
        }
        g.dispose();

        // Encode to Base64
        String imgBase64;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", bos);
            imgBase64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(bos.toByteArray());
        } catch (Exception e) { throw new RuntimeException(e); }

        // Generate encrypted token
        String rawToken = java.util.UUID.randomUUID().toString();
        String token = CryptoUtil.encrypt(rawToken + "::" + ans, SECRET);
        tokenStore.put(token, new TokenEntry(ans, System.currentTimeMillis() + EXPIRE_MIN * 60_000L));

        // Clean expired
        tokenStore.entrySet().removeIf(e -> e.getValue().expireAt < System.currentTimeMillis());

        java.util.Map<String, String> result = new java.util.LinkedHashMap<>();
        result.put("token", token);
        result.put("image", imgBase64);
        return result;
    }

    /** Validate captcha answer against token. Returns true if correct. */
    public boolean validate(String token, String userInput) {
        if (token == null || userInput == null) return false;
        TokenEntry entry = tokenStore.get(token);
        if (entry == null || System.currentTimeMillis() > entry.expireAt) {
            tokenStore.remove(token);
            return false;
        }
        boolean ok = entry.answer.equalsIgnoreCase(userInput.trim());
        if (ok) tokenStore.remove(token);
        return ok;
    }
}
