package coffee_backend_4j.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;


    private SecretKey getSignKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Integer userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("user_id", userId);
        return createToken(claims, userId.toString());
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .signWith(getSignKey())
                .compact();

//        jwt自带过期
//        Date now = new Date();
//        Date expiryDate = new Date(now.getTime() + expiration);
//
//        return Jwts.builder()
//                .claims(claims)
//                .subject(subject)
//                .issuedAt(now)
//                .expiration(expiryDate)
//                .signWith(getSignKey())
//                .compact();
    }

    public Integer getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("user_id", Integer.class);
    }

    public Boolean validateToken(String token) {
        try {
            parseToken(token);
//            return !isTokenExpired(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
//令牌过期校验
//    private Boolean isTokenExpired(String token) {
//        Date expiration = parseToken(token).getExpiration();
//        return expiration.before(new Date());
//    }
}
