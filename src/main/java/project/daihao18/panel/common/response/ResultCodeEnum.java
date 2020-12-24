package project.daihao18.panel.common.response;

import lombok.Getter;

/**
 * @EnumName: ResultCodeEnum
 * @Description:
 * @Author: code18
 * @Date: 2020-10-10 10:25
 */
@Getter
public enum ResultCodeEnum {
    SUCCESS(true, 200, "成功", "Success"),
    // 客户端异常
    PARAM_ERROR(false, 4001, "参数错误", "Params error"),
    HTTP_CLIENT_ERROR(false, 4002, "http请求错误", "Http request error"),
    UNAUTHORIZED_REQUEST_ERROR(false, 4003, "非法越权访问", "Unauthorized request"),
    // 服务端异常
    UNKNOWN_ERROR(false, 5001, "未知错误", "Unknown error"),
    NULL_POINT(false, 5002, "空指针异常", "Null point exception"),
    CLOSE_REGISTER_ERROR(false, 5003, "已关闭注册", "Prohibition of registration"),
    EXIST_EMAIL_ERROR(false, 5004, "该邮箱已注册", "The email has been registered"),
    MAIL_SEND_ERROR(false, 5005, "邮件发送失败", "Fail to send the mail"),
    MAIL_SEND_LIMIT_ERROR(false, 5006, "邮件发送频率过快", "Email is sent too quickly"),
    CHECK_CODE_ERROR(false, 5007, "验证码错误", "CheckCode error"),
    USER_NOT_FIND_ERROR(false, 5008, "用户不存在", "The user doesn't exists"),
    INVALID_INVITE_CODE_ERROR(false, 5009, "邀请码不存在或邀请次数不足", "Invalid invite code"),
    EXIST_ORDER_ERROR(false, 5010, "存在未支付订单", "Have unpaid order"),
    PROHIBIT_SALES_ERROR(false, 5011, "已关闭购买", "Prohibit sales"),
    INVENTORY_SHORTAGE_ERROR(false, 5012, "库存不足", "Inventory shortage"),
    INSUFFICIENT_BALANCE_ERROR(false, 5013, "余额不足", "Insufficient balance"),
    PAYMENT_CREATE_ORDER_ERROR(false, 5014, "支付下单异常", "Create payment order error"),
    ORDER_PAID_ERROR(false, 5015, "订单已支付", "The order has been paid"),
    ;
    // 响应是否成功
    private Boolean success;
    // 响应状态码
    private Integer code;
    // 响应信息,中文
    private String message;
    // 响应信息,英文
    private String messageEnglish;

    ResultCodeEnum(boolean success, Integer code, String message, String messageEnglish) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.messageEnglish = messageEnglish;
    }
}