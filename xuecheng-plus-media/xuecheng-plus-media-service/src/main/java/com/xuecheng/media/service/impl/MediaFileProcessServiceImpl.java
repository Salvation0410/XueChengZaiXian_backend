package com.xuecheng.media.service.impl;


import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xuecheng.base.Enum.CommonEnum;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.mapper.MediaProcessHistoryMapper;
import com.xuecheng.media.mapper.MediaProcessMapper;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.model.po.MediaProcessHistory;
import com.xuecheng.media.service.MediaFileProcessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @ClassName MediaFileProcessServiceImpl
 * @Description 任务处理接口实现
 * @Author huang
 * @Date 2025/9/4 11:12
 */


@Service
@Slf4j
@RequiredArgsConstructor
public class MediaFileProcessServiceImpl implements MediaFileProcessService {

    private final MediaProcessMapper mediaProcessMapper;

    private final MediaFilesMapper mediaFilesMapper;

    private final MediaProcessHistoryMapper mediaProcessHistoryMapper;

    /*
    * 查询任务
    * */
    @Override
    public List<MediaProcess> getMediaProcessList(int shardIndex, int shardTotal, int count) {
        return mediaProcessMapper.selectListByShardIndex(shardIndex, shardTotal, count);
    }

    /*
    * 开启任务
    * */
    @Override
    public boolean startTask(long id) {
        int result = mediaProcessMapper.startTask(id);
        return result<=0?false:true;
    }

    /*
    * 更新任务状态
    * */
    @Override
    @Transactional
    public void saveProcessFinishStatus(Long taskId, String status, String fileId, String url, String errorMsg) {
        //查询待处理任务
        MediaProcess mediaProcess = mediaProcessMapper.selectById(taskId);
        if(mediaProcess == null){
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        //1.任务执行失败
        if(status.equals(CommonEnum.VIDEO_PROCESS_FAILED.getValue())){
            //1.1 更新mediaProcess表中数据
            //写法1
            /*mediaProcess.setStatus("3");
            mediaProcess.setFailCount(mediaProcess.getFailCount()+1);
            mediaProcess.setErrormsg(errorMsg);
            mediaProcessMapper.updateById(mediaProcess);*/

            //写法2 使用mp
            // 使用原子操作直接增加失败次数，避免并发问题
            LambdaUpdateWrapper<MediaProcess> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.set(MediaProcess::getStatus,CommonEnum.VIDEO_PROCESS_FAILED.getValue())
                    .setSql("fail_count = fail_count + 1") // 使用SQL表达式原子增加
                    .set(MediaProcess::getErrormsg, errorMsg)
                    .eq(MediaProcess::getId, taskId);

            mediaProcessMapper.update(null, updateWrapper);
            log.debug("更新任务处理状态为失败，任务ID:{}", taskId);
        }


        //2.任务执行成功
        //2.1更新任务执行成功的url avi->mp4
        //2.1.1查询文件表记录
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileId);
        //2.1.2 设置url
        mediaFiles.setUrl(url);
        mediaFilesMapper.updateById(mediaFiles);
        //2.2更新MediaFiles表状态 TODO 设置完成时间失败 需完善
        LambdaUpdateWrapper<MediaProcess> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(MediaProcess::getStatus,CommonEnum.VIDEO_PROCESS_SUCCESS.getValue())
                .set(MediaProcess::getFinishDate,LocalDateTime.now())
                .set(MediaProcess::getUrl,url)
                .eq(MediaProcess::getId,taskId);
        mediaProcessMapper.update(null,updateWrapper);

        //2.3将MediaProcess表中记录插入到历史表中
        MediaProcessHistory mediaProcessHistory = new MediaProcessHistory();
        BeanUtils.copyProperties(mediaProcess,mediaProcessHistory);
        //插入时间 ??? 原先拷贝完成时间为空值
        mediaProcessHistory.setFinishDate(now);
        mediaProcessHistoryMapper.insert(mediaProcessHistory);

        //2.4删除MediaProcess表中记录
        mediaProcessMapper.deleteById(taskId);
    }
}
