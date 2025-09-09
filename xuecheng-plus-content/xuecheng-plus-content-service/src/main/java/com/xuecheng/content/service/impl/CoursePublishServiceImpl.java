package com.xuecheng.content.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CourseMarketMapper;
import com.xuecheng.content.mapper.CoursePublishPreMapper;
import com.xuecheng.content.mapper.CourseTeacherMapper;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.model.po.CourseMarket;
import com.xuecheng.content.model.po.CoursePublishPre;
import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.service.CourseBaseInfoService;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.content.service.TeachplanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @ClassName CoursePublishServiceImpl
 * @Description
 * @Author
 * @Date 2025/9/7 15:10
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class CoursePublishServiceImpl implements CoursePublishService {

    private final CourseBaseInfoService courseBaseInfoService;

    private final TeachplanService teachplanService;

    private final CourseBaseMapper courseBaseMapper;

    private final CoursePublishPreMapper coursePublishPreMapper;

    private final CourseMarketMapper courseMarketMapper;

    private final CourseTeacherMapper courseTeacherMapper;



    /*
    * 课程预览
    * */
    @Override
    public CoursePreviewDto getCoursePreviewInfo(Long courseId) {
        CoursePreviewDto coursePreviewDto =new CoursePreviewDto();

        //查询课程基本信息 营销信息
        CourseBaseInfoDto courseBaseInfo = courseBaseInfoService.getCourseBaseInfo(courseId);
        coursePreviewDto.setCourseBase(courseBaseInfo);
        //查询课程计划信息
        List<TeachplanDto> teachplanTree = teachplanService.findTeachplanTree(courseId);
        coursePreviewDto.setTeachplans(teachplanTree);
        return coursePreviewDto;
    }

    /*
    * 提交审核
    * */
    @Override
    @Transactional
    public void commitAudit(Long companyId, Long courseId) {
        //查询课程基本信息
        CourseBaseInfoDto courseBaseInfo = courseBaseInfoService.getCourseBaseInfo(courseId);
        if(courseBaseInfo == null){
            XueChengPlusException.cast("课程不存在");
        }
        //TODO 本机构只能提交本机构的课程信息 根据companyId进行校验
        //查询审核状态 当为已提交时不允许重复提交
        String auditStatus = courseBaseInfo.getAuditStatus();
        if(auditStatus.equals("202003")){
            XueChengPlusException.cast("课程已提交 请等待审核");
        }
        //校验图片是否上传
        String pic = courseBaseInfo.getPic();
        if(StringUtils.isEmpty(pic)){
            XueChengPlusException.cast("请上传课程图片");
        }
        //课程计划非空校验
        List<TeachplanDto> teachplanTree = teachplanService.findTeachplanTree(courseId);
        if(teachplanTree == null || teachplanTree.size() == 0){
            XueChengPlusException.cast("请添加课程计划");
        }

        //查询课程基本信息 师资信息 营销信息并插入到预发布表
        CoursePublishPre coursePublishPre = new CoursePublishPre();
        BeanUtils.copyProperties(courseBaseInfo,coursePublishPre);
        //设置机构id
        coursePublishPre.setCompanyId(companyId);
        //营销信息 ？？ courseBaseInfo中应该存在 重复插入？？
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        //转为json字符串存储在预发布表中(使用fastJson)
        String courseMarketJson = JSON.toJSONString(courseMarket);
        coursePublishPre.setMarket(courseMarketJson);

        //课程计划信息
        String teachplanTreeJson = JSON.toJSONString(teachplanTree);
        coursePublishPre.setTeachplan(teachplanTreeJson);

        //课程师资信息
        List<CourseTeacher> courseTeachers = courseTeacherMapper.selectList(new QueryWrapper<CourseTeacher>().eq("course_id", courseId));
        String courseTeachersJson = JSON.toJSONString(courseTeachers);
        coursePublishPre.setTeachers(courseTeachersJson);
        //设置审核状态
        courseBaseInfo.setAuditStatus("202003");
        //提交时间
        courseBaseInfo.setCreateDate(LocalDateTime.now());
        //对是否存在预发布记录进行判断 存在则进行更新 不存在则进行插入
        CoursePublishPre coursePublishPreDB = coursePublishPreMapper.selectById(courseId);
        if(coursePublishPreDB == null){
            coursePublishPreMapper.insert(coursePublishPre);
        }else{
            //更新
            coursePublishPreMapper.updateById(coursePublishPre);
        }
        //更新课程基本信息表的状态为已提交
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        courseBase.setAuditStatus("202003");
        //更新
        courseBaseMapper.updateById(courseBase);
    }


}
