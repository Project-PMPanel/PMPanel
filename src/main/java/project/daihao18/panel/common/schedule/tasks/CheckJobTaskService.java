package project.daihao18.panel.common.schedule.tasks;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import project.daihao18.panel.entity.Online;
import project.daihao18.panel.entity.Order;
import project.daihao18.panel.entity.Package;
import project.daihao18.panel.service.OnlineService;
import project.daihao18.panel.service.OrderService;
import project.daihao18.panel.service.PackageService;

import java.util.Date;

/**
 * @ClassName: CheckJobTaskService
 * @Description:
 * @Author: code18
 * @Date: 2020-11-12 10:53
 */
@Component
@Slf4j
public class CheckJobTaskService {

    @Autowired
    private OnlineService onlineService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PackageService packageService;

    // 0 * * * * ?
    @Transactional
    public void checkJob() {
        Date now = new Date();
        // 删除2分钟前的aliveIp
        QueryWrapper<Online> onlineQueryWrapper = new QueryWrapper<>();
        onlineQueryWrapper.lt("time", DateUtil.offsetMinute(now, -2));
        onlineService.remove(onlineQueryWrapper);
        // 取消5分钟前的订单
        UpdateWrapper<Order> orderUpdateWrapper = new UpdateWrapper<>();
        orderUpdateWrapper
                .set("status", 2)
                .eq("status", 0)
                .lt("create_time", DateUtil.offsetMinute(now, -5));
        orderService.update(orderUpdateWrapper);
        // 取消5分钟前的流量包订单
        UpdateWrapper<Package> packageUpdateWrapper = new UpdateWrapper<>();
        packageUpdateWrapper
                .set("status", 2)
                .eq("status", 0)
                .lt("create_time", DateUtil.offsetMinute(now, -5));
        packageService.update(packageUpdateWrapper);
    }
}