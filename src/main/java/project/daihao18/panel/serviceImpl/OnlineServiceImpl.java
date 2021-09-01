package project.daihao18.panel.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import project.daihao18.panel.entity.Online;
import project.daihao18.panel.mapper.OnlineMapper;
import project.daihao18.panel.service.OnlineService;

import java.util.List;

/**
 * @ClassName: OnlineServiceImpl
 * @Description:
 * @Author: code18
 * @Date: 2020-10-07 21:22
 */
@Service
public class OnlineServiceImpl extends ServiceImpl<OnlineMapper, Online> implements OnlineService {
    @Override
    public Integer getOnlineCountByUserId(Integer userId) {
        QueryWrapper<Online> onlineQueryWrapper = new QueryWrapper<>();
        onlineQueryWrapper
                .eq("user_id", userId);
        return this.count(onlineQueryWrapper);
    }

    @Override
    public int getOnlineCountByTypeAndId(String type, Integer nodeId) {
        QueryWrapper<Online> onlineQueryWrapper = new QueryWrapper<>();
        onlineQueryWrapper
                .eq("type", type)
                .eq("node_id", nodeId);
        return this.count(onlineQueryWrapper);
    }

    @Override
    public List<Online> getOnlineByTypeAndId(String type, Integer nodeId) {
        QueryWrapper<Online> onlineQueryWrapper = new QueryWrapper<>();
        onlineQueryWrapper
                .eq("type", type)
                .eq("node_id", nodeId);
        return this.list(onlineQueryWrapper);
    }
}