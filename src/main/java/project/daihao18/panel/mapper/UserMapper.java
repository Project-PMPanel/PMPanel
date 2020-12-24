package project.daihao18.panel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import project.daihao18.panel.entity.User;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}