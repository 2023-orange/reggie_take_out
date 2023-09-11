package com.liu.reggie.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理
 */

@ControllerAdvice(annotations = {RestController.class, Controller.class})
@ResponseBody
@Slf4j
public class GlobleExceptionHandle {

    /**
     * 异常处理方法
     * @return
     */
    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public R<String> exceptionHandle(SQLIntegrityConstraintViolationException ex){
        log.error(ex.getMessage());

        if(ex.getMessage().contains("Duplicate entry")){
            String[] spilt = ex.getMessage().split(" ");
            String msg = spilt[2] + "已存在";
            return R.error(msg);
        }
        return R.error("未知错误");
    }



    /**
     * 异常处理方法,处理自定义业务异常
     * @return
     */
    @ExceptionHandler(CustomException.class)
    public R<String> exceptionHandle(CustomException ex){
        log.error(ex.getMessage());

        return R.error(ex.getMessage());
    }
}
