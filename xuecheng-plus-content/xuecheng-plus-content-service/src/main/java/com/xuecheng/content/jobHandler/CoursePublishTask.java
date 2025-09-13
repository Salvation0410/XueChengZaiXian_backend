package com.xuecheng.content.jobHandler;

import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.feignClient.CourseIndex;
import com.xuecheng.content.feignClient.SearchServiceClient;
import com.xuecheng.content.mapper.CoursePublishMapper;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MessageProcessAbstract;
import com.xuecheng.messagesdk.service.MqMessageService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * @ClassName CoursePublishTask
 * @Description 课程发布的任务类
 * @Author huang
 * @Date 2025/9/10
 */


@Slf4j
@Component
public class CoursePublishTask extends MessageProcessAbstract {

    @Autowired
    CoursePublishService coursePublishService;

    @Autowired
    SearchServiceClient searchServiceClient;

    @Autowired
    CoursePublishMapper coursePublishMapper;

    public CoursePublishTask(MqMessageService mqMessageService) {
        super(mqMessageService);
    }
    /*
    * 任务调度入口
    * */
    @XxlJob("CoursePublishJobHandler")
    public void coursePublishTask() throws Exception {
        //分片参数
        int shardIndex = XxlJobHelper.getShardIndex();     //执行器的分片序号 0开始
        int shardTotal = XxlJobHelper.getShardTotal();     //执行器的分片总数

        //调用MqMessageService封装好的方法
        process(shardIndex,shardTotal,"course_publish",30,60);
    }


    /*
    * 执行课程发布任务的逻辑 出现异常则表示任务失败
    * */
    @Override
    public boolean execute(MqMessage mqMessage) {
        //从mqMessage中获取课程id
        Long courseId = Long.parseLong(mqMessage.getBusinessKey1());

        //课程页面静态化上传到minio
        generateCourseHtml(mqMessage,courseId);
        //向es中写索引数据
        saveCourseIndex(mqMessage,courseId);

        //向redis中写缓存数据
        saveCourseCache(mqMessage,courseId);

        //返回结果表示任务完成

        return true;
    }

    /*
    * 生成页面静态化页面并上传到minio分布式文件系统
    * */
    private void generateCourseHtml(MqMessage mqMessage,Long courseId){
        //消息id
        Long taskId = mqMessage.getId();
        MqMessageService mqMessageService = this.getMqMessageService();

        //这里为了保证任务结果的一致性 需要做任务的幂等性处理
        //查询数据库获取任务的执行状态
        int stageOne = mqMessageService.getStageOne(taskId);
        if(stageOne>0){
            log.debug("生成静态化页面已完成 无需进行处理");
            return;
        }
        //开始进行课程静态化操作 TODO
        File file = coursePublishService.generateCourseHtml(courseId);
        if(file == null){
            XueChengPlusException.cast("生成静态化页面为空");
        }
        //将生成的html文件上传到Minio
        coursePublishService.uploadCourseHtml(courseId,file);

        //任务完成 更改任务的执行状态
        mqMessageService.completedStageOne(taskId);
    }
    /*
    * 保存课程索引信息 (从es中快速查询课程信息)
    * */
    private void saveCourseIndex(MqMessage mqMessage,Long courseId){
        //消息id
        Long taskId = mqMessage.getId();
        MqMessageService mqMessageService = this.getMqMessageService();

        //这里为了保证任务结果一致性 需要做任务的幂等性处理
        //查询数据库获取任务的执行状态
        int stageTwo = mqMessageService.getStageTwo(taskId);
        if(stageTwo>0){
            log.debug("保存课程索引信息已完成 无需处理");
            return;
        }
        //查询课程信息 远程调用搜索服务添加索引接口

        //1.从课程发布表查询课程信息
        CoursePublish coursePublish = coursePublishMapper.selectById(courseId);
        CourseIndex courseIndex = new CourseIndex();
        BeanUtils.copyProperties(coursePublish,courseIndex);
        //2.远程调用接口
        Boolean add = searchServiceClient.add(courseIndex);
        if(add ==  false){
            XueChengPlusException.cast("远程调用课程添加索引服务失败");

        }


        //任务完成 更新任务执行状态
        mqMessageService.completedStageTwo(taskId);
    }

    /*
    * 添加课程信息到redis缓存中
    * */
    public void saveCourseCache(MqMessage mqMessage,long courseId){
        log.debug("将课程信息缓存至redis,课程id:{}",courseId);
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }
}
