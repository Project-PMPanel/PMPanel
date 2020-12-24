package project.daihao18.panel.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import project.daihao18.panel.entity.DetectList;
import project.daihao18.panel.entity.NodeWithDetect;
import project.daihao18.panel.mapper.NodeWithDetectMapper;
import project.daihao18.panel.service.DetectListService;
import project.daihao18.panel.service.NodeWithDetectService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @ClassName: NodeWithDetectServiceImpl
 * @Description:
 * @Author: code18
 * @Date: 2020-10-07 21:22
 */
@Service
public class NodeWithDetectServiceImpl extends ServiceImpl<NodeWithDetectMapper, NodeWithDetect> implements NodeWithDetectService {

    @Autowired
    private DetectListService detectListService;

    @Override
    @Cacheable(cacheNames = "panel::detect::nodeWithDetects", key = "#pageNo+'-'+#pageSize", unless = "#result == null")
    public Map<String, Object> getNodeWithDetect(Integer pageNo, Integer pageSize) {
        IPage<NodeWithDetect> page = this.getNodeId(pageNo, pageSize);
        List<NodeWithDetect> nodeWithDetects = page.getRecords();

        // 要返回的map
        Map<String, Object> returnMap = new HashMap<>();
        List<Map<String, Object>> nodeWithDetectList = new ArrayList<>();
        for (NodeWithDetect nodeWithDetect : nodeWithDetects) {
            // 循环查找每个节点对应了哪些审计
            List<NodeWithDetect> detectIds = this.getDetectListIdByNodeId(nodeWithDetect.getNodeId());
            List<DetectList> detectLists = new ArrayList<>();
            // 循环查找审计规则
            for (NodeWithDetect detectId : detectIds) {
                DetectList detectList = detectListService.getById(detectId.getDetectListId());
                // 加入list
                detectLists.add(detectList);
            }
            Map<String, Object> map = new HashMap<>();
            map.put("nodeId", nodeWithDetect.getNodeId());
            map.put("data", detectLists);
            nodeWithDetectList.add(map);
        }

        returnMap.put("data", nodeWithDetectList);
        returnMap.put("pageNo", page.getCurrent());
        returnMap.put("totalCount", page.getTotal());
        return returnMap;
    }

    private IPage<NodeWithDetect> getNodeId(Integer pageNo, Integer pageSize) {
        IPage<NodeWithDetect> page = new Page<>(pageNo, pageSize);
        QueryWrapper<NodeWithDetect> nodeWithDetectQueryWrapper = new QueryWrapper<>();
        nodeWithDetectQueryWrapper
                .select("node_id")
                .groupBy("node_id");
        page = this.page(page, nodeWithDetectQueryWrapper);
        return page;
    }

    @Override
    public List<NodeWithDetect> getDetectListIdByNodeId(Integer nodeId) {
        QueryWrapper<NodeWithDetect> nodeWithDetectQueryWrapper = new QueryWrapper<>();
        nodeWithDetectQueryWrapper
                .select("detect_list_id")
                .eq("node_id", nodeId);
        return this.list(nodeWithDetectQueryWrapper);
    }

    @Override
    @Cacheable(cacheNames = "panel::detect::nodeWithDetects", key = "'nodeIds'", unless = "#result == null")
    public List<Integer> getNodeId() {
        QueryWrapper<NodeWithDetect> nodeWithDetectQueryWrapper = new QueryWrapper<>();
        nodeWithDetectQueryWrapper
                .select("node_id")
                .groupBy("node_id");
        return this.list(nodeWithDetectQueryWrapper).stream().map(NodeWithDetect::getNodeId).collect(Collectors.toList());
    }

    @Override
    public boolean deleteByNodeId(Integer nodeId) {
        QueryWrapper<NodeWithDetect> nodeWithDetectQueryWrapper = new QueryWrapper<>();
        nodeWithDetectQueryWrapper
                .eq("node_id", nodeId);
        return this.remove(nodeWithDetectQueryWrapper);
    }
}