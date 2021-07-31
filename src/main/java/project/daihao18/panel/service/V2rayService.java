package project.daihao18.panel.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import project.daihao18.panel.entity.V2ray;

/**
 * @InterfaceName: V2rayService
 * @Description:
 * @Author: code18
 * @Date: 2020-10-07 21:16
 */
public interface V2rayService extends IService<V2ray> {
    IPage<V2ray> getPageNode(Integer pageNo, Integer pageSize, String... sortParam);
}