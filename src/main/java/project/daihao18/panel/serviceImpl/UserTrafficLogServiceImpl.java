package project.daihao18.panel.serviceImpl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.daihao18.panel.entity.UserTrafficLog;
import project.daihao18.panel.mapper.UserTrafficLogMapper;
import project.daihao18.panel.service.UserTrafficLogService;

import java.util.Date;

/**
 * @ClassName: UserTrafficLogServiceImpl
 * @Description:
 * @Author: code18
 * @Date: 2020-10-07 21:30
 */
@Service
@Slf4j
public class UserTrafficLogServiceImpl extends ServiceImpl<UserTrafficLogMapper, UserTrafficLog> implements UserTrafficLogService {

    @Override
    @Transactional
    public void dailyJobTask(Integer days) {
        QueryWrapper<UserTrafficLog> userTrafficLogQueryWrapper = new QueryWrapper<>();
        userTrafficLogQueryWrapper.lt("log_time", DateUtil.offsetDay(new Date(), -days).getTime() / 1000);
        if (this.remove(userTrafficLogQueryWrapper)) {
            log.info("用户流量日志清理完成");
        }
    }

    @Override
    @Transactional
    public void monthlyJobTask() {
        QueryWrapper<UserTrafficLog> userTrafficLogQueryWrapper = new QueryWrapper<>();
        userTrafficLogQueryWrapper.lt("log_time", new Date().getTime() / 1000);
        if (this.remove(userTrafficLogQueryWrapper)) {
            log.info("用户流量日志清理完成");
        }
    }
}