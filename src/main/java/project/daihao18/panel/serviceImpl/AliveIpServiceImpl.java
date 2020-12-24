package project.daihao18.panel.serviceImpl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import project.daihao18.panel.entity.AliveIp;
import project.daihao18.panel.mapper.AliveIpMapper;
import project.daihao18.panel.service.AliveIpService;

/**
 * @ClassName: AliveIpServiceImpl
 * @Description:
 * @Author: code18
 * @Date: 2020-10-07 21:14
 */
@Service
public class AliveIpServiceImpl extends ServiceImpl<AliveIpMapper, AliveIp> implements AliveIpService {

    @Autowired
    private AliveIpMapper aliveIpMapper;

    @Override
    public Integer countAliveIpByUserId(Integer userId) {
        return aliveIpMapper.countAliveIpByUserId(userId);
    }
}