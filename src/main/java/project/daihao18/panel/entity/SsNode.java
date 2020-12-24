package project.daihao18.panel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @ClassName: SsNode
 * @Description:
 * @Author: code18
 * @Date: 2020-10-07 21:07
 */
@Data
@ToString
@TableName(value = "ss_node")
public class SsNode implements Serializable {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("`name`")
    private String name;

    private Integer type;

    @TableField("`server`")
    private String server;

    private Integer port;

    private String passwd;

    private String method;

    private String protocol;

    private String obfs;

    private Integer isMultiUser;

    private String info;

    @TableField("`status`")
    private String status;

    private Integer sort;

    private String customMethod;

    private Double trafficRate;

    private Integer nodeGroup;

    private Integer nodeClass;

    private Double nodeSpeedlimit;

    private Integer nodeConnector;

    private Long nodeBandwidth;

    private Long nodeBandwidthLimit;

    private Integer bandwidthlimitResetday;

    private Integer nodeHeartbeat;

    private String nodeIp;

    private Integer customRss;

    private Integer muOnly;

    @TableField(exist = false)
    private Integer online;

    @TableField(exist = false)
    private List<Map<String, Object>> onlineIps;

    private static final long serialVersionUID = 1L;
}