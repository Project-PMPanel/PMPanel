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
import java.util.Map;

/**
 * @ClassName: Order
 * @Description:
 * @Author: code18 
 * @Date: 2020-10-07 21:05
 */
@Data
@ToString
@TableName(value = "`order`")
public class Order implements Serializable {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String orderId;

    private Integer userId;

    private Integer planId;

    private Integer monthCount;

    private BigDecimal price;

    private Boolean isMixedPay;

    private BigDecimal mixedMoneyAmount;

    private BigDecimal mixedPayAmount;

    private String payType;

    private String payer;

    private Boolean isNewPayer;

    private Date createTime;

    private Date expire;

    private Date payTime;

    private String tradeNo;

    @TableField("`status`")
    private Integer status;

    private String userDetails;

    private String planDetails;

    @TableField(exist = false)
    private Map<String, Object> userDetailsMap;

    @TableField(exist = false)
    private Map<String, Object> planDetailsMap;

    private static final long serialVersionUID = 1L;
}