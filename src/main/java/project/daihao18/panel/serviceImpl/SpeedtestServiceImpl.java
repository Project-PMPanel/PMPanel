package project.daihao18.panel.serviceImpl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.daihao18.panel.entity.Speedtest;
import project.daihao18.panel.mapper.SpeedtestMapper;
import project.daihao18.panel.service.SpeedtestService;

import java.util.Date;

/**
 * @ClassName: SpeedtestServiceImpl
 * @Description:
 * @Author: code18
 * @Date: 2020-10-07 21:27
 */
@Service
@Slf4j
public class SpeedtestServiceImpl extends ServiceImpl<SpeedtestMapper, Speedtest> implements SpeedtestService {
    @Override
    @Transactional
    public void dailyJobTask() {
        QueryWrapper<Speedtest> speedtestQueryWrapper = new QueryWrapper<>();
        speedtestQueryWrapper.lt("datetime", DateUtil.offsetDay(new Date(), -3).getTime() / 1000);
        if (this.remove(speedtestQueryWrapper)) {
            log.info("节点测速日志清理完成");
        }
    }
}