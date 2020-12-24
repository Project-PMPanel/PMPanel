package project.daihao18.panel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import project.daihao18.panel.entity.Order;

import java.util.List;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    @Select("SELECT * FROM `order` a JOIN (SELECT user_id, MIN(expire) AS expire FROM `order` WHERE(`status` = 1 AND expire > NOW()) GROUP BY user_id) b ON a.user_id = b.user_id AND a.expire = b.expire GROUP BY a.user_id")
    List<Order> getFinishedOrder();
}