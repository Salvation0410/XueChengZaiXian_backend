package com.xuecheng.contentTest;

import com.xuecheng.content.config.MultipartSupportConfig;
import com.xuecheng.content.feignClient.MediaServiceClient;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

/**
 * @ClassName FileUploadTest
 * @Description
 * @Author
 * @Date 2025/9/11 20:25
 */


@SpringBootTest
public class FileUploadTest {
    @Autowired
    private  MediaServiceClient mediaServiceClient;

    @Test
    public void testUpload() throws IOException {
        File file = new File("D:\\xczx\\IO_FileTest\\120.html");
        MultipartFile multipartFile = MultipartSupportConfig.getMultipartFile(file);

        mediaServiceClient.upload(multipartFile,"course/120.html");
    }
}
