package project.daihao18.panel.common.schedule.tasks;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import project.daihao18.panel.service.AdminService;

/**
 * @ClassName: CheckJobTaskService
 * @Description:
 * @Author: code18
 * @Date: 2020-11-12 10:53
 */
@Component
@Slf4j
public class NotifyRenewJobTaskService {

    @Autowired
    private AdminService adminService;

    // 0 0 2 28 * ?  每月28号凌晨2点发信通知
    @Transactional
    public void notifyRenewJob() {
        log.info("Notify Renew Mail start");
        adminService.notifyRenew();
        log.info("Notify Renew Mail end");
    }
}