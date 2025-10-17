package com.xuecheng.learning;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.learning.mapper.XcChooseCourseMapper;
import com.xuecheng.learning.mapper.XcCourseTablesMapper;
import com.xuecheng.learning.model.dto.XcChooseCourseDto;
import com.xuecheng.learning.model.po.XcChooseCourse;
import com.xuecheng.learning.model.po.XcCourseTables;
import com.xuecheng.learning.service.MyCourseTablesService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @author huang
 * @version 1.0
 * @description
 * @date 2025/10/17
 */
@SpringBootTest
public class TestRedisson {

    @Autowired
    private MyCourseTablesService chooseCourseService;

    @Autowired
    private XcChooseCourseMapper xcChooseCourseMapper;

    @Autowired
    private XcCourseTablesMapper xcCourseTablesMapper;

    @Test
    public void testConcurrentChooseCourse() throws InterruptedException {
        String userId = "52";
        Long courseId = 2L;
        int threadCount = 50;

        // 清理测试数据，确保每次测试都是全新的
        cleanupTestData(userId, courseId);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        List<XcChooseCourseDto> results = Collections.synchronizedList(new ArrayList<>());
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        // 创建多个线程同时选课
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await(); // 等待同时开始
                    XcChooseCourseDto result = chooseCourseService.addChooseCourse(userId, courseId);
                    results.add(result);
                } catch (Exception e) {
                    exceptions.add(e);
                } finally {
                    endLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown(); // 同时开始
        endLatch.await(); // 等待所有线程完成

        // 验证结果
        System.out.println("成功结果数量: " + results.size());
        System.out.println("异常数量: " + exceptions.size());

        // 关键断言：选课记录应该只有一条
        LambdaQueryWrapper<XcChooseCourse> queryWrapper = new LambdaQueryWrapper<XcChooseCourse>()
                .eq(XcChooseCourse::getUserId, userId)
                .eq(XcChooseCourse::getCourseId, courseId);
        int actualRecordCount = xcChooseCourseMapper.selectCount(queryWrapper);

        System.out.println("数据库中的实际记录数: " + actualRecordCount);
        System.out.println("有效结果数量: " + results.stream()
                .filter(r -> r != null && r.getLearnStatus() != null)
                .count());

        // 修正断言：由于你的业务逻辑会返回已存在记录，所以成功结果可能不止1个
        // 但数据库记录应该只有1条
        Assertions.assertEquals(1, actualRecordCount, "应该只有一条选课记录");

        // 验证Redisson锁的效果
        if (actualRecordCount == 1) {
            System.out.println("✅ Redisson分布式锁测试通过：成功防止重复选课");
        } else {
            System.out.println("❌ Redisson分布式锁测试失败：存在重复选课记录");
        }
    }

    private void cleanupTestData(String userId, Long courseId) {
        // 清理选课记录
        LambdaQueryWrapper<XcChooseCourse> chooseCourseWrapper = new LambdaQueryWrapper<XcChooseCourse>()
                .eq(XcChooseCourse::getUserId, userId)
                .eq(XcChooseCourse::getCourseId, courseId);
        xcChooseCourseMapper.delete(chooseCourseWrapper);

        // 清理选课记录
        LambdaQueryWrapper<XcCourseTables> courseTablesWrapper = new LambdaQueryWrapper<XcCourseTables>()
                .eq(XcCourseTables::getUserId, userId)
                .eq(XcCourseTables::getCourseId, courseId);
        // 清理课程表记录（如果有的话）
        xcCourseTablesMapper.delete(courseTablesWrapper);
    }
}