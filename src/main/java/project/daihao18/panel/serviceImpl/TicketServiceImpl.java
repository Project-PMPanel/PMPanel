package project.daihao18.panel.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import project.daihao18.panel.entity.Ticket;
import project.daihao18.panel.mapper.TicketMapper;
import project.daihao18.panel.service.TicketService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @ClassName: TicketServiceImpl
 * @Description:
 * @Author: code18
 * @Date: 2020-10-07 21:22
 */
@Service
public class TicketServiceImpl extends ServiceImpl<TicketMapper, Ticket> implements TicketService {
    @Override
    public Map<String, Object> getTicketByPage(Integer userId, int pageNo, int pageSize) {
        IPage<Ticket> page = new Page<>(pageNo, pageSize);
        QueryWrapper<Ticket> ticketQueryWrapper = new QueryWrapper<>();
        ticketQueryWrapper
                .eq("user_id", userId)
                .isNull("parent_id")
                .orderByDesc("time");
        page = this.page(page, ticketQueryWrapper);
        Map<String, Object> map = new HashMap<>();
        map.put("data", page.getRecords());
        map.put("pageNo", page.getCurrent());
        map.put("totalCount", page.getTotal());
        return map;
    }

    @Override
    public Map<String, Object> getTicketByPage(int pageNo, int pageSize) {
        IPage<Ticket> page = new Page<>(pageNo, pageSize);
        QueryWrapper<Ticket> ticketQueryWrapper = new QueryWrapper<>();
        ticketQueryWrapper
                .isNull("parent_id")
                .orderByAsc("status", "time");
        page = this.page(page, ticketQueryWrapper);
        Map<String, Object> map = new HashMap<>();
        map.put("data", page.getRecords());
        map.put("pageNo", page.getCurrent());
        map.put("totalCount", page.getTotal());
        return map;
    }

    @Override
    public List<Ticket> getTicketById(Integer id) {
        QueryWrapper<Ticket> ticketQueryWrapper = new QueryWrapper<>();
        ticketQueryWrapper
                .eq("parent_id", id)
                .orderByAsc("time");
        return this.list(ticketQueryWrapper);
    }
}