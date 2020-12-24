package project.daihao18.panel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * @ClassName: DisconnectIp
 * @Description:
 * @Author: code18 
 * @Date: 2020-10-07 21:07
 */
@Data
@ToString
@TableName(value = "disconnect_ip")
public class DisconnectIp implements Serializable {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer userid;

    private String ip;

    private Integer datetime;

    private static final long serialVersionUID = 1L;
}