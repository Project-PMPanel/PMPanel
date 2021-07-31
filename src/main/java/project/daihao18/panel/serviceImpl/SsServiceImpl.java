package project.daihao18.panel.serviceImpl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import project.daihao18.panel.entity.Ss;
import project.daihao18.panel.entity.V2ray;
import project.daihao18.panel.mapper.SsMapper;
import project.daihao18.panel.service.SsService;

/**
 * @ClassName: SsServiceImpl
 * @Description:
 * @Author: code18
 * @Date: 2020-10-07 21:22
 */
@Service
public class SsServiceImpl extends ServiceImpl<SsMapper, Ss> implements SsService {
    @Override
    public IPage<Ss> getPageNode(Integer pageNo, Integer pageSize, String... sortParam) {
        IPage<Ss> page = new Page<>(pageNo, pageSize);
        QueryWrapper<Ss> nodeQueryWrapper = new QueryWrapper<>();
        if (ObjectUtil.isNotEmpty(sortParam)) {
            if ("ascend".equals(sortParam[1])) {
                nodeQueryWrapper.orderByAsc(sortParam[0]);
            } else if ("descend".equals(sortParam[1])) {
                nodeQueryWrapper.orderByDesc(sortParam[0]);
            }
        }
        return this.page(page, nodeQueryWrapper);
    }
}