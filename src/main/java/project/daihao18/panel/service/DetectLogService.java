package project.daihao18.panel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import project.daihao18.panel.entity.DetectLog;

/**
 * @InterfaceName: DetectLogService
 * @Description:
 * @Author: code18
 * @Date: 2020-10-07 21:17
 */
public interface DetectLogService extends IService<DetectLog> {
    void dailyJobTask();
}