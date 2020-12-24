package project.daihao18.panel.common.exceptions;

import lombok.Data;
import project.daihao18.panel.common.response.ResultCodeEnum;

@Data
public class CustomException extends RuntimeException {
    private Integer code;

    private String messageEnglish;

    public CustomException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public CustomException(ResultCodeEnum resultCodeEnum) {
        super(resultCodeEnum.getMessage());
        this.code = resultCodeEnum.getCode();
        this.messageEnglish = resultCodeEnum.getMessageEnglish();
    }

    @Override
    public String toString() {
        return "CMSException{" + "code=" + code + ", message=" + this.getMessage() + '}';
    }
}
