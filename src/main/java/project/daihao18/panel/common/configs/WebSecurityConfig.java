package project.daihao18.panel.common.configs;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.crypto.digest.DigestUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import project.daihao18.panel.common.filters.JWTAuthenticationFilter;
import project.daihao18.panel.common.filters.JWTAuthorizationFilter;
import project.daihao18.panel.service.OperateIpService;
import project.daihao18.panel.service.UserService;

@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true) // 激活方法上的PreAuthorize注解
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    @Qualifier("userServiceImpl")
    private UserDetailsService userDetailsService;

    @Autowired
    private OperateIpService operateIpService;

    private String method;

    private String salt;

    @Autowired
    public WebSecurityConfig(@Value("${setting.pwdMethod}") String method, @Value("${setting.salt}") String salt) {
        this.method = method;
        this.salt = salt;
    }

    /**
     * 密码加密对象
     *
     * @return
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        switch (method) {
            case "md5":
                return new PasswordEncoder() {
                    @Override
                    public String encode(CharSequence rawPassword) {
                        return DigestUtil.md5Hex(rawPassword + salt);
                    }

                    @Override
                    public boolean matches(CharSequence rawPassword, String encodedPassword) {
                        return ObjectUtil.equal(DigestUtil.md5Hex(rawPassword + salt), encodedPassword);
                    }
                };
            case "sha256":
                return new PasswordEncoder() {
                    @Override
                    public String encode(CharSequence rawPassword) {
                        return DigestUtil.sha256Hex(rawPassword + salt);
                    }

                    @Override
                    public boolean matches(CharSequence rawPassword, String encodedPassword) {
                        return ObjectUtil.equal(DigestUtil.sha256Hex(rawPassword + salt), encodedPassword);
                    }
                };
            case "bcrypt":
                return new PasswordEncoder() {
                    @Override
                    public String encode(CharSequence rawPassword) {
                        return DigestUtil.bcrypt(rawPassword.toString());
                    }

                    @Override
                    public boolean matches(CharSequence rawPassword, String encodedPassword) {
                        return DigestUtil.bcryptCheck(rawPassword.toString(), encodedPassword);
                    }
                };
        }
        return NoOpPasswordEncoder.getInstance();
    }

    /**
     * 认证
     * 设置密码加密passwordEncoder
     *
     * @param auth
     * @throws Exception
     */
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
    }

    /**
     * 授权
     *
     * @param http
     * @throws Exception
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.cors().and().csrf().disable().formLogin().disable();

        // 公开接口,无需token即可请求
        http.authorizeRequests()
                .antMatchers("/").permitAll()
                .antMatchers("/auth/**").permitAll()
                .antMatchers("/v1/**").permitAll()
                .antMatchers(HttpMethod.POST, "/payment/notify/**").permitAll()
                .antMatchers(HttpMethod.GET, "/subscription/**").permitAll();
        // user认证接口
        http.authorizeRequests()
                .antMatchers("/user/**").hasAnyAuthority("user", "admin");
        // admin认证接口
        http.authorizeRequests()
                .antMatchers("/admin/**").hasAuthority("admin");
        // 其他接口全部需要认证
        http.authorizeRequests().anyRequest().authenticated();

        http.addFilter(new JWTAuthenticationFilter(authenticationManager(), (UserService) userDetailsService, operateIpService))
                .addFilter(new JWTAuthorizationFilter(authenticationManager()))
                // 不需要session
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    }
}
