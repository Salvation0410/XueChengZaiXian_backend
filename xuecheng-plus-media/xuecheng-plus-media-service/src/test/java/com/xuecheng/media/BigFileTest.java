package com.xuecheng.media;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @ClassName BigFileTest
 * @Description
 * @Author
 * @Date 2025/8/19 16:07
 */


public class BigFileTest {

    //测试文件分块方法
    @Test
    public void testChunk() throws IOException {
        //源文件
        File sourceFile = new File("D:\\TOOLS\\minio\\test_video\\sing.mp4");
        String chunkPath = "D:/TOOLS/minio/test_video/chunk/";
        //分块文件
        File chunkFolder = new File(chunkPath);
        if (!chunkFolder.exists()) {
            chunkFolder.mkdirs();
        }
        //分块大小 1MB
        long chunkSize = 1024 * 1024 * 1;
        //分块数量 Math.ceil向上取整
        long chunkNum = (long) Math.ceil(sourceFile.length() * 1.0 / chunkSize);
        System.out.println("分块总数："+chunkNum);
        //缓冲区大小 用来读取数据
        byte[] b = new byte[1024];
        //使用RandomAccessFile访问文件 RandomAccessFile可以访问文件，可以指定模式 读或写 r表示操作为读
        RandomAccessFile raf_read = new RandomAccessFile(sourceFile, "r");
        //分块
        for (int i = 0; i < chunkNum; i++) {
            //创建分块文件
            File file = new File(chunkPath + i); //i为分块编号 路径为 chunkPath/i
            if(file.exists()){
                file.delete();
            }

            boolean newFile = file.createNewFile();
            if (newFile) {
                //向分块文件中写数据 rw表示读写模式
                RandomAccessFile raf_write = new RandomAccessFile(file, "rw");
                int len = -1;
                while ((len = raf_read.read(b)) != -1) {
                    //写数据 从0开始写 len为实际读取的长度
                    raf_write.write(b, 0, len);
                    if (file.length() >= chunkSize) {
                        break;
                    }
                }
                raf_write.close();
                System.out.println("完成分块"+i);
            }

        }
        raf_read.close();

    }

    @Test
    public void testMerge() throws IOException {
        //块文件目录
        File chunkFolder = new File("D:\\TOOLS\\minio\\test_video\\chunk");
        //原始文件
        File originalFile = new File("D:\\TOOLS\\minio\\test_video\\sing.mp4");
        //合并文件
        File mergeFile = new File("D:\\TOOLS\\minio\\test_video\\collect\\Collect01.mp4");
        if (mergeFile.exists()) {
            mergeFile.delete();
        }
        //创建新的合并文件
        mergeFile.createNewFile();
        //用于写文件
        RandomAccessFile raf_write = new RandomAccessFile(mergeFile, "rw");
        //指针指向文件顶端
        raf_write.seek(0);
        //缓冲区
        byte[] b = new byte[1024];
        //分块列表
        File[] fileArray = chunkFolder.listFiles();
        // 转成集合，便于排序
        List<File> fileList = Arrays.asList(fileArray);
        // 从小到大排序 这里表示升序 根据compare的规则确定升序还是降序
        Collections.sort(fileList, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return Integer.parseInt(o1.getName()) - Integer.parseInt(o2.getName());
            }
        });

        //合并文件
        for (File chunkFile : fileList) {
            RandomAccessFile raf_read = new RandomAccessFile(chunkFile, "rw");
            int len = -1;
            while ((len = raf_read.read(b)) != -1) {
                raf_write.write(b, 0, len);

            }
            raf_read.close();
        }
        raf_write.close();

        //校验文件
        try (

                FileInputStream fileInputStream = new FileInputStream(originalFile);
                FileInputStream mergeFileStream = new FileInputStream(mergeFile);

        ) {
            //取出原始文件的md5
            String originalMd5 = DigestUtils.md5Hex(fileInputStream);
            //取出合并文件的md5进行比较
            String mergeFileMd5 = DigestUtils.md5Hex(mergeFileStream);
            if (originalMd5.equals(mergeFileMd5)) {
                System.out.println("合并文件成功");
            } else {
                System.out.println("合并文件失败");
            }

        }


    }



}
