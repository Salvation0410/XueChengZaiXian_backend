package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.mapper.CourseTeacherMapper;
import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.service.CourseTeacherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @ClassName CourseTeachServiceImpl
 * @Description
 * @Author
 * @Date 2025/8/14 21:36
 */


@Service
public class CourseTeachServiceImpl implements CourseTeacherService {
    @Autowired
    private CourseTeacherMapper courseTeacherMapper;

    @Override
    public List<CourseTeacher> queryCourseTeachers(Long courseId) {
        LambdaQueryWrapper<CourseTeacher> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(CourseTeacher::getCourseId, courseId);
        return courseTeacherMapper.selectList(queryWrapper);
    }

    @Override
    public CourseTeacher saveOrUpdateCourseTeacher(CourseTeacher courseTeacher) {
        //判断是新增还是修改
        Long id = courseTeacher.getId();
        if (id==null){
            //新增
            courseTeacher.setCreateDate(LocalDateTime.now());
            courseTeacherMapper.insert(courseTeacher);
        }else {
            //修改
            courseTeacherMapper.updateById(courseTeacher);
        }
        return courseTeacherMapper.selectById(courseTeacher);
    }

    @Override
    public void removeCourseTeacher(Long courseId, Long teacherId) {
        //判断教师是否存在
        LambdaQueryWrapper<CourseTeacher> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(CourseTeacher::getCourseId, courseId).eq(CourseTeacher::getId, teacherId);
        CourseTeacher courseTeacher = courseTeacherMapper.selectOne(queryWrapper);
        if (courseTeacher==null){
            XueChengPlusException.cast("教师不存在");
        }
        courseTeacherMapper.deleteById(courseTeacher);
    }
}
