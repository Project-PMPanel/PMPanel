package project.daihao18.panel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import project.daihao18.panel.entity.Online;

import java.util.List;

/**
 * @InterfaceName: OnlineService
 * @Description:
 * @Author: code18
 * @Date: 2020-10-07 21:16
 */
public interface OnlineService extends IService<Online> {
    int getOnlineCountByTypeAndId(String type, Integer nodeId);

    List<Online> getOnlineByTypeAndId(String type, Integer nodeId);
}