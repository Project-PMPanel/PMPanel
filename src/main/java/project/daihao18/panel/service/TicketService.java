package project.daihao18.panel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import project.daihao18.panel.entity.Ticket;

import java.util.List;
import java.util.Map;

/**
 * @InterfaceName: TicketService
 * @Description:
 * @Author: code18
 * @Date: 2020-10-07 21:16
 */
public interface TicketService extends IService<Ticket> {
    Map<String, Object> getTicketByPage(Integer userId, int pageNo, int pageSize);

    Map<String, Object> getTicketByPage(int pageNo, int pageSize);

    List<Ticket> getTicketById(Integer id);
}