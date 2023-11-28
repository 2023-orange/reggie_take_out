# 瑞吉外卖
该项目是我完成的第一份整体项目，主要采用SpringBoot框架来进行完成，该项目中我们只负责后端代码开发以及单元测试。该篇属于总结篇，主要负责总结整个项目技术点和注意点。
案例来自B站黑马程序员Java项目实战《瑞吉外卖》，建议结合课程资料阅读以下内容
## 功能架构
![Image text](https://github.com/2023-orange/reggie_take_out/blob/master/%E5%8A%9F%E8%83%BD%E6%9E%B6%E6%9E%84.png)
## 技术架构
![Image text](https://github.com/2023-orange/reggie_take_out/blob/master/%E6%8A%80%E6%9C%AF%E6%9E%B6%E6%9E%84.png)
![Image text](https://github.com/2023-orange/reggie_take_out/blob/master/%E6%8A%80%E6%9C%AF%E6%9E%B6%E6%9E%84%EF%BC%881%EF%BC%89.png)
## 数据库
我们的数据库主要采用MYSQL来保存数据。
![Image text](https://github.com/2023-orange/reggie_take_out/blob/master/%E6%95%B0%E6%8D%AE%E5%BA%93%E7%BB%93%E6%9E%84%EF%BC%882%EF%BC%89.png)
![Image text](https://github.com/2023-orange/reggie_take_out/blob/master/%E6%95%B0%E6%8D%AE%E5%BA%93%E7%BB%93%E6%9E%84.png)
![Image text](https://github.com/2023-orange/reggie_take_out/blob/master/%E6%95%B0%E6%8D%AE%E5%BA%93%E7%BB%93%E6%9E%84%EF%BC%883%EF%BC%89.png)
此外，我们采用Redis数据库来进行缓存优化阶段，用于存储页面信息以及菜品信息。
## 后台代码开发
在项目开始前，我们都需要设置映射地址，让前端页面展示直接进行页面展示而不是映射在服务层方法上：
```@Configuration
@Slf4j
public class WebMvcConfig extends WebMvcConfigurationSupport {
    @Override
    protected void addResourceHandlers(ResourceHandlerRegistry registry) {
        log.info("开始进行静态资源映射...");
        registry.addResourceHandler("/backend/**").addResourceLocations("classpath:/backend/");
        registry.addResourceHandler("/front/**").addResourceLocations("classpath:/front/");
    }
}
```
之后配置一下端口号和数据库连接四要素就能访问静态页面了。
## 登录、退出功能
我们一个项目的首先实现就是来完成登录与退出，本身的逻辑很简单，但我们需要在登录退出时对页面进行部分操作：
在Session中记录或删除用户的网页信息，因为我们后期需要根据Session内部的信息来判断用户是否登录
数据库的数据和简单的SQL语句都不用我们管，数据已经提供好了，简单的SQL语句用MyBatisPlus。
```
package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.Employee;
import com.itheima.reggie.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/employee")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    /**
     * 员工登录
     * @param request
     * @param employee
     * @return
     */
    @PostMapping("/login")
    public R<Employee> login(HttpServletRequest request,@RequestBody Employee employee){

        //1、将页面提交的密码password进行md5加密处理
        String password = employee.getPassword();
        password = DigestUtils.md5DigestAsHex(password.getBytes());

        //2、根据页面提交的用户名username查询数据库
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Employee::getUsername,employee.getUsername());
        Employee emp = employeeService.getOne(queryWrapper);

        //3、如果没有查询到则返回登录失败结果
        if(emp == null){
            return R.error("登录失败");
        }

        //4、密码比对，如果不一致则返回登录失败结果
        if(!emp.getPassword().equals(password)){
            return R.error("登录失败");
        }

        //5、查看员工状态，如果为已禁用状态，则返回员工已禁用结果
        if(emp.getStatus() == 0){
            return R.error("账号已禁用");
        }

        //6、登录成功，将员工id存入Session并返回登录成功结果
        request.getSession().setAttribute("employee",emp.getId());
        return R.success(emp);
    }

    /**
     * 员工退出
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public R<String> logout(HttpServletRequest request){
        //清理Session中保存的当前登录员工的id
        request.getSession().removeAttribute("employee");
        return R.success("退出成功");
    }
```
## 实现登陆过滤
我们的前台和后台页面不能随意直接访问，所以我们需要判定用户是否登录，若登录后才可以进入页面进行操纵。  
我们采用过滤器来完成这部分功能实现，过滤器实现步骤：  
  
-创建一个过滤器类  
-继承Filter  
-设置为过滤器类@WebFilter(fileName = "loginCheckFilter",urlPatterns = "/*")  
-实现doFilter方法  
  
在其中我们还采用了一个路径匹配器：  
//路径匹配器，支持通配符 PATH_MATCHER.match(A,B);  
`public static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();`  
我们判断是否登录成功的依据来自设置登录时的Session内容：  
```
// 前台
request.getSession().getAttribute("user") != null

// 后台
request.getSession().getAttribute("employee") != null
```
代码详情
```
package com.itheima.reggie.filter;

import com.alibaba.fastjson.JSON;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 检查用户是否已经完成登录
 */
@WebFilter(filterName = "loginCheckFilter",urlPatterns = "/*")
@Slf4j
public class LoginCheckFilter implements Filter{
    //路径匹配器，支持通配符
    public static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        //1、获取本次请求的URI
        String requestURI = request.getRequestURI();// /backend/index.html

        log.info("拦截到请求：{}",requestURI);

        //定义不需要处理的请求路径
        String[] urls = new String[]{
                "/employee/login",
                "/employee/logout",
                "/backend/**",
                "/front/**",
                "/common/**",
                "/user/sendMsg",
                "/user/login"
        };

        //2、判断本次请求是否需要处理
        boolean check = check(urls, requestURI);

        //3、如果不需要处理，则直接放行
        if(check){
            log.info("本次请求{}不需要处理",requestURI);
            filterChain.doFilter(request,response);
            return;
        }

        //4-1、判断登录状态，如果已登录，则直接放行
        if(request.getSession().getAttribute("employee") != null){
            log.info("用户已登录，用户id为：{}",request.getSession().getAttribute("employee"));

            Long empId = (Long) request.getSession().getAttribute("employee");
            BaseContext.setCurrentId(empId);

            filterChain.doFilter(request,response);
            return;
        }

        //4-2、判断登录状态，如果已登录，则直接放行
        if(request.getSession().getAttribute("user") != null){
            log.info("用户已登录，用户id为：{}",request.getSession().getAttribute("user"));

            Long userId = (Long) request.getSession().getAttribute("user");
            BaseContext.setCurrentId(userId);

            filterChain.doFilter(request,response);
            return;
        }

        log.info("用户未登录");
        //5、如果未登录则返回未登录结果，通过输出流方式向客户端页面响应数据
        response.getWriter().write(JSON.toJSONString(R.error("NOTLOGIN")));
        return;

    }

    /**
     * 路径匹配，检查本次请求是否需要放行
     * @param urls
     * @param requestURI
     * @return
     */
    public boolean check(String[] urls,String requestURI){
        for (String url : urls) {
            boolean match = PATH_MATCHER.match(url, requestURI);
            if(match){
                return true;
            }
        }
        return false;
    }
}
```
## 设置工具类
工具类是为了提供一些通用的、某一非业务领域内的公共方法，不需要配套的成员变量，仅仅是作为工具方法被使用。  
项目中的工具类是借助LocalThread的当前线程储存功能来设置工具类，我们只需要定义LocalThread并给出其方法的新方法定义即可。  
我们给出项目中的实例展示：  
```
package com.itheima.reggie.common;

/**
 * 基于ThreadLocal的工具类，用于保存和获取当前登录用户id
 * 工具类方法大多数是静态方法，因为重新定义一个类需要一定内存，直接设置为静态方法可以节省内存
 */
public class BaseContext {
    private static ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    public static void setCurrentId(Long id){
        threadLocal.set(id);
    }

    public static Long getCurrentId(){
        return threadLocal.get();
    }
}
```
## 自动填充公共字段
我们在数据库中会注意到我们的各种菜品，套餐等都具有统一的参数，我们将他们称为公共字段  
同时这些字段基本需要初始化设置或者在修改更新时进行数据的更新设置，所以我们希望统一进行设置来简化操作  
我们采用MyBatisPlus提供的公共字段自动填充的功能：  
我们先来简单介绍一下流程：  
1.首先在我们需要修改的字段属性上添加注解：  
```
// 属性包括有INSERT，UPDATE，INSERT_UPDATE

@TableField(fill = FieldFill.属性)
```
2.按照框架书写元数据对象处理器，需要实现MetaObjectHandler接口  
```
package com.itheima.reggie.common;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

// 记得设置为配置类
@Slf4j
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    /**
     * 添加时自动设置
     * @param metaObject
     */
    @Override
    public void insertFill(MetaObject metaObject) {

    }

    /**
     * 修改时自动设置
     * @param metaObject
     */
    @Override
    public void updateFill(MetaObject metaObject) {

    }
}
```
3.在元数据对象处理器中对方法进行书写，在此类中统一为公共字段设置值，借助了LocalThread来获得当前用户ID  
```
package com.itheima.reggie.common;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

/**
 * 自定义元数据对象处理器
 */
@Component
@Slf4j
public class MyMetaObjecthandler implements MetaObjectHandler {
    /**
     * 插入操作，自动填充
     * @param metaObject
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        log.info("公共字段自动填充[insert]...");
        log.info(metaObject.toString());

        metaObject.setValue("createTime", LocalDateTime.now());
        metaObject.setValue("updateTime",LocalDateTime.now());
        metaObject.setValue("createUser",BaseContext.getCurrentId());
        metaObject.setValue("updateUser",BaseContext.getCurrentId());
    }

    /**
     * 更新操作，自动填充
     * @param metaObject
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        log.info("公共字段自动填充[update]...");
        log.info(metaObject.toString());

        long id = Thread.currentThread().getId();
        log.info("线程id为：{}",id);

        metaObject.setValue("updateTime",LocalDateTime.now());
        metaObject.setValue("updateUser",BaseContext.getCurrentId());
    }
}
```
## 进行类型转换
我们在项目遇到的一个简单的小问题：  

-我们的empId设计为Long型，其中数据库为19位，但网页的JS为16位，这就会导致empId传递时会出现损失  
我们通过采用消息转换器来实现传送类型发生改变：  

-使网页的Long型传递过来时变为String类型，在传递到后端之后，再变为Long型赋值给后端代码
我们的要实现消息转化器主要需要两步：  

1.设置一个消息转换器  
```
package com.itheima.reggie.common;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

/**
 * 对象映射器:基于jackson将Java对象转为json，或者将json转为Java对象
 * 将JSON解析为Java对象的过程称为 [从JSON反序列化Java对象]
 * 从Java对象生成JSON的过程称为 [序列化Java对象到JSON]
 */
public class JacksonObjectMapper extends ObjectMapper {

    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    public static final String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String DEFAULT_TIME_FORMAT = "HH:mm:ss";

    public JacksonObjectMapper() {
        super();
        //收到未知属性时不报异常
        this.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);

        //反序列化时，属性不存在的兼容处理
        this.getDeserializationConfig().withoutFeatures(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);


        SimpleModule simpleModule = new SimpleModule()
                .addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DEFAULT_DATE_TIME_FORMAT)))
                .addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT)))
                .addDeserializer(LocalTime.class, new LocalTimeDeserializer(DateTimeFormatter.ofPattern(DEFAULT_TIME_FORMAT)))

                .addSerializer(BigInteger.class, ToStringSerializer.instance)
                .addSerializer(Long.class, ToStringSerializer.instance)
                .addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DEFAULT_DATE_TIME_FORMAT)))
                .addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT)))
                .addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ofPattern(DEFAULT_TIME_FORMAT)));

        //注册功能模块 例如，可以添加自定义序列化器和反序列化器
        this.registerModule(simpleModule);
    }
}
```
2.将消息转换器设置到配置类中  
```
package com.itheima.reggie.config;

import com.github.xiaoymin.knife4j.spring.annotations.EnableKnife4j;
import com.qiuluo.reggie.common.JacksonObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.List;

@Slf4j
@Configuration
public class WebMvcConfig extends WebMvcConfigurationSupport {

    /**
     * 扩展mvc框架的消息转换器
     * @param converters
     */
    @Override
    protected void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        log.info("扩展消息转换器...");
        //创建消息转换器对象
        MappingJackson2HttpMessageConverter messageConverter = new MappingJackson2HttpMessageConverter();
        //设置对象转换器，底层使用Jackson将Java对象转为json
        messageConverter.setObjectMapper(new JacksonObjectMapper());
        //将上面的消息转换器对象追加到mvc框架的转换器集合中
        converters.add(0,messageConverter);
    }

}
```
## 实现异常处理
我们项目中的异常处理通常分为两部分：  

-系统意外异常  
-自定义业务异常  
我们在后台不可避免地发生错误，这些错误通常被称为系统意外异常  
处理系统意外异常我们只需要设置异常处理器即可：
```
package com.itheima.reggie.common;

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
public class GlobalExceptionHandler {

    /**
     * 异常处理方法
     * @return
     */
    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public R<String> exceptionHandler(SQLIntegrityConstraintViolationException ex){
        log.error(ex.getMessage());

        if(ex.getMessage().contains("Duplicate entry")){
            String[] split = ex.getMessage().split(" ");
            String msg = split[2] + "已存在";
            return R.error(msg);
        }

        return R.error("未知错误");
    }
}
``` 
程序员在后台自我设置的异常被称为自定义业务异常，通常用于业务层的功能实现无法实现时抛出异常给用户查看  
设置自定义异常主要分为两步：  
1.设置自定义异常类  
```
package com.itheima.reggie.common;

/**
 * 自定义业务异常类
 */
public class CustomException extends RuntimeException {
    public CustomException(String message){
        super(message);
    }
}
```
2.将该自定义异常加入异常处理器即可
```
package com.itheima.reggie.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理
 * @ControllerAdvice 来书写需要修改异常的注解类（该类中包含以下注解）
 * @ResponseBody 因为返回数据为JSON数据，需要进行格式转换
 */
@ControllerAdvice(annotations = {RestController.class, Controller.class})
@ResponseBody
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 处理异常
     * @ExceptionHandler 来书写需要修改的异常
     * @return
     */
    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public Result<String> exceptionHandler(SQLIntegrityConstraintViolationException ex){

        // 我们可以通过log.error输出错误提醒(我们可以得到以下提示信息：Duplicate entry '123' for key 'employee.idx_username')
        log.error(ex.getMessage());
        // 我们希望将id：123提取出来做一个简单的反馈信息
        if (ex.getMessage().contains("Duplicate entry")){
            String[] split = ex.getMessage().split(" ");
            String msg = split[2] + "已存在";
            return Result.error(msg);
        }
        return Result.error("未知错误");
    }

    /**
     * 处理自定义异常
     * @ExceptionHandler 来书写需要修改的异常
     * @return
     */
    @ExceptionHandler(CustomException.class)
    public Result<String> CustomExceptionHandler(CustomException ex){

        log.error(ex.getMessage());

        return Result.error(ex.getMessage());
    }
}
```

