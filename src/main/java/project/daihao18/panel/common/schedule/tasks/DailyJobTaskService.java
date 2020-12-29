package project.daihao18.panel.common.schedule.tasks;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import project.daihao18.panel.common.utils.FlowSizeConverterUtil;
import project.daihao18.panel.entity.Order;
import project.daihao18.panel.entity.User;
import project.daihao18.panel.entity.UserMonthlyTraffic;
import project.daihao18.panel.service.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @ClassName: DailyJobTaskService
 * @Description:
 * @Author: code18
 * @Date: 2020-11-12 10:53
 */
@Component
@Slf4j
public class DailyJobTaskService {

    @Autowired
    private ConfigService configService;

    @Autowired
    private UserService userService;

    @Autowired
    private SsNodeInfoService ssNodeInfoService;

    @Autowired
    private SsNodeOnlineLogService ssNodeOnlineLogService;

    @Autowired
    private UserTrafficLogService userTrafficLogService;

    @Autowired
    private DetectLogService detectLogService;

    @Autowired
    private SpeedtestService speedtestService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PackageService packageService;

    @Autowired
    private RedisService redisService;

    @Autowired
    private UserMonthlyTrafficService userMonthlyTrafficService;

    /**
     * 同步用户流量/重置流量
     */
    // 0 0 0 * * *
    @Transactional
    public void syncTraffic() {
        log.info(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss") + " 执行每日任务");
        // 如果是1号,执行每月任务
        if (LocalDateTime.now().getDayOfMonth() == 1) {
            this.monthlyJob();
        } else {
            this.dailyJob();
        }
        // 清理所有缓存
        redisService.deleteByKeys("panel::config::*");
        redisService.deleteByKeys("panel::node::*");
        redisService.deleteByKeys("panel::detect::*");
        redisService.deleteByKeys("panel::tutorial::*");
        redisService.deleteByKeys("panel::plan::*");
        redisService.deleteByKeys("panel::site::*");
        redisService.deleteByKeys("panel::user::*");
        log.info("每日任务执行结束");
    }

    /**
     * 抽取出来零点的任务,以供管理员手动调用
     */
    @Transactional
    public void dailyJob() {
        // 执行每日任务
        // 开始同步用户昨日流量
        UpdateWrapper<User> userUpdateWrapper = new UpdateWrapper<>();
        userUpdateWrapper
                .setSql("p=u+d")
                .eq("is_multi_user", 0);
        userService.update(userUpdateWrapper);
        // 将每个用户昨日流量同步到userMonthlyTraffic表
        List<Map<String, Object>> traffics = userTrafficLogService.getYesterdayTraffic();
        List<UserMonthlyTraffic> list = new ArrayList<>();
        ZonedDateTime zdt = LocalDateTimeUtil.of(DateUtil.offsetDay(new Date(), -1)).withHour(23).withMinute(59).withSecond(59).withNano(0).atZone(ZoneId.systemDefault());
        Date yesterday = Date.from(zdt.toInstant());
        traffics.forEach(item -> {
            UserMonthlyTraffic userMonthlyTraffic = new UserMonthlyTraffic();
            userMonthlyTraffic.setUserId((Integer) item.get("user_id"));
            userMonthlyTraffic.setU(Long.parseLong(FlowSizeConverterUtil.convertNumber(String.valueOf(item.get("u")))));
            userMonthlyTraffic.setD(Long.parseLong(FlowSizeConverterUtil.convertNumber(String.valueOf(item.get("d")))));
            userMonthlyTraffic.setDate(yesterday);
            list.add(userMonthlyTraffic);
        });
        userMonthlyTrafficService.saveBatch(list);
    }

    /**
     * 抽取出来零点的任务,以供管理员手动调用
     */
    @Transactional
    public void monthlyJob() {
        // 如果设置流量日志保留天数,则执行dayJobTask,否则按自然月保存用户流量日志,执行monthlyJobTask
        int days = Integer.parseInt(configService.getValueByName("userTrafficLogLimitDays"));
        if (days <= 0) {
            // 清空上个月所有用户流量日志
            userTrafficLogService.monthlyJobTask();
            userMonthlyTrafficService.monthlyJobTask();
        }
        // 1.使过期status=1的套餐和流量包失效
        orderService.expiredFinishedOrder();
        packageService.expiredFinishedPackageOrder();
        // 2.清空所有用户数据, TODO 改为清空所有status=1的用户的流量清空
        // userService.cleanUserData();

        // 3.查询未到期的status=1的订单,根据订单保存的套餐内容给已付费用户重置套餐
        List<Order> finishedOrderList = orderService.getFinishedOrder();
        List<User> resetUserList = new ArrayList<>();
        for (Order order : finishedOrderList) {
            User user = userService.getById(order.getUserId());
            user.setU(0L);
            user.setD(0L);
            user.setP(0L);
            user.setTransferEnable(FlowSizeConverterUtil.GbToBytes((Integer) order.getPlanDetailsMap().get("transferEnable")));
            Integer clazz = (Integer) order.getPlanDetailsMap().get("clazz");
            user.setClazz(clazz);
            Integer nodeConnector = (Integer) order.getPlanDetailsMap().get("nodeConnector");
            user.setNodeConnector(nodeConnector);
            user.setNodeSpeedlimit((Integer) order.getPlanDetailsMap().get("nodeSpeedlimit"));
            Integer nodeGroup = (Integer) order.getPlanDetailsMap().get("nodeGroup");
            user.setNodeGroup(nodeGroup);
            resetUserList.add(user);
        }
        userService.updateBatchById(resetUserList, 1000);
    }

    /**
     * 清理数据库
     */
    // 0 0 0 * * *
    @Transactional
    public void cleanDB() {
        log.info(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss") + " 清理数据库");
        // 清理数据库日志
        ssNodeInfoService.dailyJobTask();
        ssNodeOnlineLogService.dailyJobTask();
        // 如果设置流量日志保留天数,则执行dayJobTask,否则按自然月保存用户流量日志,执行monthlyJobTask
        int days = Integer.parseInt(configService.getValueByName("userTrafficLogLimitDays"));
        if (days > 0) {
            userTrafficLogService.dailyJobTask(days);
        }
        detectLogService.dailyJobTask();
        speedtestService.dailyJobTask();
        log.info("清理数据库执行结束");
    }
}