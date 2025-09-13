package com.xuecheng.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.search.po.CourseIndex;
import com.xuecheng.search.service.IndexService;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * @description 课程索引管理接口实现
 * @author Mr.M
 * @date 2022/9/25 7:23
 * @version 1.0
 */
@Slf4j
@Service
public class IndexServiceImpl implements IndexService {



 @Autowired
 RestHighLevelClient client;

 /*
 * @param indexName 索引名称
 * @param id 文档id
 * @param object 索引数据对象
 * */
 @Override
 public Boolean addCourseIndex(String indexName,String id,Object object) {
  //准备json文档
  String jsonString = JSON.toJSONString(object);
  // 原dsl语句：POST /course_index/doc/1 course_index ->索引库名 doc ->文档类型 1 ->文档id
  //创建索引请求对象
  IndexRequest indexRequest = new IndexRequest(indexName).id(id);
  //设置请求源 并说明数据格式
  indexRequest.source(jsonString,XContentType.JSON);
  //索引响应对象
  IndexResponse indexResponse = null;
  try {
   indexResponse = client.index(indexRequest, RequestOptions.DEFAULT);
  } catch (IOException e) {
   log.error("添加索引出错:{}",e.getMessage());
   e.printStackTrace();
   XueChengPlusException.cast("添加索引出错");
  }
  // getResult ->从相应结果中获取本次操作的结果类型枚举 .name() 将这个枚举值转为对应的字符串名称
  String name = indexResponse.getResult().name();
  System.out.println(name);
  // equalsIgnoreCase ->忽略大小写比较字符串
  return name.equalsIgnoreCase("created") || name.equalsIgnoreCase("updated");

 }

 @Override
 public Boolean updateCourseIndex(String indexName,String id,Object object) {

  String jsonString = JSON.toJSONString(object);
  UpdateRequest updateRequest = new UpdateRequest(indexName, id);
  //UpdateRequest.doc() ->设置文档数据 仅修改部分文档 IndexRequest.source() ->设置文档数据 替换整个文档
  updateRequest.doc(jsonString, XContentType.JSON);
  UpdateResponse updateResponse = null;
  try {
   updateResponse = client.update(updateRequest, RequestOptions.DEFAULT);
  } catch (IOException e) {
   log.error("更新索引出错:{}",e.getMessage());
   e.printStackTrace();
   XueChengPlusException.cast("更新索引出错");
  }
  // DocWriteResponse ->文档操作结果 无需解析json 是IndexResponse UpdateResponse DeleteResponse的父类
  //这里跟上面方法一样 是拿到本次操作类型的枚举值
  DocWriteResponse.Result result = updateResponse.getResult();
  return result.name().equalsIgnoreCase("updated");

 }

 @Override
 public Boolean deleteCourseIndex(String indexName,String id) {

  //删除索引请求对象
  DeleteRequest deleteRequest = new DeleteRequest(indexName,id);
  //响应对象
  DeleteResponse deleteResponse = null;
  try {
   deleteResponse = client.delete(deleteRequest, RequestOptions.DEFAULT);
  } catch (IOException e) {
   log.error("删除索引出错:{}",e.getMessage());
   e.printStackTrace();
   XueChengPlusException.cast("删除索引出错");
  }
  //获取响应结果
  DocWriteResponse.Result result = deleteResponse.getResult();
  return result.name().equalsIgnoreCase("deleted");
 }
}
