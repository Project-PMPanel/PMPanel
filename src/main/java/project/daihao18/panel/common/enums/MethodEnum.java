package project.daihao18.panel.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @EnumName: Method
 * @Description:
 * @Author: code18
 * @Date: 2020-10-10 16:46
 */
@Getter
@AllArgsConstructor
public enum MethodEnum {
    AES_256_CFB("aes-256-cfb"),
    AES_256_GCM("aes-256-gcm"),
    CHACHA20("chacha20"),
    ;
    private String method;
}