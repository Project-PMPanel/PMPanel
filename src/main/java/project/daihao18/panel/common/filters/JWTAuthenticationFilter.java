package project.daihao18.panel.common.filters;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.transaction.annotation.Transactional;
import project.daihao18.panel.common.response.Result;
import project.daihao18.panel.common.response.ResultCodeEnum;
import project.daihao18.panel.common.utils.IpUtil;
import project.daihao18.panel.common.utils.JwtTokenUtil;
import project.daihao18.panel.entity.OperateIp;
import project.daihao18.panel.entity.User;
import project.daihao18.panel.service.OperateIpService;
import project.daihao18.panel.service.UserService;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

public class JWTAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private AuthenticationManager authenticationManager;

    private UserService userService;

    private OperateIpService operateIpService;

    public JWTAuthenticationFilter(AuthenticationManager authenticationManager, UserService userService, OperateIpService operateIpService) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.operateIpService = operateIpService;
        super.setFilterProcessesUrl("/auth/login");
    }

    @Override
    @Transactional
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        // 从输入流中获取到登录的信息
        try {
            User loginUser = new ObjectMapper().readValue(request.getInputStream(), User.class);
            Integer id = userService.getIdByEmail(loginUser.getEmail());
            User user = null;
            if (ObjectUtil.isNotEmpty(id)) {
                user = userService.getById(id);
            } else {
                return null;
            }
            request.setAttribute("rememberMe", loginUser.getRememberMe());
            return authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(user.getId(), loginUser.getPassword(), new ArrayList<>()));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // 成功验证后调用的方法
    // 如果验证成功，就生成token并返回
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException {

        // 查看源代码会发现调用getPrincipal()方法会返回一个实现了`UserDetails`接口的对象
        // 所以就是JwtUser啦
        User jwtUser = (User) authResult.getPrincipal();
        User user = userService.getUserById(jwtUser.getId(), false);
        String role = "";
        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
        // springcache 导致authorities为null, 但是从缓存中可以取到role,所以在这里判断一下从哪获取role
        if (ObjectUtil.isNotEmpty(authorities)) {
            for (GrantedAuthority authority : authorities) {
                role = authority.getAuthority();
            }
        } else {
            role = user.getRole().get("id").toString();
        }
        Boolean remember = (Boolean) request.getAttribute("rememberMe");
        String token = JwtTokenUtil.createToken(user.getUsername(), role, ObjectUtil.equal(remember, true));
        // 记录登录ip
        OperateIp operateIp = new OperateIp();
        operateIp.setIp(IpUtil.getIpAddr(request));
        operateIp.setTime(new Date());
        operateIp.setType(1);
        operateIp.setUserId(user.getId());
        operateIpService.save(operateIp);
        // 返回创建成功的token
        // 但是这里创建的token只是单纯的token
        // 按照jwt的规定，最后请求的格式应该是 `Bearer token`
        response.setHeader("Authorization", JwtTokenUtil.TOKEN_PREFIX + token);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/json;charset=UTF-8");
        response.getWriter().write(JSONUtil.toJsonStr(Result.ok().data("authorization", token)));
    }

    // 这是验证失败时候调用的方法
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/json;charset=UTF-8");
        response.getWriter().write(JSONUtil.toJsonStr(Result.setResult(ResultCodeEnum.PARAM_ERROR)));
    }
}
