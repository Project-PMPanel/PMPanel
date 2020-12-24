package project.daihao18.panel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * @ClassName: Relay
 * @Description:
 * @Author: code18 
 * @Date: 2020-10-07 21:07
 */
@Data
@ToString
@TableName(value = "relay")
public class Relay implements Serializable {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer userId;

    private Integer sourceNodeId;

    private Integer distNodeId;

    private String distIp;

    @TableField("`port`")
    private Integer port;

    private Integer priority;

    private static final long serialVersionUID = 1L;
}