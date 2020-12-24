package project.daihao18.panel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import project.daihao18.panel.entity.Speedtest;

/**
 * @InterfaceName: SpeedtestService
 * @Description:
 * @Author: code18
 * @Date: 2020-10-07 21:18
 */
public interface SpeedtestService extends IService<Speedtest> {
    void dailyJobTask();
}