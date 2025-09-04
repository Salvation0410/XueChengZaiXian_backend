package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
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
        //1.任务执行失败
        if(status.equals("3")){
            //1.1 更新mediaProcess表中数据
            //写法1
            /*mediaProcess.setStatus("3");
            mediaProcess.setFailCount(mediaProcess.getFailCount()+1);
            mediaProcess.setErrormsg(errorMsg);
            mediaProcessMapper.updateById(mediaProcess);*/

            //写法2 使用mp
            LambdaUpdateWrapper<MediaProcess> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.set(MediaProcess::getStatus,"3")
                    .set(MediaProcess::getFailCount,mediaProcess.getFailCount()+1)
                    .set(MediaProcess::getErrormsg,errorMsg)
                    .eq(MediaProcess::getId,taskId);

            mediaProcessMapper.update(null,updateWrapper);
        }


        //2.任务执行成功
        //2.1更新任务执行成功的url avi->mp4
        //2.1.1查询文件表记录
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileId);
        //2.1.2 设置url
        mediaFiles.setUrl(url);
        mediaFilesMapper.updateById(mediaFiles);
        //2.2更新MediaFiles表状态
        LambdaUpdateWrapper<MediaProcess> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(MediaProcess::getStatus,"2")
                .set(MediaProcess::getFinishDate, LocalDateTime.now())
                .set(MediaProcess::getUrl,url)
                .eq(MediaProcess::getId,taskId);
        mediaProcessMapper.update(null,updateWrapper);

        //2.3将MediaProcess表中记录插入到历史表中
        MediaProcessHistory mediaProcessHistory = new MediaProcessHistory();
        BeanUtils.copyProperties(mediaProcess,mediaProcessHistory);
        mediaProcessHistoryMapper.insert(mediaProcessHistory);

        //2.4删除MediaProcess表中记录
        mediaProcessMapper.deleteById(taskId);
    }
}
