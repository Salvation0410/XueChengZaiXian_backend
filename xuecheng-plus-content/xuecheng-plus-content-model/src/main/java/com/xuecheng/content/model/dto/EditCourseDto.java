package com.xuecheng.content.model.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author Mr.M
 * @version 1.0
 * @description 修改课程的dto 只比新增多了一个id
 * @date 2023/2/14
 */
@Data
public class EditCourseDto extends AddCourseDto {

 @ApiModelProperty(value = "课程id", required = true)
 private Long id;

}
