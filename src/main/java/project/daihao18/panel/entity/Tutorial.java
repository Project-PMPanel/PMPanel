package project.daihao18.panel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * @ClassName: Tutorial
 * @Description:
 * @Author: code18 
 * @Date: 2020-10-07 21:05
 */
@Data
@ToString
@TableName(value = "tutorial")
public class Tutorial implements Serializable {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String type;

    private String name;

    private String markdownContent;

    private static final long serialVersionUID = 1L;
}