package project.daihao18.panel.serviceImpl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.daihao18.panel.entity.DetectLog;
import project.daihao18.panel.mapper.DetectLogMapper;
import project.daihao18.panel.service.DetectLogService;

import java.util.Date;

/**
 * @ClassName: DetectLogServiceImpl
 * @Description:
 * @Author: code18
 * @Date: 2020-10-07 21:25
 */
@Service
@Slf4j
public class DetectLogServiceImpl extends ServiceImpl<DetectLogMapper, DetectLog> implements DetectLogService {
    @Override
    @Transactional
    public void dailyJobTask() {
        QueryWrapper<DetectLog> detectLogQueryWrapper = new QueryWrapper<>();
        detectLogQueryWrapper.lt("datetime", DateUtil.offsetDay(new Date(), -3).getTime() / 1000);
        if (this.remove(detectLogQueryWrapper)) {
            log.info("审计记录日志清理完成");
        }
    }
}