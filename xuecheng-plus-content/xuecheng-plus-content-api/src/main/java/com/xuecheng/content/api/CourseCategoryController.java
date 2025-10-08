package com.xuecheng.content.api;

import com.xuecheng.content.Enum.CommonEnum;
import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.xuecheng.content.service.CourseCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @ClassName CourseCategoryController
 * @Description 课程分类相关接口
 * @Author huang
 * @Date 2025/8/13
 */


@RestController
public class CourseCategoryController {

    @Autowired
    CourseCategoryService courseCategoryService;

    @GetMapping("/course-category/tree-nodes")
    public List<CourseCategoryTreeDto> queryTreeNodes(){
        //传入根节点
        return courseCategoryService.queryTreeNodes(CommonEnum.ROOT_NODE_ID.getValue());
    }
}
