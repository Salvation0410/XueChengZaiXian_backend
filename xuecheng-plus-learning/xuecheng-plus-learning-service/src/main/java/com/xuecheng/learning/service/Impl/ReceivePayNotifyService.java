package com.xuecheng.learning.service.Impl;

import com.alibaba.fastjson.JSON;
import com.xuecheng.base.Enum.CommonEnum;
import com.xuecheng.learning.config.PayNotifyConfig;
import com.xuecheng.learning.service.MyCourseTablesService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

/**
 * @author huang
 * @version 1.0
 * @description 学习中心接收mq发送的消息
 * @date 2025/9/22
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceivePayNotifyService {

    private final MyCourseTablesService myCourseTablesService;

    @RabbitListener(queues = PayNotifyConfig.PAYNOTIFY_QUEUE)
    public void receive(Message message){
        //解析消息
        byte[] body = message.getBody();
        String jsonString = new String(body);
        //转换成对象
        MqMessage mqMessage = JSON.parseObject(jsonString, MqMessage.class);

        //根据消息内容 更新选课记录表并插入数据到我的课程表
        String chooseCourseId = mqMessage.getBusinessKey1();
        String orderType = mqMessage.getBusinessKey2();
        //这里是对消息的类型进行判断 只需要处理支付成功的消息 60201表示购买课程
        if(orderType.equals(CommonEnum.RESOURCE_PURCHASED.getValue())){
            //更新 插入操作
            myCourseTablesService.saveChooseCourseSuccess(chooseCourseId);
        }
    }
}
