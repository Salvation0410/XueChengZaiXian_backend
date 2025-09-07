package com.xuecheng.content.model.dto;

import com.xuecheng.content.model.po.CourseBase;
import lombok.Data;

import java.util.List;

/**
 * @description 课程预览基本信息
 * @author huang
 * @date 2025.9.7
 * @version 1.0
 */
@Data
public class CoursePreviewDto{

 //课程基本信息+课课程营销信息
 CourseBaseInfoDto courseBase;

 //课程计划信息
 List<TeachplanDto> teachplans;

 //课程师资信息 （暂时不加）


}
