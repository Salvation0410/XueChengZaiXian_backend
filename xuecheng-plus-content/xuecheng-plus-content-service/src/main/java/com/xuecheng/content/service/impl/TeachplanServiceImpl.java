package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.mapper.TeachplanMapper;
import com.xuecheng.content.mapper.TeachplanMediaMapper;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.model.po.TeachplanMedia;
import com.xuecheng.content.service.TeachplanService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author Mr.M
 * @version 1.0
 * @description TODO
 * @date 2023/2/14 12:11
 */
@Service
@RequiredArgsConstructor
public class TeachplanServiceImpl implements TeachplanService {

    @Autowired
    TeachplanMapper teachplanMapper;

    private final TeachplanMediaMapper teachplanMediaMapper;

    @Override
    public List<TeachplanDto> findTeachplanTree(Long courseId) {
        List<TeachplanDto> teachplanDtos = teachplanMapper.selectTreeNodes(courseId);
        return teachplanDtos;
    }


    @Override
    public void saveTeachplan(SaveTeachplanDto saveTeachplanDto) {
        //通过课程计划id判断添加还是修改
        Long teachPlanId = saveTeachplanDto.getId();
        if(teachPlanId == null){
            //当前执行新增操作
            Teachplan teachplan = new Teachplan();
            BeanUtils.copyProperties(saveTeachplanDto,teachplan);
            //确定当前节点的顺序 节点数加一即插到末尾
            //select count(1) from teachplan where parentid=#{parentid} and course_id=#{courseId}
            Long parentId = saveTeachplanDto.getParentid();
            Long courseId = saveTeachplanDto.getCourseId();
            int count =getTeachplanCount(parentId,courseId);
            teachplan.setOrderby(count);
            teachplanMapper.insert(teachplan);
        }else{
            //当前执行修改操作
            Teachplan teachplan = teachplanMapper.selectById(teachPlanId);
            BeanUtils.copyProperties(saveTeachplanDto,teachplan);
            teachplanMapper.updateById(teachplan);
        }

    }
    private int getTeachplanCount(Long parentId, Long courseId) {
        LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Teachplan::getParentid,parentId).eq(Teachplan::getCourseId,courseId);
        int count = teachplanMapper.selectCount(queryWrapper);
        return count+1;
    }

    @Override
    @Transactional
    public void deleteTeachPlan(Long courseId) {
        //根据parentId判断当前节点为叶子节点还是根节点
        Teachplan teachplan = teachplanMapper.selectById(courseId);
        if(!teachplan.getId().equals(0L) && teachplan != null){
            //存在小章节

            //根据teachplanId查询关联的媒资表中的数据
            LambdaQueryWrapper<TeachplanMedia> queryWrapper = new LambdaQueryWrapper<>();
            // 构建查询条件
            queryWrapper.eq(TeachplanMedia::getTeachplanId,teachplan.getId());
            //selectOne:根据查询条件返回单个查询的结果，如果结果有多个会抛出异常，如果结果为空则返回null
            TeachplanMedia teachplanMedia = teachplanMediaMapper.selectOne(queryWrapper);
            if(teachplanMedia != null){
                //存在关联的媒资
                //删除视频
                int count = teachplanMediaMapper.delete(queryWrapper);
                if(count<=0){
                    XueChengPlusException.cast("删除失败");
                }
            }
            //删除小章节
            int count1 = teachplanMapper.deleteById(courseId);
            if(count1<=0){
                XueChengPlusException.cast("删除小章节失败");
            }

        }else{
            //不存在小章节
            //删除大章节
            Long count2 = teachplanMapper.selectCountByParentid(courseId);
            if(count2 == 0){
                //大章节下没有下章节 直接删除
                teachplanMapper.deleteById(teachplan);
            }else{
                XueChengPlusException.cast("请先删除该大章节下的小章节");
            }
        }
    }


}
