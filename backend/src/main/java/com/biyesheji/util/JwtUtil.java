package com.biyesheji.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private Key getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long userId, String username, Integer role) {
        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isTokenValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Long getUserId(String token) {
        return parseToken(token).get("userId", Long.class);
    }

    public String getUsername(String token) {
        return parseToken(token).getSubject();
    }

    public Integer getRole(String token) {
        return parseToken(token).get("role", Integer.class);
    }

    /** 返回 token 距离过期的毫秒数，已过期返回负数 */
    public long getTimeToExpire(String token) {
        Date exp = parseToken(token).getExpiration();
        return exp.getTime() - System.currentTimeMillis();
    }

    /** 用 token 中已有的 userId/username/role 重新签发，重置过期时间 */
    public String renewToken(String token) {
        Claims claims = parseToken(token);
        return generateToken(
                claims.get("userId", Long.class),
                claims.getSubject(),
                claims.get("role", Integer.class));
    }
}
