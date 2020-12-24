package project.daihao18.panel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * @ClassName: Unblockip
 * @Description:
 * @Author: code18 
 * @Date: 2020-10-07 21:07
 */
@Data
@ToString
@TableName(value = "unblockip")
public class Unblockip implements Serializable {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String ip;

    private Integer datetime;

    private Integer userid;

    private static final long serialVersionUID = 1L;
}