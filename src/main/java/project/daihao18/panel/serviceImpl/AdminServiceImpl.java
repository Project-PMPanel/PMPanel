package project.daihao18.panel.serviceImpl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.alipay.api.AlipayApiException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.extern.slf4j.Slf4j;
import net.ipip.ipdb.City;
import net.ipip.ipdb.CityInfo;
import net.ipip.ipdb.IPFormatException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.daihao18.panel.common.enums.PayStatusEnum;
import project.daihao18.panel.common.payment.alipay.Alipay;
import project.daihao18.panel.common.response.Result;
import project.daihao18.panel.common.response.ResultCodeEnum;
import project.daihao18.panel.common.schedule.CronTaskRegistrar;
import project.daihao18.panel.common.schedule.SchedulingRunnable;
import project.daihao18.panel.common.utils.EmailUtil;
import project.daihao18.panel.common.utils.FlowSizeConverterUtil;
import project.daihao18.panel.common.utils.UuidUtil;
import project.daihao18.panel.entity.*;
import project.daihao18.panel.service.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * @ClassName: AdminServiceImpl
 * @Description:
 * @Author: code18
 * @Date: 2020-11-28 15:32
 */
@Service
@Slf4j
public class AdminServiceImpl implements AdminService {

    @Autowired
    private ConfigService configService;

    @Autowired
    private Alipay alipay;

    @Autowired
    private SsService ssService;

    @Autowired
    private V2rayService v2rayService;

    @Autowired
    private TrojanService trojanService;

    @Autowired
    private DetectListService detectListService;

    @Autowired
    private NodeWithDetectService nodeWithDetectService;

    @Autowired
    private RedisService redisService;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PlanService planService;

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TutorialService tutorialService;

    @Autowired
    private AnnouncementService announcementService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private FundsService fundsService;

    @Autowired
    private WithdrawService withdrawService;

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private CronTaskRegistrar cronTaskRegistrar;

    @Autowired
    private TelegramBot bot;

    @Override
    public Result getDashboardInfo() {
        Map<String, Object> map = new HashMap<>();
        // 获取待办工单数量
        map.put("ticketCount", ticketService.count(new QueryWrapper<Ticket>().eq("status", 0).isNull("parent_id")));
        // 获取用户数
        map.put("userCount", userService.count());
        map.put("monthRegisterCount", userService.getRegisterCountByDateToNow(DateUtil.beginOfMonth(new Date())));
        map.put("todayRegisterCount", userService.getRegisterCountByDateToNow(DateUtil.beginOfDay(new Date())));
        // 获取收入
        map.put("monthIncome", orderService.getMonthIncome());
        map.put("todayIncome", orderService.getTodayIncome());
        map.put("todayOrderCount", orderService.getTodayOrderCount());
        map.put("monthPaidUserCount", userService.getMonthPaidUserCount());
        return Result.ok().data(map);
    }

    @Override
    public Result cleanRedisCache() {
        redisService.deleteByKeys("panel::config::*");
        redisService.deleteByKeys("panel::node::*");
        redisService.deleteByKeys("panel::detect::*");
        redisService.deleteByKeys("panel::tutorial::*");
        redisService.deleteByKeys("panel::plan::*");
        redisService.deleteByKeys("panel::site::*");
        redisService.deleteByKeys("panel::user::*");
        return Result.ok().message("缓存已清理").messageEnglish("Cache is already cleaned");
    }

