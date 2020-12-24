package project.daihao18.panel.serviceImpl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import project.daihao18.panel.entity.SsNode;
import project.daihao18.panel.mapper.SsNodeMapper;
import project.daihao18.panel.service.SsNodeService;

import java.util.List;

/**
 * @ClassName: SsNodeServiceImpl
 * @Description:
 * @Author: code18
 * @Date: 2020-10-07 21:28
 */
@Service
public class SsNodeServiceImpl extends ServiceImpl<SsNodeMapper, SsNode> implements SsNodeService {

    /**
     * 获取按分组和等级显示的非单端口承载节点
     *
     * @return
     */
    @Override
    public List<SsNode> listEnableNodesByGroupAndClass(Integer group, Integer clazz) {
        QueryWrapper<SsNode> ssNodeQueryWrapper = new QueryWrapper<>();
        ssNodeQueryWrapper
                .eq("type", 1)
                .ne("sort", 9)
                .eq("node_group", group)
                .le("node_class", clazz);
        return this.list(ssNodeQueryWrapper);
    }

    /**
     * 获取显示的非单端口承载节点
     *
     * @return
     */
    @Override
    @Cacheable(cacheNames = "panel::node", key = "'enableNodes'", unless = "#result == null")
    public List<SsNode> listEnableNodes() {
        QueryWrapper<SsNode> ssNodeQueryWrapper = new QueryWrapper<>();
        ssNodeQueryWrapper
                .eq("type", 1)
                .in("sort", 0, 11)// ss or ssr or v2ray
                .orderByAsc("name");
        return this.list(ssNodeQueryWrapper);
    }

    @Override
    // @Cacheable(cacheNames = "panel::node::nodes", key = "#pageNo+'-'+#pageSize", unless = "#result == null")
    public IPage<SsNode> getPageNode(Integer pageNo, Integer pageSize, String... sortParam) {
        IPage<SsNode> page = new Page<>(pageNo, pageSize);
        QueryWrapper<SsNode> nodeQueryWrapper = new QueryWrapper<>();
        if (ObjectUtil.isNotEmpty(sortParam)) {
            if ("ascend".equals(sortParam[1])) {
                nodeQueryWrapper.orderByAsc(sortParam[0]);
            } else if ("descend".equals(sortParam[1])) {
                nodeQueryWrapper.orderByDesc(sortParam[0]);
            }
        }
        return this.page(page, nodeQueryWrapper);
    }

    @Override
    @Cacheable(cacheNames = "panel::node", key = "'nodes'", unless = "#result == null")
    public List<SsNode> getAllNodes() {
        return this.list();
    }
}