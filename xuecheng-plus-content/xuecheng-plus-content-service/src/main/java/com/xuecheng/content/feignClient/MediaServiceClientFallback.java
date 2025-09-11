package com.xuecheng.content.feignClient;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * @ClassName MediaServiceClientFallback
 * @Description media服务熔断降级处理
 * @Author huang
 * @Date 2025/9/11
 */

@Component
@Slf4j
public class MediaServiceClientFallback implements MediaServiceClient{

    /*
    * 这里使用的是Hystrix的fallback方法 TODO 使用Feign整合sentinel进行熔断降级处理
    * */
    @Override
    public String upload(MultipartFile filedata, String objectName) throws IOException {
        return null;
    }
}
