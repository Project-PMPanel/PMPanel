package project.daihao18.panel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

/**
 * @ClassName: Group
 * @Description:
 * @Author: code18
 * @Date: 2020-10-07 21:06
 */
@Data
@ToString
@TableName(value = "operate_ip")
public class OperateIp implements Serializable {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer type;

    private String ip;

    private Date time;

    private Integer userId;

    private static final long serialVersionUID = 1L;
}