package project.daihao18.panel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import project.daihao18.panel.entity.SsNode;
import project.daihao18.panel.entity.SsNodeInfo;

/**
 * @InterfaceName: SsNodeInfoService
 * @Description:
 * @Author: code18
 * @Date: 2020-10-07 21:20
 */
public interface SsNodeInfoService extends IService<SsNodeInfo> {
    void dailyJobTask();
}