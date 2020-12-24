package project.daihao18.panel.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @EnumName: ProtocolEnum
 * @Description:
 * @Author: code18
 * @Date: 2020-10-10 16:53
 */
@Getter
@AllArgsConstructor
public enum ProtocolEnum {
    ORIGIN("origin"),
    VERIFY_SIMPLE("verify_simple"),
    VERIFY_DEFLATE("verify_deflate"),
    VERIFY_SHA1("verify_sha1"),
    AUTH_SHA1_V2("auth_sha1_v2"),
    AUTH_SHA1_V4("auth_sha1_v4"),
    AUTH_AES128_MD5("auth_aes128_md5"),
    AUTH_AES128_SHA1("auth_aes128_sha1"),
    ;

    private String protocol;
}