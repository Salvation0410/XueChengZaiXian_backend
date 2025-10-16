package com.xuecheng.ucenter.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.ucenter.feignclient.CheckCodeClient;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * @author huang
 * @version 1.0
 * @description 账号密码认证
 * @date 2025/9/16
 */
@Service("password_authservice")
@Slf4j
@RequiredArgsConstructor
public class    PasswordAuthServiceImpl implements AuthService {

    private final XcUserMapper xcUserMapper;
    //密码校验
    private final PasswordEncoder passwordEncoder;
    //验证码服务
    private final CheckCodeClient checkCodeClient;
    @Override
    public XcUserExt execute(AuthParamsDto authParamsDto) {

        //账号是否存在
        String username = authParamsDto.getUsername();

        //远程调用验证码服务验证验证码
        String checkcode = authParamsDto.getCheckcode();
        String checkcodekey = authParamsDto.getCheckcodekey();
        if(StringUtils.isEmpty(checkcodekey) || StringUtils.isEmpty(checkcode)){
            throw new RuntimeException("请输入验证码");
        }
        Boolean verify = checkCodeClient.verify(checkcodekey,checkcode);
        if(verify == null ){
            throw new RuntimeException("验证码错误");
        }

        XcUser xcUser = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getUsername, username));
        if(xcUser == null){
            throw  new RuntimeException("账号不存在");
        }

        //验证密码
        String password = xcUser.getPassword();
        //获取用户输入的密码
        String inputPassword = authParamsDto.getPassword();
        //使用passwordEncoder进行密码对比 参数:输入的密码,数据库中的密码
        boolean matches = passwordEncoder.matches(inputPassword,password);
        if(matches == false){
            throw new RuntimeException("密码错误");
        }
        XcUserExt xcUserExt = new XcUserExt();
        BeanUtils.copyProperties(xcUser,xcUserExt);

        return xcUserExt;
    }
}
