package com.xuecheng.content.api;

import com.xuecheng.base.exception.ValidationGroups;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.EditCourseDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.service.CourseBaseInfoService;
import com.xuecheng.content.util.SecurityUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @author Mr.M
 * @version 1.0
 * @description 课程信息管理接口
 * @date 2023/2/11 15:44
 */
@Api(value = "课程信息管理接口",tags = "课程信息管理接口")
@RestController
public class CourseBaseInfoController {

    @Autowired
    private CourseBaseInfoService courseBaseInfoService;

    @ApiOperation("课程分页查询接口")
    //@PreAuthorize("hasAuthority('xc_teachmanager_course_list')") //指定权限标识符 详情查询xc_menu表
    @PostMapping("/course/list")
    public PageResult<CourseBase> list(PageParams pageParams, @RequestBody(required=false) QueryCourseParamsDto queryCourseParamsDto) {

        SecurityUtil.XcUser xcUser = SecurityUtil.getUser();
        String companyId = xcUser.getCompanyId();
        PageResult<CourseBase> courseBasePageResult = courseBaseInfoService.queryCourseBaseList(Long.parseLong(companyId),pageParams,queryCourseParamsDto);
        return courseBasePageResult;
    }

    @PostMapping("/content/course")
    @ApiOperation("新增课程")
    //使用@Validated注解对参数进行验证 并说明分组校验的类型
    public CourseBaseInfoDto createCourseBase(@RequestBody @Validated(ValidationGroups.Insert.class) AddCourseDto addCourseDto){
        SecurityUtil.XcUser xcUser = SecurityUtil.getUser();
        String companyId = xcUser.getCompanyId();
        CourseBaseInfoDto courseBaseInfoDto = courseBaseInfoService.createCourseBase(Long.parseLong(companyId),addCourseDto);
        return courseBaseInfoDto;
    }

    @GetMapping("/course/{courseId}")
    @ApiOperation("获取课程信息")
    public CourseBaseInfoDto getCourseBaseById(@PathVariable Long courseId){
        CourseBaseInfoDto courseBaseInfoDto = courseBaseInfoService.getCourseBaseInfo(courseId);
        return courseBaseInfoDto;
    }

    @PutMapping("/course")
    @ApiOperation("修改课程信息")
    public CourseBaseInfoDto modifyCourseBase(@RequestBody @Validated(ValidationGroups.Update.class) EditCourseDto editCourseDto){
        SecurityUtil.XcUser xcUser = SecurityUtil.getUser();
        String companyId = xcUser.getCompanyId();
        CourseBaseInfoDto courseBaseInfoDto = courseBaseInfoService.modifyCourseBase(Long.parseLong(companyId),editCourseDto);
        return courseBaseInfoDto;
    }
    @DeleteMapping("/course/{courseId}")
    @ApiOperation("删除课程信息")
    public void deleteCourseBase(@PathVariable Long courseId){
        SecurityUtil.XcUser xcUser = SecurityUtil.getUser();
        String companyId = xcUser.getCompanyId();
        courseBaseInfoService.deleteCourseBase(Long.parseLong(companyId),courseId);
    }


}
