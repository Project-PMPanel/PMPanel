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
import java.util.List;

/**
 * @ClassName: Plan
 * @Description:
 * @Author: code18 
 * @Date: 2020-10-07 21:05
 */
@Data
@ToString
@TableName(value = "plan")
public class Plan implements Serializable {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("`name`")
    private String name;

    private String nameEnglish;

    private String months;

    private String price;

    private Long transferEnable;

    private Integer nodeConnector;

    private Double nodeSpeedlimit;

    private Integer nodeGroup;

    @TableField("`package`")
    private Long packagee;

    @TableField("`class`")
    private Integer clazz;

    @TableField("`sort`")
    private Integer sort;

    private Integer buyLimit;

    private Boolean isDiscount;

    private Date discountStart;

    private Date discountEnd;

    @TableField("`enable`")
    private Boolean enable;

    private Boolean enableRenew;

    private Boolean supportMedia;

    private Boolean supportDirectline;

    private String extraInfo;

    private String extraInfoEnglish;

    @TableField(exist = false)
    private Long currentMonthTransferEnable;

    @TableField(exist = false)
    private Integer transferEnableGb;

    @TableField(exist = false)
    private Integer packageGb;

    @TableField(exist = false)
    private List<Integer> monthsList;

    @TableField(exist = false)
    private List<BigDecimal> priceList;

    @TableField(exist = false)
    private List<Date> expireList;

    private static final long serialVersionUID = 1L;
}