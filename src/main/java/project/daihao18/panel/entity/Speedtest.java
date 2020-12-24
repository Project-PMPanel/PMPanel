package project.daihao18.panel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * @ClassName: Speedtest
 * @Description:
 * @Author: code18 
 * @Date: 2020-10-07 21:07
 */
@Data
@ToString
@TableName(value = "speedtest")
public class Speedtest implements Serializable {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer nodeid;

    private Integer datetime;

    private String telecomping;

    private String telecomeupload;

    private String telecomedownload;

    private String unicomping;

    private String unicomupload;

    private String unicomdownload;

    private String cmccping;

    private String cmccupload;

    private String cmccdownload;

    private static final long serialVersionUID = 1L;
}