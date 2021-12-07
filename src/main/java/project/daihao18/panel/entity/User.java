package project.daihao18.panel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

/**
 * @ClassName: User
 * @Description:
 * @Author: code18
 * @Date: 2020-10-07 21:07
 */
@Data
@ToString
@NoArgsConstructor
@TableName(value = "user")
public class User implements Serializable, UserDetails {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 登陆密码
     */
    @TableField("`password`")
    private String password;

    /**
     * 余额
     */
    private BigDecimal money;

    /**
     * 邀请码
     */
    private String inviteCode;

    /**
     * 邀请数
     */
    private Integer inviteCount;

    /**
     * 是否循环返利
     */
    private Boolean inviteCycleEnable;

    /**
     * 循环返利百分比
     */
    private BigDecimal inviteCycleRate;

    /**
     * 邀请人id
     */
    private Integer parentId;

    /**
     * 订阅标志
     */
    private String link;

    /**
     * 用户等级
     */
    @TableField("`class`")
    private Integer clazz;

    /**
     * 是否启用
     */
    @TableField("`enable`")
    private Boolean enable;

    /**
     * 过期时间
     */
    private Date expireIn;

    /**
     * 上次使用时间
     */
    private Long t;

    /**
     * 上传流量
     */
    private Long u;

    /**
     * 下载流量
     */
    private Long d;

    /**
     * 过去已用流量
     */
    private Long p;

    /**
     * 全部可用流量
     */
    private Long transferEnable;

    /**
     * 节点链接密码
     */
    private String passwd;

    /**
     * 节点限速 mbps
     */
    private Integer nodeSpeedlimit;

    /**
     * 链接ip数
     */
    private Integer nodeConnector;

    /**
     * 节点组
     */
    private Integer nodeGroup;

    /**
     * 注册时间
     */
    private Date regDate;

    /**
     * tg的ID
     */
    private Long tgId;

    /**
     * 签到时间
     */
    private Date checkinTime;

    /**
     * 是否是管理员
     */
    private Integer isAdmin;

    // 以下字段非数据库字段

    /**
     * 提醒流量
     */
    @TableField(exist = false)
    private Boolean remainTraffic;

    /**
     * 所有已用流量 u+d  gb
     */
    @TableField(exist = false)
    private String hasUsedGb;

    /**
     * 今日已用流量 u+d-p  gb
     */
    @TableField(exist = false)
    private String todayUsedGb;

    /**
     * 剩余流量 transferEnable - u - d  gb
     */
    @TableField(exist = false)
    private String remainingGb;

    /**
     * 所有可用流量  gb
     */
    @TableField(exist = false)
    private Integer transferEnableGb;

    /**
     * 账户资金money + commission
     */
    @TableField(exist = false)
    private BigDecimal funds;

    /**
     * 在线ip数
     */
    @TableField(exist = false)
    private Integer onlineCount;

    /**
     * 邀请链接
     */
    @TableField(exist = false)
    private String inviteLink;

    /**
     * 订阅链接
     */
    @TableField(exist = false)
    private String subsLink;

    /**
     * 邀请人数
     */
    @TableField(exist = false)
    private Integer commissionCount;

    /**
     * 验证码
     */
    @TableField(exist = false)
    private String checkCode;

    /**
     * 新验证码
     */
    @TableField(exist = false)
    private String newCheckCode;

    /**
     * 新密码
     */
    @TableField(exist = false)
    private String newPass;

    /**
     * 记住我
     */
    @TableField(exist = false)
    private Boolean rememberMe;

    @TableField(exist = false)
    private Map<String, Object> role;

    @TableField(exist = false)
    private List<String> permissions;

    private static final long serialVersionUID = 1L;

    //Spring Security
    @TableField(exist = false)
    private String username;

    @TableField(exist = false)
    private Boolean enabled;

    @TableField(exist = false)
    private Boolean accountNonExpired;

    @TableField(exist = false)
    private Boolean accountNonLocked;

    @TableField(exist = false)
    private Boolean credentialsNonExpired;

    @TableField(exist = false)
    private Collection<? extends GrantedAuthority> authorities;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getUsername() {
        return id.toString();
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    // 写一个user的构造器
    public User(Integer id, String password, Map<String, Object> role, Boolean enable) {
        this.id = id;
        this.password = password;
        this.role = role;
        this.enable = enable;
        authorities = Collections.singleton(new SimpleGrantedAuthority(role.get("id").toString()));
    }
}