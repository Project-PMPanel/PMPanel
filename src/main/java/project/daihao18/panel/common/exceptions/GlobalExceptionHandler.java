package project.daihao18.panel.common.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.HttpClientErrorException;
import project.daihao18.panel.common.response.Result;
import project.daihao18.panel.common.response.ResultCodeEnum;
import project.daihao18.panel.common.utils.ExceptionUtil;

import javax.mail.MessagingException;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * -------- 通用异常处理方法 --------
     **/
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public Result error(Exception e) {
        // e.printStackTrace();
        log.error(ExceptionUtil.getMessage(e));
        return Result.error();    // 通用异常结果
    }

    /**
     * -------- 指定异常处理方法 --------
     **/
    @ExceptionHandler(NullPointerException.class)
    @ResponseBody
    public Result error(NullPointerException e) {
        e.printStackTrace();
        return Result.setResult(ResultCodeEnum.NULL_POINT);
    }

    @ExceptionHandler(HttpClientErrorException.class)
    @ResponseBody
    public Result error(HttpClientErrorException e) {
        e.printStackTrace();
        return Result.setResult(ResultCodeEnum.HTTP_CLIENT_ERROR);
    }

    @ExceptionHandler(MessagingException.class)
    @ResponseBody
    public Result error(MessagingException e) {
        e.printStackTrace();
        return Result.setResult(ResultCodeEnum.MAIL_SEND_ERROR);
    }

    /**
     * -------- 自定义定异常处理方法 --------
     **/
    @ExceptionHandler(CustomException.class)
    @ResponseBody
    public Result error(CustomException e) {
        e.printStackTrace();
        return Result.error().message(e.getMessage()).messageEnglish(e.getMessageEnglish()).code(e.getCode());
    }
}
