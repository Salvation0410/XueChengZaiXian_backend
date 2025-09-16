package com.xuecheng.ucenter.feignclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author huang
 * @version 1.0
 * @description 远程调用验证服务码接口
 * @date 2025/9/16
 */
@FeignClient(value = "checkcode", fallbackFactory = CheckCodeClientFactory.class)
public interface CheckCodeClient{

    @PostMapping(value = "/checkcode/verify")
    public Boolean verify(@RequestParam("key")String key,@RequestParam("code") String code);
}
