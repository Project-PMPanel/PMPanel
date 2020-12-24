package project.daihao18.panel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import project.daihao18.panel.entity.Announcement;

/**
 * @InterfaceName: Announcement
 * @Description:
 * @Author: code18
 * @Date: 2020-10-07 21:16
 */
public interface AnnouncementService extends IService<Announcement> {
    Announcement getLatestAnnouncement();
}