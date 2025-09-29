package com.xuecheng.auth.Enums;

import lombok.Getter;

/**
 * @author huang
 * @version 1.0
 * @description 认证相关枚举
 * @date 2025/9/29
 */
@Getter
public enum AuthEnum {

    AUTH_CLIENT_ID("客户端id", "XcWebApp"),
    AUTH_CLIENT_SECRET("客户端密钥", "XcWebApp"),
    AUTH_RESOURCE_ID("资源列表", "xuecheng-plus"),
    AUTH_AUTHORIZED_GRANT_TYPES("授权类型", "authorization_code,password,client_credentials,refresh_token"),
    AUTH_SCOPE("授权范围", "all"),
    AUTH_REDIRECT_URI("客户端接收授权码重定向地址", "http://www.51xuecheng.cn"),
    AUTH_LOGIN_SUCCESS_URL("登录成功跳转路径","/login-success"),
    AUTH_WX_LOGIN_FAILURL("微信登录失败跳转网页","redirect:http://www.51xuecheng.cn/error.html"),
    AUTH_WX_LOGIN_SUCCESS_URL("微信登录成功跳转网页","redirect:http://www.51xuecheng.cn/sign.html?username="),
    AUTH_WX_LOGIN_TYPE("微信授权类型","&authType=wx"),
    AUTH_ACCESS_TOKEN("令牌","access_token"),
    AUTH_USERNAME("openId","openid"),
    ;

    /** 描述信息 */
    private final String desc;

    /** 实际值 */
    private final String value;

    AuthEnum(String desc, String value) {
        this.desc = desc;
        this.value = value;
    }

    /**
     * 根据 value 查找枚举
     */
    public static AuthEnum fromValue(String value) {
        for (AuthEnum authEnum : AuthEnum.values()) {
            if (authEnum.value.equals(value)) {
                return authEnum;
            }
        }
        return null;
    }

    /**
     * 根据枚举名查找
     */
    public static AuthEnum fromName(String name) {
        try {
            return AuthEnum.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
