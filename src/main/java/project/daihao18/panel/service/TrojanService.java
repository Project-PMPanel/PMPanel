package project.daihao18.panel.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import project.daihao18.panel.entity.Trojan;

/**
 * @InterfaceName: TrojanService
 * @Description:
 * @Author: code18
 * @Date: 2020-10-07 21:16
 */
public interface TrojanService extends IService<Trojan> {
    IPage<Trojan> getPageNode(Integer pageNo, Integer pageSize, String... sortParam);
}