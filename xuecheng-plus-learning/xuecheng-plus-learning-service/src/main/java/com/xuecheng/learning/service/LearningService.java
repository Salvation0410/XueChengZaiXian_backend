package com.xuecheng.learning.service;

import com.xuecheng.base.model.RestResponse;

/**
 * @author huang
 * @version 1.0
 * @description 在线学习相关方法
 * @date 2025/9/23
 */
public interface LearningService {
    /**
     * @description 获取教学视频
     * @param courseId 课程id
     * @param teachplanId 课程计划id
     * @param mediaId 视频文件id
     * @return com.xuecheng.base.model.RestResponse<java.lang.String>
     * @author huang
     * @date 2025/9/23
     */
    public RestResponse<String> getVideo(String userId, Long courseId, Long teachplanId, String mediaId);
}

