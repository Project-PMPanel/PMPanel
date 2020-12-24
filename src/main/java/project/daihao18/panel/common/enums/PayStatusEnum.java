package project.daihao18.panel.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @EnumName: PayStatusEnum
 * @Description:
 * @Author: code18
 * @Date: 2020-11-01 12:29
 */
@Getter
@AllArgsConstructor
public enum PayStatusEnum {
    WAIT_FOR_PAY(0),
    SUCCESS(1),
    CANCELED(2),
    INVALID(3),
    ;

    private Integer status;
}