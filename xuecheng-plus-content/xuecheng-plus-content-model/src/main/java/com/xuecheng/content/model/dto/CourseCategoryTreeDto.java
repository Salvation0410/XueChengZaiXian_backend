package com.xuecheng.content.model.dto;


import com.xuecheng.content.model.po.CourseCategory;
import lombok.Data;

import java.util.List;

/**
 * @ClassName CourseCategoryTreeDto
 * @Description
 * @Author
 * @Date 2025/8/13 9:37
 */

@Data
public class CourseCategoryTreeDto extends CourseCategory {
    //子节点
    List<CourseCategoryTreeDto> childrenTreeNodes;
}
