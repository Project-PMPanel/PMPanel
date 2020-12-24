package project.daihao18.panel.serviceImpl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.daihao18.panel.entity.SsNodeInfo;
import project.daihao18.panel.mapper.SsNodeInfoMapper;
import project.daihao18.panel.service.SsNodeInfoService;

import java.util.Date;

/**
 * @ClassName: SsNodeInfoServiceImpl
 * @Description:
 * @Author: code18
 * @Date: 2020-10-07 21:27
 */
@Service
@Slf4j
public class SsNodeInfoServiceImpl extends ServiceImpl<SsNodeInfoMapper, SsNodeInfo> implements SsNodeInfoService {
    @Override
    @Transactional
    public void dailyJobTask() {
        QueryWrapper<SsNodeInfo> ssNodeInfoQueryWrapper = new QueryWrapper<>();
        ssNodeInfoQueryWrapper.lt("log_time", DateUtil.offsetDay(new Date(), -3).getTime() / 1000);
        if (this.remove(ssNodeInfoQueryWrapper)) {
            log.info("节点负载日志清理完成");
        }
    }
}