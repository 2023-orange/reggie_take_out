package com.liu.reggie.common;

/**
 * 基于ThreadLocal封装的工具类，用于保存和获取当前登录用户的id,作用范围是当前线程之内
 */
public class BaseContext {
    private static ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    /**
     * 设置值
     * @param id
     */
    public static void setCurrentId(Long id){
        threadLocal.set(id);
    }

    /**
     * 获取值
     * @return
     */
    public static Long getCurrentId(){
        return threadLocal.get();
    }
}
