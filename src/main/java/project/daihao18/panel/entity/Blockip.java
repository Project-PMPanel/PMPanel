package project.daihao18.panel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * @ClassName: Blockip
 * @Description:
 * @Author: code18 
 * @Date: 2020-10-07 21:05
 */
@Data
@ToString
@TableName(value = "blockip")
public class Blockip implements Serializable {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer nodeid;

    private String ip;

    private Integer datetime;

    private static final long serialVersionUID = 1L;
}