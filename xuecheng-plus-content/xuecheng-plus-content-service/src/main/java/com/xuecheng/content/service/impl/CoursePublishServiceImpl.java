package com.xuecheng.content.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xuecheng.base.exception.CommonError;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.config.MultipartSupportConfig;
import com.xuecheng.content.feignClient.MediaServiceClient;
import com.xuecheng.content.api.*;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.*;
import com.xuecheng.content.service.CourseBaseInfoService;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.content.service.TeachplanService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MqMessageService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

/**
 * @ClassName CoursePublishServiceImpl
 * @Description
 * @Author
 * @Date 2025/9/7 15:10
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class CoursePublishServiceImpl implements CoursePublishService {

    private final CourseBaseInfoService courseBaseInfoService;

    private final TeachplanService teachplanService;

    private final CourseBaseMapper courseBaseMapper;

    private final CoursePublishPreMapper coursePublishPreMapper;

    private final CourseMarketMapper courseMarketMapper;

    private final CourseTeacherMapper courseTeacherMapper;

    private final CoursePublishMapper coursePublishMapper;

    private final MqMessageService mqMessageService;

    private final MediaServiceClient mediaServiceClient;



    /*
    * 课程预览
    * */
    @Override
    public CoursePreviewDto getCoursePreviewInfo(Long courseId) {
        CoursePreviewDto coursePreviewDto =new CoursePreviewDto();

        //查询课程基本信息 营销信息
        CourseBaseInfoDto courseBaseInfo = courseBaseInfoService.getCourseBaseInfo(courseId);
        coursePreviewDto.setCourseBase(courseBaseInfo);
        //查询课程计划信息
        List<TeachplanDto> teachplanTree = teachplanService.findTeachplanTree(courseId);
        coursePreviewDto.setTeachplans(teachplanTree);
        return coursePreviewDto;
    }

    /*
    * 提交审核
    * */
    @Override
    @Transactional
    public void commitAudit(Long companyId, Long courseId) {
        //查询课程基本信息
        CourseBaseInfoDto courseBaseInfo = courseBaseInfoService.getCourseBaseInfo(courseId);
        if(courseBaseInfo == null){
            XueChengPlusException.cast("课程不存在");
        }
        //TODO 本机构只能提交本机构的课程信息 根据companyId进行校验
        //查询审核状态 当为已提交时不允许重复提交
        String auditStatus = courseBaseInfo.getAuditStatus();
        if(auditStatus.equals("202003")){
            XueChengPlusException.cast("课程已提交 请等待审核");
        }
        //校验图片是否上传
        String pic = courseBaseInfo.getPic();
        if(StringUtils.isEmpty(pic)){
            XueChengPlusException.cast("请上传课程图片");
        }
        //课程计划非空校验
        List<TeachplanDto> teachplanTree = teachplanService.findTeachplanTree(courseId);
        if(teachplanTree == null || teachplanTree.size() == 0){
            XueChengPlusException.cast("请添加课程计划");
        }

        //查询课程基本信息 师资信息 营销信息并插入到预发布表
        CoursePublishPre coursePublishPre = new CoursePublishPre();
        BeanUtils.copyProperties(courseBaseInfo,coursePublishPre);
        //设置机构id
        coursePublishPre.setCompanyId(companyId);
        //营销信息 ？？ courseBaseInfo中应该存在 重复插入？？
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        //转为json字符串存储在预发布表中(使用fastJson)
        String courseMarketJson = JSON.toJSONString(courseMarket);
        coursePublishPre.setMarket(courseMarketJson);

        //课程计划信息
        String teachplanTreeJson = JSON.toJSONString(teachplanTree);
        coursePublishPre.setTeachplan(teachplanTreeJson);

        //课程师资信息
        List<CourseTeacher> courseTeachers = courseTeacherMapper.selectList(new QueryWrapper<CourseTeacher>().eq("course_id", courseId));
        String courseTeachersJson = JSON.toJSONString(courseTeachers);
        coursePublishPre.setTeachers(courseTeachersJson);
        //设置审核状态
        courseBaseInfo.setAuditStatus("202003");
        //提交时间
        courseBaseInfo.setCreateDate(LocalDateTime.now());
        //对是否存在预发布记录进行判断 存在则进行更新 不存在则进行插入
        CoursePublishPre coursePublishPreDB = coursePublishPreMapper.selectById(courseId);
        if(coursePublishPreDB == null){
            coursePublishPreMapper.insert(coursePublishPre);
        }else{
            //更新
            coursePublishPreMapper.updateById(coursePublishPre);
        }
        //更新课程基本信息表的状态为已提交
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        courseBase.setAuditStatus("202003");
        //更新
        courseBaseMapper.updateById(courseBase);
    }

    /*
    * 发布课程
    * */
    @Override
    public void publish(Long companyId, Long courseId) {

        //查询预发布表数据
        CoursePublishPre coursePublishPre = coursePublishPreMapper.selectById(courseId);
        //状态校验 ->没有审核通过不允许发布
        String status = coursePublishPre.getStatus();
        if(!status.equals("202004")){
            XueChengPlusException.cast("课程没有审核通过不允许发布");
        }
        //复制课程信息到发布表
        CoursePublish coursePublish = new CoursePublish();
        BeanUtils.copyProperties(coursePublishPre,coursePublish);
        //查询发布表 有则更新 没有则插入
        CoursePublish coursePublishDB = coursePublishMapper.selectById(courseId);
        if(coursePublishDB == null){
            coursePublishMapper.insert(coursePublish);
        }else{
            coursePublishMapper.updateById(coursePublish);
        }

        //写入消息表 使用消息 sdk工具包实现
        saveCoursePublishMessage(courseId);
        //删除预发布表数据
        coursePublishPreMapper.deleteById(courseId);


    }
    /*
    * 课程页面静态化
    * */
    @Override
    public File generateCourseHtml(Long courseId) {
        //最终的静态化页面文件
        File htmlFile = null;

        try{
            //创建配置对象
            Configuration configuration = new Configuration(Configuration.getVersion());

            //获取资源文件路径
            String classPath = this.getClass().getResource("/").getPath();
            //指定模板的目录
            configuration.setDirectoryForTemplateLoading(new File(classPath+"/templates/"));
            //指定编码格式
            configuration.setDefaultEncoding("utf-8");

            //获取模板
            Template template = configuration.getTemplate("course_template.html");
            //准备页面数据
            CoursePreviewDto coursePreviewDto = this.getCoursePreviewInfo(courseId);
            //封装后端数据 与前端访问的数据一致
            HashMap<String, Object> map = new HashMap<>();
            map.put("model",coursePreviewDto);

            //使用FreeMarKer工具类对页面进行静态化
            String html = FreeMarkerTemplateUtils.processTemplateIntoString(template, map);
            //将字符串转化成流 输出到文件中
            //输入流
            InputStream inputStream = IOUtils.toInputStream(html, "utf-8");
            //创建临时文件
             htmlFile = File.createTempFile("coursePublish",".html");
            //输出文件 这里仅仅做测试 输出的文件规则为id+后缀
            FileOutputStream outputStream = new FileOutputStream(htmlFile);
            //拷贝文件 将html写入到文件中
            IOUtils.copy(inputStream,outputStream);
        } catch (Exception e){
            log.info("课程页面静态化出现问题，课程id：{}",courseId);
            e.printStackTrace();
        }

        return htmlFile;
    }

    /*
    * 上传静态化页面的文件
    * */
    @Override
    public void uploadCourseHtml(Long courseId, File file) {
       try{
           //将file文件转成MultipartFile
           MultipartFile multipartFile = MultipartSupportConfig.getMultipartFile(file);
            //远程调用得到返回值
           String upload = mediaServiceClient.upload(multipartFile,"course/"+courseId+".html");
           if(upload == null){
               log.debug("远程调用走降级逻辑得到上传的结果为null");
               XueChengPlusException.cast("上传静态化页面文件失败");
           }
       }catch (Exception e){
           log.info("上传静态化页面文件失败，课程id：{}",courseId);
           XueChengPlusException.cast("上传静态化页面文件失败");
           e.printStackTrace();
       }
    }

    @Override
    public CoursePublish getCoursePublish(Long courseId) {
        CoursePublish coursePublish = coursePublishMapper.selectById(courseId);
        return coursePublish;
    }

    /**
     * @description 保存消息表记录
     * @param courseId  课程id
     * @return void
     * @author huang
     * @date 2025.9.10
     */
    private void saveCoursePublishMessage(Long courseId){
        MqMessage mqMessage = mqMessageService.addMessage("course_publish", String.valueOf(courseId), null, null);
        if(mqMessage==null){
            XueChengPlusException.cast(CommonError.UNKOWN_ERROR);
        }
    }


}
