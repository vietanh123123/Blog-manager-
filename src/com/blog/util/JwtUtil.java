package com.blog.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Map;
import com.blog.util.JsonUtil;

public class JwtUtil {

    private static final String SECRET = "7f14d7abf3eeb14834598627a31c75a0b84b15aaa333fc8e5f430d7dd7221764";
    private static final long EXPIRY_MS = 1000 * 60 * 10; // 10 minutes

    public static String generateToken(long userId, String email) {
        String header  = base64Encode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payload = base64Encode(
                "{\"userId\":" + userId + ",\"email\":\"" + email + "\"," +
                        "\"exp\":" + (System.currentTimeMillis() + EXPIRY_MS) + "}"
        );
        String signature = hmacSign(header + "." + payload);
        return header + "." + payload + "." + signature;
    }

    public static Map<String, String> verifyToken(String token) {
        // 1. Split into 3 parts
        String[] parts = token.split("\\.");
        if (parts.length != 3) throw new RuntimeException("Invalid token");

        // 2. Re-sign header+payload and compare — this is the verification
        String expectedSig = hmacSign(parts[0] + "." + parts[1]);
        if (!expectedSig.equals(parts[2]))
            throw new RuntimeException("Invalid signature");

        // 3. Decode payload and parse claims
        String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
        Map<String, String> claims = JsonUtil.parseJsonBody(payloadJson);

        // 4. Check expiry
        long exp = Long.parseLong(claims.get("exp"));
        if (System.currentTimeMillis() > exp)
            throw new RuntimeException("Token expired");

        return claims; // contains userId, email
    }

    private static String hmacSign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(), "HmacSHA256"));
            return base64Encode(mac.doFinal(data.getBytes()));
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private static String base64Encode(String data) {
        return base64Encode(data.getBytes());
    }

    private static String base64Encode(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }
}