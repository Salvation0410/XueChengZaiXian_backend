package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.content.mapper.TeachplanMapper;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.service.TeachplanService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Mr.M
 * @version 1.0
 * @description TODO
 * @date 2023/2/14 12:11
 */
@Service
public class TeachplanServiceImpl implements TeachplanService {

    @Autowired
    TeachplanMapper teachplanMapper;

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
}
