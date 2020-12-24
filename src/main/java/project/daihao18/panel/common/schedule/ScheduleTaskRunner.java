package project.daihao18.panel.common.schedule;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import project.daihao18.panel.entity.Schedule;
import project.daihao18.panel.service.ScheduleService;

import java.util.List;

@Service
@Slf4j
public class ScheduleTaskRunner implements CommandLineRunner {

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private CronTaskRegistrar cronTaskRegistrar;

    @Override
    public void run(String... args) throws Exception {
        // 初始加载数据库里状态为正常的定时任务
        QueryWrapper<Schedule> scheduleQueryWrapper = new QueryWrapper<>();
        scheduleQueryWrapper.eq("job_status", 1);
        List<Schedule> schedules = scheduleService.list(scheduleQueryWrapper);
        if (schedules.size() > 0) {
            schedules.forEach(schedule -> {
                SchedulingRunnable task = new SchedulingRunnable(schedule.getBeanName(), schedule.getMethodName(), schedule.getMethodParams());
                cronTaskRegistrar.addCronTask(task, schedule.getCronExpression());
                log.info("定时任务 - bean：{}，方法：{}，参数：{} 已加载", schedule.getBeanName(), schedule.getMethodName(), schedule.getMethodParams());
            });
        }
    }
}