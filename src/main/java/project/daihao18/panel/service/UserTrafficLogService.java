package project.daihao18.panel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import project.daihao18.panel.entity.UserTrafficLog;

import java.util.List;
import java.util.Map;

/**
 * @InterfaceName: UserTrafficLogService
 * @Description:
 * @Author: code18
 * @Date: 2020-10-07 21:21
 */
public interface UserTrafficLogService extends IService<UserTrafficLog> {
    void dailyJobTask(Integer days);

    void monthlyJobTask();

    List<Map<String, Object>> getYesterdayTraffic();
}