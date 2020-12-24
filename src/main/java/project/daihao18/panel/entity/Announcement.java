package project.daihao18.panel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

/**
 * @ClassName: Announcement
 * @Description:
 * @Author: code18 
 * @Date: 2020-10-07 21:05
 */
@Data
@ToString
@TableName(value = "announcement")
public class Announcement implements Serializable {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String title;

    private String markdownContent;

    @TableField("`time`")
    private Date time;

    @TableField(exist = false)
    private Boolean save;

    @TableField(exist = false)
    private Boolean bot;

    @TableField(exist = false)
    private Boolean mail;

    @TableField(exist = false)
    private String html;

    @TableField(exist = false)
    private Integer userFilter;

    private static final long serialVersionUID = 1L;
}