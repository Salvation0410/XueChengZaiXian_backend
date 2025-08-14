package com.xuecheng.contentTest;

import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.model.po.CourseCategory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @ClassName Test
 * @Description
 * @Author
 * @Date 2025/8/13 16:31
 */


@SpringBootTest
public class TestMapper {

    @Autowired
    CourseCategoryMapper courseCategoryMapper;


    @Test
    public void testCourseCategoryMapper() {
        CourseCategory courseCategory = courseCategoryMapper.selectById("1");
        System.out.println("++++++++++++++++"+courseCategory);
    }


}
