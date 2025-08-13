package com.xuecheng.content.service.impl;

import com.sun.xml.internal.bind.v2.TODO;
import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.xuecheng.content.service.CourseCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Mr.M
 * @version 1.0
 * @description 课程分类相关服务层
 * @date 2023/2/12 14:49
 */

@Slf4j
@Service
public class CourseCategoryServiceImpl implements CourseCategoryService {

    @Autowired
    CourseCategoryMapper courseCategoryMapper;

    @Override
    public List<CourseCategoryTreeDto> queryTreeNodes(String id) {
        //调用mapper递归查询出分类信息
        List<CourseCategoryTreeDto> courseCategoryTreeDtos = courseCategoryMapper.selectTreeNodes(id);
        //根据前端的接口数据 将courseCategoryTreeDtos封装成List<CourseCategoryTreeDto>类型 数据库和前端返回数据类型不一样
        //先将List转成map key就是节点的id value就是CourseCategoryTreeDto对象 从而方便从map获取节点

        Map<String,CourseCategoryTreeDto> mapTemp =courseCategoryTreeDtos.stream()
                //排除根节点
                .filter(item->!id.equals(item.getId()))
                .collect(Collectors.toMap(key->key.getId(),value->value,(key1,key2)->key2));

        //存放返回结果集
        List<CourseCategoryTreeDto> courseCategoryList = new ArrayList<>();

        //遍历List<CourseCategoryTreeDto> 一边遍历一边找子节点放在父节点的childrenTreeNodes
        courseCategoryTreeDtos.stream()
                .filter(item->!id.equals(item.getId()))
                .forEach(item->{
                    if(item.getParentid().equals(id)){
                           courseCategoryList.add(item);
                    }
                //找到当前节点的父节点
                CourseCategoryTreeDto courseCategoryParent = mapTemp.get(item.getParentid());
                if(courseCategoryParent!=null){
                    if(courseCategoryParent.getChildrenTreeNodes() == null){
                        //该父节点的ChildrenTreeNodes属性空时new一个新的集合 放该节点的子节点
                        courseCategoryParent.setChildrenTreeNodes(new ArrayList<CourseCategoryTreeDto>());
                    }
                    courseCategoryParent.getChildrenTreeNodes().add(item);
                }

                });

        return courseCategoryList;
    }
}
