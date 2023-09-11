package com.liu.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liu.reggie.common.R;
import com.liu.reggie.entity.User;
import com.liu.reggie.service.UserService;
import com.liu.reggie.utils.MailUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.mail.MessagingException;
import javax.servlet.http.HttpSession;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 发送验证码
     * @param user
     * @return
     */
    @PostMapping("/sendMsg")
    public R<String> sendMsg(@RequestBody User user, HttpSession httpSession) throws MessagingException {
        //获取邮箱号
        String phone = user.getPhone();
        if (!phone.isEmpty()){
            //生成随机的4位验证码
            String code = MailUtils.achieveCode();
            log.info(code);
            //调用邮箱API发送验证码,这里的phone其实就是邮箱，code是我们生成的验证码
            MailUtils.sendTestMail(phone, code);
            //验证码存session，方便后面拿出来比对
            httpSession.setAttribute(phone, code);
            return R.success("验证码发送成功");
        }
        return R.error("验证码发送失败");
        }

    /**
     * 移动端用户登录
      * @param map
     * @param httpSession
     * @return
     */
    @PostMapping("/login")
    public R<User> login(@RequestBody Map map, HttpSession httpSession){
        log.info(map.toString());

        //获取手机号
        String phone = map.get("phone").toString();

        //获取验证码
        String code = map.get("code").toString();

        //从Session中获取保存的验证码
        Object codeInSession = httpSession.getAttribute(phone);

        //验证码比对（页面提交的验证码和Session中保存的验证码）
        if (codeInSession != null && codeInSession.equals(code)){
            //如果能够比对成功，那就说明登陆成功

            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getPhone,phone);
            User user = userService.getOne(queryWrapper);
            if (user == null){
                //判断当前手机号对应的用户是否为新用户，如果是新用户自动完成注册
                user = new User();
                user.setPhone(phone);
                user.setStatus(1);
                userService.save(user);
            }
            httpSession.setAttribute("user",user.getId());
            return R.success(user);
        }
        return R.error("登陆失败");
    }

    /**
     * 退出登录
     * @param httpSession
     * @return
     */
    @PostMapping("/loginout")
    public R<String> loginout(HttpSession httpSession){
        httpSession.removeAttribute("user");
        return R.success("退出成功");
    }
}
