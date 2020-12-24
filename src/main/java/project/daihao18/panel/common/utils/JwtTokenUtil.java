package project.daihao18.panel.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import project.daihao18.panel.entity.User;
import project.daihao18.panel.service.UserService;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.HashMap;

@Component
public class JwtTokenUtil {

    public static final String TOKEN_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";
    private static final String ROLE_CLAIMS = "role";
    private static final String SECRET = "daihao18";
    private static final String ISS = "daihao18";

    // 过期时间是3600秒，既是1个小时
    private static final long EXPIRATION_USER = 3600L;
    private static final long EXPIRATION_ADMIN = 604800L;
    private static final long EXPIRATION_REMEMBER = 2592000L;

    private static UserService userService;

    @Autowired
    public void setUserservice(UserService userservice) {
        JwtTokenUtil.userService = userservice;
    }

    // 创建token
    public static String createToken(String username, String role, boolean isRememberMe) {
        HashMap<String, Object> map = new HashMap<>();
        map.put(ROLE_CLAIMS, role);
        // 默认60分钟
        Date expiration;
        if (isRememberMe) {
            // 30天
            expiration = new Date(System.currentTimeMillis() + EXPIRATION_REMEMBER * 1000);
        } else {
            // user 60分钟, admin 7天
            expiration = new Date(System.currentTimeMillis() + ("admin".equals(role) ? EXPIRATION_ADMIN : EXPIRATION_USER) * 1000);
        }
        return Jwts.builder()
                .signWith(SignatureAlgorithm.HS512, SECRET)
                .setClaims(map)
                .setIssuer(ISS)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(expiration)
                .compact();
    }

    // 从token中获取id
    public static Integer getId(String token) {
        return Integer.parseInt(getTokenBody(token).getSubject());
    }

    // 从request中获取id
    public static Integer getId(HttpServletRequest request) {
        return Integer.parseInt(getTokenBody(request.getHeader(JwtTokenUtil.TOKEN_HEADER).replace(JwtTokenUtil.TOKEN_PREFIX, "")).getSubject());
    }

    // 从request获取该用户
    public static User getUser(HttpServletRequest request) {
        return userService.getUserById(getId(request), false);
    }

    // 获取用户角色
    public static String getUserRole(String token) {
        return (String) getTokenBody(token).get(ROLE_CLAIMS);
    }

    // 是否已过期
    public static boolean isExpiration(String token) {
        return getTokenBody(token).getExpiration().before(new Date());
    }

    public static boolean isExpiration(HttpServletRequest request) {
        return getTokenBody(request.getHeader(JwtTokenUtil.TOKEN_HEADER).replace(JwtTokenUtil.TOKEN_PREFIX, "")).getExpiration().before(new Date());
    }

    public static Date getExpiration(HttpServletRequest request) {
        return getTokenBody(request.getHeader(JwtTokenUtil.TOKEN_HEADER).replace(JwtTokenUtil.TOKEN_PREFIX, "")).getExpiration();
    }


    private static Claims getTokenBody(String token) {
        return Jwts.parser()
                .setSigningKey(SECRET)
                .parseClaimsJws(token)
                .getBody();
    }
}
