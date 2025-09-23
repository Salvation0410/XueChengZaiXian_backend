package com.xuecheng.learning.service.Impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.api.R;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.learning.feignclient.ContentServiceClient;
import com.xuecheng.learning.feignclient.MediaServiceClient;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.xuecheng.learning.service.LearningService;
import com.xuecheng.learning.service.MyCourseTablesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author huang
 * @version 1.0
 * @description
 * @date 2025/9/23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LearningServiceImpl implements LearningService {

    private final MyCourseTablesService myCourseTablesService;
    private final MediaServiceClient mediaServiceClient;
    private final ContentServiceClient contentServiceClient;

    /*
    * 获取视频
    * */
    @Override
    public RestResponse<String> getVideo(String userId, Long courseId, Long teachplanId, String mediaId) {
        //获取课程信息
        CoursePublish coursepublish = contentServiceClient.getCoursepublish(courseId);
        if(coursepublish==null){
            return RestResponse.validfail("课程不存在");
        }
        //获取课程计划信息
        String teachplanJson = coursepublish.getTeachplan();
        List<Teachplan> teachplans = JSON.parseArray(teachplanJson,Teachplan.class);
        //判断是否支持试看 TODO
        String is_preview = teachplans.get(0).getIsPreview();
        if("1".equals(is_preview)){
            RestResponse<String> playUrl = mediaServiceClient.getPlayUrlByMediaId(mediaId);
            return playUrl;
        }

        //判断学习资格
        if(StringUtils.isNotEmpty(userId)){
            //用户已经登陆
            XcCourseTablesDto xcCourseTablesDto = myCourseTablesService.getLearningStatus(userId, courseId);
            String learnStatus = xcCourseTablesDto.getLearnStatus();
            if("702002".equals(learnStatus)){
                return RestResponse.validfail("没有选课或选课后没有支付");
            } else if ("702003".equals(learnStatus)) {
                return RestResponse.validfail("选课已过期");
            }else{
                //返回视频的播放地址
                // 远程调用媒资管理模块获取视频url
                RestResponse<String> playUrl = mediaServiceClient.getPlayUrlByMediaId(mediaId);
                return playUrl;
            }
        }
        //用户没登陆的情况 查询课程信息 如果不收费则可以正常学习
        String charge = coursepublish.getCharge();
        if("201000".equals( charge)){
            //有资格学习远程调用获取视频播放地址
            RestResponse<String> playUrl = mediaServiceClient.getPlayUrlByMediaId(mediaId);
            return playUrl;

        }

        return RestResponse.validfail("该课程没有选课");
    }
}
