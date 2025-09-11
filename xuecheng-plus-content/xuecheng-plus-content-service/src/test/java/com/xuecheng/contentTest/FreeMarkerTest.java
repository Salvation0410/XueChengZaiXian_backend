package com.xuecheng.contentTest;

import com.xuecheng.content.config.XxlJobConfig;
import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.service.CoursePublishService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

/**
 * @ClassName FreeMarkerTest
 * @Description FreeMarker测试类
 * @Author huang
 * @Date 2025/9/10
 */

@SpringBootTest
@RequiredArgsConstructor
public class FreeMarkerTest {

    //排除xxl-job-admin的配置影响测试
    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public XxlJobConfig mockXxlJobConfig() {
            // 返回一个空的或模拟的配置对象
            return new XxlJobConfig();
        }
    }

    @Autowired
    private CoursePublishService coursePublishService;

    @Test
    public void testGenerateHtmlByTemplate() throws IOException, TemplateException {
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
        CoursePreviewDto coursePreviewDto = coursePublishService.getCoursePreviewInfo(120L);
        //封装后端数据 与前端访问的数据一致
        HashMap map = new HashMap<>();
        map.put("model",coursePreviewDto);

        //使用FreeMarKer工具类对页面进行静态化
        String html = FreeMarkerTemplateUtils.processTemplateIntoString(template, map);
        //将字符串转化成流 输出到文件中
        //输入流
        InputStream inputStream = IOUtils.toInputStream(html, "utf-8");
        //输出文件 这里仅仅做测试 输出的文件规则为id+后缀
        FileOutputStream outputStream = new FileOutputStream(new File("D:\\xczx\\IO_FileTest\\120.html"));
        //拷贝文件 将html写入到文件中
        IOUtils.copy(inputStream,outputStream);

    }

}
