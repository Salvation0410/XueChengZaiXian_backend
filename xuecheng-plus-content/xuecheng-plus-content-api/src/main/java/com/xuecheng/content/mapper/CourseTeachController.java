package com.xuecheng.content.mapper;

import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.service.CourseTeacherService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @ClassName CourseTeachController
 * @Description 教师信息编辑接口
 * @Author huang
 * @Date 2025/8/14 21:33
 */

@RestController
@Slf4j
@Api(tags = "教师信息编辑接口")
@RequiredArgsConstructor
public class CourseTeachController {
    private final CourseTeacherService courseTeacherService;

    @ApiOperation("查询教师信息接口")
    @GetMapping("/courseTeacher/list/{courseId}")
    public List<CourseTeacher> getCourseTeacher(@PathVariable Long courseId) {
        return courseTeacherService.queryCourseTeachers(courseId);
    }

    @ApiOperation("添加或修改教师信息接口")
    @PostMapping("/courseTeacher")
    public CourseTeacher addOrUpdateCourseTeacher(@RequestBody CourseTeacher courseTeacher) {
        return courseTeacherService.saveOrUpdateCourseTeacher(courseTeacher);
    }

    @ApiOperation("删除教师信息接口")
    @DeleteMapping("/courseTeacher/course/{courseId}/{teacherId}")
    public void deleteCourseTeacher(@PathVariable Long courseId, @PathVariable Long teacherId) {
        courseTeacherService.removeCourseTeacher(courseId, teacherId);
    }
}
