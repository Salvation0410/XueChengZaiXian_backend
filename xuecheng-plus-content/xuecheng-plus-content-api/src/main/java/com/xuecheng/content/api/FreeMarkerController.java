package com.xuecheng.content.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * @ClassName FreeMarkerController
 * @Description FreeMarker入门测试接口
 * @Author huang
 * @Date 2025/9/6
 */

//这里不使用rest风格的api接口 rest返回的是json数据
@Controller
public class FreeMarkerController {
    @GetMapping("/testfreemarker")
    public ModelAndView test(){
        ModelAndView modelAndView = new ModelAndView();
        //设置模型数据
        modelAndView.addObject("name","小明");
        //设置模板名称
        //这里会根据视图名称加上nacos配置的页面模板后缀名在resources下寻找页面
        modelAndView.setViewName("test");
        return modelAndView;
    }
}
