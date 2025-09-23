package com.xuecheng.learning.service;

import com.xuecheng.base.model.PageResult;
import com.xuecheng.learning.model.dto.MyCourseTableParams;
import com.xuecheng.learning.model.dto.XcChooseCourseDto;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.xuecheng.learning.model.po.XcCourseTables;

/**
 * @author huang
 * @version 1.0
 * @description 课程选课
 * @date 2025/9/19
 */
public interface MyCourseTablesService {
    /**
     * @description 添加选课
     * @param userId 用户id
     * @param courseId 课程id
     * @author Mr.M
     * @date 2022/10/24 17:33
     */
    public XcChooseCourseDto addChooseCourse(String userId, Long courseId);
    /**
     * @description 判断学习资格
     * @param userId
     * @param courseId
     * @return XcCourseTablesDto 学习资格状态 [{"code":"702001","desc":"正常学习"},{"code":"702002","desc":"没有选课或选课后没有支付"},{"code":"702003","desc":"已过期需要申请续期或重新支付"}]
     * @author Mr.M
     * @date 2022/10/3 7:37
     */
    public XcCourseTablesDto getLearningStatus(String userId, Long courseId);

    /**
     * @description 根据mq的消息更新选课状态并插入数据到课程表
     * @param chooseCourseId 选课记录id
     * @return void
     * @author huang
     * @date 2025/9/19
     * */
    public boolean saveChooseCourseSuccess(String chooseCourseId);

    /**
     * @description 分页查询我的课程表
     * @param params
     * @author huang
     * @date huang
     */
    public PageResult<XcCourseTables> mycoursetables(MyCourseTableParams params);

}

