package project.daihao18.panel.serviceImpl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.daihao18.panel.entity.SsNodeOnlineLog;
import project.daihao18.panel.mapper.SsNodeOnlineLogMapper;
import project.daihao18.panel.service.SsNodeOnlineLogService;

import java.util.Date;

/**
 * @ClassName: SsNodeOnlineLogServiceImpl
 * @Description:
 * @Author: code18
 * @Date: 2020-10-07 21:28
 */
@Service
@Slf4j
public class SsNodeOnlineLogServiceImpl extends ServiceImpl<SsNodeOnlineLogMapper, SsNodeOnlineLog> implements SsNodeOnlineLogService {
    @Override
    @Transactional
    public void dailyJobTask() {
        QueryWrapper<SsNodeOnlineLog> ssNodeOnlineLogQueryWrapper = new QueryWrapper<>();
        ssNodeOnlineLogQueryWrapper.lt("log_time", DateUtil.offsetDay(new Date(), -3).getTime() / 1000);
        if (this.remove(ssNodeOnlineLogQueryWrapper)) {
            log.info("节点在线日志清理完成");
        }
    }
}