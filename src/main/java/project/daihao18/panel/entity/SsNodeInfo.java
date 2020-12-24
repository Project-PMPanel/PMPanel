package project.daihao18.panel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * @ClassName: SsNodeInfo
 * @Description:
 * @Author: code18 
 * @Date: 2020-10-07 21:07
 */
@Data
@ToString
@TableName(value = "ss_node_info")
public class SsNodeInfo implements Serializable {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer nodeId;

    private Double uptime;

    @TableField("`load`")
    private String load;

    private Integer logTime;

    private static final long serialVersionUID = 1L;
}