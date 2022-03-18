package com.nowcoder.community.controller;

import com.google.code.kaptcha.Producer;
import com.nowcoder.community.config.KaptchaConfig;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.MailCilent;
import com.nowcoder.community.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;


@Controller
public class LoginController implements CommunityConstant {
    private static final Logger logger=LoggerFactory.getLogger(LoginController.class);
    @Autowired
    private UserService userService;
    @Autowired
    private Producer kaptchaProducer;
    @Autowired
    private TemplateEngine templateEngine;
    @Autowired
    private MailCilent mailCilent;

    @Autowired
    private RedisTemplate redisTemplate;
    @Value("server.servlet.context-path")
    private String contextPath;
    @GetMapping("/register")
    public String getRegisterPage(){
        return "/site/register";
    }
    @GetMapping("/login")
    public String getLoginPage(){
        return "/site/login";
    }

    @PostMapping("/register")
    public String register(Model model, User user){
        Map<String,Object> map=userService.register(user);
        if(map==null || map.isEmpty()){
            model.addAttribute("msg", "注册成功,我们已经向您的邮箱发送了一封激活邮件,请尽快激活!");
            model.addAttribute("target", "/index");
            return "/site/operate-result";
        }else {
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            model.addAttribute("emailMsg", map.get("emailMsg"));
            return "/site/register";
        }
    }
    @GetMapping("/activation/{userId}/{code}")
    public String ActivationCode(Model model,@PathVariable("userId") int userId,@PathVariable("code") String code){
        int result=userService.activation(userId,code);
        if (result == ACTIVATION_SUCCESS) {
            model.addAttribute("msg", "激活成功,您的账号已经可以正常使用了!");
            model.addAttribute("target", "/login");
        } else if (result == ACTIVATION_REPEAT) {
            model.addAttribute("msg", "无效操作,该账号已经激活过了!");
            model.addAttribute("target", "/index");
        } else {
            model.addAttribute("msg", "激活失败,您提供的激活码不正确!");
            model.addAttribute("target", "/index");
        }
        return "/site/operate-result";
    }
    @GetMapping("/kaptcha")
    public void getKaptcha(HttpServletResponse response/*, HttpSession session*/){
        //生成验证码
        String text = kaptchaProducer.createText();
        BufferedImage image = kaptchaProducer.createImage(text);
        //将验证码存入session
        //session.setAttribute("kaptcha",text);
        // 验证码临时凭证存入cookie
        String kaptchaOwner = CommunityUtil.generateUUID();
        Cookie cookie = new Cookie("kaptchaOwner", kaptchaOwner);
        cookie.setMaxAge(60);
        cookie.setPath(contextPath);
        response.addCookie(cookie);
        // 将验证码存入Redis
        String redisKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
        redisTemplate.opsForValue().set(redisKey, text, 60, TimeUnit.SECONDS);

        //返回图片
        response.setContentType("image/png");
        try {
            OutputStream os = response.getOutputStream();
            ImageIO.write(image,"png",os);
        } catch (IOException e) {
            logger.error("响应验证码失败"+e.getMessage());
        }

    }
    @PostMapping("/login")
    public String login(HttpServletResponse response,/*HttpSession session,*/
                        String code,String username,String password,
                        Model model,boolean rememberme,@CookieValue("kaptchaOwner") String kaptchaOwner){
        //检查验证码
        //String kaptcha = (String) session.getAttribute("kaptcha");

        //根据验证码临时凭证从redis取出验证码
        String kaptcha = null;
        if (StringUtils.isNotBlank(kaptchaOwner)) {
            String redisKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
            kaptcha = (String) redisTemplate.opsForValue().get(redisKey);
        }


        if (StringUtils.isBlank(kaptcha) || StringUtils.isBlank(code) || !kaptcha.equalsIgnoreCase(code)) {
            model.addAttribute("codeMsg", "验证码不正确!");
            return "/site/login";
        }
        //检查账号，密码
        int expiredSeconds=rememberme?REMEMBER_EXPIRED_SECONDS:DEFAULT_EXPIRED_SECONDS;
        Map<String, Object> login = userService.login(username, password, expiredSeconds);
        if(login.containsKey("ticket")){
            Cookie cookie = new Cookie("ticket",login.get("ticket").toString());
            cookie.setPath(contextPath);
            cookie.setMaxAge(expiredSeconds);
            response.addCookie(cookie);
            return "redirect:/index";
        }else{
            model.addAttribute("usernameMsg", login.get("usernameMsg"));
            model.addAttribute("passwordMsg", login.get("passwordMsg"));
            return "/site/login";
        }
    }
    @GetMapping("/logout")
    public String logout(@CookieValue("ticket") String ticket){
        userService.logout(ticket);
        return "redirect:/index";
    }
    //忘记密码
    @GetMapping("/forget")
    public String getForgetPage(){
        return "/site/forget";
    }

    //获取验证码
    @GetMapping("/forget/code")
    @ResponseBody
    public String getForgetCode(String email,HttpSession session){
        if(StringUtils.isBlank(email)){
            return CommunityUtil.getJSONString(1, "邮箱不能为空！");
        }
        if (!userService.isEmailExist(email)) {
            return CommunityUtil.getJSONString(1, "该邮箱尚未注册！");
        }
        Context context = new Context();
        context.setVariable("email",email);
        String code=CommunityUtil.generateUUID().substring(0,4);
        context.setVariable("verifyCode",code);
        String content = templateEngine.process("/mail/forget", context);
        mailCilent.sendMail(email,"找回密码",content);
        session.setAttribute(email+"verifyCode",code);
        return CommunityUtil.getJSONString(0);
    }


    //重置密码
    @PostMapping("/forget/password")
    public String resetPassword(String email,String password,String verifyCode,Model model,HttpSession session){
        String code = (String) session.getAttribute(email+"verifyCode");
        if(StringUtils.isBlank(code) || StringUtils.isBlank(verifyCode) || !code.equalsIgnoreCase(verifyCode)){
            model.addAttribute("codeMsg", "验证码错误!");
            return "/site/forget";
        }
        Map<String, Object> map = userService.resetPassword(email, password);
        if(map.containsKey("user")){
            return "redirect:/index";
        }else{
            model.addAttribute("emailMsg", map.get("emailMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            return "/site/forget";
        }
    }
}
