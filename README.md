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
# 后台代码开发
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
## 文件上传、下载
我们的文件上传下载操作之前主要依靠Apache的两个组件：commons-fileupload 和 commons-io  

现在我们的文件上传下载经过简化可以采用简单的方法来实现  

首先我们给出文件上传代码：  
```
package com.itheima.reggie.controller;

import com.qiuluo.reggie.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.CoyoteOutputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.UUID;

/*
我们通过MultipartFile获得文件，但这个文件是暂时性的，我们需要把他保存在服务器中
*/

@Slf4j
@RestController
@RequestMapping("/common")
public class CommonController {

    // 定义主路径（在yaml中配置一个自定义路径即可）
    @Value("${reggie.path}")
    private String BasePath;

    /**
     * 上传操作
     * @param file 注意需要与前端传来的数据名一致
     * @return
     */
    @PostMapping("/upload")
    public Result<String> upload(MultipartFile file){
        // 注意：file只是一个临时文件，当我们的request请求结束时，file也会消失，所以我们需要将它保存起来

        // 这个方法可以获得文件的原名称，但不推荐设置为文件名保存（因为可能出现重复名称导致文件覆盖）
        String originalFilename = file.getOriginalFilename();

        // 将原始文件的后缀截取下来
        String substring = originalFilename.substring(originalFilename.lastIndexOf("."));

        // UUID生成随机名称，文件名设置为 UUID随机值+源文件后缀
        String fileName = UUID.randomUUID().toString() + substring;

        // 判断文件夹是否存在，若不存在需创建一个
        File dir = new File(BasePath);

        if (!dir.exists()){
            dir.mkdirs();
        }

        // 这个方法可以转载文件到指定目录
        try {
            file.transferTo(new File(BasePath + fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Result.success(fileName);
    }
}
```
文件下载
```
package com.qiuluo.reggie.controller;

import com.qiuluo.reggie.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.CoyoteOutputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/common")
public class CommonController {

    @Value("${reggie.path}")
    private String BasePath;

    /**
     * 文件下载
     * @param name
     * @param response
     * @return
     */
    @GetMapping("/download")
    public void download(String name, HttpServletResponse response){

        try {
            // 输入流获得数据
            FileInputStream fileInputStream = new FileInputStream(new File(BasePath + name));

            // 输出流写出数据
            ServletOutputStream outputStream = response.getOutputStream();

            // 设置文件类型(可设可不设)
            response.setContentType("image/jpeg");

            // 转载数据
            int len = 0;
            byte[] bytes = new byte[1024];
            while ((len = fileInputStream.read(bytes)) != -1){
                outputStream.write(bytes,0,len);
                outputStream.flush();
            }

            // 关闭数据
            fileInputStream.close();
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```
## 简单功能开发
我们的项目中大多都是简单功能(单表)，可以直接根据MyBatisPlus提供的基本方法完成，我们在这里介绍简单模板：  
1.在项目中查看该方法的请求信息  
![Image text](https://github.com/2023-orange/reggie_take_out/blob/master/%E8%AF%B7%E6%B1%82%E4%BF%A1%E6%81%AF%EF%BC%88%E7%AE%80%E5%8D%95%EF%BC%89.png)
2.在项目中查看该方法的请求数据
![Image text](https://github.com/2023-orange/reggie_take_out/blob/master/%E8%AF%B7%E6%B1%82%E6%95%B0%E6%8D%AE%EF%BC%88%E7%AE%80%E5%8D%95%EF%BC%89.png)
3.实现实体类
```
package com.itheima.reggie.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 分类
 */
@Data
public class Category implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //类型 1 菜品分类 2 套餐分类
    private Integer type;


    //分类名称
    private String name;


    //顺序
    private Integer sort;


    //创建时间
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;


    //更新时间
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;


    //创建人
    @TableField(fill = FieldFill.INSERT)
    private Long createUser;


    //修改人
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateUser;

}
```
4.实现业务层接口
```
package com.itheima.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.reggie.entity.Category;

public interface CategoryService extends IService<Category> {
}

```
5.实现业务层
```
package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.mapper.CategoryMapper;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.DishService;
import com.itheima.reggie.service.SetmealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper,Category> implements CategoryService{

}
```
6.实现服务层
```
package com.qiuluo.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qiuluo.reggie.common.Result;
import com.qiuluo.reggie.domain.Category;
import com.qiuluo.reggie.service.impl.CategoryServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/category")
public class CategoryController {

    @Autowired
    private CategoryServiceImpl categoryService;

    @PostMapping
    public Result<String> save(@RequestBody Category category){
        categoryService.save(category);
        return Result.success("新增成功");
    }

}
```
## 复杂功能开发
有时候我们的MyBatisPlus提供的简单方法不足以满足我们的需求，这时我们就需要采用MyBatis的原始方法来定义方法完成功能开发  
例如我们的需求中需要进行部分判断或操作两个数据表，我们需要创建新方法来完成新功能的开发：  
1.在项目中查看该方法的请求信息
![Image text](https://github.com/2023-orange/reggie_take_out/blob/master/%E8%AF%B7%E6%B1%82%E4%BF%A1%E6%81%AF%EF%BC%88%E5%A4%8D%E6%9D%82%EF%BC%89.png)
2.在项目中查看该方法的请求数据
![Image text](https://github.com/2023-orange/reggie_take_out/blob/master/%E8%AF%B7%E6%B1%82%E6%95%B0%E6%8D%AE%EF%BC%88%E5%A4%8D%E6%9D%82%EF%BC%89.png)
3.在业务层接口定义方法
```
package com.itheima.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.reggie.entity.Category;

public interface CategoryService extends IService<Category> {

    public void remove(Long id);

}
```
4.在业务层实现方法
```
package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.mapper.CategoryMapper;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.DishService;
import com.itheima.reggie.service.SetmealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper,Category> implements CategoryService{

    @Autowired
    private DishService dishService;

    @Autowired
    private SetmealService setmealService;

    /**
     * 根据id删除分类，删除之前需要进行判断
     * @param id
     */
    @Override
    public void remove(Long id) {
        LambdaQueryWrapper<Dish> dishLambdaQueryWrapper = new LambdaQueryWrapper<>();
        //添加查询条件，根据分类id进行查询
        dishLambdaQueryWrapper.eq(Dish::getCategoryId,id);
        int count1 = dishService.count(dishLambdaQueryWrapper);

        //查询当前分类是否关联了菜品，如果已经关联，抛出一个业务异常
        if(count1 > 0){
            //已经关联菜品，抛出一个业务异常
            throw new CustomException("当前分类下关联了菜品，不能删除");
        }

        //查询当前分类是否关联了套餐，如果已经关联，抛出一个业务异常
        LambdaQueryWrapper<Setmeal> setmealLambdaQueryWrapper = new LambdaQueryWrapper<>();
        //添加查询条件，根据分类id进行查询
        setmealLambdaQueryWrapper.eq(Setmeal::getCategoryId,id);
        int count2 = setmealService.count();
        if(count2 > 0){
            //已经关联套餐，抛出一个业务异常
            throw new CustomException("当前分类下关联了套餐，不能删除");
        }

        //正常删除分类
        super.removeById(id);
    }
}
```
5.在服务层使用方法
```
package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类管理
 */
@RestController
@RequestMapping("/category")
@Slf4j
public class CategoryController {
    @Autowired
    private CategoryService categoryService;
    /**
     * 根据id删除分类
     * @param id
     * @return
     */
    @DeleteMapping
    public R<String> delete(Long id){
        log.info("删除分类，id为：{}",id);

        //categoryService.removeById(id);
        categoryService.remove(id);

        return R.success("分类信息删除成功");
    }
}
```
## DTO的使用
我们在实际开发中，其操作可能会同时兼顾两张数据表，这时我们就需要采用DTO并且采用复杂功能开发来重新定义方法  
首先我们先来讲解DTO的具体使用：  
1.首先我们需要一张数据表的实体类
```
package com.itheima.reggie.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 菜品
 */
@Data
public class Dish implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;


    //菜品名称
    private String name;


    //菜品分类id
    private Long categoryId;


    //菜品价格
    private BigDecimal price;


    //商品码
    private String code;


    //图片
    private String image;


    //描述信息
    private String description;


    //0 停售 1 起售
    private Integer status;


    //顺序
    private Integer sort;


    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;


    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;


    @TableField(fill = FieldFill.INSERT)
    private Long createUser;


    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateUser;

}
```
2.我们根据实际需求，在实体类的基础上，添加一些其他属性  
```
package com.itheima.reggie.dto;

import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.DishFlavor;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;


// 在Dish的基础上添加了DishFlavor数据表，以及categoryName所属分类名

@Data
public class DishDto extends Dish {

    //菜品对应的口味数据
    private List<DishFlavor> flavors = new ArrayList<>();

    private String categoryName;

    private Integer copies;
}
```
3.然后我们在业务层使用时，就可以引入DTO类作为参数，对内部数据进行操作
```
package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.DishFlavor;
import com.itheima.reggie.mapper.DishMapper;
import com.itheima.reggie.service.DishFlavorService;
import com.itheima.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DishServiceImpl extends ServiceImpl<DishMapper,Dish> implements DishService {

    @Autowired
    private DishFlavorService dishFlavorService;

    /**
     * 新增菜品，同时保存对应的口味数据
     * @param dishDto
     */
    @Transactional
    public void saveWithFlavor(DishDto dishDto) {
        //保存菜品的基本信息到菜品表dish
        this.save(dishDto);

        Long dishId = dishDto.getId();//菜品id

        //菜品口味
        List<DishFlavor> flavors = dishDto.getFlavors();
        flavors = flavors.stream().map((item) -> {
            item.setDishId(dishId);
            return item;
        }).collect(Collectors.toList());

        //保存菜品口味数据到菜品口味表dish_flavor
        dishFlavorService.saveBatch(flavors);

    }

    /**
     * 根据id查询菜品信息和对应的口味信息
     * @param id
     * @return
     */
    public DishDto getByIdWithFlavor(Long id) {
        //查询菜品基本信息，从dish表查询
        Dish dish = this.getById(id);

        DishDto dishDto = new DishDto();
        BeanUtils.copyProperties(dish,dishDto);

        //查询当前菜品对应的口味信息，从dish_flavor表查询
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishFlavor::getDishId,dish.getId());
        List<DishFlavor> flavors = dishFlavorService.list(queryWrapper);
        dishDto.setFlavors(flavors);

        return dishDto;
    }

    @Override
    @Transactional
    public void updateWithFlavor(DishDto dishDto) {
        //更新dish表基本信息
        this.updateById(dishDto);

        //清理当前菜品对应口味数据---dish_flavor表的delete操作
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper();
        queryWrapper.eq(DishFlavor::getDishId,dishDto.getId());

        dishFlavorService.remove(queryWrapper);

        //添加当前提交过来的口味数据---dish_flavor表的insert操作
        List<DishFlavor> flavors = dishDto.getFlavors();

        flavors = flavors.stream().map((item) -> {
            item.setDishId(dishDto.getId());
            return item;
        }).collect(Collectors.toList());

        dishFlavorService.saveBatch(flavors);
    }
}
```
# 用户端代码开发
## 短信发送技术
我们的短信发送技术的原理其实很简单：  

-自定义生成验证码并暂时保存  
-将验证码通过短信服务发给用户手机  
-用户收到后填写进行比对判断是否登陆成功  
其实黑马这里用的是短信业务，但咱也没那条件，所以我只能自己换成QQ邮箱验证码了，这个简单，具体操作我们也只需要开启POP3/STMP服务，获取一个16位的授权码  
为了方便用户登录，移动端通常都会提供通过手机验证码登录的功能(咱平替成邮箱验证码)  
手机（邮箱）验证码登录的优点：  

-方便快捷，无需注册，直接登录  
-使用短信验证码作为登录凭证，无需记忆密码  
-安全  
-登录流程:  

输入手机号（邮箱） > 获取验证码 > 输入验证码 > 点击登录 > 登录成功  
在开发业务功能之前，我们先将要用到的类和接口的基本结构都创建好  
-实体类User  
-Mapper接口UserMapper  
-业务层接口UserService  
-业务层实现类UserServiceImpl  
-控制层UserController  
## 短信发送实现
最后我们再来介绍整个短信发送流程：  

1.制作工具类生成四位随机数  
```
package com.itheima.reggie.utils;

import java.util.Random;

/**
 * 随机生成验证码工具类
 */
public class ValidateCodeUtils {
    /**
     * 随机生成验证码
     * @param length 长度为4位或者6位
     * @return
     */
    public static Integer generateValidateCode(int length){
        Integer code =null;
        if(length == 4){
            code = new Random().nextInt(9999);//生成随机数，最大为9999
            if(code < 1000){
                code = code + 1000;//保证随机数为4位数字
            }
        }else if(length == 6){
            code = new Random().nextInt(999999);//生成随机数，最大为999999
            if(code < 100000){
                code = code + 100000;//保证随机数为6位数字
            }
        }else{
            throw new RuntimeException("只能生成4位或6位数字验证码");
        }
        return code;
    }

    /**
     * 随机生成指定长度字符串验证码
     * @param length 长度
     * @return
     */
    public static String generateValidateCode4String(int length){
        Random rdm = new Random();
        String hash1 = Integer.toHexString(rdm.nextInt());
        String capstr = hash1.substring(0, length);
        return capstr;
    }
}
```
2.实现用户发送短信功能
```
package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.User;
import com.itheima.reggie.service.UserService;
import com.itheima.reggie.utils.SMSUtils;
import com.itheima.reggie.utils.ValidateCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.Map;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 发送手机短信验证码
     * @param user
     * @return
     */
    @PostMapping("/sendMsg")
    public R<String> sendMsg(@RequestBody User user, HttpSession session){
        //获取手机号
        String phone = user.getPhone();

        if(StringUtils.isNotEmpty(phone)){
            //生成随机的4位验证码
            String code = ValidateCodeUtils.generateValidateCode(4).toString();
            log.info("code={}",code);

            //调用阿里云提供的短信服务API完成发送短信
            //SMSUtils.sendMessage("瑞吉外卖","",phone,code);

            //需要将生成的验证码保存到Session
            session.setAttribute(phone,code);

            return R.success("手机验证码短信发送成功");
        }

        return R.error("短信发送失败");
    }
}
```
3.完成比对验证码用户登录功能
```
package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.User;
import com.itheima.reggie.service.UserService;
import com.itheima.reggie.utils.SMSUtils;
import com.itheima.reggie.utils.ValidateCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.Map;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 移动端用户登录
     * @param map
     * @param session
     * @return
     */
    @PostMapping("/login")
    public R<User> login(@RequestBody Map map, HttpSession session){
        log.info(map.toString());

        //获取手机号
        String phone = map.get("phone").toString();

        //获取验证码
        String code = map.get("code").toString();

        //从Session中获取保存的验证码
        Object codeInSession = session.getAttribute(phone);

        //进行验证码的比对（页面提交的验证码和Session中保存的验证码比对）
        if(codeInSession != null && codeInSession.equals(code)){
            //如果能够比对成功，说明登录成功

            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getPhone,phone);

            User user = userService.getOne(queryWrapper);
            if(user == null){
                //判断当前手机号对应的用户是否为新用户，如果是新用户就自动完成注册
                user = new User();
                user.setPhone(phone);
                user.setStatus(1);
                userService.save(user);
            }
            session.setAttribute("user",user.getId());
            return R.success(user);
        }
        return R.error("登录失败");
    }

}
```
## Redis缓存技术
我们在菜品选择界面会发现有很多套餐分类菜品数据，如果访问人数过多，数据库访问次数过多会导致系统崩毁  
所以我们希望将相关重要的数据进行缓存，同时为了保证前台后台数据一致的前提下，我们采用Redis来实现缓存技术  
## Redis环境搭建
首先我们来回顾Redis基础环境搭建：  
1.导入Redis相关依赖坐标
```
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
```
2.配置Redis相关信息  
```
server:
  port: 8080
  
#redis设置在spring下
server:
  port: 8080
spring:
  #应用的名称，可选
  application:
    name: reggie_take_out
  datasource:
    druid:
      driver-class-name: com.mysql.cj.jdbc.Driver
      url: jdbc:mysql://localhost:3306/reggie?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8&zeroDateTimeBehavior=convertToNull&useSSL=false&allowPublicKeyRetrieval=true
      username: root
      password: 1305174214
  redis:
    host: 127.0.0.1
    port: 6379
    database: 0
    password: 1305174214
  cache:
    redis:
      time-to-live: 1800000 #设置过期时间，注意单位是毫秒
mybatis-plus:
  configuration:
    #在映射实体或者属性时，将数据库中表名和字段名中的下划线去掉，按照驼峰命名法映射
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      id-type: ASSIGN_ID
reggie:
  path: D:\Desktop\Reggie\
```
3.配置序列化配置类
```
package com.liu.reggie.config;

// 我们希望在Redis数据库中可以直接查看到key的原始名称，所以我们需要修改其序列化方法

import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig extends CachingConfigurerSupport {
    @Bean
    public RedisTemplate<Object,Object> redisTemplate(RedisConnectionFactory connectionFactory){
        RedisTemplate<Object,Object> redisTemplate = new RedisTemplate<>();
        //默认的key序列化器为：JdkSerializationRedisSerializer
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setConnectionFactory(connectionFactory);
        return redisTemplate;
    }
}
```
## Redis基本操作
在完成上述环境搭建操作之后，我们就可以来实现RedisTemplate的自动装配，然后我们就可以采用RedisTemplate来实现Redis操作  
```
@Autowired
private RedisTemplate redisTemplate;
```
我们项目中以Dish为例来完成了Redis的基本菜品缓存操作：
```
package com.liu.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liu.reggie.common.CustomException;
import com.liu.reggie.common.R;
import com.liu.reggie.dto.DishDto;
import com.liu.reggie.entity.Category;
import com.liu.reggie.entity.Dish;
import com.liu.reggie.entity.DishFlavor;
import com.liu.reggie.service.CategoryService;
import com.liu.reggie.service.DishFlavorService;
import com.liu.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 菜品管理
 */
@RestController
@RequestMapping("/dish")
@Slf4j
public class DishController {
    @Autowired
    private DishService dishService;

    @Autowired
    private DishFlavorService dishFlavorService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 根据条件查询对应的菜品数据
     * @param dish
     * @return
     */
//    @GetMapping("/list")
//    public R<List<Dish>> list(Dish dish){
//        //构造查询条件
//        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
//        queryWrapper.eq(dish.getCategoryId() != null,Dish::getCategoryId,dish.getCategoryId());
//        //添加条件，查询状态为1（起售状态）这些菜品
//        queryWrapper.eq(Dish::getStatus,1);
//        //添加排序条件
//        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);
//
//        List<Dish> list = dishService.list(queryWrapper);
//
//        return R.success(list);
//    }

    @GetMapping("/list")
    public R<List<DishDto>> list(Dish dish){
        List<DishDto> dishDtoList =null;
        //动态构造key
        String key = "dish_" + dish.getCategoryId() + "_" + dish.getStatus();//dish_id_1
        //先从Redis中获取缓存数据
        dishDtoList = (List<DishDto>) redisTemplate.opsForValue().get(key);

        if (dishDtoList != null){
            //如果存在，则直接返回，无需查询数据库
            return R.success(dishDtoList);
        }

        //构造查询条件
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(dish.getCategoryId() != null,Dish::getCategoryId,dish.getCategoryId());
        //添加条件，查询状态为1（起售状态）这些菜品
        queryWrapper.eq(Dish::getStatus,1);
        //添加排序条件
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);

        List<Dish> list = dishService.list(queryWrapper);

        dishDtoList = list.stream().map((item)->{
            DishDto dishDto = new DishDto();

            BeanUtils.copyProperties(item,dishDto);
            Long categoryId = item.getCategoryId();//分类ID
            //根据ID查询分类对象
            Category category = categoryService.getById(categoryId);
            if (category != null){
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }

            //当前菜品Id
            Long dishId = item.getId();
            LambdaQueryWrapper<DishFlavor> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(DishFlavor::getDishId,dishId);
            //select * from dish_flavor where dish_id=?
            List<DishFlavor> dishFlavorList = dishFlavorService.list(lambdaQueryWrapper);
            dishDto.setFlavors(dishFlavorList);

            return dishDto;
        }).collect(Collectors.toList());
        //如果不存在，需要查询数据库，将查询到的菜品信息缓存到Redis
        redisTemplate.opsForValue().set(key,dishDtoList,60, TimeUnit.MINUTES);
        return R.success(dishDtoList);
    }

}
```
同时为了保证前后台数据一致，我们在后台进行数据修改时，需要将缓存消除，使前台再次从MYSQL中读取数据：
```
package com.liu.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liu.reggie.common.CustomException;
import com.liu.reggie.common.R;
import com.liu.reggie.dto.DishDto;
import com.liu.reggie.entity.Category;
import com.liu.reggie.entity.Dish;
import com.liu.reggie.entity.DishFlavor;
import com.liu.reggie.service.CategoryService;
import com.liu.reggie.service.DishFlavorService;
import com.liu.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 菜品管理
 */
@RestController
@RequestMapping("/dish")
@Slf4j
public class DishController {
    @Autowired
    private DishService dishService;

    @Autowired
    private DishFlavorService dishFlavorService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 新增菜品
     * @param dishDto
     * @return
     */
    @PostMapping
    public R<String> save(@RequestBody DishDto dishDto){
        log.info(dishDto.toString());

        dishService.saveWithFlavor(dishDto);

        //清理某个分类下面的菜品缓存
        String key = "dish_" + dishDto.getCategoryId() + "_1";
        redisTemplate.delete(key);
        return R.success("新增菜品成功");
    }

    /**
     * 菜品信息分页查询
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page,int pageSize,String name){

        //构造分页构造器
        Page<Dish> pageInfo = new Page<>(page,pageSize);
        Page<DishDto> dishDtoPage = new Page<>();

        //条件构造器
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        //添加过滤条件
        queryWrapper.like(name != null,Dish::getName,name);
        //添加排序条件
        queryWrapper.orderByDesc(Dish::getUpdateTime);

        //执行分页查询
        dishService.page(pageInfo,queryWrapper);

        //对象拷贝
        BeanUtils.copyProperties(pageInfo,dishDtoPage,"records");

        List<Dish> records = pageInfo.getRecords();
        List<DishDto> list = records.stream().map((item)->{
            DishDto dishDto = new DishDto();

            BeanUtils.copyProperties(item,dishDto);


            Long categoryId = item.getCategoryId();//分类ID
            //根据ID查询分类对象
            Category category = categoryService.getById(categoryId);
            if (category != null){
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }

            return dishDto;
        }).collect(Collectors.toList());

        dishDtoPage.setRecords(list);

        return R.success(dishDtoPage);
    }

    /**
     * 根据id查询菜品信息和口味信息
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<DishDto> get(@PathVariable Long id){
        DishDto dishDto = dishService.getByIdWithFlavor(id);

        return R.success(dishDto);
    }

    /**
     * 新增菜品
     * @param dishDto
     * @return
     */
    @PutMapping
    public R<String> update(@RequestBody DishDto dishDto){
        log.info(dishDto.toString());

        dishService.updateWithFlavor(dishDto);

        //清理某个分类下面的菜品缓存
        String key = "dish_" + dishDto.getCategoryId() + "_1";
        redisTemplate.delete(key);

        return R.success("修改菜品成功");
    }

    /**
     * 商品为启售状态，其status为1，但点击停售按钮时，发送的status为0，
     * 前端是直接对这个status取反了，
     * 我们直接用发送的这个status来更新我们的商品状态就好了，不用在后端再次进行判断
     * @param status
     * @param ids
     * @return
     */
    @PostMapping("/status/{status}")
    public R<String> updateStatus(@PathVariable Integer status,@RequestParam List<Long> ids){
        log.info("status:{},ids:{}",status,ids);
        //创建了一个用于更新 Dish 实体的条件包装器对象 updateWrapper。
        LambdaUpdateWrapper<Dish> updateWrapper= new LambdaUpdateWrapper<>();
        //构建了一个条件，该条件表示在 Dish 表中，如果 ids 列表不为 null，那么更新的操作将仅应用于那些在 ids 列表中的记录。
        updateWrapper.in(ids != null,Dish::getId,ids);
        //设置了需要更新的字段
        updateWrapper.set(Dish::getStatus,status);
        //执行更新操作
        dishService.update(updateWrapper);
        return R.success("批量操作成功");
    }

    @DeleteMapping
    public R<String> deleteDish(@RequestParam List<Long> ids){
        DishDto dishDto = new DishDto();
        log.info("ids:{}",ids);
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Dish::getId,ids);
        queryWrapper.eq(Dish::getStatus,1);
        int count = dishService.count(queryWrapper);
        if (count > 0){
            throw new CustomException("删除列表中存在启售状态商品，无法删除");
        }
        dishService.removeByIds(ids);
        //清理某个分类下面的菜品缓存
        String key = "dish_" + dishDto.getCategoryId() + "_1";
        redisTemplate.delete(key);
        return R.success("批量删除成功");
    }
}
```
### Redis高级操作
Redis为我们提供了一种注解缓存的方法来简化操作，主要依赖于框架Spring Cache  
Spring Cache提供了一层抽象，底层可以切换不同的Cache实现，我们主要使用RedisCacheManager这个接口来完成操作  
我们来介绍Spring Cache用于缓存的常用的四个注解：  
@EnableCaching	开启缓存注解功能  

@Cacheable	在方法执行前先查看缓存中是否存有数据，如果有数据直接返回数据；如果没有，调用方法并将返回值存入缓存  

@CachePut	将方法的返回值放到缓存  

@CacheEvict	将一条或多条从缓存中删除  

下面我们来介绍Spring Cache的具体实现步骤：
1.导入相关依赖坐标
```
        <!--Cache坐标-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-cache</artifactId>
        </dependency>
```
2.在配置文件中统一设置过期时间
```
server:
  port: 8080
spring:
  #应用的名称，可选
  application:
    name: reggie_take_out
  datasource:
    druid:
      driver-class-name: com.mysql.cj.jdbc.Driver
      url: jdbc:mysql://localhost:3306/reggie?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8&zeroDateTimeBehavior=convertToNull&useSSL=false&allowPublicKeyRetrieval=true
      username: root
      password: 1305174214
  redis:
    host: 127.0.0.1
    port: 6379
    database: 0
    password: 1305174214
  cache:
    redis:
      time-to-live: 1800000 #设置过期时间，注意单位是毫秒
mybatis-plus:
  configuration:
    #在映射实体或者属性时，将数据库中表名和字段名中的下划线去掉，按照驼峰命名法映射
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      id-type: ASSIGN_ID
reggie:
  path: D:\Desktop\Reggie\
```
3.在启动类上添加@EnableCaching注解，开启缓存注解功能
```
package com.liu.reggie;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Slf4j
@SpringBootApplication
@ServletComponentScan
@EnableTransactionManagement
@EnableCaching  //开启springcache注解方式缓存功能
public class ReggieApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReggieApplication.class,args);
        log.info("项目启动成功...");
    }
}
```
4.在SetmealController的list方法上加上@Cacheable注解
```
package com.liu.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liu.reggie.common.R;
import com.liu.reggie.dto.DishDto;
import com.liu.reggie.dto.SetmealDto;
import com.liu.reggie.entity.Category;
import com.liu.reggie.entity.Dish;
import com.liu.reggie.entity.Setmeal;
import com.liu.reggie.entity.SetmealDish;
import com.liu.reggie.service.CategoryService;
import com.liu.reggie.service.DishService;
import com.liu.reggie.service.SetmealDishService;
import com.liu.reggie.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 套餐管理
 */

@RestController
@Slf4j
@RequestMapping("/setmeal")
public class SetmealController {

    @Autowired
    private SetmealService setmealService;

    @Autowired
    private SetmealDishService setmealDishService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private DishService dishService;

    /**
     * 根据条件查询套餐数据
     * @param setmeal
     * @return
     */
    @GetMapping("/list")
    @Cacheable(value = "setmealCache",key = "#setmeal.categoryId + '_' + #setmeal.status")
    public R<List<Setmeal>> list(Setmeal setmeal){
        log.info(setmeal.toString());
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(setmeal.getCategoryId() != null,Setmeal::getCategoryId,setmeal.getCategoryId());
        queryWrapper.eq(setmeal.getStatus() != null,Setmeal::getStatus,setmeal.getStatus());
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);
        List<Setmeal> list =list = setmealService.list(queryWrapper);
        return R.success(list);
    }

    @GetMapping("/dish/{id}")
    public R<List<DishDto>> showSetmealDish(@PathVariable Long id){
        //条件构造器
        LambdaQueryWrapper<SetmealDish> wrapper = new LambdaQueryWrapper<>();
        //手里的数据只有setmealId
        wrapper.eq(SetmealDish::getSetmealId,id);
        //查询数据
        List<SetmealDish> setmealDishList = setmealDishService.list(wrapper);
        List<DishDto> dtoList = setmealDishList.stream().map((item) -> {
            DishDto dishDto = new DishDto();
            //copy数据
            BeanUtils.copyProperties(item,dishDto);
            //查询对应菜品id
            Long dishId = item.getDishId();
            //根据菜品id获取具体菜品数据，这里要自动装配 dishService
            Dish dish = dishService.getById(dishId);
            //其实主要数据是要那个图片，不过我们这里多copy一点也没事
            BeanUtils.copyProperties(dish,dishDto);
            return dishDto;
        }).collect(Collectors.toList());
        return R.success(dtoList);
    }

}
```
5.在SetmealController的save，update，delete方法上加上@CacheEvict注解
```
package com.liu.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liu.reggie.common.R;
import com.liu.reggie.dto.DishDto;
import com.liu.reggie.dto.SetmealDto;
import com.liu.reggie.entity.Category;
import com.liu.reggie.entity.Dish;
import com.liu.reggie.entity.Setmeal;
import com.liu.reggie.entity.SetmealDish;
import com.liu.reggie.service.CategoryService;
import com.liu.reggie.service.DishService;
import com.liu.reggie.service.SetmealDishService;
import com.liu.reggie.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 套餐管理
 */


@Slf4j
@RestController
@RequestMapping("/setmeal")
public class SetmealController {

    @Autowired
    private DishServiceImpl dishService;

    @Autowired
    private SetmealServiceImpl setmealService;

    @Autowired
    private SetmealDishServiceImpl setmealDishService;

    @Autowired
    private CategoryServiceImpl categoryService;

    /**
     * 新增
     * @CacheEvict:删除缓存功能，allEntries = true表示删除该value类型的所有缓存
     * @param setmealDto
     * @return
     */
    @CacheEvict(value = "setmealCache",allEntries = true)
    @PostMapping
    public Result<String> save(@RequestBody SetmealDto setmealDto){

        setmealService.saveWithDish(setmealDto);

        log.info("套餐新增成功");

        return Result.success("新创套餐成功");
    }

     /**
     * 修改
     * @CacheEvict:删除缓存功能，allEntries = true表示删除该value类型的所有缓存
     * @param setmealDto
     * @return
     */
    @PutMapping
    @CacheEvict(value = "setmealCache",allEntries = true)
    public Result<String> update(@RequestBody SetmealDto setmealDto){

        setmealService.updateById(setmealDto);

        return Result.success("修改成功");
    }
    
    /**
     * 删除
     * @CacheEvict:删除缓存功能，allEntries = true表示删除该value类型的所有缓存
     * @param ids
     * @return
     */
    @CacheEvict(value = "setmealCache",allEntries = true)
    @DeleteMapping
    public Result<String> delete(@RequestParam List<Long> ids){

        setmealService.removeWithDish(ids);

        return Result.success("删除成功");
    }
}
```
# 项目部署阶段
## 数据库读写分离
数据库的读写分离操作相对而言比较简单，但前置的mysql主从复制相对比较繁琐  
## 主从复制
我们先来介绍主从复制的具体流程：  
1.主库从库设置固定ID，并且给主库设置日志打开  
```
# 进入配置文件
vim /etc/my.cnf

# 主库设置
[mysqld]
log-bin=mysql-bin # 启动二进制日志
server-id=128 # 设置服务器唯一ID

# 从库设置
server-id=129 # 设置服务器唯一ID

# 记得刷新数据库服务
systemctl restart mysqld
```
2.主库创建用户并记录日志当前状况
```
# 登录数据库
mysql -uroot -p123456

# 执行下列语句（生成一个用户，使其具有查询日志的权力）
GRANT REPLICATION SLAVE ON *.* to 'xiaoming'@'%' identified by 'Root@123456';

# 执行语句,你将会看到File和Position信息，该页面不要改变
# (你将会看到日志相关信息，接下来不要对数据库操作，因为操作会导致日志信息改变)
show master status;
```
3.从库使用用户连接主库并记录日志信息，实现slave同步
```
# 执行下列语句（使用该用户查询日志，注意内容是需要修改的）
# master_host主库IP，master_user主库用户，master_password主库用户密码，master_log_file，master_log_pos为日志信息
change master to
master_host='192.168.44.128',master_user='xiaoming',master_password='Root@123456',master_log_file='mysql-bin.000001',master_log_pos=439;

# 输入后执行以下语句开启slave
start slave;

# 如果显示slave冲突（如果你之前执行过slave），使用下列方法结束之前slave
stop slave;
```
4.从库查看主从复制是否成功
```
# 查看语句
show slave starts\G;

# 我们只需要关注三个点：（为下述即为成功）
Slave_IO_State: Waiting for master to send event
Slave_IO_Running: Yes
Slave_SQL_Running: Yes
```
## 读写分离
我们再来介绍读写分离的具体流程：  

1.导入Sharding-JDBC的maven坐标
```
        <!--Sharding-jdbc坐标-->
        <dependency>
            <groupId>org.apache.shardingsphere</groupId>
            <artifactId>sharding-jdbc-spring-boot-starter</artifactId>
            <version>4.0.0-RC1</version>
        </dependency>
```
2.在配置文件中书写读写分离原则和Bean定义覆盖原则
```
server:
  port: 8080
spring:
  #应用的名称，可选
  application:
    name: reggie_take_out
  shardingsphere:
    datasource:
      names:
        master,slave
      #主数据源
      master:
        type: com.alibaba.druid.pool.DruidDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        url: jdbc:mysql://192.168.150.130:3306/reggie?characterEncoding=utf-8
        username: root
        password: root
      #从数据源
      slave:
        type: com.alibaba.druid.pool.DruidDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        url: jdbc:mysql://192.168.150.131:3306/reggie?characterEncoding=utf-8
        username: root
        password: root
    masterslave:
      #读写分离配置
      load-balance-algorithm-type: round_robin #轮询
      #最终的数据源名称
      name: dataSource
      #主库数据源名称
      master-data-source-name: master
      #从库数据源列表，多个逗号分割
      slave-data-source-names: slave
    props:
      sql:
        show: true #开启sql显示
  main:
    allow-bean-definition-overriding: true
  #  datasource:
#    druid:
#      driver-class-name: com.mysql.cj.jdbc.Driver
#      url: jdbc:mysql://localhost:3306/reggie?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8&zeroDateTimeBehavior=convertToNull&useSSL=false&allowPublicKeyRetrieval=true
#      username: root
#      password: 1305174214
  redis:
    host: 127.0.0.1
    port: 6379
    database: 0
    password: 1305174214
  cache:
    redis:
      time-to-live: 1800000 #设置过期时间
mybatis-plus:
  configuration:
    #在映射实体或者属性时，将数据库中表名和字段名中的下划线去掉，按照驼峰命名法映射
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      id-type: ASSIGN_ID
reggie:
  path: D:\Desktop\Reggie\
```
## 前后端项目部署
我们的实际部署通常分为两台服务器，来完成前后端分开部署  
## 前端项目部署
我们首先来完成前端项目的部署：  

1.在服务器中安装Nginx，并将课程中的dist目录（已打包的前端数据）上传至Nginx下的html页面  
2.修改Nginx配置文件nginx.conf  
## 后端项目部署
我们再来完成后端项目的部署：  
1.使用git clone命令将git远程仓库的代码克隆下来
2.将资料中的reggieStart.sh文件上传到服务器B中，通过chmod命令设置权限  
3.然后我们直接执行sh文件即可，后端项目开启  
# 结束语
到这里我们的第一个项目就彻底完成了，以上就是《瑞吉外卖》所有技术点的总结内容，希望能为你带来帮助！


