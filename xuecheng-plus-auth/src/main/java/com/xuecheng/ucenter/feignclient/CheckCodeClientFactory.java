package com.xuecheng.ucenter.feignclient;

import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author huang
 * @version 1.0
 * @description 远程调用熔断降级处理
 * @date 2025/9/16
 */

@Component
@Slf4j
public class CheckCodeClientFactory implements FallbackFactory<CheckCodeClient> {
    @Override
    public CheckCodeClient create(Throwable throwable) {
        return new CheckCodeClient() {
            @Override
            public Boolean verify(String key, String code) {
                log.error("调用验证码服务发生异常：{}",throwable.getMessage());
                return null;
            }
        };
    }
}
