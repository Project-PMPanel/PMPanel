package project.daihao18.panel.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @EnumName: ObfsEnum
 * @Description:
 * @Author: code18
 * @Date: 2020-10-10 16:57
 */
@Getter
@AllArgsConstructor
public enum ObfsEnum {

    PLAIN("plain"),
    HTTP_SIMPLE("http_simple"),
    HTTP_POST("http_post"),
    TLS1_2_TICKET_AUTH("tls1.2_ticket_auth"),
    SIMPLE_OBFS_HTTP("simple_obfs_http"),
    ;

    private String obfs;
}