package project.daihao18.panel.serviceImpl;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.alipay.api.AlipayApiException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.daihao18.panel.common.enums.MethodEnum;
import project.daihao18.panel.common.enums.ObfsEnum;
import project.daihao18.panel.common.enums.PayStatusEnum;
import project.daihao18.panel.common.enums.ProtocolEnum;
import project.daihao18.panel.common.exceptions.CustomException;
import project.daihao18.panel.common.payment.alipay.Alipay;
import project.daihao18.panel.common.response.Result;
import project.daihao18.panel.common.response.ResultCodeEnum;
import project.daihao18.panel.common.utils.*;
import project.daihao18.panel.entity.Package;
import project.daihao18.panel.entity.*;
import project.daihao18.panel.mapper.UserMapper;
import project.daihao18.panel.service.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * @ClassName: UserServiceImpl
 * @Description:
 * @Author: code18
 * @Date: 2020-10-07 21:29
 */
@Slf4j
@Service
@CacheConfig(cacheNames = "user")
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService, UserDetailsService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private RedisService redisService;

    @Autowired
    private ConfigService configService;

    @Autowired
    private AnnouncementService announcementService;

    @Autowired
    private AliveIpService aliveIpService;

    @Autowired
    private PlanService planService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PackageService packageService;

    @Autowired
    private Alipay alipay;

    @Autowired
    private TicketService ticketService;

    @Autowired
    private FundsService fundsService;

    @Autowired
    private WithdrawService withdrawService;

    @Autowired
    private TutorialService tutorialService;

    @Autowired
    private UserMonthlyTrafficService userMonthlyTrafficService;

    @Autowired
    private UserTrafficLogService userTrafficLogService;

    @Autowired
    private TelegramBot bot;

    @Value("${setting.botUsername}")
    private String botUsername;

    @Override
    public UserDetails loadUserByUsername(String id) throws UsernameNotFoundException {
        User user = this.getUserById(Integer.parseInt(id), false);
        return new User(user.getId(), user.getPassword(), user.getRole(), user.getEnable());
    }

    @Override
    public User getById(Integer id) {
        User user = (User) redisService.get("panel::user::" + id);
        if (ObjectUtil.isEmpty(user)) {
            user = super.getById(id);
            if (ObjectUtil.isNotEmpty(user)) {
                redisService.set("panel::user::" + id, user, 3600);
            }
        }
        return user;
    }

    @Override
    public User getUserById(Integer id, Boolean originGetById) {
        User user = this.getById(id);
        if (ObjectUtil.isNotEmpty(user) && originGetById) {
            return user;
        } else if (ObjectUtil.isNotEmpty(user) && !originGetById) {
            Map<String, Object> role = new HashMap<>();
            if (user.getIsAdmin() == 0) {
                role.put("id", "user");
                role.put("name", "普通用户");
            } else if (user.getIsAdmin() == 1) {
                role.put("id", "admin");
                role.put("name", "管理员");
            }
            List<Permission> permissions = permissionService.getPermissionByRole(user.getIsAdmin());
            role.put("permissions", permissions);
            user.setRole(role);

            // 流量转换
            user.setHasUsedGb(FlowSizeConverterUtil.BytesConverter(user.getU() + user.getD()));
            user.setTodayUsedGb(FlowSizeConverterUtil.BytesConverter(user.getU() + user.getD() - user.getP()));
            if (user.getTransferEnable() - user.getU() - user.getD() < 0) {
                user.setRemainingGb("0");
                // 流量小于30%发出提醒
                user.setRemainTraffic(true);
            } else {
                user.setRemainingGb(FlowSizeConverterUtil.BytesConverter(user.getTransferEnable() - user.getU() - user.getD()));
                user.setRemainTraffic((user.getTransferEnable() - user.getU() - user.getD()) < user.getTransferEnable() * 0.3);
            }
            user.setTransferEnableGb(FlowSizeConverterUtil.BytesToGb(user.getTransferEnable()));
            // 设置邀请链接
            String siteUrl = configService.getValueByName("siteUrl");
            if (siteUrl.endsWith("/")) {
                siteUrl += "auth/register/" + user.getInviteCode();
            } else {
                siteUrl += "/auth/register/" + user.getInviteCode();
            }
            user.setInviteLink(siteUrl);
            // 设置订阅链接
            String subUrl = configService.getValueByName("subUrl");
            if (subUrl.endsWith("/")) {
                subUrl += "api/subscription/" + CommonUtil.subsEncryptId(user.getId()) + user.getLink() + "/";
            } else {
                subUrl += "/api/subscription/" + CommonUtil.subsEncryptId(user.getId()) + user.getLink() + "/";
            }
            user.setSubsLink(subUrl);
            // 累计资金,余额+佣金
            user.setFunds(user.getMoney());
            // 查当前在线ip
            Integer aliveCount = aliveIpService.countAliveIpByUserId(user.getId());
            user.setAliveCount(aliveCount);
            // 查询邀请了多少人
            Integer commissionCount = this.count(new QueryWrapper<User>().eq("parent_id", user.getId()));
            user.setCommissionCount(commissionCount);
            return user;
        } else {
            return null;
        }
    }

    @Override
    public Integer getIdByEmail(String email) {
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.select("id").eq("email", email);
        User user = this.getOne(userQueryWrapper);
        if (ObjectUtil.isNotEmpty(user)) {
            return user.getId();
        } else {
            return null;
        }
    }

    @Override
    public User getUserByInviteCode(String inviteCode) {
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("invite_code", inviteCode);
        return this.getOne(userQueryWrapper);
    }

    @Override
    public Result getSiteConfig() {
        String siteName = configService.getValueByName("siteName");
        boolean regEnable = Boolean.parseBoolean(configService.getValueByName("regEnable"));
        Boolean panelMailRegisterEnable = Boolean.parseBoolean(configService.getValueByName("mailRegEnable"));
        if (regEnable) {
            boolean inviteOnly = Boolean.parseBoolean(configService.getValueByName("inviteOnly"));
            // 如果启用了邮件注册,查询邮件后缀
            List<String> emailList = null;
            if (panelMailRegisterEnable) {
                emailList = Arrays.asList(configService.getValueByName("enableEmailSuffix").split(";").clone());
            }
            return panelMailRegisterEnable ?
                    Result.ok()
                            .data("panelSiteRegisterEnable", regEnable)
                            .data("panelSiteRegisterInviteOnly", inviteOnly)
                            .data("emailList", emailList)
                            .data("panelSiteTitle", siteName)
                            .data("panelMailRegisterEnable", panelMailRegisterEnable) :
                    Result.ok()
                            .data("panelSiteRegisterEnable", regEnable)
                            .data("panelSiteRegisterInviteOnly", inviteOnly)
                            .data("panelSiteTitle", siteName)
                            .data("panelMailRegisterEnable", panelMailRegisterEnable);
        } else {
            return Result.ok()
                    .data("panelSiteRegisterEnable", regEnable)
                    .data("panelSiteTitle", siteName)
                    .data("panelMailRegisterEnable", panelMailRegisterEnable);
        }
    }

    @Override
    @Transactional
    public Result register(User regUser) {
        // 判断是否允许注册
        if (!Boolean.parseBoolean(configService.getValueByName("regEnable"))) {
            throw new CustomException(ResultCodeEnum.CLOSE_REGISTER_ERROR);
        }
        User user = null;
        Integer id = this.getIdByEmail(regUser.getEmail());
        if (ObjectUtil.isNotEmpty(id)) {
            // 存在该用户
            throw new CustomException(ResultCodeEnum.EXIST_EMAIL_ERROR);
        } else {
            user = new User();
        }
        // 判断邮箱后缀
        boolean mailRegEnable = Boolean.parseBoolean(configService.getValueByName("mailRegEnable"));
        if (mailRegEnable) {
            String[] suffixs = configService.getValueByName("enableEmailSuffix").split(";");
            boolean suffixFlag = false;
            for (int i = 0; i < suffixs.length; i++) {
                if (regUser.getEmail().endsWith(suffixs[i])) {
                    suffixFlag = true;
                }
            }
            if (!suffixFlag) {
                // 注册邮箱不在白名单里,直接返回参数错误
                return Result.setResult(ResultCodeEnum.PARAM_ERROR);
            }
        }
        User inviteUser = null;
        String inviteCode = regUser.getInviteCode();

        // 通过邀请码找到邀请人
        if (ObjectUtil.isNotEmpty(inviteCode)) {
            // url的邀请码非空,查该用户
            inviteUser = this.getUserByInviteCode(inviteCode);
        }
        // 启用邮件验证码,验证验证码
        if (Boolean.parseBoolean(configService.getValueByName("mailRegEnable"))) {
            Object redisCheckCode = redisService.get("panel::RegCheckCode::" + regUser.getEmail());
            if (ObjectUtil.isEmpty(redisCheckCode) || ObjectUtil.notEqual(regUser.getCheckCode(), redisCheckCode)) {
                throw new CustomException(ResultCodeEnum.CHECK_CODE_ERROR);
            } else {
                // 验证通过,将redis中该key删除
                redisService.del("panel::RegCheckCode::" + regUser.getEmail());
                redisService.del("panel::RegCheckCodeLimit::" + regUser.getEmail());
            }
        }
        // 设置用户
        user.setParentId(0);
        if (ObjectUtil.isNotEmpty(inviteUser) && Objects.requireNonNull(inviteUser).getClazz() > 0 && Objects.requireNonNull(inviteUser).getInviteCount() > 0) {
            // 邀请人在,邀请人为会员,邀请次数>0,设置邀请
            user.setParentId(inviteUser.getId());
            // 邀请数-1
            inviteUser.setInviteCount(inviteUser.getInviteCount() - 1);
            this.updateById(inviteUser);
        } else if ((ObjectUtil.isNotEmpty(inviteUser) && Objects.requireNonNull(inviteUser).getInviteCount() <= 0)
                || (ObjectUtil.isNotEmpty(inviteUser) && Objects.requireNonNull(inviteUser).getClazz() <= 0)
                || (ObjectUtil.isNotEmpty(inviteCode) && ObjectUtil.isEmpty(inviteUser))) {
            // 邀请人在,非VIP,邀请次数<=0,或者填了邀请码但是邀请人不在
            throw new CustomException(ResultCodeEnum.INVALID_INVITE_CODE_ERROR);
        }

        int count = this.count();
        user.setEmail(regUser.getEmail());
        user.setPassword(passwordEncoder.encode(regUser.getPassword()));
        user.setMoney(BigDecimal.ZERO);
        user.setInviteCode(RandomUtil.randomString(4));
        user.setInviteCount(Integer.parseInt(configService.getValueByName("inviteCount")));
        user.setInviteCycleEnable(false);
        // 默认给user设置config里设置的返利数值
        user.setInviteCycleRate(new BigDecimal(configService.getValueByName("inviteRate")));
        user.setLink(RandomUtil.randomString(10));
        user.setClazz(0);
        user.setEnable(true);
        user.setExpireIn(new Date());
        user.setT(new Date().getTime());
        user.setU(0L);
        user.setD(0L);
        user.setP(0L);
        user.setTransferEnable(0L);
        String[] userPortRange = configService.getValueByName("userPortRange").split(":");
        Integer port = 0;
        while (true) {
            port = RandomUtil.randomInt(new Integer(userPortRange[0]), new Integer(userPortRange[1]));
            QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
            userQueryWrapper.eq("port", port);
            int count1 = this.count(userQueryWrapper);
            if (count1 == 0) {
                break;
            }
        }
        user.setPort(port);
        user.setPasswd(RandomUtil.randomStringUpper(8));
        user.setMethod(MethodEnum.AES_256_CFB.getMethod());
        user.setProtocol(ProtocolEnum.ORIGIN.getProtocol());
        user.setProtocolParam("");
        user.setObfs(ObfsEnum.PLAIN.getObfs());
        user.setProtocolParam("");
        user.setUuid(UuidUtil.uuid3(user.getId() + "|" + user.getPasswd()));
        user.setNodeSpeedlimit(0);
        user.setNodeConnector(0);
        if (count == 0) {
            user.setIsAdmin(1);
        } else {
            user.setIsAdmin(0);
        }
        user.setForbiddenIp("127.0.0.0/8,::1/128");
        user.setForbiddenPort("");
        user.setDisconnectIp("");
        user.setIsMultiUser(0);
        user.setRegDate(new Date());
        return this.save(user) ? Result.ok() : Result.error();
    }

    @Override
    @Transactional
    public Result findPass(User findPassUser) {
        Integer id = this.getIdByEmail(findPassUser.getEmail());
        User user = null;
        if (ObjectUtil.isNotEmpty(id)) {
            user = this.getById(id);
        }
        if (ObjectUtil.isEmpty(user)) {
            throw new CustomException(ResultCodeEnum.USER_NOT_FIND_ERROR);
        }
        // 检查验证码
        Object redisCheckCode = redisService.get("panel::ForgotPassCheckCode::" + findPassUser.getEmail());
        if (ObjectUtil.isEmpty(redisCheckCode) || ObjectUtil.notEqual(findPassUser.getCheckCode(), redisCheckCode)) {
            throw new CustomException(ResultCodeEnum.CHECK_CODE_ERROR);
        } else {
            // 验证通过,将redis中该key删除
            redisService.del("panel::ForgotPassCheckCode::" + findPassUser.getEmail());
        }
        // 重新设置密码
        Objects.requireNonNull(user).setPassword(passwordEncoder.encode(findPassUser.getPassword()));
        if (this.updateById(user)) {
            redisService.del("panel::user::" + user.getId());
            return Result.ok().message("密码修改成功").messageEnglish("Update password successful");
        } else {
            return Result.setResult(ResultCodeEnum.UNKNOWN_ERROR);
        }
    }

    @Override
    public List<User> listNoMultiUser() {
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("is_multi_user", 0);
        return this.list(userQueryWrapper);
    }

    @Override
    @Transactional
    public boolean cleanExpiredUserData() {
        UpdateWrapper<User> userUpdateWrapper = new UpdateWrapper<>();
        userUpdateWrapper
                .set("u", 0)
                .set("d", 0)
                .set("p", 0)
                .set("transfer_enable", 0)
                .set("class", 0)
                .set("node_connector", 0)
                .set("node_speedlimit", 0)
                .lt("expire_in", new Date());
        return this.update(userUpdateWrapper);
    }

    @Override
    public Result refreshInfo(Integer userId) {
        return redisService.del("panel::user::" + userId) ? Result.ok().data("user", this.getUserById(userId, false)) : Result.error();
    }

    @Override
    public Result getAnnouncement() {
        Announcement announcement = announcementService.getLatestAnnouncement();
        return Result.ok().data("announcement", announcement);
    }

    @Override
    public Result resetInviteCode(Integer userId) {
        UpdateWrapper<User> userUpdateWrapper = new UpdateWrapper<>();
        userUpdateWrapper
                .set("invite_code", RandomUtil.randomString(4))
                .eq("id", userId);
        return this.update(userUpdateWrapper) && redisService.del("panel::user::" + userId) ? Result.ok() : Result.setResult(ResultCodeEnum.UNKNOWN_ERROR);
    }

    @Override
    // @Cacheable(cacheNames = "panel", key = "'planList'")
    public Result getPlan(Integer userId) {
        return Result.ok().data("plans", planService.getPlan(userId));
    }

    @Override
    public Result getCurrentPlan(Integer userId) {
        return Result.ok().data("plan", orderService.getCurrentPlan(userId));
    }

    @Override
    @Transactional
    public Result addOrder(User user, Order order) {
        synchronized (OrderLockUtil.class) {
            // 先查询该用户是否存在未支付的订单
            Order existOrder = orderService.getOne(new QueryWrapper<Order>().eq("user_id", user.getId()).eq("status", PayStatusEnum.WAIT_FOR_PAY.getStatus()));
            if (ObjectUtil.isNotEmpty(existOrder)) {
                // 存在未支付订单
                return Result.setResult(ResultCodeEnum.EXIST_ORDER_ERROR);
            }
            // 查询订购的套餐详情
            Plan plan = planService.getById(order.getPlanId());
            // 如果当前库存不足,直接返回
            if (plan.getBuyLimit() == 0) {
                return Result.setResult(ResultCodeEnum.INVENTORY_SHORTAGE_ERROR);
            }
            // 套餐已下架直接返回
            if (!plan.getEnable()) {
                return Result.setResult(ResultCodeEnum.PROHIBIT_SALES_ERROR);
            }
            // 是折扣套餐,但是在折扣 开始前或者结束后不允许新购
            Date now = new Date();
            if (plan.getIsDiscount() && now.before(plan.getDiscountStart()) && now.after(plan.getDiscountEnd())) {
                return Result.setResult(ResultCodeEnum.PROHIBIT_SALES_ERROR);
            }
            Plan copyPlan = new Plan();
            // transferEnable, packagee 转GB
            copyPlan.setTransferEnable(FlowSizeConverterUtil.BytesToGb(plan.getTransferEnable()).longValue());
            copyPlan.setPackagee(FlowSizeConverterUtil.BytesToGb(plan.getPackagee()).longValue());
            copyPlan.setClazz(plan.getClazz());
            copyPlan.setNodeSpeedlimit(plan.getNodeSpeedlimit());
            copyPlan.setNodeConnector(plan.getNodeConnector());
            copyPlan.setName(plan.getName());
            copyPlan.setNameEnglish(plan.getNameEnglish());
            copyPlan.setMonths(plan.getMonths());
            copyPlan.setPrice(plan.getPrice());
            copyPlan.setNodeGroup(plan.getNodeGroup());
            copyPlan.setEnableRenew(plan.getEnableRenew());
            // 如果是初次购买,计算百分比的流量
            // 用户 是否过期
            if (user.getExpireIn().before(now)) {
                // 已过期, 计算到本月底
                // 当天到月底的天数,包含当天
                long toMonthEndDays = DateUtil.betweenDay(now, DateUtil.endOfMonth(now), true) + 1;
                // 计算到本月底的流量
                copyPlan.setCurrentMonthTransferEnable(copyPlan.getTransferEnable() * toMonthEndDays / LocalDate.now().lengthOfMonth());
            }
            Result calcInfo = this.getChoosePlanInfo(user, plan.getMonths(), plan.getPrice(), order.getMonthCount());
            order.setPrice(new BigDecimal(calcInfo.getData().get("calcPrice").toString()));
            order.setExpire(DateUtil.parse(calcInfo.getData().get("calcExpire").toString()));
            order.setOrderId(OrderUtil.getOrderId());
            order.setStatus(PayStatusEnum.WAIT_FOR_PAY.getStatus());
            order.setUserId(user.getId());
            order.setCreateTime(new Date());
            // 设置需要存储的user的json
            User userJson = new User();
            userJson.setId(user.getId());
            userJson.setEmail(user.getEmail());
            userJson.setMoney(user.getMoney());
            userJson.setClazz(user.getClazz());
            userJson.setExpireIn(user.getExpireIn());
            userJson.setU(user.getU());
            userJson.setD(user.getD());
            userJson.setTransferEnable(user.getTransferEnable());
            userJson.setNodeSpeedlimit(user.getNodeSpeedlimit());
            userJson.setNodeConnector(user.getNodeConnector());
            order.setUserDetails(JSONUtil.toJsonStr(userJson));
            // 设置需要存储的plan的json
            order.setPlanDetails(JSONUtil.toJsonStr(copyPlan));
            // 保存成功,修改库存
            if (orderService.save(order)) {
                // 有库存限制,更新库存
                if (plan.getBuyLimit() > 0) {
                    plan.setBuyLimit(plan.getBuyLimit() - 1);
                    planService.updateById(plan);
                }
            }
            return Result.ok().data("orderId", order.getOrderId());
        }
    }

    @Override
    @Transactional
    public Result deleteOrderByOrderId(Integer userId, String orderId) {
        // 查询订单
        Order order = orderService.getOrderByOrderId(orderId);
        if (ObjectUtil.isNotEmpty(order)) {
            // 确认是该用户订单,否则非法越权访问
            if (ObjectUtil.equal(userId, order.getUserDetailsMap().get("id"))) {
                return orderService.deleteOrderByOrderId(orderId) ? Result.ok().message("删除成功").messageEnglish("Delete Successfully") : Result.error().message("删除失败").messageEnglish("Delete Failure");
            } else {
                return Result.setResult(ResultCodeEnum.UNAUTHORIZED_REQUEST_ERROR);
            }
        } else {
            return Result.error().message("无效订单ID").messageEnglish("Invalid Order ID");
        }
    }

    @Override
    @Transactional
    public Result cancelOrderByOrderId(Integer userId, String orderId) {
        // 查询订单
        Order order = orderService.getOrderByOrderId(orderId);
        if (ObjectUtil.isNotEmpty(order)) {
            // 确认是该用户订单,否则非法越权访问
            if (ObjectUtil.equal(userId, order.getUserDetailsMap().get("id"))) {
                order.setStatus(PayStatusEnum.CANCELED.getStatus());
                return orderService.updateById(order) ? Result.ok().message("订单取消成功").messageEnglish("Cancel Successfully").data("order", order) : Result.error().message("订单取消失败").messageEnglish("Cancel Failure");
            } else {
                return Result.setResult(ResultCodeEnum.UNAUTHORIZED_REQUEST_ERROR);
            }
        } else {
            return Result.error().message("无效订单ID").messageEnglish("Invalid Order ID");
        }
    }

    @Override
    public Result getOrder(HttpServletRequest request) {
        User user = JwtTokenUtil.getUser(request);
        int pageNo = Integer.parseInt(request.getParameter("pageNo"));
        int pageSize = Integer.parseInt(request.getParameter("pageSize"));
        IPage<Order> page = new Page<>(pageNo, pageSize);
        page = orderService.page(page, new QueryWrapper<Order>().eq("user_id", user.getId()).orderByDesc("create_time", "expire"));

        List<Order> lists = page.getRecords();
        lists.forEach(item -> {
            Map<String, Object> userDetailsMap = JSONUtil.toBean(item.getUserDetails(), Map.class);
            Map<String, Object> planDetailsMap = JSONUtil.toBean(item.getPlanDetails(), Map.class);
            item.setPlanDetailsMap(userDetailsMap);
            item.setPlanDetailsMap(planDetailsMap);
            item.setUserDetails(null);
            item.setPlanDetails(null);
        });
        Map<String, Object> map = new HashMap<>();
        map.put("data", lists);
        map.put("pageNo", page.getCurrent());
        map.put("totalCount", page.getTotal());
        return Result.ok().data("data", map);
    }

    @Override
    public Result getOrderByOrderId(Integer userId, String orderId) {
        Order order = orderService.getOrderByOrderId(orderId);
        if (ObjectUtil.isNotEmpty(order)) {
            // 确认是该用户订单,否则非法越权访问
            if (ObjectUtil.equal(userId, order.getUserDetailsMap().get("id"))) {
                return Result.ok().data("order", order);
            } else {
                return Result.setResult(ResultCodeEnum.UNAUTHORIZED_REQUEST_ERROR);
            }
        } else {
            return Result.error().message("无效订单ID").messageEnglish("Invalid Order ID");
        }
    }

    @Override
    public Result getChoosePlanInfo(User user, String monthString, String priceString, Integer monthCount) {
        BigDecimal calcPrice = null;
        Date calcExpire = null;
        // 套餐价格
        String[] months = monthString.split("-");
        String[] prices = priceString.split("-");
        BigDecimal price = null; // 总价格
        BigDecimal averagePrice = null; // 每个月的价格
        BigDecimal overPrice = null; // 除去第一个月的价格
        for (int i = 0; i < months.length; i++) {
            Double month = Double.parseDouble(months[i]);
            if (monthCount == month.intValue()) {
                price = new BigDecimal(prices[i]);
                averagePrice = price.divide(new BigDecimal(months[i]), 2, BigDecimal.ROUND_HALF_UP);
                overPrice = price.subtract(averagePrice);
                break;
            }
        }
        // 用户 是否过期
        Date now = new Date();
        if (user.getExpireIn().before(now)) {
            // 已过期, 计算到本月底
            // 当天到月底的天数,包含当天
            BigDecimal toMonthEndDays = new BigDecimal(String.valueOf(DateUtil.betweenDay(now, DateUtil.endOfMonth(now), true) + 1));
            ZonedDateTime zdt = null;
            if (monthCount == 1 && toMonthEndDays.intValue() < 15) {
                // 到当月底不满15天,续费到下个月
                // 计算到下月底的价格
                calcPrice = averagePrice.multiply(toMonthEndDays).divide(new BigDecimal(String.valueOf(LocalDate.now().lengthOfMonth())), 2, BigDecimal.ROUND_UP).add(price);
                // 加上剩余月数的价格
                calcPrice = calcPrice.add(overPrice);
                // 计算真实到期时间
                zdt = LocalDateTime.now().plusMonths(new BigDecimal(String.valueOf(monthCount)).intValue()).with(TemporalAdjusters.lastDayOfMonth()).withHour(23).withMinute(59).withSecond(59).withNano(0).atZone(ZoneId.systemDefault());
            } else {
                // 计算到本月底的价格
                calcPrice = averagePrice.multiply(toMonthEndDays).divide(new BigDecimal(String.valueOf(LocalDate.now().lengthOfMonth())), 2, BigDecimal.ROUND_UP);
                // 加上剩余月数的价格
                calcPrice = calcPrice.add(overPrice);
                // 计算真实到期时间
                zdt = LocalDateTime.now().plusMonths(new BigDecimal(String.valueOf(monthCount - 1)).intValue()).with(TemporalAdjusters.lastDayOfMonth()).withHour(23).withMinute(59).withSecond(59).withNano(0).atZone(ZoneId.systemDefault());
            }
            calcExpire = Date.from(zdt.toInstant());
        } else {
            // 未过期就是默认价格*续费月数以及当前时长+续费月数
            calcPrice = price;
            ZonedDateTime zdt = LocalDateTimeUtil.of(user.getExpireIn()).plusMonths(monthCount).with(TemporalAdjusters.lastDayOfMonth()).withHour(23).withMinute(59).withSecond(59).withNano(0).atZone(ZoneId.systemDefault());
            calcExpire = Date.from(zdt.toInstant());
        }
        return Result.ok().data("calcPrice", calcPrice).data("calcExpire", DateUtil.format(calcExpire, "yyyy-MM-dd HH:mm:ss"));
    }

    @Override
    @Transactional
    public Result addRenewOrder(User user, Order order) {
        synchronized (OrderLockUtil.class) {
            // 先查询该用户是否存在未支付的订单
            Order existOrder = orderService.getOne(new QueryWrapper<Order>().eq("user_id", user.getId()).eq("status", 0));
            if (ObjectUtil.isNotEmpty(existOrder)) {
                // 存在未支付订单
                return Result.setResult(ResultCodeEnum.EXIST_ORDER_ERROR);
            }
            Order beRenewOrder = orderService.getOrderByOrderId(order.getOrderId());
            if (!(Boolean) beRenewOrder.getPlanDetailsMap().get("enableRenew")) {
                // 不允许续费订单
                return Result.error().message("该订单不允许续费").messageEnglish("This order is not allow to renew");
            }

            order.setOrderId(OrderUtil.getOrderId());
            order.setUserId(user.getId());
            order.setPlanId(beRenewOrder.getPlanId());
            order.setMonthCount(order.getMonthCount());
            Map<String, Object> calcInfo = this.getChooseRenewPlanInfo(user, beRenewOrder.getOrderId(), order.getMonthCount()).getData();
            order.setPrice(new BigDecimal(calcInfo.get("calcPrice").toString()));
            order.setExpire(DateUtil.parse(calcInfo.get("calcExpire").toString()));

            order.setCreateTime(new Date());
            order.setStatus(PayStatusEnum.WAIT_FOR_PAY.getStatus());
            // 设置需要存储的user的json
            User userJson = new User();
            userJson.setId(user.getId());
            userJson.setEmail(user.getEmail());
            userJson.setMoney(user.getMoney());
            userJson.setClazz(user.getClazz());
            userJson.setExpireIn(user.getExpireIn());
            userJson.setU(user.getU());
            userJson.setD(user.getD());
            userJson.setTransferEnable(user.getTransferEnable());
            userJson.setNodeSpeedlimit(user.getNodeSpeedlimit());
            userJson.setNodeConnector(user.getNodeConnector());
            order.setUserDetails(JSONUtil.toJsonStr(userJson));
            // 设置需要存储的plan的json
            Map<String, Object> planMap = beRenewOrder.getPlanDetailsMap();
            planMap.put("currentMonthTransferEnable", 0);
            ArrayList<Integer> monthsIntegerList = (ArrayList<Integer>) planMap.get("monthsList");
            List<String> monthsStringList = monthsIntegerList.stream().map(Object::toString).collect(Collectors.toList());
            String months = String.join("-", monthsStringList);
            planMap.put("months", months);
            ArrayList<BigDecimal> priceBigDecimalList = (ArrayList<BigDecimal>) planMap.get("priceList");
            List<String> priceStringList = priceBigDecimalList.stream().map(Object::toString).collect(Collectors.toList());
            String price = String.join("-", priceStringList);
            planMap.put("price", price);

            beRenewOrder.setPlanDetailsMap(planMap);
            order.setPlanDetails(JSONUtil.toJsonStr(beRenewOrder.getPlanDetailsMap()));
            // 保存成功,修改库存
            orderService.save(order);
            return Result.ok().data("orderId", order.getOrderId());
        }
    }

    @Override
    public Result getChooseRenewPlanInfo(User user, String orderId, Integer monthCount) {
        Order order = orderService.getOrderByOrderId(orderId);
        BigDecimal calcPrice = null;
        Date calcExpire = null;
        // 套餐价格
        String[] months = order.getPlanDetailsMap().get("months").toString().split("-");
        String[] prices = order.getPlanDetailsMap().get("price").toString().split("-");
        BigDecimal price = null; // 总价格
        BigDecimal averagePrice = null; // 每个月的价格
        BigDecimal overPrice = null; // 除去第一个月的价格
        for (int i = 0; i < months.length; i++) {
            Double month = Double.parseDouble(months[i]);
            if (monthCount == month.intValue()) {
                price = new BigDecimal(prices[i]);
                averagePrice = price.divide(new BigDecimal(months[i]), 2, BigDecimal.ROUND_HALF_UP);
                overPrice = price.subtract(averagePrice);
                break;
            }
        }
        // 用户 是否过期
        Date now = new Date();
        if (user.getExpireIn().before(now)) {
            // 已过期, 计算到本月底
            // 当天到月底的天数,包含当天
            BigDecimal toMonthEndDays = new BigDecimal(String.valueOf(DateUtil.betweenDay(now, DateUtil.endOfMonth(now), true) + 1));
            // 计算到本月底的价格
            calcPrice = averagePrice.multiply(toMonthEndDays).divide(new BigDecimal(String.valueOf(LocalDate.now().lengthOfMonth())), 2, BigDecimal.ROUND_UP);
            // 加上剩余月数的价格
            calcPrice = calcPrice.add(overPrice);
            // 计算真实到期时间
            ZonedDateTime zdt = LocalDateTime.now().plusMonths(new BigDecimal(String.valueOf(monthCount - 1)).intValue()).with(TemporalAdjusters.lastDayOfMonth()).withHour(23).withMinute(59).withSecond(59).withNano(0).atZone(ZoneId.systemDefault());
            calcExpire = Date.from(zdt.toInstant());
        } else {
            // 未过期就是默认价格*续费月数以及当前时长+续费月数
            calcPrice = price;
            ZonedDateTime zdt = LocalDateTimeUtil.of(user.getExpireIn()).plusMonths(monthCount).with(TemporalAdjusters.lastDayOfMonth()).withHour(23).withMinute(59).withSecond(59).withNano(0).atZone(ZoneId.systemDefault());
            calcExpire = Date.from(zdt.toInstant());
        }
        return Result.ok().data("calcPrice", calcPrice).data("calcExpire", DateUtil.format(calcExpire, "yyyy-MM-dd HH:mm:ss"));
    }

    @Override
    @Transactional
    public Result addPackageOrder(Integer userId, Package pack) {
        synchronized (OrderLockUtil.class) {
            // 先查询该用户是否存在未支付的流量包订单
            Package existPackageOrder = packageService.getOne(new QueryWrapper<Package>().eq("user_id", userId).eq("status", PayStatusEnum.WAIT_FOR_PAY.getStatus()));
            if (ObjectUtil.isNotEmpty(existPackageOrder)) {
                // 存在未支付订单
                return Result.setResult(ResultCodeEnum.EXIST_ORDER_ERROR);
            }
            Order currentPlan = orderService.getCurrentPlan(userId);
            pack.setUserId(userId);
            pack.setOrderId(currentPlan.getOrderId());
            pack.setTransferEnable(FlowSizeConverterUtil.GbToBytes(pack.getPrice().intValue() * Integer.parseInt(currentPlan.getPlanDetailsMap().get("packagee").toString())));
            Date now = new Date();
            pack.setExpire(Date.from(LocalDateTime.now().with(TemporalAdjusters.lastDayOfMonth()).withHour(23).withMinute(59).withSecond(59).withNano(0).atZone(ZoneId.systemDefault()).toInstant()));
            pack.setCreateTime(now);
            packageService.save(pack);
            return Result.ok().data("package", pack);
        }
    }

    @Override
    @Transactional
    public Result deletePackageOrderById(Integer userId, Integer id) {
        // 查询订单
        Package pack = packageService.getById(id);
        Integer uid = null;
        if (ObjectUtil.isNotEmpty(pack)) {
            uid = pack.getUserId();
            // 确认是该用户订单,否则非法越权访问
            if (ObjectUtil.equal(userId, uid)) {
                return packageService.removeById(pack.getId()) ? Result.ok().message("删除成功").messageEnglish("Delete Successfully") : Result.error().message("删除失败").messageEnglish("Delete Failure");
            } else {
                return Result.setResult(ResultCodeEnum.UNAUTHORIZED_REQUEST_ERROR);
            }
        } else {
            return Result.error().message("无效订单ID").messageEnglish("Invalid Order ID");
        }
    }

    @Override
    @Transactional
    public Result cancelPackageOrderById(Integer userId, Integer id) {
        // 查询订单
        Package pack = packageService.getById(id);
        Integer uid = null;
        if (ObjectUtil.isNotEmpty(pack)) {
            uid = pack.getUserId();
            // 确认是该用户订单,否则非法越权访问
            if (ObjectUtil.equal(userId, uid)) {
                pack.setStatus(PayStatusEnum.CANCELED.getStatus());
                return packageService.updateById(pack) ? Result.ok().message("订单取消成功").messageEnglish("Cancel Successfully").data("package", pack) : Result.error().message("订单取消失败").messageEnglish("Cancel Failure");
            } else {
                return Result.setResult(ResultCodeEnum.UNAUTHORIZED_REQUEST_ERROR);
            }
        } else {
            return Result.error().message("无效订单ID").messageEnglish("Invalid Order ID");
        }
    }

    @Override
    public Result getPackageOrder(HttpServletRequest request) {
        User user = JwtTokenUtil.getUser(request);
        int pageNo = Integer.parseInt(request.getParameter("pageNo"));
        int pageSize = Integer.parseInt(request.getParameter("pageSize"));
        IPage<Package> page = new Page<>(pageNo, pageSize);
        page = packageService.page(page, new QueryWrapper<Package>().eq("user_id", user.getId()).orderByDesc("create_time", "expire"));

        List<Package> lists = page.getRecords();
        lists.forEach(item -> {
            item.setTransferEnableGb(FlowSizeConverterUtil.BytesToGb(item.getTransferEnable()));
            item.setTransferEnable(null);
        });
        Map<String, Object> map = new HashMap<>();
        map.put("data", lists);
        map.put("pageNo", page.getCurrent());
        map.put("totalCount", page.getTotal());
        return Result.ok().data("data", map);
    }

    @Override
    public Result getPackageOrderById(Integer userId, Integer id) {
        Package pack = packageService.getById(id);
        pack.setTransferEnableGb(FlowSizeConverterUtil.BytesToGb(pack.getTransferEnable()));
        pack.setTransferEnable(null);
        pack.setTradeNo(null);
        Integer uid = null;
        if (ObjectUtil.isNotEmpty(pack)) {
            uid = pack.getUserId();
            // 确认是该用户订单,否则非法越权访问
            if (ObjectUtil.equal(userId, uid)) {
                return Result.ok().data("order", pack);
            } else {
                return Result.setResult(ResultCodeEnum.UNAUTHORIZED_REQUEST_ERROR);
            }
        } else {
            return Result.error().message("无效订单ID").messageEnglish("Invalid Order ID");
        }
    }

    /**
     * 获取支付配置
     *
     * @return
     */
    @Override
    public Result getPaymentConfig() {
        // 默认支持余额支付
        Map<String, Object> map = new HashMap<>();
        map.put("money", true);
        String alipay = configService.getValueByName("alipay");
        String wxpay = configService.getValueByName("wxpay");
        map.put("alipay", !"none".equals(alipay));
        map.put("wxpay", !"none".equals(wxpay));
        return Result.ok().data("config", map);
    }

    /**
     * 支付订单
     *
     * @param request
     * @param commonOrder
     * @return
     * @throws AlipayApiException
     */
    @Override
    @Transactional
    public Result payOrder(HttpServletRequest request, CommonOrder commonOrder) throws AlipayApiException {
        synchronized (OrderLockUtil.class) {
            if (BigDecimal.ZERO.compareTo(commonOrder.getMixedMoneyAmount()) == 0) {
                // 如果余额为0,那就是单一支付
                commonOrder.setIsMixedPay(false);
            }
            User user = this.getById(JwtTokenUtil.getId(request));
            // 1.根据payType获取订单,再判断是否非法请求
            Order order = null;
            Package pack = null;
            if ("plan".equals(commonOrder.getType())) {
                // 用户id不一样,非法请求
                order = orderService.getOrderByOrderId(commonOrder.getId());
                if (ObjectUtil.notEqual(user.getId(), order.getUserId())) {
                    return Result.setResult(ResultCodeEnum.UNAUTHORIZED_REQUEST_ERROR);
                }
                if (PayStatusEnum.SUCCESS.getStatus().equals(order.getStatus())) {
                    return Result.setResult(ResultCodeEnum.ORDER_PAID_ERROR);
                }
            } else if ("package".equals(commonOrder.getType())) {
                // 用户id不一样,非法请求
                pack = packageService.getById(commonOrder.getId());
                if (ObjectUtil.notEqual(user.getId(), pack.getUserId())) {
                    return Result.setResult(ResultCodeEnum.UNAUTHORIZED_REQUEST_ERROR);
                }
                if (PayStatusEnum.SUCCESS.getStatus().equals(pack.getStatus())) {
                    return Result.setResult(ResultCodeEnum.ORDER_PAID_ERROR);
                }
            } else {
                return Result.setResult(ResultCodeEnum.PARAM_ERROR);
            }
            // 2.根据是否是混合支付来设定订单
            // 获取支付配置
            String alipay = configService.getValueByName("alipay");
            String wxpay = configService.getValueByName("wxpay");
            if (commonOrder.getIsMixedPay()) {
                // 调用外部支付
                // 2.1 校验一下混合支付金额是否正确
                if ("plan".equals(commonOrder.getType())) {
                    if (order.getPrice().compareTo(commonOrder.getMixedMoneyAmount().add(commonOrder.getMixedPayAmount())) != 0) {
                        return Result.setResult(ResultCodeEnum.PARAM_ERROR);
                    }
                } else {
                    if (pack.getPrice().compareTo(commonOrder.getMixedMoneyAmount().add(commonOrder.getMixedPayAmount())) != 0) {
                        return Result.setResult(ResultCodeEnum.PARAM_ERROR);
                    }
                }
                switch (commonOrder.getPayType()) {
                    case "alipay":
                        return payOrderByAlipay(alipay, commonOrder, true);
                    case "wxpay":
                        // return "plan".equals(commonOrder.getType()) ? singlePayOrderByWxpay(user, order) : singlePayOrderByWxpay(user, pack);
                    default:
                        return Result.setResult(ResultCodeEnum.PARAM_ERROR);
                }

            } else {
                // 根据单一支付来执行逻辑
                // 2.1 校验一下单一支付金额是否正确
                if ("plan".equals(commonOrder.getType())) {
                    if ("money".equals(commonOrder.getPayType())) {
                        if (!(order.getPrice().compareTo(commonOrder.getMixedMoneyAmount()) == 0 && commonOrder.getMixedPayAmount().compareTo(BigDecimal.ZERO) == 0)) {
                            return Result.setResult(ResultCodeEnum.PARAM_ERROR);
                        }
                    } else {
                        if (!(order.getPrice().compareTo(commonOrder.getMixedPayAmount()) == 0 && commonOrder.getMixedMoneyAmount().compareTo(BigDecimal.ZERO) == 0)) {
                            return Result.setResult(ResultCodeEnum.PARAM_ERROR);
                        }
                    }
                } else {
                    if ("money".equals(commonOrder.getPayType())) {
                        if (!(pack.getPrice().compareTo(commonOrder.getMixedMoneyAmount()) == 0 && commonOrder.getMixedPayAmount().compareTo(BigDecimal.ZERO) == 0)) {
                            return Result.setResult(ResultCodeEnum.PARAM_ERROR);
                        }
                    } else {
                        if (!(pack.getPrice().compareTo(commonOrder.getMixedPayAmount()) == 0 && commonOrder.getMixedMoneyAmount().compareTo(BigDecimal.ZERO) == 0)) {
                            return Result.setResult(ResultCodeEnum.PARAM_ERROR);
                        }
                    }
                }
                switch (commonOrder.getPayType()) {
                    case "money":
                        return "plan".equals(commonOrder.getType()) ? singlePayOrderByMoney(user, order) : singlePayOrderByMoney(user, pack);
                    case "alipay":
                        return payOrderByAlipay(alipay, commonOrder, false);
                    case "wxpay":
                        // return "plan".equals(commonOrder.getType()) ? singlePayOrderByWxpay(user, order) : singlePayOrderByWxpay(user, pack);
                    default:
                        return Result.setResult(ResultCodeEnum.PARAM_ERROR);
                }

            }
        }
    }

    /**
     * 套餐支付成功后更新用户
     *
     * @param order
     * @param isNewBuy
     * @return
     */
    @Override
    @Transactional
    public boolean updateUserAfterBuyOrder(Order order, boolean isNewBuy) {
        // 不管是新购还是预先购买套餐,先设置user公共部分
        UpdateWrapper<User> userUpdateWrapper = new UpdateWrapper<>();
        userUpdateWrapper
                .setSql("money=money-" + order.getMixedMoneyAmount())
                .set("expire_in", order.getExpire());
        if (isNewBuy) {
            // 如果是新购套餐,给用户设置套餐所有内容,不是新购套餐,等到期的月初重置
            userUpdateWrapper
                    .set("u", 0L)
                    .set("d", 0L)
                    .set("p", 0L)
                    .set("class", order.getPlanDetailsMap().get("clazz"))
                    .set("node_connector", order.getPlanDetailsMap().get("nodeConnector"))
                    .set("node_speedlimit", order.getPlanDetailsMap().get("nodeSpeedlimit"))
                    .set("node_group", order.getPlanDetailsMap().get("nodeGroup"));
            if (ObjectUtil.isNotEmpty(order.getPlanDetailsMap().get("currentMonthTransferEnable"))) {
                userUpdateWrapper.set("transfer_enable", FlowSizeConverterUtil.GbToBytes(Integer.parseInt(order.getPlanDetailsMap().get("currentMonthTransferEnable").toString())));
            } else {
                userUpdateWrapper.set("transfer_enable", FlowSizeConverterUtil.GbToBytes(Integer.parseInt(order.getPlanDetailsMap().get("transferEnable").toString())));
            }
        }
        userUpdateWrapper.eq("id", order.getUserId());
        if (this.update(userUpdateWrapper)) {
            redisService.del("panel::user::" + order.getUserId());
            return true;
        } else {
            return false;
        }
    }

    /**
     * 流量包支付成功后更新用户
     *
     * @param currentOrder
     * @param pack
     * @return
     */
    @Override
    @Transactional
    public boolean updateUserAfterBuyPackageOrder(Order currentOrder, Package pack) {
        // 计算需要累加的流量
        Integer packagee = (Integer) currentOrder.getPlanDetailsMap().get("packagee");
        // 更新用户
        UpdateWrapper<User> userUpdateWrapper = new UpdateWrapper<>();
        userUpdateWrapper
                .setSql("money=money-" + pack.getMixedMoneyAmount())
                .setSql("transfer_enable=transfer_enable+" + pack.getPrice().longValue() * FlowSizeConverterUtil.GbToBytes(packagee))
                .eq("id", pack.getUserId());
        if (this.update(userUpdateWrapper)) {
            redisService.del("panel::user::" + pack.getUserId());
            return true;
        } else {
            return false;
        }
    }

    /**
     * 余额单一支付套餐订单
     *
     * @param user
     * @param order
     * @return
     */
    private Result singlePayOrderByMoney(User user, Order order) {
        Lock lock = new ReentrantLock();
        lock.lock();
        // 余额不足直接返回
        if (user.getMoney().compareTo(order.getPrice()) < 0) {
            return Result.setResult(ResultCodeEnum.INSUFFICIENT_BALANCE_ERROR);
        }
        // 查询用户当前套餐(更新order之前查询)
        Order currentPlan = orderService.getCurrentPlan(user.getId());
        // 更新order成功后更新用户
        Date now = new Date();
        if (orderService.updateFinishedOrder(false, order.getPrice(), BigDecimal.ZERO, "余额", null, null, null, now, PayStatusEnum.SUCCESS.getStatus(), order.getId())) {
            this.updateUserAfterBuyOrder(orderService.getOrderByOrderId(order.getOrderId()), ObjectUtil.isEmpty(currentPlan));
            // 新增资金明细表
            Funds funds = new Funds();
            funds.setUserId(user.getId());
            funds.setPrice(BigDecimal.ZERO.subtract(order.getPrice()));
            funds.setTime(now);
            funds.setRelatedOrderId(order.getOrderId());
            funds.setContent(order.getPlanDetailsMap().get("name").toString());
            funds.setContentEnglish(order.getPlanDetailsMap().get("nameEnglish").toString());
            fundsService.save(funds);
            lock.unlock();
            return Result.ok().data("type", "money");
        } else {
            lock.unlock();
            return Result.setResult(ResultCodeEnum.UNKNOWN_ERROR);
        }
    }

    /**
     * 余额单一支付流量包订单
     *
     * @param user
     * @param pack
     * @return
     */
    private Result singlePayOrderByMoney(User user, Package pack) {
        Lock lock = new ReentrantLock();
        lock.lock();
        // 余额不足直接返回
        if (user.getMoney().compareTo(pack.getPrice()) < 0) {
            return Result.setResult(ResultCodeEnum.INSUFFICIENT_BALANCE_ERROR);
        }
        // 查询用户当前套餐
        Order currentOrder = orderService.getCurrentPlan(pack.getUserId());
        if (ObjectUtil.isEmpty(currentOrder)) {
            return Result.error().message("当前无激活的套餐").messageEnglish("There is no active plan");
        }
        Date now = new Date();
        // 更新pack成功后更新用户
        if (packageService.updateFinishedPackageOrder(false, pack.getPrice(), BigDecimal.ZERO, "余额", null, now, PayStatusEnum.SUCCESS.getStatus(), pack.getId())) {
            this.updateUserAfterBuyPackageOrder(currentOrder, packageService.getById(pack.getId()));
            // 新增资金明细表
            Funds funds = new Funds();
            funds.setUserId(user.getId());
            funds.setPrice(BigDecimal.ZERO.subtract(currentOrder.getPrice()));
            funds.setTime(now);
            funds.setRelatedOrderId(pack.getOrderId());
            funds.setContent("流量包-" + currentOrder.getPlanDetailsMap().get("transferEnable").toString() + "GB");
            funds.setContentEnglish("Package-" + currentOrder.getPlanDetailsMap().get("transferEnable").toString() + "GB");
            fundsService.save(funds);
            lock.unlock();
            return Result.ok().data("type", "money");
        } else {
            lock.unlock();
            return Result.setResult(ResultCodeEnum.UNKNOWN_ERROR);
        }
    }

    /**
     * 用支付宝支付订单
     *
     * @param alipay
     * @param order
     * @param isMixedPay
     * @return
     * @throws AlipayApiException
     */
    private Result payOrderByAlipay(String alipay, CommonOrder order, Boolean isMixedPay) throws AlipayApiException {
        Lock lock = new ReentrantLock();
        lock.lock();
        // 根据配置去调用接口
        Map<String, Object> result = null;
        switch (alipay) {
            case "alipay":
                result = this.alipay.create(order, isMixedPay);
                break;
        }
        lock.unlock();
        return ObjectUtil.isNotEmpty(result) ? Result.ok().data(result) : Result.setResult(ResultCodeEnum.PAYMENT_CREATE_ORDER_ERROR);
    }


    @Override
    public Result getFunds(HttpServletRequest request) {
        Integer userId = JwtTokenUtil.getId(request);
        return Result.ok().data("funds", fundsService.getFundsByPage(userId, Integer.parseInt(request.getParameter("pageNo")), Integer.parseInt(request.getParameter("pageSize"))));
    }

    @Override
    @Transactional
    public Result submitWithdraw(User user, Withdraw withdraw) {
        // 判断金额是否足够提现
        if (user.getMoney().compareTo(withdraw.getAmount()) < 0) {
            return Result.error().message("余额不足").messageEnglish("You don't have enough money to withdraw");
        }
        // 判断是否达到最低提现额度
        BigDecimal minWithdraw = new BigDecimal(configService.getValueByName("minWithdraw"));
        if (withdraw.getAmount().compareTo(minWithdraw) < 0) {
            return Result.error().message("未达到最低提现金额:" + minWithdraw).messageEnglish("The minimum withdrawal amount " + minWithdraw + " is not reached");
        }
        // 先查是否有未完成的提现
        Withdraw existWithdraw = withdrawService.getOne(new QueryWrapper<Withdraw>().eq("user_id", user.getId()).eq("status", 0));
        if (ObjectUtil.isNotEmpty(existWithdraw)) {
            return Result.error().message("请等待上一笔提现单完成后发起下一笔订单").messageEnglish("Waiting for another withdraw to be completed");
        }
        withdraw.setUserId(user.getId());
        withdraw.setStatus(0);
        // 重新计算可提现金额
        withdraw.setRealAmount(withdraw.getAmount().multiply(BigDecimal.ONE.subtract(new BigDecimal(configService.getValueByName("withdrawRate")))));
        if (withdrawService.save(withdraw)) {
            // 更新用户余额
            UpdateWrapper<User> userUpdateWrapper = new UpdateWrapper<>();
            userUpdateWrapper
                    .setSql("money=money-" + withdraw.getAmount())
                    .eq("id", withdraw.getUserId());
            if (this.update(userUpdateWrapper)) {
                // 删除该用户缓存
                redisService.del("panel::user::" + user.getId());
                // 给管理员发信
                QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
                userQueryWrapper
                        .select("email")
                        .eq("is_admin", 1);
                List<User> admins = this.list(userQueryWrapper);
                for (User admin : admins) {
                    EmailUtil.sendEmail("有新的提现需要处理~", "有新的提现单待处理~", false, admin.getEmail());
                }
                return Result.ok().message("已发起提现申请,请等待审核").messageEnglish("Please wait for review");
            } else {
                return Result.setResult(ResultCodeEnum.UNKNOWN_ERROR);
            }
        }
        return Result.setResult(ResultCodeEnum.UNKNOWN_ERROR);
    }

    @Override
    @Cacheable(cacheNames = "panel::user", key = "'mu'", unless = "#result == null")
    public User getMuUserByNodeServer(String port) {
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper
                .eq("port", Integer.parseInt(port))
                .ne("is_multi_user", 0);
        return this.getOne(userQueryWrapper);
    }

    @Override
    @Transactional
    public boolean handleCommission(Integer userId, BigDecimal commission) {
        UpdateWrapper<User> userUpdateWrapper = new UpdateWrapper<>();
        userUpdateWrapper
                .setSql("money=money+" + commission)
                .eq("id", userId);
        if (this.update(userUpdateWrapper)) {
            redisService.del("panel::user::" + userId);
            return true;
        }
        return false;
    }

    @Override
    public Result getTutorialsByType(String type) {
        List<Tutorial> tutorials = tutorialService.getTutorialsByType(type);
        return Result.ok().data("tutorials", tutorials);
    }

    @Override
    public Result getTicket(HttpServletRequest request) {
        Integer userId = JwtTokenUtil.getId(request);
        return Result.ok().data("tickets", ticketService.getTicketByPage(userId, Integer.parseInt(request.getParameter("pageNo")), Integer.parseInt(request.getParameter("pageSize"))));
    }

    @Override
    @Transactional
    public Result saveTicket(Integer userId, Ticket ticket, String type) {
        ticket.setUserId(userId);
        ticket.setTime(new Date());
        if ("reply".equals(type)) {
            // 将该ticket的父ticket状态置0
            UpdateWrapper<Ticket> ticketUpdateWrapper = new UpdateWrapper<>();
            ticketUpdateWrapper
                    .set("status", 0)
                    .eq("id", ticket.getParentId());
            ticketService.update(ticketUpdateWrapper);
        }
        if (ticketService.save(ticket)) {
            List<User> admins = this.getAdmins();
            for (User admin : admins) {
                EmailUtil.sendEmail("新的工单提醒~", "有新的工单待处理~", false, admin.getEmail());
                if (ObjectUtil.isNotEmpty(admin.getTgId())) {
                    bot.execute(new SendMessage(admin.getTgId(), "有新的工单待处理~"));
                }
            }
            return Result.ok().message("提交成功").messageEnglish("Submit successfully");
        } else {
            return Result.setResult(ResultCodeEnum.UNKNOWN_ERROR);
        }
    }

    @Override
    @Transactional
    public Result deleteTicketById(Integer userId, Integer id) {
        Ticket ticket = ticketService.getById(id);
        if (ObjectUtil.isNotEmpty(ticket) && ticket.getUserId().equals(userId)) {
            return ticketService.removeById(id) ? Result.ok().message("删除成功").messageEnglish("Delete successfully") : Result.setResult(ResultCodeEnum.UNKNOWN_ERROR);
        }
        return Result.setResult(ResultCodeEnum.UNKNOWN_ERROR);
    }

    @Override
    public Result getTicketById(Integer userId, Integer id) {
        Ticket ticket = ticketService.getById(id);
        if (!ticket.getUserId().equals(userId)) {
            return Result.setResult(ResultCodeEnum.UNAUTHORIZED_REQUEST_ERROR);
        }
        List<Ticket> list = ticketService.getTicketById(id);
        List<Ticket> tickets = new ArrayList<>();
        tickets.add(ticket);
        tickets.addAll(list);
        return Result.ok().data("tickets", tickets);
    }

    @Override
    @Transactional
    public Result closeTicket(Integer userId, Integer id) {
        Ticket ticket = ticketService.getById(id);
        if (!ticket.getUserId().equals(userId)) {
            return Result.setResult(ResultCodeEnum.UNAUTHORIZED_REQUEST_ERROR);
        }
        if (ticket.getStatus() == 2) {
            return Result.error().message("该工单已结单").messageEnglish("The ticket has closed");
        }
        UpdateWrapper<Ticket> ticketUpdateWrapper = new UpdateWrapper<>();
        ticketUpdateWrapper
                .set("status", 2)
                .eq("id", id)
                .in("status", 0, 1);
        return ticketService.update(ticketUpdateWrapper) ? Result.ok().message("已关闭工单").messageEnglish("The ticket closed successfully") : Result.setResult(ResultCodeEnum.UNKNOWN_ERROR);
    }

    @Override
    @Transactional
    public Result changePass(User user, User requestUser) {
        if (passwordEncoder.matches(requestUser.getPassword(), user.getPassword())) {
            // 旧密码相同,更新新密码
            UpdateWrapper<User> userUpdateWrapper = new UpdateWrapper<>();
            userUpdateWrapper
                    .set("password", passwordEncoder.encode(requestUser.getNewPass()))
                    .eq("id", user.getId());
            return redisService.del("panel::user::" + user.getId()) && this.update(userUpdateWrapper) ? Result.ok().message("密码修改成功,请重新登录").messageEnglish("password has been changed, please login again.") : Result.setResult(ResultCodeEnum.UNKNOWN_ERROR);
        } else {
            return Result.error().message("旧密码错误").messageEnglish("old password input error");
        }
    }

    @Override
    @Transactional
    public Result changeEmail(String oldCheckCode, String newCheckCode, String oldEmail, String newEmail, Integer userId) {
        // 判断验证码
        String oldEmailCheckCode = redisService.get("panel::ResetEmailCheckCode::" + oldEmail).toString();
        String newEmailCheckCode = redisService.get("panel::ResetEmailCheckCode::" + newEmail).toString();
        if (ObjectUtil.notEqual(oldCheckCode, oldEmailCheckCode) || ObjectUtil.notEqual(newCheckCode, newEmailCheckCode)) {
            return Result.error().message("验证码错误").messageEnglish("Invalid checkCode");
        } else {
            UpdateWrapper<User> userUpdateWrapper = new UpdateWrapper<>();
            userUpdateWrapper.set("email", newEmail).eq("id", userId);
            if (this.update(userUpdateWrapper)) {
                redisService.del("panel::user::" + userId);
                redisService.del("panel::ResetEmailCheckCode::" + oldEmail);
                redisService.del("panel::ResetEmailCheckCode::" + newEmail);
                return Result.ok().message("修改成功").messageEnglish("Update Successfully");
            } else {
                return Result.setResult(ResultCodeEnum.UNKNOWN_ERROR);
            }
        }
    }

    @Override
    @Cacheable(cacheNames = "panel::user::users", key = "#pageNo+'-'+#pageSize", unless = "#p3")
    public Result getUserByPageAndQueryParam(HttpServletRequest request, Integer pageNo, Integer pageSize, boolean cacheFlag) {
        // 设置查询参数
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        // 存在参数的时候才查询
        if (cacheFlag) {
            // 获取查询参数
            Integer id = null;
            if (ObjectUtil.isNotEmpty(request.getParameter("id"))) {
                id = Integer.parseInt(request.getParameter("id"));
            }
            String email = null;
            if (ObjectUtil.isNotEmpty(request.getParameter("email"))) {
                email = request.getParameter("email");
            }
            Integer clazz = null;
            if (ObjectUtil.isNotEmpty(request.getParameter("clazz"))) {
                clazz = Integer.parseInt(request.getParameter("clazz"));
            }
            Date expireIn = null;
            if (ObjectUtil.isNotEmpty(request.getParameter("expire"))) {
                LocalDateTime dateTime = DateUtil.parseDate(request.getParameter("expire").replace("\"", "")).toTimestamp().toLocalDateTime().plusHours(23).plusMinutes(59).plusSeconds(59);
                expireIn = Date.from(dateTime.atZone(ZoneId.of("Asia/Shanghai")).toInstant());
            }
            Integer role = null;
            if (ObjectUtil.isNotEmpty(request.getParameter("role"))) {
                role = Integer.parseInt(request.getParameter("role"));
            }
            Integer enable = null;
            if (ObjectUtil.isNotEmpty(request.getParameter("enable"))) {
                enable = Integer.parseInt(request.getParameter("enable"));
            }

            if (ObjectUtil.isNotEmpty(id)) {
                userQueryWrapper.eq("id", id);
            }
            if (ObjectUtil.isNotEmpty(email)) {
                userQueryWrapper.like("email", email);
            }
            if (ObjectUtil.isNotEmpty(clazz)) {
                userQueryWrapper.eq("class", clazz);
            }
            if (ObjectUtil.isNotEmpty(expireIn)) {
                userQueryWrapper.lt("expire_in", expireIn);
            }
            if (ObjectUtil.isNotEmpty(role)) {
                userQueryWrapper.eq("is_admin", role);
            }
            if (ObjectUtil.isNotEmpty(enable)) {
                userQueryWrapper.eq("enable", enable);
            }
        }
        IPage<User> page = new Page<>(pageNo, pageSize);
        page = this.page(page, userQueryWrapper);
        Map<String, Object> map = new HashMap<>();
        map.put("data", page.getRecords());
        map.put("pageNo", page.getCurrent());
        map.put("totalCount", page.getTotal());
        return Result.ok().data(map);
    }

    @Override
    public Integer getRegisterCountByDateToNow(DateTime beginDate) {
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.gt("reg_date", beginDate);
        return this.count(userQueryWrapper);
    }

    @Override
    public List<User> getExpiredUser() {
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper
                .gt("class", 0)
                .eq("enable", 1)
                .ne("is_admin", 1)
                .lt("expire_in", DateUtil.offsetDay(new Date(), 3));
        return this.list(userQueryWrapper);
    }

    @Override
    public Object getMonthPaidUserCount() {
        DateTime beginOfMonth = DateUtil.beginOfMonth(new Date());
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper
                .gt("class", 0)
                .gt("reg_date", DateUtil.beginOfDay(beginOfMonth));
        return this.count(userQueryWrapper);
    }

    @Override
    public Result getTrafficDetails(Integer userId) {
        List<Map<String, Object>> monthList = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        // 本月的第一天00:00:00
        LocalDateTime monthBeginTimeStart = now.with(TemporalAdjusters.firstDayOfMonth()).withHour(0).withMinute(0).withSecond(0);
        // 本月的最后一天23:59:59
        LocalDateTime monthEndTimeEnd = now.with(TemporalAdjusters.lastDayOfMonth()).withHour(23).withMinute(59).withSecond(59);

        // 查当月每天的流量
        QueryWrapper<UserMonthlyTraffic> userMonthlyTrafficQueryWrapper = new QueryWrapper<>();
        userMonthlyTrafficQueryWrapper
                .gt("date", monthBeginTimeStart)
                .le("date", monthEndTimeEnd)
                .eq("user_id", userId);
        List<UserMonthlyTraffic> traffics = userMonthlyTrafficService.list(userMonthlyTrafficQueryWrapper);
        if (ObjectUtil.isNotEmpty(traffics)) {
            for (UserMonthlyTraffic traffic : traffics) {
                Map<String, Object> map = new HashMap<>();
                Map<String, Object> map2 = new HashMap<>();
                map.put("day", DateUtil.dayOfMonth(traffic.getDate()) + "号");
                map.put("traffic", "上传");
                map.put("value", FlowSizeConverterUtil.BytesToMb(traffic.getU()));
                map2.put("day",DateUtil.dayOfMonth(traffic.getDate()) + "号");
                map2.put("traffic", "下载");
                map2.put("value",FlowSizeConverterUtil.BytesToMb(traffic.getD()));
                monthList.add(map);
                monthList.add(map2);
            }
        }
        // 统计今日流量
        List<Map<String, Object>> todayTraffic = userTrafficLogService.getTodayTraffic();
        if (todayTraffic.size() > 0) {
            Map<String, Object> map = new HashMap<>();
            Map<String, Object> map2 = new HashMap<>();
            map.put("day", DateUtil.dayOfMonth(new Date()) + "号");
            map.put("traffic", "上传");
            map.put("value", FlowSizeConverterUtil.BytesToMb(Long.parseLong(FlowSizeConverterUtil.convertNumber(String.valueOf(todayTraffic.get(0).get("u"))))));
            map2.put("day",DateUtil.dayOfMonth(new Date()) + "号");
            map2.put("traffic", "下载");
            map2.put("value",FlowSizeConverterUtil.BytesToMb(Long.parseLong(FlowSizeConverterUtil.convertNumber(String.valueOf(todayTraffic.get(0).get("d"))))));
            monthList.add(map);
            monthList.add(map2);
        }
        return Result.ok().data("trafficDetails", monthList);
    }

    @Override
    public User getUserByUUID(String uuid) {
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper
                .eq("uuid", uuid);
        return this.getOne(userQueryWrapper);
    }

    @Override
    public User getUserByTgId(Integer tgId) {
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper
                .eq("tg_id", tgId);
        return this.getOne(userQueryWrapper);
    }

    @Override
    public List<User> getAdmins() {
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper
                .eq("is_admin", 1);
        return this.list(userQueryWrapper);
    }

    @Override
    public Result getTGConfig() {
        return Result.ok().data("botUsername", botUsername);
    }

    @Override
    public Result unBindTG(Integer id) {
        UpdateWrapper<User> userUpdateWrapper = new UpdateWrapper<>();
        userUpdateWrapper
                .set("tg_id", null)
                .eq("id", id);
        return this.update(userUpdateWrapper) && redisService.del("panel::user::" + id) ? Result.ok().message("解绑成功").messageEnglish("Unbinding Successfully") : Result.error().message("解绑失败").messageEnglish("Failed");
    }

    @Override
    public List<User> getTGUsers() {
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper
                .select("tg_id")
                .isNotNull("tg_id");
        return this.list(userQueryWrapper);
    }
}