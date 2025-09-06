package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.BindTeachPlanMediaDto;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;

import java.util.List;

/**
 * @author Mr.M
 * @version 1.0
 * @description 课程计划管理相关接口
 * @date 2023/2/14 12:10
 */

public interface TeachplanService {
 /**
  * 根据课程id查询课程计划
  * @param courseId 课程计划
  * @return
  */
  public List<TeachplanDto> findTeachplanTree(Long courseId);

 /**
  * 新增/修改/保存课程计划
  * @param saveTeachplanDto
  */
 public void saveTeachplan(SaveTeachplanDto saveTeachplanDto);

 /*
 * 删除课程计划
 * */
 void deleteTeachPlan(Long courseId);

 /*
 * 课程计划上移
 * */
 void moveUp(Long id);

 /*
 * 课程计划下移
 * */
 void moveDown(Long id);

 /*
 * 教学计划绑定媒资（添加课程相关视频）
 * */
 public void associationMedia(BindTeachPlanMediaDto bindTeachPlanMediaDto);
}
