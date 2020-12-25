package project.daihao18.panel.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.daihao18.panel.entity.UserMonthlyTraffic;
import project.daihao18.panel.mapper.UserMonthlyTrafficMapper;
import project.daihao18.panel.service.UserMonthlyTrafficService;

import java.util.Date;

/**
 * @ClassName: UserMonthlyTrafficServiceImpl
 * @Description:
 * @Author: code18
 * @Date: 2020-10-07 21:22
 */
@Service
@Slf4j
public class UserMonthlyTrafficServiceImpl extends ServiceImpl<UserMonthlyTrafficMapper, UserMonthlyTraffic> implements UserMonthlyTrafficService {
    @Override
    @Transactional
    public void monthlyJobTask() {
        QueryWrapper<UserMonthlyTraffic> userMonthlyTrafficQueryWrapper = new QueryWrapper<>();
        userMonthlyTrafficQueryWrapper.lt("date", new Date().getTime() / 1000);
        if (this.remove(userMonthlyTrafficQueryWrapper)) {
            log.info("用户每月流量日志清理完成");
        }
    }
}