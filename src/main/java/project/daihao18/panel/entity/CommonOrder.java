package project.daihao18.panel.entity;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @ClassName: CommonOrder
 * @Description:
 * @Author: code18 
 * @Date: 2020-10-07 21:05
 */
@Data
@ToString
public class CommonOrder implements Serializable {

    private String id;

    private BigDecimal payAmount;

    private String payType;

    private String type;

    /**
     * pc or h5
     */
    private String platform;

    private String domain;

    private static final long serialVersionUID = 1L;
}