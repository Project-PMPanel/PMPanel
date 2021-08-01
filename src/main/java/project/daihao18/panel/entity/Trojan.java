package project.daihao18.panel.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * @ClassName: Trojan
 * @Description:
 * @Author: code18 
 * @Date: 2020-10-07 21:06
 */
@Data
@ToString
@TableName(value = "trojan")
public class Trojan extends Node implements Serializable {

    private Boolean grpc;

    private String sni;

    private static final long serialVersionUID = 1L;
}