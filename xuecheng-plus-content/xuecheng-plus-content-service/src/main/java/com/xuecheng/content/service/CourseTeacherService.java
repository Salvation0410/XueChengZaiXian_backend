package com.xuecheng.content.service;

import com.xuecheng.content.model.po.CourseTeacher;

import java.util.List;

/**
 * @ClassName CourseTeachService
 * @Description 教师信息编辑接口
 * @Author huang
 * @Date 2025/8/14
 */


public interface CourseTeacherService {
    /**
     * 根据课程id查询课程教师信息
     * @param courseId
     * @return List<CourseTeacher>
     */
    List<CourseTeacher> queryCourseTeachers(Long courseId);

    /**
     * 添加或修改教师信息
     * @param courseTeacher
     * @return
     */
    CourseTeacher saveOrUpdateCourseTeacher(CourseTeacher courseTeacher);

    /**
     * 删除教师信息
     * @param courseId
     * @param teacherId
     */
    void removeCourseTeacher(Long courseId, Long teacherId);

}
