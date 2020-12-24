package project.daihao18.panel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import project.daihao18.panel.entity.AliveIp;

@Mapper
public interface AliveIpMapper extends BaseMapper<AliveIp> {

    @Select("SELECT COUNT(1) FROM (SELECT * FROM alive_ip WHERE userid = #{userId} GROUP BY ip) AS a")
    Integer countAliveIpByUserId(@Param("userId") Integer userId);
}