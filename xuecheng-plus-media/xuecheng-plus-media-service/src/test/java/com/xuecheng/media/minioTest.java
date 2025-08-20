package com.xuecheng.media;

import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.Source;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @ClassName minioTest
 * @Description minio测试类
 * @Author
 * @Date 2025/8/19 8:46
 */


public class minioTest {
    MinioClient minioClient =
            MinioClient.builder()
                    .endpoint("http://172.24.224.1:9000")
                    .credentials("minioadmin", "minioadmin")
                    .build();

    @Test
    public void test_upload() throws Exception {

        //通过扩展名得到媒体资源类型 mimeType
        //根据扩展名取出mimeType
        ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(".mp4");
        //枚举类型是未知类型
        String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;//通用mimeType，字节流
        if (extensionMatch != null) {
            mimeType = extensionMatch.getMimeType();
        }

        //上传文件的参数信息
        UploadObjectArgs uploadObjectArgs = UploadObjectArgs.builder()
                .bucket("testbucket")//桶名称
                .filename("D:\\TOOLS\\minio\\test_video\\1.mp4") //指定本地文件路径
//                .object("1.mp4")//对象名 在桶下存储该文件
                .object("test/01/1.mp4")//对象名 放在子目 录下
                .contentType(mimeType)//设置媒体文件类型
                .build();

        //上传文件
        minioClient.uploadObject(uploadObjectArgs);


    }

    //删除文件
    @Test
    public void test_delete() throws Exception {

        //RemoveObjectArgs
        RemoveObjectArgs removeObjectArgs = RemoveObjectArgs.builder().bucket("testbucket").object("1.mp4").build();

        //删除文件 删除单个文件  删除多个文件的api是 removeObjects
        minioClient.removeObject(removeObjectArgs);


    }

    //查询文件 从minio中下载
    @Test
    public void test_getFile() throws Exception {

        GetObjectArgs getObjectArgs = GetObjectArgs.builder().bucket("testbucket").object("test/01/1.mp4").build();
        //查询远程服务获取到一个流对象
        FilterInputStream inputStream = minioClient.getObject(getObjectArgs);
        //指定输出流
        FileOutputStream outputStream = new FileOutputStream(new File("D:\\TOOLS\\minio\\download_video\\1a.mp4"));
        IOUtils.copy(inputStream, outputStream);

        //校验文件的完整性对文件的内容进行md5 minio文件的md5跟下载文件的md5进行对比 一致则下载成功 不使用上面代码的远程流 远程流在进行网络传输的时候 不稳定
        FileInputStream fileInputStream1 = new FileInputStream(new File("D:\\TOOLS\\minio\\test_video\\1.mp4"));
        String source_md5 = DigestUtils.md5Hex(fileInputStream1);
        FileInputStream fileInputStream = new FileInputStream(new File("D:\\TOOLS\\minio\\download_video\\1a.mp4"));
        String local_md5 = DigestUtils.md5Hex(fileInputStream);
        if (source_md5.equals(local_md5)) {
            System.out.println("下载成功");
        }
    }
    //上传分块文件到minio
    @Test
    public void uploadChunk() throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        for (int i = 0; i < 10; i++){
            //上传文件的参数信息
            UploadObjectArgs uploadObjectArgs = UploadObjectArgs.builder()
                    .bucket("testbucket")//桶名称
                    .filename("D:\\TOOLS\\minio\\test_video\\chunk\\"+i) //指定本地文件路径
                    .object("chunk/"+i)//对象名 放在子目 录下
                    .build();

            //上传文件
            minioClient.uploadObject(uploadObjectArgs);
            System.out.println("上传分块文件"+i+"成功");
        }
    }
    //调用minio接口合并分块文件
    @Test
    public void testMerge() throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        //创建源文件列表

        //普通循环写法
        /*List<ComposeSource> sourceList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ComposeSource composeSource = ComposeSource
                    .builder()
                    .bucket("testbucket")
                    .object("chunk/"+i)
                    .build();
            sourceList.add(composeSource);
        }*/
        //采用流式写法
        List<ComposeSource> sourceList= Stream.iterate(0,i->++i).limit(10).map(i->
                ComposeSource.builder().
                        bucket("testbucket").
                        object("chunk/"+i).build()).
                        collect(Collectors.toList());

        //指定合并后的objectName等信息
        ComposeObjectArgs composeObjectArgs = ComposeObjectArgs.builder()
                .bucket("testbucket")
                .object("merge01.mp4")
                .sources(sourceList) //指定源文件信息
                .build();

        //合并文件
        minioClient.composeObject(composeObjectArgs);
    }
}
