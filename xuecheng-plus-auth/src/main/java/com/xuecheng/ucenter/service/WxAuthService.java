package com.xuecheng.ucenter.service;

import com.xuecheng.ucenter.model.po.XcUser;

/**
 * @author huang
 * @version 1.0
 * @description 微信认证接口
 * @date 2025/9/17
 */

public interface WxAuthService {
    public XcUser wxAuth(String code);
}
