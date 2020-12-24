package project.daihao18.panel.common.schedule.tasks;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import project.daihao18.panel.entity.AliveIp;
import project.daihao18.panel.service.AliveIpService;
import project.daihao18.panel.service.SsNodeService;

import java.util.Date;

/**
 * @ClassName: CheckJobTaskService
 * @Description:
 * @Author: code18
 * @Date: 2020-11-12 10:53
 */
@Component
@Slf4j
public class CheckJobTaskService {

    @Autowired
    private SsNodeService ssNodeService;

    @Autowired
    private AliveIpService aliveIpService;

    // 0 * * * * ?
    @Transactional
    public void checkJob() {
        // 删除2分钟前的aliveIp
        QueryWrapper<AliveIp> aliveIpQueryWrapper = new QueryWrapper<>();
        aliveIpQueryWrapper.lt("datetime", DateUtil.offsetMinute(new Date(), -2).getTime() / 1000);
        aliveIpService.remove(aliveIpQueryWrapper);

        // log.info(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss") + " 执行更新节点IP任务");
        // 查所有node
        /*
            sort=0 是Shadowsocks
            sort=9是Shadowsocks - 单端口多用户
            sort=10是Shadowsocks - 中转
            sort=11是V2ray
            sort=12是V2ray中转
            sort=13是Shadowsocks - V2Ray-Plugin
        */
        /*List<SsNode> nodes = ssNodeService.list();
        List<SsNode> nodeList = nodes.stream().filter(node -> {
            return (node.getSort() == 0 || node.getSort() == 11);
        }).collect(Collectors.toList());
        List<SsNode> ssNodeList = new ArrayList<>();
        nodeList.forEach(node -> {
            String[] serverList = node.getServer().split(",");
            if (!IpUtil.isIp(serverList[0])) {
                String nodeIp = null;
                try {
                    nodeIp = IpUtil.getIpFromAdress(serverList[0]);
                    node.setNodeIp(nodeIp);
                    ssNodeList.add(node);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                node.setNodeIp(nodeIp);
            } else {
                if (ObjectUtil.isEmpty(node.getNodeIp())) {
                    node.setNodeIp(serverList[0]);
                    ssNodeList.add(node);
                }
            }
        });
        ssNodeService.updateBatchById(ssNodeList);
        log.info("更新节点IP执行结束");*/
    }
}