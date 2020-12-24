package project.daihao18.panel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * @ClassName: Package
 * @Description:
 * @Author: code18 
 * @Date: 2020-10-07 21:05
 */
@Data
@ToString
@TableName(value = "`package`")
public class Package implements Serializable {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer userId;

    private String orderId;

    private BigDecimal price;

    private Long transferEnable;

    private Date createTime;

    private Date expire;

    private Boolean isMixedPay;

    private BigDecimal mixedMoneyAmount;

    private BigDecimal mixedPayAmount;

    private String payType;

    private String payer;

    private Date payTime;

    private String tradeNo;

    @TableField("`status`")
    private Integer status;

    @TableField(exist = false)
    private Integer transferEnableGb;

    private static final long serialVersionUID = 1L;
}