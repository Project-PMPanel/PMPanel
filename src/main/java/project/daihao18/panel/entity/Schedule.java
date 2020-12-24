package project.daihao18.panel.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

/**
 * @ClassName: Schedule
 * @Description:
 * @Author: code18
 * @Date: 2020-10-07 21:05
 */
@Data
@ToString
@TableName(value = "schedule")
public class Schedule implements Serializable {

    private Integer id;

    private String beanName;

    private String methodName;

    private String methodParams;

    private String cronExpression;

    private String remark;

    private Integer jobStatus;

    private Date createdTime;

    private Date updateTime;
}