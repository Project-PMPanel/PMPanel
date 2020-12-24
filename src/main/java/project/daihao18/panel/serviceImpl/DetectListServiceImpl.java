package project.daihao18.panel.serviceImpl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import project.daihao18.panel.entity.DetectList;
import project.daihao18.panel.mapper.DetectListMapper;
import project.daihao18.panel.service.DetectListService;

import java.util.List;

/**
 * @ClassName: DetectListServiceImpl
 * @Description:
 * @Author: code18
 * @Date: 2020-10-07 21:25
 */
@Service
public class DetectListServiceImpl extends ServiceImpl<DetectListMapper, DetectList> implements DetectListService {
    @Override
    @Cacheable(cacheNames = "panel::detect::detects", key = "#pageNo+'-'+#pageSize", unless = "#result == null")
    public IPage<DetectList> getPageDetect(Integer pageNo, Integer pageSize) {
        IPage<DetectList> page = new Page<>(pageNo, pageSize);
        return this.page(page);
    }

    @Override
    @Cacheable(cacheNames = "panel::detect", key = "'detects'", unless = "#result == null")
    public List<DetectList> getAllDetects() {
        return this.list();
    }
}