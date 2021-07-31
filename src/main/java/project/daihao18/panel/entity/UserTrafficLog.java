package project.daihao18.panel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * @ClassName: UserTrafficLog
 * @Description:
 * @Author: code18 
 * @Date: 2020-10-07 21:07
 */
@Data
@ToString
@TableName(value = "user_traffic_log")
public class UserTrafficLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer userId;

    private Integer u;

    private Integer d;

    private String type;

    private Integer nodeId;

    private Double rate;

    private String traffic;

    private String ip;

    private Integer logTime;

    private static final long serialVersionUID = 1L;
}