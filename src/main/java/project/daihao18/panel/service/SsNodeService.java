package project.daihao18.panel.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import project.daihao18.panel.entity.SsNode;

import java.util.List;

/**
 * @InterfaceName: SsNodeService
 * @Description:
 * @Author: code18
 * @Date: 2020-10-07 21:20
 */
public interface SsNodeService extends IService<SsNode> {

    List<SsNode> listEnableNodesByGroupAndClass(Integer group, Integer clazz);

    List<SsNode> listEnableNodes();

    IPage<SsNode> getPageNode(Integer pageNo, Integer pageSize, String... sortParam);

    List<SsNode> getAllNodes();
}