    /**
     * 通知续费
     */
    @Override
    public Result notifyRenew() {
        Lock lock = new ReentrantLock();
        lock.lock();
        // 获取要发信的内容
        String siteName = configService.getValueByName("siteName");
        String siteUrl = configService.getValueByName("siteUrl");
        String title = siteName + " - 过期提醒";
        // 获取通知续费邮件模板
        String content = configService.getValueByName("renewMail");
        content = content.replaceAll("\\{siteName}", siteName);
        content = content.replaceAll("\\{siteUrl}", siteUrl);
        // 查到期时间<3天的用户
        List<String> emails = userService.getExpiredUser().stream().map(User::getEmail).collect(Collectors.toList());
        Long lSize = redisService.lSize("panel::emails");
        if (lSize == 0) {
            emails.forEach(email -> {
                redisService.lPush("panel::emails", email, 86400);
            });
        }
        String mailType = configService.getValueByName("notifyMailType");
        if ("smtp".equals(mailType)) {
            for (int i = 0; i < 10; i++) {
                String finalContent = content;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (redisService.lSize("panel::emails") != 0) {
                            String email = (String) redisService.lLeftPop("panel::emails");
                            EmailUtil.sendEmail(title, finalContent, true, email);
                        }
                    }
                }).start();
            }
        } else if ("postalAPI".equals(mailType) || "aliyunAPI".equals(mailType)) {
            EmailUtil.sendEmail(title, content, true, null);
        }
        redisService.del("panel::emailTitle");
        redisService.del("panel::emailContent");
        // 给管理员发送失败的email
        while (redisService.lSize("panel::failedEmails") != 0) {
            List<String> failedEmails = (List) redisService.lRange("panel::failedEmails", 0, -1);
            redisService.del("panel::failedEmails");
            List<String> admins = userService.list(new QueryWrapper<User>().eq("is_admin", 1)).stream().map(User::getEmail).collect(Collectors.toList());
            admins.forEach(admin -> {
                EmailUtil.sendEmail("Failed to send email ", failedEmails.toString(), false, admin);
            });
        }
        lock.unlock();
        return Result.ok().message("正在处理发信,发信数: " + emails.size()).messageEnglish("Handling send renew email");
    }

    @Override
    public Result getSiteConfig() {
        String[] keys = {"siteName", "siteUrl", "subUrl", "regEnable", "inviteOnly", "mailRegEnable", "mailLimit", "mailType", "mailConfig", "notifyMailType", "notifyMailConfig", "enableNotifyRenew"};
        Map<String, Object> siteConfig = new HashMap<>();
        for (int i = 0; i < keys.length; i++) {
            String value = configService.getValueByName(keys[i]);
            if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                siteConfig.put(keys[i], Boolean.parseBoolean(value));
            } else if (NumberUtil.isInteger(value)) {
                siteConfig.put(keys[i], Integer.parseInt(value));
            } else {
                // mailConfig 转成map
                if ("mailConfig".equalsIgnoreCase(keys[i]) || "notifyMailConfig".equalsIgnoreCase(keys[i])) {
                    Map<String, Object> map = JSONUtil.toBean(value, Map.class);
                    siteConfig.put(keys[i], map);
                } else {
                    siteConfig.put(keys[i], value);
                }
            }
        }
        return Result.ok().data("siteConfig", siteConfig);
    }

    @Override
    public Result getRegisterConfig() {
        String[] keys = {"enableEmailSuffix", "inviteCount", "inviteRate", "enableWithdraw", "minWithdraw", "withdrawRate"};
        Map<String, Object> registerConfig = new HashMap<>();
        for (int i = 0; i < keys.length; i++) {
            String value = configService.getValueByName(keys[i]);
            if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                registerConfig.put(keys[i], Boolean.parseBoolean(value));
            } else {
                registerConfig.put(keys[i], value);
            }
        }
        return Result.ok().data("registerConfig", registerConfig);
    }

    @Override
    public Result getPaymentConfig() {
        String[] keys = {"alipay", "wxpay", "alipayConfig"};
        Map<String, Object> paymentConfig = new HashMap<>();
        for (int i = 0; i < keys.length; i++) {
            String value = configService.getValueByName(keys[i]);
            if ("alipay".equalsIgnoreCase(keys[i]) || "wxpay".equalsIgnoreCase(keys[i])) {
                paymentConfig.put(keys[i], value);
            } else {
                // 支付详细配置,转成map
                Map<String, Object> map = JSONUtil.toBean(value, Map.class);
                map.put("isCertMode", Boolean.parseBoolean(map.get("isCertMode").toString()));
                map.put("web", Boolean.parseBoolean(map.get("web").toString()));
                map.put("wap", Boolean.parseBoolean(map.get("wap").toString()));
                map.put("f2f", Boolean.parseBoolean(map.get("f2f").toString()));
                paymentConfig.put(keys[i], map);
            }
        }
        return Result.ok().data("paymentConfig", paymentConfig);
    }

    @Override
    public Result getOtherConfig() {
        String[] keys = {"muSuffix", "userTrafficLogLimitDays"};
        Map<String, Object> otherConfig = new HashMap<>();
        for (int i = 0; i < keys.length; i++) {
            String value = configService.getValueByName(keys[i]);
            if (NumberUtil.isInteger(value)) {
                otherConfig.put(keys[i], Integer.parseInt(value));
            } else {
                otherConfig.put(keys[i], value);
            }
        }
        return Result.ok().data("otherConfig", otherConfig);
    }

    @Override
    public Result getOauthConfig() {
        Map<String, Object> oauthConfig;
        String config = configService.getValueByName("oauthConfig");
        if (ObjectUtil.isNotEmpty(config)) {
            oauthConfig = JSONUtil.toBean(config, Map.class);
        } else {
            oauthConfig = null;
        }
        return Result.ok().data("oauthConfig", oauthConfig);
    }

    @Override
    public Result getClientConfig() {
        Map<String, Object> clientConfig;
        String config = configService.getValueByName("clientConfig");
        if (ObjectUtil.isNotEmpty(config)) {
            clientConfig = JSONUtil.toBean(config, Map.class);
        } else {
            clientConfig = new HashMap<>();
        }
        return Result.ok().data("clientConfig", clientConfig);
    }

    @Override
    @Transactional
    public Result updateValueByName(Config config) throws AlipayApiException {
        if (configService.updateValueByName(config.getName(), config.getValue())) {
            // 如果config.getName() == "alipayConfig",去设置alipayConfig
            if ("alipayConfig".equals(config.getName())) {
                alipay.refreshAlipayConfig();
            }
            return Result.ok().message("更新成功").messageEnglish("Update Successfully");
        } else {
            // 插入
            if (configService.save(config)) {
                return Result.ok().message("更新成功").messageEnglish("Update Successfully");
            } else {
                return Result.setResult(ResultCodeEnum.UNKNOWN_ERROR);
            }
        }
    }

    @Override
    public Result getNode(HttpServletRequest request, String type) {
        Map<String, Object> map = new HashMap<>();

        Integer pageNo = Integer.parseInt(request.getParameter("pageNo"));
        Integer pageSize = Integer.parseInt(request.getParameter("pageSize"));
        String sortField = request.getParameter("sortField");
        String sortOrder = request.getParameter("sortOrder");
        switch (type) {
            case "ss":
                IPage<Ss> ssIPage = ssService.getPageNode(pageNo, pageSize, sortField, sortOrder);
                List<Ss> sss = ssIPage.getRecords();
                map.put("data", sss);
                map.put("pageNo", ssIPage.getCurrent());
                map.put("totalCount", ssIPage.getTotal());
                break;
            case "v2ray":
                IPage<V2ray> v2rayIPage = v2rayService.getPageNode(pageNo, pageSize, sortField, sortOrder);
                List<V2ray> v2rays = v2rayIPage.getRecords();
                map.put("data", v2rays);
                map.put("pageNo", v2rayIPage.getCurrent());
                map.put("totalCount", v2rayIPage.getTotal());
                break;
            case "trojan":
                IPage<Trojan> trojanIPage = trojanService.getPageNode(pageNo, pageSize, sortField, sortOrder);
                List<Trojan> trojans = trojanIPage.getRecords();
                map.put("data", trojans);
                map.put("pageNo", trojanIPage.getCurrent());
                map.put("totalCount", trojanIPage.getTotal());
                break;
        }

        /*IPage<SsNode> page = ssNodeService.getPageNode(pageNo, pageSize, sortField, sortOrder);
        List<SsNode> ssNodes = page.getRecords();
        // 查询节点对应的在线ip数以及ip
        for (SsNode node : ssNodes) {
            QueryWrapper<AliveIp> aliveIpQueryWrapper = new QueryWrapper<>();
            aliveIpQueryWrapper.eq("nodeid", node.getId());
            List<AliveIp> aliveIps = aliveIpService.list(aliveIpQueryWrapper);
            node.setOnline(aliveIps.size());
        }*/
        return Result.ok().data("data", map);
    }

    @Override
    @Transactional
    public Result addSsNode(Ss ss) {
        if (ssService.save(ss)) {
            redisService.deleteByKeys("panel::node::*");
            return Result.ok().message("添加成功").messageEnglish("Add successfully");
        } else {
            return Result.setResult(ResultCodeEnum.UNKNOWN_ERROR);
        }
    }

    @Override
    @Transactional
    public Result editSsNode(Ss ss) {
        if (ssService.updateById(ss)) {
            redisService.deleteByKeys("panel::node::*");
            return Result.ok().message("修改成功").messageEnglish("Update successfully");
        } else {
            return Result.setResult(ResultCodeEnum.UNKNOWN_ERROR);
        }
    }

    @Override
    @Transactional
    public Result addV2rayNode(V2ray v2ray) {
        if (v2rayService.save(v2ray)) {
            redisService.deleteByKeys("panel::node::*");
            return Result.ok().message("添加成功").messageEnglish("Add successfully");
        } else {
            return Result.setResult(ResultCodeEnum.UNKNOWN_ERROR);
        }
    }

    @Override
    @Transactional
    public Result editV2rayNode(V2ray v2ray) {
        if (v2rayService.updateById(v2ray)) {
            redisService.deleteByKeys("panel::node::*");
            return Result.ok().message("修改成功").messageEnglish("Update successfully");
        } else {
            return Result.setResult(ResultCodeEnum.UNKNOWN_ERROR);
        }
    }

    @Override
    @Transactional
    public Result addTrojanNode(Trojan trojan) {
        if (trojanService.save(trojan)) {
            redisService.deleteByKeys("panel::node::*");
            return Result.ok().message("添加成功").messageEnglish("Add successfully");
        } else {
            return Result.setResult(ResultCodeEnum.UNKNOWN_ERROR);
        }
    }

    @Override
    @Transactional
    public Result editTrojanNode(Trojan trojan) {
        if (trojanService.updateById(trojan)) {
            redisService.deleteByKeys("panel::node::*");
            return Result.ok().message("修改成功").messageEnglish("Update successfully");
        } else {
            return Result.setResult(ResultCodeEnum.UNKNOWN_ERROR);
        }
    }

    @Override
    @Transactional
    public Result deleteNodeByTypeAndId(String type, Integer id) {
        switch (type) {
            case "ss":
                if (ssService.removeById(id)) {
                    redisService.deleteByKeys("panel::node::*");
                    return Result.ok().message("删除成功").messageEnglish("Delete successfully");
                } else {
                    return Result.setResult(ResultCodeEnum.UNKNOWN_ERROR);
                }
            case "v2ray":
                if (v2rayService.removeById(id)) {
                    redisService.deleteByKeys("panel::node::*");
                    return Result.ok().message("删除成功").messageEnglish("Delete successfully");
                } else {
                    return Result.setResult(ResultCodeEnum.UNKNOWN_ERROR);
                }
            case "trojan":
                if (trojanService.removeById(id)) {
                    redisService.deleteByKeys("panel::node::*");
                    return Result.ok().message("删除成功").messageEnglish("Delete successfully");
                } else {
                    return Result.setResult(ResultCodeEnum.UNKNOWN_ERROR);
                }
        }
        return Result.setResult(ResultCodeEnum.UNKNOWN_ERROR);
    }

    @Override
    public Result getAllDetects() {
        List<DetectList> detectLists = detectListService.getAllDetects();
        return Result.ok().data("allDetects", detectLists);
    }

    @Override
    public Result getDetect(Integer pageNo, Integer pageSize) {
        IPage<DetectList> page = detectListService.getPageDetect(pageNo, pageSize);
        Map<String, Object> map = new HashMap<>();
        map.put("data", page.getRecords());
        map.put("pageNo", page.getCurrent());
        map.put("totalCount", page.getTotal());
        return Result.ok().data("data", map);
    }

    @Override
    @Transactional
    public Result addDetect(DetectList detectList) {
        if (detectListService.save(detectList)) {
            redisService.deleteByKeys("panel::detect::*");
            return Result.ok().message("添加成功").messageEnglish("Add successfully");
        } else {
            return Result.setResult(ResultCodeEnum.UNKNOWN_ERROR);
        }
    }

    @Override
    @Transactional
    public Result editDetect(DetectList detectList) {
        if (detectListService.updateById(detectList)) {
            redisService.deleteByKeys("panel::detect::*");
            return Result.ok().message("修改成功").messageEnglish("Update successfully");
        } else {
            return Result.setResult(ResultCodeEnum.UNKNOWN_ERROR);
        }
    }

    @Override
    @Transactional
    public Result deleteDetectById(Integer id) {
        if (detectListService.removeById(id)) {
            redisService.deleteByKeys("panel::detect::*");
            return Result.ok().message("删除成功").messageEnglish("Delete successfully");
        } else {
            return Result.setResult(ResultCodeEnum.UNKNOWN_ERROR);
        }
    }

    @Override
    public Result getNodeWithDetect(Integer pageNo, Integer pageSize) {
        Map<String, Object> map = nodeWithDetectService.getNodeWithDetect(pageNo, pageSize);
        return Result.ok().data("data", map);
    }

    @Override
    @Transactional
    public Result addNodeWithDetect(Map<String, Object> map) {
        Double doubleNodeId = Double.parseDouble(map.get("nodeId").toString());
        Integer nodeId = doubleNodeId.intValue();
        ArrayList<String> detectIds = (ArrayList<String>) map.get("detectIds");
        List<Integer> detectListId = detectIds.stream().map(Integer::parseInt).collect(Collectors.toList());
        // 设置需要进行存储的NodeDetectList的List
        List<NodeWithDetect> list = new ArrayList<>();
        for (int i : detectListId) {
            NodeWithDetect item = new NodeWithDetect();
            item.setNodeId(nodeId);
            item.setDetectListId(i);
            list.add(item);
        }
        // 批量保存更新
        if (nodeWithDetectService.saveBatch(list)) {
            redisService.deleteByKeys("panel::detect::*");
            return Result.ok().message("添加成功").messageEnglish("Add successfully");
        } else {
            return Result.setResult(ResultCodeEnum.UNKNOWN_ERROR);
        }
    }

    @Override
    @Transactional
    public Result editNodeWithDetect(Map<String, Object> map) {
        Double doubleNodeId = Double.parseDouble(map.get("nodeId").toString());
        Integer nodeId = doubleNodeId.intValue();
        // 删除原来的记录
        nodeWithDetectService.deleteByNodeId(nodeId);
        ArrayList<String> detectIds = (ArrayList<String>) map.get("detectIds");
        List<Integer> detectListId = detectIds.stream().map(Integer::parseInt).collect(Collectors.toList());
        // 设置需要进行存储的NodeDetectList的List
        List<NodeWithDetect> list = new ArrayList<>();
        for (int i : detectListId) {
            NodeWithDetect item = new NodeWithDetect();
            item.setNodeId(nodeId);
            item.setDetectListId(i);
            list.add(item);
        }
        // 批量保存更新
        if (nodeWithDetectService.saveBatch(list)) {
            redisService.deleteByKeys("panel::detect::*");
            return Result.ok().message("修改成功").messageEnglish("Update successfully");
        } else {
            return Result.setResult(ResultCodeEnum.UNKNOWN_ERROR);
        }
    }

    @Override
    @Transactional
    public Result deleteNodeWithDetectById(Integer id) {
        if (nodeWithDetectService.deleteByNodeId(id)) {
            redisService.deleteByKeys("panel::detect::*");
            return Result.ok().message("删除成功").messageEnglish("Delete successfully");
        } else {
            return Result.setResult(ResultCodeEnum.UNKNOWN_ERROR);
        }
    }

    @Override
    public Result getUser(HttpServletRequest request) {
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
        // 带参数查询,查询结果不缓存
        boolean cacheFlag = false;
        if (ObjectUtil.isNotEmpty(id) || ObjectUtil.isNotEmpty(email) || ObjectUtil.isNotEmpty(clazz) || ObjectUtil.isNotEmpty(expireIn) || ObjectUtil.isNotEmpty(role) || ObjectUtil.isNotEmpty(enable)) {
            // 先删除缓存
            redisService.deleteByKeys("panel::user::users::*");
            cacheFlag = true;
        }
        return userService.getUserByPageAndQueryParam(request, Integer.parseInt(request.getParameter("pageNo")), Integer.parseInt(request.getParameter("pageSize")), cacheFlag);
    }

    @Override
    public Result getUserDetail(Integer id) {
        Map<String, Object> info = new HashMap<>();
        User user = userService.getUserById(id, false);
        info.put("user", user);
        return Result.ok().data("info", info);
    }

    @Override
    @Transactional
    public Result updateUserById(User user) {
        User existUser = userService.getById(user.getId());
        // 重新设置user的信息
        if (ObjectUtil.notEqual(existUser.getEmail(), user.getEmail())) {
            existUser.setEmail(user.getEmail());
        }
        if (ObjectUtil.isNotEmpty(user.getPassword())) {
            // 重新设置密码
            existUser.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        if (ObjectUtil.notEqual(existUser.getClazz(), user.getClazz())) {
            existUser.setClazz(user.getClazz());
        }
        if (ObjectUtil.notEqual(existUser.getExpireIn(), user.getExpireIn())) {
            existUser.setExpireIn(user.getExpireIn());
        }
        if (ObjectUtil.notEqual(existUser.getMoney(), user.getMoney())) {
            existUser.setMoney(user.getMoney());
        }
        if (ObjectUtil.notEqual(existUser.getTransferEnable(), FlowSizeConverterUtil.GbToBytes(user.getTransferEnableGb()))) {
            existUser.setTransferEnable(FlowSizeConverterUtil.GbToBytes(user.getTransferEnableGb()));
        }
        if (ObjectUtil.notEqual(existUser.getInviteCount(), user.getInviteCount())) {
            existUser.setInviteCount(user.getInviteCount());
        }
        if (ObjectUtil.notEqual(existUser.getInviteCycleRate(), user.getInviteCycleRate())) {
            existUser.setInviteCycleRate(user.getInviteCycleRate());
        }
        if (ObjectUtil.notEqual(existUser.getNodeSpeedlimit(), user.getNodeSpeedlimit())) {
            existUser.setNodeSpeedlimit(user.getNodeSpeedlimit());
        }
        if (ObjectUtil.notEqual(existUser.getNodeConnector(), user.getNodeConnector())) {
            existUser.setNodeConnector(user.getNodeConnector());
        }
        if (ObjectUtil.notEqual(existUser.getNodeGroup(), user.getNodeGroup())) {
            existUser.setNodeGroup(user.getNodeGroup());
        }
        if (ObjectUtil.notEqual(existUser.getIsAdmin(), user.getIsAdmin())) {
            existUser.setIsAdmin(user.getIsAdmin());
        }
        if (ObjectUtil.notEqual(existUser.getInviteCycleEnable(), user.getInviteCycleEnable())) {
            existUser.setInviteCycleEnable(user.getInviteCycleEnable());
        }
        if (ObjectUtil.notEqual(existUser.getEnable(), user.getEnable())) {
            existUser.setEnable(user.getEnable());
        }
        if (userService.updateById(existUser)) {
            redisService.del("panel::user::" + existUser.getId());
            return Result.ok().message("修改成功").messageEnglish("Update successfully");
        } else {
            return Result.setResult(ResultCodeEnum.UNKNOWN_ERROR);
        }
    }

    @Override
    @Transactional
    public Result deleteUserById(Integer id) {
        if (userService.removeById(id)) {
            redisService.deleteByKeys("panel::user::*");
            return Result.ok().message("删除成功").messageEnglish("Delete Successfully");
        } else {
            return Result.setResult(ResultCodeEnum.UNKNOWN_ERROR);
        }
    }

    @Override
    @Transactional
    public Result resetPasswdById(User user) {
        User toUpdateUser = new User();
        toUpdateUser.setId(user.getId());
        toUpdateUser.setPasswd(RandomUtil.randomStringUpper(8));
        // 重新生成uuid
        toUpdateUser.setPasswd(UuidUtil.uuid3(user.getId() + "|" + DateUtil.currentSeconds()));
        // 重新生成订阅
        toUpdateUser.setLink(RandomUtil.randomString(10));
        return userService.updateById(toUpdateUser) && redisService.del("panel::user::" + toUpdateUser.getId()) ? Result.ok().message("重置成功").messageEnglish("Reset Successfully") : Result.error().message("重置失败").messageEnglish("Reset failed");
    }

    @Override
    public Result getPlan(HttpServletRequest request) {
        return planService.getPlan(Integer.parseInt(request.getParameter("pageNo")), Integer.parseInt(request.getParameter("pageSize")));
    }

    @Override
    @Transactional
    public Result addPlan(Plan plan) {
        plan.setTransferEnable(FlowSizeConverterUtil.GbToBytes(plan.getTransferEnableGb()));
        plan.setPackagee(FlowSizeConverterUtil.GbToBytes(plan.getPackageGb()));
        if (planService.save(plan)) {
            // redisService.deleteByKeys("panel::plan::*");
            return Result.ok().message("新增成功").messageEnglish("Add Successfully");
        } else {
            return Result.setResult(ResultCodeEnum.UNKNOWN_ERROR);
        }
    }

    @Override
    @Transactional
    public Result updatePlanById(Plan plan) {
        plan.setTransferEnable(FlowSizeConverterUtil.GbToBytes(plan.getTransferEnableGb()));
        plan.setPackagee(FlowSizeConverterUtil.GbToBytes(plan.getPackageGb()));
        if (planService.updateById(plan)) {
            // redisService.deleteByKeys("panel::plan::*");
            return Result.ok().message("修改成功").messageEnglish("Update Successfully");
        } else {
            return Result.setResult(ResultCodeEnum.UNKNOWN_ERROR);
        }
    }

    @Override
    @Transactional
    public Result deletePlanById(Integer id) {
        if (planService.removeById(id)) {
            // redisService.deleteByKeys("panel::plan::*");
            return Result.ok().message("删除成功").messageEnglish("Delete Successfully");
        } else {
            return Result.setResult(ResultCodeEnum.UNKNOWN_ERROR);
        }
    }

    @Override
    public Result getTicket(HttpServletRequest request) {
        return Result.ok().data("tickets", ticketService.getTicketByPage(Integer.parseInt(request.getParameter("pageNo")), Integer.parseInt(request.getParameter("pageSize"))));
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
                    .set("status", 1)
                    .eq("id", ticket.getParentId());
            ticketService.update(ticketUpdateWrapper);
        }
        if (ticketService.save(ticket)) {
            List<User> admins = userService.getAdmins();
            List<Integer> ids = admins.stream().map(User::getId).collect(Collectors.toList());
            if (ids.contains(userId)) {
                // send to user
                Ticket parentTicket = ticketService.getById(ticket.getParentId());
                User user = userService.getById(parentTicket.getUserId());
                EmailUtil.sendEmail(configService.getValueByName("siteName") + " - 工单提醒", "您提交的工单已回复<br/><a href='" + configService.getValueByName("siteUrl") + "/ticket/detail/" + ticket.getParentId() + "'>点击查看详情</a>", true, user.getEmail());
                // tg bot
                if (ObjectUtil.isNotEmpty(user.getTgId())) {
                    bot.execute(new SendMessage(user.getTgId(), "您提交的工单已回复"));
                }
            }
            return Result.ok().message("回复成功").messageEnglish("Reply successfully");
        } else {
            return Result.setResult(ResultCodeEnum.UNKNOWN_ERROR);
        }
    }

    @Override
    @Transactional
    public Result deleteTicketById(Integer id) {
        Ticket ticket = ticketService.getById(id);
        if (ObjectUtil.isNotEmpty(ticket)) {
            return ticketService.removeById(id) ? Result.ok().message("删除成功").messageEnglish("Delete successfully") : Result.setResult(ResultCodeEnum.UNKNOWN_ERROR);
        }
        return Result.setResult(ResultCodeEnum.UNKNOWN_ERROR);
    }

    @Override
    public Result getTicketById(Integer id) {
        Ticket ticket = ticketService.getById(id);
        List<Ticket> list = ticketService.getTicketById(id);
        List<Ticket> tickets = new ArrayList<>();
        tickets.add(ticket);
        tickets.addAll(list);
        return Result.ok().data("tickets", tickets);
    }

    @Override
    @Transactional
    public Result closeTicket(Integer id) {
        Ticket ticket = ticketService.getById(id);
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
    public Result getTutorial(HttpServletRequest request) {
        return tutorialService.getTutorial(Integer.parseInt(request.getParameter("pageNo")), Integer.parseInt(request.getParameter("pageSize")));
    }

    @Override
    @Transactional
    public Result addTutorial(Tutorial tutorial) {
        if (tutorialService.save(tutorial)) {
            redisService.deleteByKeys("panel::tutorial::*");
            return Result.ok().message("新增成功").messageEnglish("Add Successfully");
        } else {
            return Result.setResult(ResultCodeEnum.UNKNOWN_ERROR);
        }
    }

    @Override
    @Transactional
    public Result updateTutorialById(Tutorial tutorial) {
        if (tutorialService.updateById(tutorial)) {
            redisService.deleteByKeys("panel::tutorial::*");
            return Result.ok().message("修改成功").messageEnglish("Update Successfully");
        } else {
            return Result.setResult(ResultCodeEnum.UNKNOWN_ERROR);
        }
    }

    @Override
    @Transactional
    public Result deleteTutorialById(Integer id) {
        if (tutorialService.removeById(id)) {
            redisService.deleteByKeys("panel::tutorial::*");
            return Result.ok().message("删除成功").messageEnglish("Delete Successfully");
        } else {
            return Result.setResult(ResultCodeEnum.UNKNOWN_ERROR);
        }
    }

    @Override
    public Result getAnnouncement() {
        return userService.getAnnouncement();
    }

    @Override
    @Transactional
    public Result saveOrUpdateAnnouncement(Announcement announcement) {
        Lock lock = new ReentrantLock();
        lock.lock();
        announcement.setTime(new Date());
        Boolean saveFlag = true;
        if (announcement.getSave()) {
            saveFlag = announcementService.saveOrUpdate(announcement);
            if (saveFlag) {
                redisService.set("panel::site::announcement", announcement);
            }
        }
        if (announcement.getBot()) {
            // TODO 发送bot提醒
        }
        if (announcement.getMail()) {
            // 根据userFilter获取用户,0 过期用户,1 付费用户,2 全体用户
            List<String> emails = new ArrayList<>();
            switch (announcement.getUserFilter()) {
                case 0:
                    emails = userService.list(new QueryWrapper<User>().eq("class", 0)).stream().map(User::getEmail).collect(Collectors.toList());
                    break;
                case 1:
                    emails = userService.list(new QueryWrapper<User>().gt("class", 0)).stream().map(User::getEmail).collect(Collectors.toList());
                    break;
                case 2:
                    emails = userService.list().stream().map(User::getEmail).collect(Collectors.toList());
                    break;
            }
            Long lSize = redisService.lSize("panel::emails");
            if (lSize == 0) {
                emails.forEach(email -> {
                    redisService.lPush("panel::emails", email, 86400);
                });
                // 存储要发信的内容
                redisService.set("panel::emailTitle", announcement.getTitle());
                redisService.set("panel::emailContent", announcement.getHtml());
                if ("smtp".equals(configService.getValueByName("notifyMailType"))) {
                    for (int i = 0; i < 10; i++) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                while (redisService.lSize("panel::emails") != 0) {
                                    String email = (String) redisService.lLeftPop("panel::emails");
                                    EmailUtil.sendEmail(announcement.getTitle(), announcement.getHtml(), true, email);
                                }
                            }
                        }).start();
                    }
                } else {
                    EmailUtil.sendEmail(announcement.getTitle(), announcement.getHtml(), true, null);
                }
                redisService.del("panel::emailTitle");
                redisService.del("panel::emailContent");
                // 给管理员发送失败的email
                while (redisService.lSize("panel::failedEmails") != 0) {
                    List<String> failedEmails = (List) redisService.lRange("panel::failedEmails", 0, -1);
                    redisService.del("panel::failedEmails");
                    List<String> admins = userService.list(new QueryWrapper<User>().eq("is_admin", 1)).stream().map(User::getEmail).collect(Collectors.toList());
                    admins.forEach(admin -> {
                        EmailUtil.sendEmail("Failed to send email ", failedEmails.toString(), false, admin);
                    });
                }
            } else {
                // 获取要发信的内容
                String mailType = configService.getValueByName("notifyMailType");
                String title = (String) redisService.get("panel::emailTitle");
                String content = (String) redisService.get("panel::emailContent");
                if ("smtp".equals(mailType)) {
                    for (int i = 0; i < 10; i++) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                while (redisService.lSize("panel::emails") != 0) {
                                    String email = (String) redisService.lLeftPop("panel::emails");
                                    EmailUtil.sendEmail(title, content, true, email);
                                }
                            }
                        }).start();
                    }
                } else if ("postalAPI".equals(mailType)) {
                    EmailUtil.sendEmail(title, content, true, null);
                }
                redisService.del("panel::emailTitle");
                redisService.del("panel::emailContent");
                // 给管理员发送失败的email
                while (redisService.lSize("panel::failedEmails") != 0) {
                    List<String> failedEmails = (List) redisService.lRange("panel::failedEmails", 0, -1);
                    redisService.del("panel::failedEmails");
                    List<String> admins = userService.list(new QueryWrapper<User>().eq("is_admin", 1)).stream().map(User::getEmail).collect(Collectors.toList());
                    admins.forEach(admin -> {
                        EmailUtil.sendEmail("Failed to send email ", failedEmails.toString(), false, admin);
                    });
                }
                lock.unlock();
                return Result.error().message("邮件队列任务未完成, 正在尝试继续发送, 请稍后再试").messageEnglish("Redis Queue is busy, Try again later");
            }
        }
        lock.unlock();
        if (saveFlag) {
            return Result.ok().message("操作成功").messageEnglish("Successfully");
        } else {
            return Result.setResult(ResultCodeEnum.UNKNOWN_ERROR);
        }
    }

    @Override
    public Result getOrder(HttpServletRequest request) {
        return orderService.getOrder(request);
    }

    @Override
    public Result getOrderByOrderId(String orderId) {
        Order order = orderService.getOrderByOrderId(orderId);
        if (ObjectUtil.isNotEmpty(order)) {
            return Result.ok().data("order", order);
        } else {
            return Result.error().message("无效订单ID").messageEnglish("Invalid Order ID");
        }
    }

    @Override
    @Transactional
    public Result confirmOrder(String orderId) {
        Order order = orderService.getOrderByOrderId(orderId);
        if (ObjectUtil.isNotEmpty(order) && (order.getStatus() == 0 || order.getStatus() == 2)) {
            // 根据订单修改用户
            redisService.del("panel::user::" + order.getUserId());
            order.setIsMixedPay(false);
            order.setMixedMoneyAmount(BigDecimal.ZERO);
            order.setMixedPayAmount(order.getPrice());
            // 查询用户当前套餐
            Order currentPlan = orderService.getCurrentPlan(order.getUserId());
            // 查询是否新用户
            int buyCount = orderService.getBuyCountByUserId(order.getUserId());
            Boolean isNewPayer = buyCount == 0;
            // 更新订单
            if (orderService.updateFinishedOrder(order.getIsMixedPay(), order.getMixedMoneyAmount(), order.getMixedPayAmount(), "余额", null, isNewPayer, null, new Date(), PayStatusEnum.SUCCESS.getStatus(), order.getId())) {
                userService.updateUserAfterBuyOrder(order, ObjectUtil.isEmpty(currentPlan));
                Date now = new Date();
                // 给该用户新增资金明细表
                Funds funds = new Funds();
                funds.setUserId(order.getUserId());
                funds.setPrice(BigDecimal.ZERO.subtract(order.getPrice()));
                funds.setTime(now);
                funds.setRelatedOrderId(order.getOrderId());
                funds.setContent(order.getPlanDetailsMap().get("name").toString());
                funds.setContentEnglish(order.getPlanDetailsMap().get("nameEnglish").toString());
                fundsService.save(funds);

                // 如果有邀请人,给他加余额,并且给他新增一笔资金明细
                User user = userService.getById(order.getUserId());
                if (ObjectUtil.isNotEmpty(user.getParentId())) {
                    User inviteUser = userService.getById(user.getParentId());
                    // 用户有等级的话,给返利
                    if (ObjectUtil.isNotEmpty(inviteUser) && inviteUser.getClazz() > 0) {
                        redisService.del("panel::user::" + inviteUser.getId());
                        // 判断是循环返利还是首次返利
                        if (inviteUser.getInviteCycleEnable()) {
                            userService.handleCommission(inviteUser.getId(), order.getMixedPayAmount().multiply(inviteUser.getInviteCycleRate()).setScale(2, BigDecimal.ROUND_HALF_UP));
                        } else {
                            // 首次返利,查该用户是否第一次充值
                            int count = fundsService.count(new QueryWrapper<Funds>().eq("user_id", user.getId()));
                            if (count == 1) {
                                // 首次
                                userService.handleCommission(inviteUser.getId(), order.getMixedPayAmount().multiply(inviteUser.getInviteCycleRate()).setScale(2, BigDecimal.ROUND_HALF_UP));
                            }
                        }
                        // 给邀请人新增返利明细
                        Funds inviteFund = new Funds();
                        inviteFund.setUserId(inviteUser.getId());
                        inviteFund.setPrice(order.getMixedPayAmount().multiply(inviteUser.getInviteCycleRate()).setScale(2, BigDecimal.ROUND_HALF_UP));
                        inviteFund.setTime(now);
                        inviteFund.setRelatedOrderId(order.getOrderId());
                        inviteFund.setContent("佣金");
                        inviteFund.setContentEnglish("Commission");
                        fundsService.save(inviteFund);
                        log.info("id为{}的用户获得返利{}元", inviteUser.getId(), inviteFund.getPrice());
                    }
                }
                return Result.ok();
            } else {
                return Result.error();
            }
        } else {
            return Result.error().message("无效订单ID").messageEnglish("Invalid Order ID");
        }
    }

    @Override
    public Result getCommission(Integer pageNo, Integer pageSize) {
        Map<String, Object> map = fundsService.getCommission(pageNo, pageSize);
        return Result.ok().data("data", map);
    }

    @Override
    public Result getWithdraw(Integer pageNo, Integer pageSize) {
        Map<String, Object> map = withdrawService.getWithdraw(pageNo, pageSize);
        return Result.ok().data("data", map);
    }

    @Override
    @Transactional
    public Result ackWithdrawById(Integer id) {
        // 修改status
        UpdateWrapper<Withdraw> withdrawUpdateWrapper = new UpdateWrapper<>();
        withdrawUpdateWrapper.set("status", 1).eq("status", 0).eq("id", id);
        if (withdrawService.update(withdrawUpdateWrapper)) {
            Withdraw withdraw = withdrawService.getById(id);
            // 给该用户生成一笔资金明细
            Funds funds = new Funds();
            funds.setUserId(withdraw.getUserId());
            funds.setTime(new Date());
            funds.setPrice(BigDecimal.ZERO.subtract(withdraw.getAmount()));
            funds.setContent("提现");
            funds.setContentEnglish("Withdrawal");
            funds.setRelatedOrderId(withdraw.getId().toString());
            if (fundsService.save(funds)) {
                // 发送消息给用户
                User user = userService.getById(withdraw.getUserId());
                EmailUtil.sendEmail("提现到账提醒", "您申请的提现已到账,请注意查看", false, user.getEmail());
                if (ObjectUtil.isNotEmpty(user.getTgId())) {
                    bot.execute(new SendMessage(user.getTgId(), "您申请的提现已到账,请注意查看"));
                }
                return Result.ok().message("操作成功").messageEnglish("Successfully");
            }
        }
        return Result.setResult(ResultCodeEnum.UNKNOWN_ERROR);
    }

    @Override
    @Transactional
    public Result addScheduleTask(Schedule schedule) {
        if (!scheduleService.save(schedule)) {
            return Result.error().message("新增失败").messageEnglish("Failed");
        } else {
            if (schedule.getJobStatus() == 1) {
                SchedulingRunnable task = new SchedulingRunnable(schedule.getBeanName(), schedule.getMethodName(), schedule.getMethodParams());
                cronTaskRegistrar.addCronTask(task, schedule.getCronExpression());
            }
        }
        return Result.ok();
    }

    @Override
    @Transactional
    public Result updateScheduleTask(Schedule schedule) {
        // 保存原来的任务
        Schedule existedSchedule = scheduleService.getById(schedule.getId());
        // 更新新任务
        if (!scheduleService.updateById(schedule)) {
            return Result.error().message("修改失败").messageEnglish("Failed");
        } else {
            // 如果原来的任务status=1,停止原来的任务
            if (existedSchedule.getJobStatus() == 1) {
                SchedulingRunnable task = new SchedulingRunnable(existedSchedule.getBeanName(), existedSchedule.getMethodName(), existedSchedule.getMethodParams());
                cronTaskRegistrar.removeCronTask(task);
            }

            //添加新任务
            if (schedule.getJobStatus() == 1) {
                SchedulingRunnable task = new SchedulingRunnable(schedule.getBeanName(), schedule.getMethodName(), schedule.getMethodParams());
                cronTaskRegistrar.addCronTask(task, schedule.getCronExpression());
            }
        }
        return Result.ok();
    }

    @Override
    @Transactional
    public Result deleteScheduleTask(Integer id) {
        // 保存原来的任务
        Schedule existedSchedule = scheduleService.getById(id);
        // 删除任务
        if (!scheduleService.removeById(id)) {
            return Result.error().message("删除失败").messageEnglish("Failed");
        } else {
            // 移除任务
            if (existedSchedule.getJobStatus() == 1) {
                SchedulingRunnable task = new SchedulingRunnable(existedSchedule.getBeanName(), existedSchedule.getMethodName(), existedSchedule.getMethodParams());
                cronTaskRegistrar.removeCronTask(task);
            }
        }
        return Result.ok();
    }

    @Override
    @Transactional
    public Result toggleScheduleTask(Schedule schedule) {
        // 保存原来的任务
        Schedule existedSchedule = scheduleService.getById(schedule.getId());

        // 已启动的关闭
        if (existedSchedule.getJobStatus() == 1) {
            // 修改状态
            existedSchedule.setJobStatus(0);
            if (scheduleService.updateById(existedSchedule)) {
                SchedulingRunnable task = new SchedulingRunnable(existedSchedule.getBeanName(), existedSchedule.getMethodName(), existedSchedule.getMethodParams());
                cronTaskRegistrar.removeCronTask(task);
                return Result.ok().message("定时任务已关闭").messageEnglish("Close successfully");
            } else {
                return Result.error().message("切换定时任务状态出现错误").messageEnglish("Toggle failed");
            }
            // 未启动的启动
        } else if (existedSchedule.getJobStatus() == 0) {
            // 修改状态
            existedSchedule.setJobStatus(1);
            if (scheduleService.updateById(existedSchedule)) {
                SchedulingRunnable task = new SchedulingRunnable((existedSchedule.getBeanName()), existedSchedule.getMethodName(), existedSchedule.getMethodParams());
                cronTaskRegistrar.addCronTask(task, existedSchedule.getCronExpression());
                return Result.ok().message("定时任务已启动").messageEnglish("Start successfully");
            } else {
                return Result.error().message("切换定时任务状态出现错误").messageEnglish("Toggle failed");
            }
        }
        return Result.error().message("切换定时任务状态出现错误").messageEnglish("Toggle failed");
    }
}