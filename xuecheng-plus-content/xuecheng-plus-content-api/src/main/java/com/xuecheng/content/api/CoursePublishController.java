package com.xuecheng.content.api;

import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.service.CoursePublishService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

/**
 * @ClassName CoursePublishController
 * @Description 课程发布相关接口
 * @Author huang
 * @Date 2025/9/7 9:16
 */

// RestController注解响应json数据 Controller响应页面
@Controller
@Slf4j
@Api(tags = "课程发布相关接口")
@RequiredArgsConstructor
public class CoursePublishController {

    private final CoursePublishService coursePublishService;

    /*
    * 课程预览
    * */
    @ApiOperation("课程预览")
    @GetMapping("/coursepreview/{courseId}")
    public ModelAndView preview(@PathVariable("courseId") Long courseId){
        ModelAndView modelAndView = new ModelAndView();
        //查询到的课程信息作为模板数据
        CoursePreviewDto coursePreviewInfo = coursePublishService.getCoursePreviewInfo(courseId);
        //指定模型
        modelAndView.addObject("model",coursePreviewInfo);
        //指定模板
        modelAndView.setViewName("course_template");    //根据视图名称加nacos中配置的后缀拼接找到模板(+.html)
        return modelAndView;
    }

    /*
    * 课程审核
    * */
    @ApiOperation("课程审核")
    @ResponseBody
    @PostMapping("/courseaudit/commit/{courseId}")
    public void commitAudit(@PathVariable("courseId") Long courseId){
        Long companyId = 1232141425L;
        coursePublishService.commitAudit(companyId,courseId);
    }
    /*
    * 课程发布
    * */
    @ApiOperation("课程发布")
    @ResponseBody
    @PostMapping ("/coursepublish/{courseId}")
    public void coursepublish(@PathVariable("courseId") Long courseId){
        Long companyId = 1232141425L;
        coursePublishService.publish(companyId,courseId);
    }


}
