package com.xuecheng.content.mapper;

import com.xuecheng.content.model.dto.BindTeachPlanMediaDto;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.service.TeachplanService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @ClassName TeachPlanController
 * @Description 课程计划管理模块相关接口
 * @Author huang
 * @Date 2025/8/14
 */


@RestController
@Slf4j
@Api(tags = "课程计划管理接口")
@RequiredArgsConstructor
public class TeachPlanController {

    @Autowired
    private TeachplanMapper teachplanMapper;


    private final TeachplanService teachplanService;

    @GetMapping("/teachplan/{courseId}/tree-nodes")
    @ApiOperation("查询课程计划树形结构")
    public List<TeachplanDto> getTreeNodes(@PathVariable Long courseId){
        List<TeachplanDto> teachplanTree = teachplanMapper.selectTreeNodes(courseId);
        return teachplanTree;
    }

    @PostMapping("/teachplan")
    @ApiOperation("课程计划创建或修改")
    public void saveTeachPlan(@RequestBody SaveTeachplanDto saveTeachplanDto){
        teachplanService.saveTeachplan(saveTeachplanDto);
    }

    @DeleteMapping("/teachplan/{courseId}")
    @ApiOperation("课程计划删除")
    public void deleteTeachPlan(@PathVariable Long courseId){
        log.info("删除课程计划：课程id:{}", courseId);
        teachplanService.deleteTeachPlan(courseId);
    }
    @PostMapping("/teachplan/moveup/{id}")
    @ApiOperation("课程计划上移")
    public void moveUp(@PathVariable Long id){
        log.info("课程计划上移：id:{}", id);
        teachplanService.moveUp(id);
    }

    @PostMapping("/teachplan/movedown/{id}")
    @ApiOperation("课程计划上移")
    public void moveDown(@PathVariable Long id){
        log.info("课程计划上移：id:{}", id);
        teachplanService.moveDown(id);
    }
    @ApiOperation(value = "课程计划和媒资信息绑定")
    @PostMapping("/teachplan/association/media")
    public void associationMedia(@RequestBody BindTeachPlanMediaDto bindTeachPlanMediaDto){
        teachplanService.associationMedia(bindTeachPlanMediaDto);
    }

}
