package com.xuecheng.learning.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.learning.feignclient.ContentServiceClient;
import com.xuecheng.learning.mapper.XcChooseCourseMapper;
import com.xuecheng.learning.mapper.XcCourseTablesMapper;
import com.xuecheng.learning.model.dto.MyCourseTableParams;
import com.xuecheng.learning.model.dto.XcChooseCourseDto;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.xuecheng.learning.model.po.XcChooseCourse;
import com.xuecheng.learning.model.po.XcCourseTables;
import com.xuecheng.learning.service.MyCourseTablesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author huang
 * @version 1.0
 * @description
 * @date 2025/9/19
 */


@Service
@Slf4j
@RequiredArgsConstructor
public class MyCourseTablesServiceImpl implements MyCourseTablesService {

    //选课相关
    private final XcChooseCourseMapper xcChooseCourseMapper;
    //个人课程
    private final XcCourseTablesMapper xcCourseTablesMapper;
    //远程调用内容管理接口
    private final ContentServiceClient contentServiceClient;

    @Override
    @Transactional
    //TODO 优化 这里不能使用@Transactional @Transactional 只能控制单个数据库的事务 这里涉及远程调用 需要用到分布式事务 最终一致性+mq补偿机制(优化方案)
    public XcChooseCourseDto addChooseCourse(String userId, Long courseId) {
        //选课调用内容管理模块查询课程的收费规则
        CoursePublish coursePublish = contentServiceClient.getCoursepublish(courseId);
        if(coursePublish == null){
            XueChengPlusException.cast("课程不存在");
        }
        String charge = coursePublish.getCharge();
        XcChooseCourse xcChooseCourse = null;
        if("201000".equals( charge)){
            //免费课程则插入选课记录表 我的课程表信息

            //添加选课记录表
             xcChooseCourse = addFreeCourse(userId, coursePublish);
            //添加我的课程表 我的课程表的信息即来源于选课记录表
            XcCourseTables xcCourseTables = addCourseTables(xcChooseCourse);

        }else{
            //收费只插入选课记录表
             xcChooseCourse = addChargeCourse(userId, coursePublish);
        }
        //判断学生的学习资格 XcChooseCourseDto
        XcCourseTablesDto xcCourseTablesDto = getLearningStatus(userId, courseId);

        XcChooseCourseDto xcChooseCourseDto = new XcChooseCourseDto();
        BeanUtils.copyProperties(xcChooseCourse,xcChooseCourseDto);
        xcChooseCourseDto.setLearnStatus(xcCourseTablesDto.getLearnStatus());

        return xcChooseCourseDto;
    }

    /*
    * 获取学习资格
    * XcCourseTablesDto 学习资格状态 [{"code":"702001","desc":"正常学习"},{"code":"702002","desc":"没有选课或选课后没有支付"},{"code":"702003","desc":"已过期需要申请续期或重新支付"}]
    * */
    @Override
    public XcCourseTablesDto getLearningStatus(String userId, Long courseId) {
        //查询我的课程表
        XcCourseTables xcCourseTables = getXcCourseTables(userId, courseId);
        if(xcCourseTables==null){
            XcCourseTablesDto xcCourseTablesDto = new XcCourseTablesDto();
            //没有选课或选课后没有支付
            xcCourseTablesDto.setLearnStatus("702002");
            return xcCourseTablesDto;
        }
        XcCourseTablesDto xcCourseTablesDto = new XcCourseTablesDto();
        BeanUtils.copyProperties(xcCourseTables,xcCourseTablesDto);
        //是否过期,true过期，false未过期
        boolean isExpires = xcCourseTables.getValidtimeEnd().isBefore(LocalDateTime.now());
        if(!isExpires){
            //正常学习
            xcCourseTablesDto.setLearnStatus("702001");
            return xcCourseTablesDto;

        }else{
            //已过期
            xcCourseTablesDto.setLearnStatus("702003");
            return xcCourseTablesDto;
        }
    }
    //接收mq发送的消息后进行对选课记录表的状态并且添加我的课程表
    @Override
    public boolean saveChooseCourseSuccess(String chooseCourseId) {
        XcChooseCourse xcChooseCourse = xcChooseCourseMapper.selectById(chooseCourseId);
        if(xcChooseCourse == null){
            log.info("接收购买课程的消息，根据id从数据库查询不到数据，选课id:{}",chooseCourseId);
            return false;
        }
        String status = xcChooseCourse.getStatus();
        if("701002".equals(status)){
            //更新选课记录的状态为支付成功x
            xcChooseCourse.setStatus("701001");
            int i =xcChooseCourseMapper.updateById(xcChooseCourse);
            if(i<=0){
                log.error("添加选课记录失败:{}",xcChooseCourse);
                XueChengPlusException.cast("添加选课记录失败");
            }
            //添加我的课程表
            XcCourseTables xcCourseTables = addCourseTables(xcChooseCourse);
            return true;
        }


        return false;
    }

    @Override
    public PageResult<XcCourseTables> mycoursetables(MyCourseTableParams params) {
        //当前页数
        int page = params.getPage();
        //每页记录数
        int pageSize = params.getPage();

        Page<XcChooseCourse> chooseCoursePage = new Page<>(page,pageSize);
        LambdaQueryWrapper<XcChooseCourse> queryWrapper =new LambdaQueryWrapper<XcChooseCourse>()
                .eq(XcChooseCourse::getUserId,params.getUserId());
        //查询数据
        Page<XcChooseCourse> xcChooseCoursePage = xcChooseCourseMapper.selectPage(chooseCoursePage, queryWrapper);
        //获取数据列表
        List<XcChooseCourse> records = xcChooseCoursePage.getRecords();

        PageResult pageResult = new PageResult<>(records,xcChooseCoursePage.getTotal(),page,pageSize);

        return pageResult;
    }

