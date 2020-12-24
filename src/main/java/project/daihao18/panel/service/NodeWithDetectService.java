package project.daihao18.panel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import project.daihao18.panel.entity.NodeWithDetect;

import java.util.List;
import java.util.Map;

/**
 * @InterfaceName: NodeWithDetectService
 * @Description:
 * @Author: code18
 * @Date: 2020-10-07 21:16
 */
public interface NodeWithDetectService extends IService<NodeWithDetect> {
    Map<String, Object> getNodeWithDetect(Integer pageNo, Integer pageSize);

    List<NodeWithDetect> getDetectListIdByNodeId(Integer nodeId);

    List<Integer> getNodeId();

    boolean deleteByNodeId(Integer nodeId);
}