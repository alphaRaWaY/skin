package org.mypetstore.mypetstore.exception;
import org.mypetstore.mypetstore.pojo.Result;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(Exception.class)
    public Result handleException(Exception exception) {
        // 打印异常堆栈  
        exception.printStackTrace();
        // 获取异常消息  
        String errormsg = StringUtils.hasLength(exception.getMessage()) ? exception.getMessage() : "操作失败";
       return Result.error(errormsg);
    }
}
