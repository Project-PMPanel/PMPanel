package project.daihao18.panel.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import project.daihao18.panel.entity.Announcement;
import project.daihao18.panel.mapper.AnnouncementMapper;
import project.daihao18.panel.service.AnnouncementService;

/**
 * @ClassName: AnnouncementServiceImpl
 * @Description:
 * @Author: code18
 * @Date: 2020-10-07 21:22
 */
@Service
public class AnnouncementServiceImpl extends ServiceImpl<AnnouncementMapper, Announcement> implements AnnouncementService {
    @Override
    @Cacheable(cacheNames = "panel::site", key = "'announcement'", unless = "#result == null")
    public Announcement getLatestAnnouncement() {
        return this.getOne(new QueryWrapper<Announcement>().orderByDesc("time").last("limit 1"));
    }
}