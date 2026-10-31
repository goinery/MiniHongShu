package com.hongshu.common.utils;

import com.hongshu.common.constant.TokenConstant;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Date;

/**
 * @author hongshu
 */
@Slf4j
public class JwtUtils {

    private static volatile String appSecret;

    private JwtUtils() {
    }

    public static void setAppSecret(String secret) {
        if (StringUtils.isBlank(secret)) {
            throw new IllegalArgumentException("hongshu.jwt.app-secret must not be blank");
        }
        appSecret = secret;
    }

    private static String signingKey() {
        String secret = appSecret;
        if (StringUtils.isBlank(secret)) {
            throw new IllegalStateException("hongshu.jwt.app-secret is not configured");
        }
        return secret;
    }

    /**
     * 生成token
     *
     */
    public static String getJwtToken(String uid, long expirationTime) {
        return getJwtToken(uid, expirationTime, null, null);
    }

    public static String getJwtToken(String uid, long expirationTime, String sessionId, String tokenType) {
        return Jwts.builder()
                //设置token的头信息
                .setHeaderParam("typ", "JWT")
                .setHeaderParam("alg", "HS256")
                //设置过期时间
                .setSubject("user")
                .setIssuedAt(new Date())
                //设置刷新
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                //设置token的主题部分
                .claim("userId", uid)
                .claim("sessionId", sessionId)
                .claim("tokenType", tokenType)
                //签名哈希
                .signWith(SignatureAlgorithm.HS256, signingKey())
                .compact();
    }

    /**
     * 判断token是否存在与有效
     *
     */
    public static boolean checkToken(String jwtToken) {
        if (StringUtils.isEmpty(jwtToken)) {
            return false;
        }
        try {
            //验证是否有效的token
            Jwts.parser().setSigningKey(signingKey()).parseClaimsJws(jwtToken);
        } catch (Exception e) {
            log.error(e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * 根据token信息得到getUserId
     *
     */
    public static String getUserId(String jwtToken) {
        //验证是否有效的token
        Jws<Claims> claimsJws = Jwts.parser().setSigningKey(signingKey()).parseClaimsJws(jwtToken);
        //得到字符串的主题部分
        Claims claims = claimsJws.getBody();
        return (String) claims.get("userId");
    }

    public static Claims getClaims(String jwtToken) {
        return Jwts.parser().setSigningKey(signingKey()).parseClaimsJws(jwtToken).getBody();
    }

    /**
     * 判断token是否存在与有效
     *
     */
    public static boolean checkToken(HttpServletRequest request) {
        try {
            String jwtToken = request.getHeader(TokenConstant.ACCESS_TOKEN);
            if (StringUtils.isEmpty(jwtToken)) {
                return false;
            }
            Jwts.parser().setSigningKey(signingKey()).parseClaimsJws(jwtToken);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(JwtUtils.class).warn("操作失败", e);
            return false;
        }
        return true;
    }
}
