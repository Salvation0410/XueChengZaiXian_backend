package com.xuecheng.content.feignClient;

import com.xuecheng.content.config.MultipartSupportConfig;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * @ClassName MediaServiceClient
 * @Description FeignClient 调用媒资管理服务
 * @Author huang
 * @Date 2025/9/11
 */

//指定调用的微服务 TODO 抽取到一个独立的模块 将远程调用涉及到的Po以及Feign的相关配置放到该模块
//第一种写法 使用fallback 但这种方法无法取出熔断所抛出的异常

//@FeignClient(value = "media-api",configuration = MultipartSupportConfig.class, fallback = MediaServiceClientFallback.class)
@FeignClient(value = "media-api",configuration = MultipartSupportConfig.class, fallbackFactory = MediaServiceClientFallbackFactory.class)
public interface MediaServiceClient {


    @RequestMapping(value = "/media/upload/coursefile",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String upload(@RequestPart("filedata") MultipartFile filedata,
                                      @RequestParam(value = "objectName",required = false)
                                      String objectName
    ) throws IOException;
}
