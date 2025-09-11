package com.xuecheng.content.feignClient;

import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * @ClassName MediaServiceClientFallbackFactory
 * @Description 媒资管理服务熔断处理
 * @Author huang
 * @Date 2025/9/11
 */

/*
* 说明:接口类型为你所需要远程调用的接口类型
*
* */
    @Component
    @Slf4j
public class MediaServiceClientFallbackFactory implements FallbackFactory<MediaServiceClient> {

    //通过这种方法 可以拿到熔断的异常信息 throwable
    @Override
    public MediaServiceClient create(Throwable throwable) {
        return new MediaServiceClient() {
            //发生熔断时 上游服务会调用此方法进行降级逻辑处理
            @Override
            public String upload(MultipartFile filedata, String objectName) throws IOException {
                log.error("远程调用媒资服务上传文件发生熔断,异常信息:{}",throwable.getMessage());
                return null;
            }
        };
    }
}
