package com.xuecheng.ucenter.service.Impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.service.AuthService;
import javafx.application.Application;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

/**
 * @author huang
 * @version 1.0
 * @description 实现springSecurity UserDetailsService接口 获取用户信息
 * @date 2025/9/15
 */

@Slf4j
@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserDetailsService {

    private final XcUserMapper xcUserMapper;

    //注入Spring容器 获取bean
    private final ApplicationContext applicationContext;

    /*
    * 获取用户信息
    * */

    //传入的请求认证参数就是AuthParamsDto 即登录参数
    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {

        //将传入的请求参数json传转为AuthParamsDto对象
        AuthParamsDto authParamsDto = null;
        try {
            authParamsDto = JSON.parseObject(s, AuthParamsDto.class);
        } catch (Exception e) {
            throw new RuntimeException("请求认证参数不符合要求");
        }
        //获取用户认证的类型
        String authType = authParamsDto.getAuthType();
        //根据认证的类型将对应的Bean注入 策略模式
        String beanName = authType + "_authservice";
        AuthService authService = applicationContext.getBean(beanName, AuthService.class);
        //调用统一的认证方法 完成认证
        XcUserExt xcUserExt = authService.execute(authParamsDto);
        //封装用户信息
        UserDetails userDetails = getUserPrincipal(xcUserExt);
        return userDetails;
    }

    /**
     * 封装用户信息
     *
     * @param xcUserExt
     * @return
     */
    private static UserDetails getUserPrincipal(XcUserExt xcUserExt) {
        //权限
        String[] authorities = {"test"};
        String password = xcUserExt.getPassword();
        //将敏感信息置空
        xcUserExt.setPassword(null);
        String userJson = JSON.toJSONString(xcUserExt);
        UserDetails userDetails = User.withUsername(userJson).password(password).authorities(authorities).build();
        return userDetails;
    }
}
