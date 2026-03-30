package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.api.TeachplanMapper;
import com.xuecheng.content.api.TeachplanMediaMapper;
import com.xuecheng.content.model.dto.BindTeachPlanMediaDto;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.model.po.TeachplanMedia;
import com.xuecheng.content.service.TeachplanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

/**
 * @author Mr.M
 * @version 1.0
 * @description 课程计划方法实现
 * @date 2023/2/14
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TeachplanServiceImpl implements TeachplanService {

    private final TeachplanMapper teachplanMapper;

    private final TeachplanMediaMapper teachplanMediaMapper;

    @Override
    public List<TeachplanDto> findTeachplanTree(Long courseId) {
        List<TeachplanDto> teachplanDtos = teachplanMapper.selectTreeNodes(courseId);
        return teachplanDtos;
    }

    /*
    * 添加/修改课程计划
    * */
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

        /*
        * 获取课程计划数
        * */
    }
        private int getTeachplanCount(Long parentId, Long courseId) {
        LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Teachplan::getParentid,parentId).eq(Teachplan::getCourseId,courseId);
        int count = teachplanMapper.selectCount(queryWrapper);
        return count+1;
    }

    /*
    * 删除课程计划
    * */
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

    /*
    * 课程计划上移
    * */
    @Override
    @Transactional
    public void moveUp(Long id) {
        // 查询课程信息
        Teachplan currentPlan = teachplanMapper.selectById(id);
        if(currentPlan == null){
            XueChengPlusException.cast("课程计划不存在");
        }

        // 查询同级课程计划
        LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Teachplan::getCourseId, currentPlan.getCourseId())
                .eq(Teachplan::getParentid, currentPlan.getParentid())
                .orderByAsc(Teachplan::getOrderby);

        List<Teachplan> teachplanList = teachplanMapper.selectList(queryWrapper);

        // 检查是否为第一个课程计划
        if(teachplanList.get(0).getId().equals(id)){
            XueChengPlusException.cast("当前为第一个课程计划，无法上移");
        }
        // 获取当前课程计划在列表中的位置
        //创建一个整数流 获取从0到teachplanList.size()-1 的所有整数
        int currentIndex = IntStream.range(0, teachplanList.size())
                //teachplanList.get(i)获取列表中的索引位置
                .filter(i -> teachplanList.get(i).getId().equals(id))
                //返回第一个匹配的结果
                .findFirst()
                //如果Optionallnt 即findFirst()返回结果为空时 抛出指定异常
                .orElseThrow(() -> new XueChengPlusException("课程计划数据异常"));

        // 获取前一个课程计划
        Teachplan prePlan = teachplanList.get(currentIndex - 1);

        // 交换排序号
        Integer currentOrder = currentPlan.getOrderby();
        currentPlan.setOrderby(prePlan.getOrderby());
        prePlan.setOrderby(currentOrder);

        // 批量更新
        List<Teachplan> updates = Arrays.asList(currentPlan, prePlan);
        updates.forEach(teachplanMapper::updateById);
    }

    /*
    * 课程计划下移
    * */
    @Override
    @Transactional
    public void moveDown(Long id) {
        // 1. 查询课程计划
        Teachplan currentTeachplan = teachplanMapper.selectById(id);
        if (currentTeachplan == null) {
            XueChengPlusException.cast("课程计划不存在");
        }

        // 2. 查询同级课程计划（使用链式调用）
        List<Teachplan> teachplanList = teachplanMapper.selectList(new LambdaQueryWrapper<Teachplan>()
                .eq(Teachplan::getParentid, currentTeachplan.getParentid())
                .eq(Teachplan::getCourseId, currentTeachplan.getCourseId())
                .orderByAsc(Teachplan::getOrderby));

        // 3. 检查是否为最后一个元素
        if (teachplanList.get(teachplanList.size() - 1).getId().equals(id)) {
            XueChengPlusException.cast("课程计划已经属于最后一个，无法下移");
        }

        // 4. 获取当前计划位置（优化后的查找方式）
        int currentIndex = IntStream.range(0, teachplanList.size())
                .filter(i -> teachplanList.get(i).getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new XueChengPlusException("课程计划数据异常"));

        // 5. 获取下一个计划
        Teachplan nextTeachplan = teachplanList.get(currentIndex + 1);

        // 6. 交换排序号（使用事务保证一致性）
        Integer currentOrder = currentTeachplan.getOrderby();
        currentTeachplan.setOrderby(nextTeachplan.getOrderby());
        nextTeachplan.setOrderby(currentOrder);

        // 7. 批量更新
        List<Teachplan> updates = Arrays.asList(currentTeachplan, nextTeachplan);
        updates.forEach(teachplanMapper::updateById);

        }

   /*
   * 教学计划绑定媒资（添加课程计划相关视频）
   *
   * */
    @Override
    @Transactional
    public void associationMedia(BindTeachPlanMediaDto bindTeachPlanMediaDto) {
        Teachplan teachplan = teachplanMapper.selectById(bindTeachPlanMediaDto.getTeachplanId());
        if(teachplan == null){
            XueChengPlusException.cast("课程计划不存在");
        }
        Integer grade = teachplan.getGrade();
        if (grade != 2){
            XueChengPlusException.cast("只允许第二级课程计划绑定媒资");
        }
        //删除原有记录 再新增
        teachplanMediaMapper.delete(new LambdaQueryWrapper<TeachplanMedia>().eq(TeachplanMedia::getTeachplanId, bindTeachPlanMediaDto.getTeachplanId()));

        TeachplanMedia teachplanMedia = new TeachplanMedia();
        BeanUtils.copyProperties(bindTeachPlanMediaDto,teachplanMedia);
        teachplanMedia.setCourseId(teachplan.getCourseId());
        teachplanMedia.setCreateDate(LocalDateTime.now());
        teachplanMedia.setMediaFilename(bindTeachPlanMediaDto.getFileName());

        teachplanMediaMapper.insert(teachplanMedia);
    }
}


