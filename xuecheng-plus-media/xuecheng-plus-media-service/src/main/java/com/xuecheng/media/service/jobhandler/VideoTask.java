package com.xuecheng.media.service.jobhandler;

import com.xuecheng.base.utils.Mp4VideoUtil;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFileProcessService;
import com.xuecheng.media.service.MediaFileService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * XxlJob开发示例（Bean模式）
 *
 * 开发步骤：
 *      1、任务开发：在Spring Bean实例中，开发Job方法；
 *      2、注解配置：为Job方法添加注解 "@XxlJob(value="自定义jobhandler名称", init = "JobHandler初始化方法", destroy = "JobHandler销毁方法")"，注解value值对应的是调度中心新建任务的JobHandler属性的值。
 *      3、执行日志：需要通过 "XxlJobHelper.log" 打印执行日志；
 *      4、任务结果：默认任务结果为 "成功" 状态，不需要主动设置；如有诉求，比如设置任务结果为失败，可以通过 "XxlJobHelper.handleFail/handleSuccess" 自主设置任务结果；
 *
 * @author xuxueli 2019-12-11 21:52:51
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class VideoTask {

    private final MediaFileProcessService mediaFileProcessService;

    //从nacos配置中心中获取ffmpeg路径
    @Value("${videoprocess.ffmpegpath}")
    private String ffmpegpath;

    private final MediaFileService mediaFileService;

    @Qualifier("mediaProcessThreadPool")
    private final ExecutorService executorService;


    /**
     * 视频处理任务
     */
    @XxlJob("videoJobHandler")
    public void shardingJobHandler() throws Exception {

        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex(); //执行器的序号 从0开始
        int shardTotal = XxlJobHelper.getShardTotal(); //执行器的总数

        //查询cpu的核心数 代表能开启几个线程
        int processors = Runtime.getRuntime().availableProcessors();

        //1.查询任务 注：第三个参数表示查询的任务数量
        List<MediaProcess> mediaProcessList =  mediaFileProcessService.getMediaProcessList(shardIndex, shardTotal, processors);
        //1.1查询任务数量确定线程池的大小
        int size = mediaProcessList.size();
        if(size<=0){
            log.debug("任务数量：{}",size);
            return;
        }
        //1.2 创建线程池 已提取到Bean
        //ExecutorService executorService = Executors.newFixedThreadPool(size);
        //创建一个计数器
        CountDownLatch countDownLatch = new CountDownLatch(size);
        mediaProcessList.forEach(mediaProcess -> {
            executorService.execute(() -> {
                //任务执行逻辑
                try {
                    //2.通过乐观锁开启任务
                    //2.1 获取任务id
                    Long taskId = mediaProcess.getId();
                    //获取文件md5值 即文件id
                    String fileId = mediaProcess.getFileId();
                    //2.2 通过乐观锁抢占任务
                    boolean b = mediaFileProcessService.startTask(taskId);
                    if (!b) {
                        log.debug("抢占任务失败，任务id:{}", taskId);
                        return;
                    }
                    //获取桶的以及文件名称
                    String bucket = mediaProcess.getBucket();
                    String objectName = mediaProcess.getFilePath();

                    // 下载视频到本地
                    File file = mediaFileService.downloadFileFromMinIO(bucket, objectName);
                    if (file == null) {
                        log.debug("下载视频失败，bucket:{},objectName:{},任务id:{}",bucket,objectName, taskId);
                        //保存视频下载失败的结果
                        mediaFileProcessService.saveProcessFinishStatus(taskId, "3", fileId, null, "下载视频到本地失败");
                        return;
                    }

                    //3.执行任务转码 使用ffmpeg工具类

                    //源avi文件的路径
                    String video_path = file.getAbsolutePath();
                    //转换后mp4文件的名称
                    String mp4_name = fileId + ".mp4";
                    //转换后mp4文件的路径
                    //先创建一个临时文件 作为转换后的文件 (放在try-catch外面的原因是放在try里面访问不了)
                    File mp4File = null;
                    try {
                        mp4File = File.createTempFile("minio", ".mp4");
                    } catch (IOException e) {
                        log.debug("创建临时文件失败，任务id:{}", taskId);
                        //保存任务处理结果
                        mediaFileProcessService.saveProcessFinishStatus(taskId, "3", fileId, null, "创建临时文件失败");
                        return;
                    }

                    String mp4_path = mp4File.getAbsolutePath();
                    //创建工具类对象
                    Mp4VideoUtil videoUtil = new Mp4VideoUtil(ffmpegpath, video_path, mp4_name, mp4_path);
                    //开始视频转换，成功将返回success
                    String s = videoUtil.generateMp4();
                    if (!s.equals("success")) {
                        log.debug("视频转换失败，原因{}，bucket:{}任务id:{}", s, bucket, taskId);
                        mediaFileProcessService.saveProcessFinishStatus(taskId, "3", fileId, null, s);
                        return;
                    }


                    //4.上传视频到minio
                    boolean b1 = mediaFileService.addMediaFilesToMinIO(mp4File.getAbsolutePath(), "video/mp4", bucket, objectName);
                    if (!b1) {
                        log.debug("上传视频失败，任务id:{}", taskId);
                        mediaFileProcessService.saveProcessFinishStatus(taskId, "3", fileId, null, "上传MP4视频到minio失败");
                        return;
                    }

                    //5.保存任务处理结果
                    //获取url
                    String url = mediaFileService.getFilePathByMd5(fileId, ".mp4");
                    //只在任务状态为成功
                    mediaFileProcessService.saveProcessFinishStatus(taskId, "2", fileId, url, null);
                }finally{
                    countDownLatch.countDown();
                }
            });

        });
        //阻塞线程 指定最大限制的等待时间 阻塞最多等待一定的时间后就解除阻塞
        countDownLatch.await(30, TimeUnit.MINUTES);

    }



}
