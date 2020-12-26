package project.daihao18.panel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

/**
 * @ClassName: Ticket
 * @Description:
 * @Author: code18 
 * @Date: 2020-10-07 21:05
 */
@Data
@ToString
@TableName(value = "ticket")
public class Ticket implements Serializable {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer userId;

    private String title;

    private String content;

    private Date time;

    private Integer parentId;

    private Integer status;

    private static final long serialVersionUID = 1L;
}