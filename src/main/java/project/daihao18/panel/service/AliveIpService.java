package project.daihao18.panel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import project.daihao18.panel.entity.AliveIp;

/**
 * @InterfaceName: AliveIpService
 * @Description:
 * @Author: code18
 * @Date: 2020-10-07 21:13
 */
public interface AliveIpService extends IService<AliveIp> {
    Integer countAliveIpByUserId(Integer userId);
}