    //添加免费课程,免费课程加入选课记录表
    public XcChooseCourse addFreeCourse(String userId, CoursePublish coursepublish) {
        Long courseId = coursepublish.getId();
        //构造查询条件
        LambdaQueryWrapper<XcChooseCourse> queryWrapper = new LambdaQueryWrapper<XcChooseCourse>()
                .eq(XcChooseCourse::getUserId,userId)
                .eq(XcChooseCourse::getCourseId,courseId)
                .eq(XcChooseCourse::getOrderType,"700001") //免费课程
                .eq(XcChooseCourse::getStatus,"701001"); //选课成功

        List<XcChooseCourse> xcChooseCourses = xcChooseCourseMapper.selectList(queryWrapper);
        //极小概率存在查询多条记录 健壮性判断
        if(xcChooseCourses.size()>0){
            return xcChooseCourses.get(0);
        }
        XcChooseCourse xcChooseCourse = new XcChooseCourse();
        //添加选课记录表
        xcChooseCourse.setCourseId(coursepublish.getId());
        xcChooseCourse.setCourseName(coursepublish.getName());
        xcChooseCourse.setCoursePrice(0f);//免费课程价格为0
        xcChooseCourse.setUserId(userId);
        xcChooseCourse.setCompanyId(coursepublish.getCompanyId());
        xcChooseCourse.setOrderType("700001");//免费课程
        xcChooseCourse.setCreateDate(LocalDateTime.now());
        xcChooseCourse.setStatus("701001");//选课成功
        xcChooseCourse.setValidDays(365);//免费课程默认365
        xcChooseCourse.setValidtimeStart(LocalDateTime.now());
        xcChooseCourse.setValidtimeEnd(LocalDateTime.now().plusDays(365));
        xcChooseCourseMapper.insert(xcChooseCourse);

        return xcChooseCourse;
    }

    //添加收费课程
    public XcChooseCourse addChargeCourse(String userId,CoursePublish coursepublish){

        //如果存在待支付交易记录直接返回
        LambdaQueryWrapper<XcChooseCourse> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper = queryWrapper.eq(XcChooseCourse::getUserId, userId)
                .eq(XcChooseCourse::getCourseId, coursepublish.getId())
                .eq(XcChooseCourse::getOrderType, "700002")//收费订单
                .eq(XcChooseCourse::getStatus, "701002");//待支付
        List<XcChooseCourse> xcChooseCourses = xcChooseCourseMapper.selectList(queryWrapper);
        if (xcChooseCourses != null && xcChooseCourses.size()>0) {
            return xcChooseCourses.get(0);
        }

        XcChooseCourse xcChooseCourse = new XcChooseCourse();
        xcChooseCourse.setCourseId(coursepublish.getId());
        xcChooseCourse.setCourseName(coursepublish.getName());
        xcChooseCourse.setCoursePrice(coursepublish.getPrice());
        xcChooseCourse.setUserId(userId);
        xcChooseCourse.setCompanyId(coursepublish.getCompanyId());
        xcChooseCourse.setOrderType("700002");//收费课程
        xcChooseCourse.setCreateDate(LocalDateTime.now());
        xcChooseCourse.setStatus("701002");//待支付

        xcChooseCourse.setValidDays(coursepublish.getValidDays());
        xcChooseCourse.setValidtimeStart(LocalDateTime.now());
        xcChooseCourse.setValidtimeEnd(LocalDateTime.now().plusDays(coursepublish.getValidDays()));
        xcChooseCourseMapper.insert(xcChooseCourse);
        return xcChooseCourse;
    }
    //添加到我的课程表
    public XcCourseTables addCourseTables(XcChooseCourse xcChooseCourse){
        //选课记录完成且未过期可以添加课程到课程表
        String status = xcChooseCourse.getStatus();
        if (!"701001".equals(status)){
            XueChengPlusException.cast("选课未成功，无法添加到课程表");
        }
        //查询我的课程表
        XcCourseTables xcCourseTables = getXcCourseTables(xcChooseCourse.getUserId(), xcChooseCourse.getCourseId());
        if(xcCourseTables!=null){
            return xcCourseTables;
        }
        XcCourseTables xcCourseTablesNew = new XcCourseTables();
        xcCourseTablesNew.setChooseCourseId(xcChooseCourse.getId());
        xcCourseTablesNew.setUserId(xcChooseCourse.getUserId());
        xcCourseTablesNew.setCourseId(xcChooseCourse.getCourseId());
        xcCourseTablesNew.setCompanyId(xcChooseCourse.getCompanyId());
        xcCourseTablesNew.setCourseName(xcChooseCourse.getCourseName());
        xcCourseTablesNew.setCreateDate(LocalDateTime.now());
        xcCourseTablesNew.setValidtimeStart(xcChooseCourse.getValidtimeStart());
        xcCourseTablesNew.setValidtimeEnd(xcChooseCourse.getValidtimeEnd());
        xcCourseTablesNew.setCourseType(xcChooseCourse.getOrderType());
        xcCourseTablesMapper.insert(xcCourseTablesNew);

        return xcCourseTablesNew;
    }

    /**
     * @description 根据课程和用户id查询我的课程表中某一门课程
     * @param userId
     * @param courseId
     * @author Mr.M
     * @date 2022/10/2 17:07
     */
    public XcCourseTables getXcCourseTables(String userId,Long courseId){
        XcCourseTables xcCourseTables = xcCourseTablesMapper.selectOne(new LambdaQueryWrapper<XcCourseTables>().eq(XcCourseTables::getUserId, userId).eq(XcCourseTables::getCourseId, courseId));
        return xcCourseTables;

    }
}
