package project.daihao18.panel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

/**
 * @ClassName: Oauth
 * @Description:
 * @Author: code18 
 * @Date: 2020-10-07 21:06
 */
@Data
@ToString
@TableName(value = "oauth")
public class Oauth implements Serializable {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer userId;

    private String oauthType;

    private String email;

    private String uuid;

    private Date time;

    private Boolean valid;

    private static final long serialVersionUID = 1L;
}