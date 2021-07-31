package project.daihao18.panel.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * @ClassName: shadowsocks
 * @Description:
 * @Author: code18 
 * @Date: 2020-10-07 21:06
 */
@Data
@ToString
@TableName(value = "shadowsocks")
public class Ss extends Node implements Serializable {

    private String method;

    private static final long serialVersionUID = 1L;
}