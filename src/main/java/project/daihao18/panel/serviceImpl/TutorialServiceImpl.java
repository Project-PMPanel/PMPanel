package project.daihao18.panel.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import project.daihao18.panel.common.response.Result;
import project.daihao18.panel.entity.Tutorial;
import project.daihao18.panel.mapper.TutorialMapper;
import project.daihao18.panel.service.TutorialService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @ClassName: TutorialServiceImpl
 * @Description:
 * @Author: code18
 * @Date: 2020-10-07 21:22
 */
@Service
public class TutorialServiceImpl extends ServiceImpl<TutorialMapper, Tutorial> implements TutorialService {
    @Override
    @Cacheable(cacheNames = "panel::tutorial::tutorials", key = "#pageNo+'-'+#pageSize", unless = "#result == null")
    public Result getTutorial(int pageNo, int pageSize) {
        IPage<Tutorial> page = new Page<>(pageNo, pageSize);
        page = this.page(page);
        Map<String, Object> map = new HashMap<>();
        map.put("data", page.getRecords());
        map.put("pageNo", page.getCurrent());
        map.put("totalCount", page.getTotal());
        return Result.ok().data("data", map);
    }

    @Override
    @Cacheable(cacheNames = "panel::tutorial::tutorials", key = "#type", unless = "#result == null")
    public List<Tutorial> getTutorialsByType(String type) {
        QueryWrapper<Tutorial> tutorialQueryWrapper = new QueryWrapper<>();
        tutorialQueryWrapper.eq("type", type);
        return this.list(tutorialQueryWrapper);
    }
}