package project.daihao18.panel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

/**
 * @ClassName: UserMonthlyTraffic
 * @Description:
 * @Author: code18
 * @Date: 2020-10-07 21:06
 */
@Data
@ToString
@TableName(value = "user_monthly_traffic")
public class UserMonthlyTraffic implements Serializable {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer userId;

    private Date date;

    private Long u;

    private Long d;

    private static final long serialVersionUID = 1L;
}