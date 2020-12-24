package project.daihao18.panel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * @ClassName: DetectList
 * @Description:
 * @Author: code18 
 * @Date: 2020-10-07 21:05
 */
@Data
@ToString
@TableName(value = "detect_list")
public class DetectList implements Serializable {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("`name`")
    private String name;

    private String regex;

    private Integer type;

    private static final long serialVersionUID = 1L;
}