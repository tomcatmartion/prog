package com.shechubbb.smdc.common.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

/**
 * JWT工具类
 * 用于生成和解析JWT令牌（简化版，无第三方依赖）
 */
@Component
public class JwtUtil {

    // JWT密钥
    @Value("${jwt.secret:yourSecretKey123456789012345678901234567890}")
    private String secret;

    // JWT过期时间，默认7天
    @Value("${jwt.expiration:604800}")
    private long expiration;
    
    // 用户类型：普通用户
    public static final String USER_TYPE_USER = "USER";
    
    // 用户类型：管理员
    public static final String USER_TYPE_ADMIN = "ADMIN";

    /**
     * 创建JWT Token
     * @param userId 用户ID
     * @param username 用户名
     * @param userType 用户类型（USER/ADMIN）
     * @return JWT Token
     */
    public String createToken(Long userId, String username, String userType) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("userType", userType);
        
        // 过期时间
        long now = System.currentTimeMillis();
        claims.put("exp", now / 1000 + expiration);
        claims.put("iat", now / 1000);
        
        return createToken(claims);
    }

    /**
     * 从JWT令牌中获取用户ID
     * @param token JWT令牌
     * @return 用户ID
     */
    public Long getUserIdFromToken(String token) {
        Map<String, Object> claims = getClaimsFromToken(token);
        return claims != null ? Long.valueOf(claims.get("userId").toString()) : null;
    }

    /**
     * 从JWT令牌中获取用户类型
     * @param token JWT令牌
     * @return 用户类型
     */
    public String getUserTypeFromToken(String token) {
        Map<String, Object> claims = getClaimsFromToken(token);
        return claims != null ? claims.get("userType").toString() : null;
    }

    /**
     * 验证令牌是否有效
     * @param token JWT令牌
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        Map<String, Object> claims = getClaimsFromToken(token);
        if (claims == null) {
            return false;
        }
        
        // 检查过期时间
        Long exp = Long.valueOf(claims.getOrDefault("exp", 0).toString());
        return exp > System.currentTimeMillis() / 1000;
    }

    /**
     * 根据数据声明创建令牌
     * @param claims 数据声明
     * @return 令牌
     */
    private String createToken(Map<String, Object> claims) {
        try {
            // 1. 创建Header部分
            Map<String, Object> header = new HashMap<>();
            header.put("alg", "HS256");
            header.put("typ", "JWT");
            
            // 2. Base64编码Header和Payload部分
            String encodedHeader = base64UrlEncode(JSON.toJSONString(header));
            String encodedPayload = base64UrlEncode(JSON.toJSONString(claims));
            
            // 3. 创建签名
            String content = encodedHeader + "." + encodedPayload;
            String signature = hmacSha256(content, secret);
            
            // 4. 组装JWT
            return content + "." + signature;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从令牌中获取数据声明
     * @param token 令牌
     * @return 数据声明
     */
    private Map<String, Object> getClaimsFromToken(String token) {
        try {
            // 1. 分割JWT
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }
            
            // 2. 验证签名
            String content = parts[0] + "." + parts[1];
            String signature = parts[2];
            
            String expectedSignature = hmacSha256(content, secret);
            if (!signature.equals(expectedSignature)) {
                return null;
            }
            
            // 3. 解析Payload
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            return JSONObject.parseObject(payload);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Base64 URL安全编码
     */
    private String base64UrlEncode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * HMAC SHA-256签名
     */
    private String hmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } catch (Exception e) {
            throw new RuntimeException("签名失败", e);
        }
    }
} 