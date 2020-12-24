package project.daihao18.panel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import project.daihao18.panel.entity.SsNodeOnlineLog;

/**
 * @InterfaceName: SsNodeOnlineLogService
 * @Description:
 * @Author: code18
 * @Date: 2020-10-07 21:20
 */
public interface SsNodeOnlineLogService extends IService<SsNodeOnlineLog> {
    void dailyJobTask();
}