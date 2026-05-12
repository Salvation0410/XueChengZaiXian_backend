package com.xuecheng.messagesdk.service;

import com.xuecheng.messagesdk.model.po.MqMessage;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Mr.M
 * @version 1.0
 * @description 消息处理抽象类
 * @date 2022/9/21 19:44
 */
@Slf4j
@Data
public abstract class MessageProcessAbstract {

    private final MqMessageService mqMessageService;
    private final ExecutorService messageProcessThreadPool;

    protected MessageProcessAbstract(MqMessageService mqMessageService, ExecutorService messageProcessThreadPool) {
        this.mqMessageService = mqMessageService;
        this.messageProcessThreadPool = messageProcessThreadPool;
    }

    /**
     * @param mqMessage 执行任务内容
     * @return boolean true:处理成功，false处理失败
     * @description 任务处理
     * @author Mr.M
     * @date 2022/9/21 19:47
     */
    public abstract boolean execute(MqMessage mqMessage);


    /**
     * @description 扫描消息表多线程执行任务
     * @param shardIndex 分片序号
     * @param shardTotal 分片总数
     * @param messageType  消息类型
     * @param count  一次取出任务总数
     * @param timeout 预估任务执行时间,到此时间如果任务还没有结束则强制结束 单位秒
     * @return void
     * @author Mr.M
     * @date 2022/9/21 20:35
    */
    public void process(int shardIndex, int shardTotal,  String messageType,int count,long timeout) {

        try {
            List<MqMessage> messageList = mqMessageService.getMessageList(shardIndex, shardTotal,messageType, count);
            int size = messageList.size();
            log.debug("取出待处理消息{}条", size);
            if(size<=0){
                return ;
            }

            CountDownLatch countDownLatch = new CountDownLatch(size);
            messageList.forEach(message -> {
                messageProcessThreadPool.execute(() -> {
                    log.debug("开始任务:{}",message);
                    try {
                        boolean result = execute(message);
                        if(result){
                            int completed = mqMessageService.completed(message.getId());
                            if (completed>0){
                                log.debug("任务执行成功:{}",message);
                            }else{
                                log.debug("任务执行失败:{}",message);
                            }
                        }
                    } catch (Exception e) {
                        log.error("任务出现异常,任务:{}", message, e);
                    } finally {
                        countDownLatch.countDown();
                    }
                    log.debug("结束任务:{}",message);

                });
            });

            countDownLatch.await(timeout,TimeUnit.SECONDS);
            log.debug("消息处理结束");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("消息处理线程被中断", e);
        }


    }



}
