package project.daihao18.panel.common.schedule.tasks;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import project.daihao18.panel.entity.Online;
import project.daihao18.panel.service.OnlineService;

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

    // 0 * * * * ?
    @Transactional
    public void checkJob() {
        // 删除2分钟前的aliveIp
        QueryWrapper<Online> onlineQueryWrapper = new QueryWrapper<>();
        onlineQueryWrapper.lt("time", DateUtil.offsetMinute(new Date(), -2));
        onlineService.remove(onlineQueryWrapper);
    }
}