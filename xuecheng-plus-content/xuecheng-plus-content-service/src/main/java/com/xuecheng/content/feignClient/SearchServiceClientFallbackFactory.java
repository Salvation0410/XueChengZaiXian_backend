package com.xuecheng.content.feignClient;

import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author huang
 * @description 降级处理
 * @date 2025/9/13
 * @version 1.0
 */

@Component
@Slf4j
public class SearchServiceClientFallbackFactory implements FallbackFactory<SearchServiceClient> {
    @Override
    public SearchServiceClient create(Throwable throwable) {
        return new SearchServiceClient() {
            @Override
            public Boolean add(CourseIndex courseIndex) {
                log.error("添加课程索引发生异常，课程索引：{}，熔断信息：{}",courseIndex,throwable.toString(),throwable);
                //降级处理函数返回false
                return false;
            }
        };
    }
}
