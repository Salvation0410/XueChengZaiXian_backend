package com.xuecheng.orders.service.Impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.utils.IdWorkerUtils;
import com.xuecheng.base.utils.QRCodeUtil;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MqMessageService;
import com.xuecheng.orders.config.AlipayConfig;
import com.xuecheng.orders.config.PayNotifyConfig;
import com.xuecheng.orders.mapper.XcOrdersGoodsMapper;
import com.xuecheng.orders.mapper.XcOrdersMapper;
import com.xuecheng.orders.mapper.XcPayRecordMapper;
import com.xuecheng.orders.model.dto.AddOrderDto;
import com.xuecheng.orders.model.dto.PayRecordDto;
import com.xuecheng.orders.model.dto.PayStatusDto;
import com.xuecheng.orders.model.po.XcOrders;
import com.xuecheng.orders.model.po.XcOrdersGoods;
import com.xuecheng.orders.model.po.XcPayRecord;
import com.xuecheng.orders.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @author huang
 * @version 1.0
 * @description
 * @date 2025/9/21
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final XcOrdersMapper xcOrdersMapper;
    private final XcOrdersGoodsMapper xcOrdersGoodsMapper;
    private final XcPayRecordMapper xcPayRecordMapper;
    private final MqMessageService mqMessageService;
    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    OrderServiceImpl currentProxy;

    @Value("${pay.qrcodeurl}")
    String qrcodeurl;
    @Value("${pay.alipay.APP_ID}")
    String APP_ID;
    @Value("${pay.alipay.APP_PRIVATE_KEY}")
    String APP_PRIVATE_KEY;
    @Value("${pay.alipay.ALIPAY_PUBLIC_KEY}")
    String ALIPAY_PUBLIC_KEY;


    /*
    * 创建商品订单
    *
    * */

    @Transactional
    @Override
    public PayRecordDto createOrder(String userId, AddOrderDto addOrderDto) {
        //插入商品订单表
        XcOrders xcOrders = saveXcOrders(userId,addOrderDto);

        //插入支付记录表
        XcPayRecord xcPayRecord = createPayRecord(xcOrders);

        Long payNO = xcPayRecord.getPayNo();
        //生成二维码
        QRCodeUtil qrCodeUtil = new QRCodeUtil();
        String url = String.format(qrcodeurl, payNO);
        String qrcode = null;
        try {
             qrcode= qrCodeUtil.createQRCode(url, 200, 200);
        } catch (IOException e) {
            XueChengPlusException.cast("生成二维码失败");
        }
        PayRecordDto payRecordDto = new PayRecordDto();
        BeanUtils.copyProperties(xcPayRecord,payRecordDto);
        payRecordDto.setQrcode(qrcode);


        return payRecordDto;
    }

    /*
    * 插入订单
    * */
    @Transactional
    public XcOrders saveXcOrders(String userId, AddOrderDto addOrderDto){
        //插入商品订单表 插入明细表

        //幂等性判断 一个选课记录只能有一个订单
        XcOrders order= getOrderByBusinessId(addOrderDto.getOutBusinessId());
        if(order!=null){
            return order;
        }
        order = new XcOrders();
        //生成订单号
        Long orderId = IdWorkerUtils.getInstance().nextId();
        order.setId(orderId);
        order.setTotalPrice(addOrderDto.getTotalPrice());
        order.setCreateDate(LocalDateTime.now());
        order.setStatus("600001");//未支付
        order.setUserId(userId);
        order.setOrderType(addOrderDto.getOrderType());
        order.setOrderName(addOrderDto.getOrderName());
        order.setOrderDetail(addOrderDto.getOrderDetail());
        order.setOrderDescrip(addOrderDto.getOrderDescrip());
        order.setOutBusinessId(addOrderDto.getOutBusinessId());//选课记录id
        xcOrdersMapper.insert(order);

        //TODO  循环插入数据库性能差 使用xml批量插入 字符串转数组使用hutool工具
        String orderDetailJson = addOrderDto.getOrderDetail();
        List<XcOrdersGoods> xcOrdersGoodsList = JSON.parseArray(orderDetailJson, XcOrdersGoods.class);
        xcOrdersGoodsList.forEach(goods->{
            XcOrdersGoods xcOrdersGoods = new XcOrdersGoods();
            BeanUtils.copyProperties(goods,xcOrdersGoods);
            xcOrdersGoods.setOrderId(orderId);//订单号
            xcOrdersGoodsMapper.insert(xcOrdersGoods);
        });
        return order;

    }

    /*
    * 根据业务id查询订单 业务id即为选课记录id
    * */
    public XcOrders getOrderByBusinessId(String businessId){
        XcOrders xcOrders = xcOrdersMapper.selectOne(new LambdaQueryWrapper<XcOrders>().eq(XcOrders::getOutBusinessId,businessId));
        return xcOrders;
    }

    /*
    * 创建支付交易订单
    *
    * */
    public XcPayRecord createPayRecord(XcOrders orders){
        if(orders==null){
            XueChengPlusException.cast("订单不存在");
        }
        if(orders.getStatus().equals("600002")){
            XueChengPlusException.cast("订单已支付");
        }
        XcPayRecord payRecord = new XcPayRecord();
        //生成支付交易流水号
        long payNo = IdWorkerUtils.getInstance().nextId();
        payRecord.setPayNo(payNo);
        payRecord.setOrderId(orders.getId());//商品订单号
        payRecord.setOrderName(orders.getOrderName());
        payRecord.setTotalPrice(orders.getTotalPrice());
        payRecord.setCurrency("CNY");
        payRecord.setCreateDate(LocalDateTime.now());
        payRecord.setStatus("601001");//未支付
        payRecord.setUserId(orders.getUserId());
        xcPayRecordMapper.insert(payRecord);
        return payRecord;
    }

    /*
    * 查询支付交易记录
    * */
    @Override
    public XcPayRecord getPayRecordByPayno(String payNo) {
        XcPayRecord xcPayRecord = xcPayRecordMapper.selectOne(new LambdaQueryWrapper<XcPayRecord>().eq(XcPayRecord::getPayNo, payNo));
        return xcPayRecord;
    }

    /*
    * 请求支付宝查询支付结果
    * */
    @Override
    public PayRecordDto queryPayResult(String payNo) {
        //调用支付宝查询支付结果
        PayStatusDto payStatusDto = queryPayResultFromAlipay(payNo);
        //拿到结果后更新订单表和商品记录表的支付状态
        currentProxy.saveAliPayStatus(payStatusDto);

        PayRecordDto payRecordDto = new PayRecordDto();
        //返回更新后的数据
        XcPayRecord payRecord = getPayRecordByPayno(payNo);
        BeanUtils.copyProperties(payRecord,payRecordDto);

        return payRecordDto;
    }



    /**
     * 请求支付宝查询支付结果
     * @param payNo 支付交易号
     * @return 支付结果
     */
    public PayStatusDto queryPayResultFromAlipay(String payNo){
        AlipayClient alipayClient = new DefaultAlipayClient(AlipayConfig.URL, APP_ID, APP_PRIVATE_KEY, "json", AlipayConfig.CHARSET, ALIPAY_PUBLIC_KEY, AlipayConfig.SIGNTYPE); //获得初始化的AlipayClient
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", payNo);
        request.setBizContent(bizContent.toString());
        AlipayTradeQueryResponse response = null;
        try {
            response = alipayClient.execute(request);
            if (!response.isSuccess()) {
                XueChengPlusException.cast("请求支付查询结果失败");
            }
        } catch (AlipayApiException e) {
            log.error("请求支付宝查询支付结果异常:{}", e.toString(), e);
            XueChengPlusException.cast("请求支付查询查询失败");
        }
        //获取支付结果
        String resultJson = response.getBody();
        //转map
        Map resultMap = JSON.parseObject(resultJson, Map.class);
        Map alipay_trade_query_response = (Map) resultMap.get("alipay_trade_query_response");
        //支付结果
        String trade_status = (String) alipay_trade_query_response.get("trade_status");
        String total_amount = (String) alipay_trade_query_response.get("total_amount");
        String trade_no = (String) alipay_trade_query_response.get("trade_no");
        //保存支付结果
        PayStatusDto payStatusDto = new PayStatusDto();
        payStatusDto.setOut_trade_no(payNo);
        payStatusDto.setTrade_status(trade_status);
        payStatusDto.setApp_id(APP_ID);
        payStatusDto.setTrade_no(trade_no);
        payStatusDto.setTotal_amount(total_amount);
        return payStatusDto;
    }

    /*
     * 保存支付宝支付结果
     * */
    @Override
    @Transactional
    public void saveAliPayStatus(PayStatusDto payStatusDto) {
        //支付成功 更新支付记录表的状态以及订单表的状态
        String payNo = payStatusDto.getOut_trade_no();
        //查询支付记录
        XcPayRecord payRecord = getPayRecordByPayno(payNo);
        if (payRecord == null) {
            XueChengPlusException.cast("支付记录不存在");
        }
        Long orderId = payRecord.getOrderId();
        XcOrders orders = xcOrdersMapper.selectById(orderId);
        if(orders == null){
            XueChengPlusException.cast("相关联的订单不存在");
        }
        String statusDb = payStatusDto.getTrade_status();
        //这是从数据库获取的支付状态信息
        if(statusDb.equals("601002")){
            //支付成功
            return ;
        }
        //从支付宝获取支付状态信息
        String status = payStatusDto.getTrade_status();
        if("TRADE_SUCCESS".equals(status)){
            //更新记录表的状态信息以及订单表的状态信息
            payRecord.setStatus("601002");
            //支付宝的订单号
            payRecord.setOutPayNo(payStatusDto.getTrade_no());
            //第三方的支付渠道编号 这里只有支付宝 后续增加接口需要增加字段添加枚举类
            payRecord.setOutPayChannel("ALIPAY");
            //支付成功时间
            payRecord.setPaySuccessTime(LocalDateTime.now());
            //更新支付记录表
            xcPayRecordMapper.updateById(payRecord);
        }

        //添加消息到mq_message中
        MqMessage mqMessage = mqMessageService.addMessage("payresult_notify",orders.getOutBusinessId(),orders.getOrderType(),null);
        //发送消息
        notifyPayResult(mqMessage);

    }

    /*
    * mq 消息通知
    * */
    @Override
    public void notifyPayResult(MqMessage message) {
        ///准备一个消息
        String jsonString = JSON.toJSONString(message);
        Message messageObj = MessageBuilder
                .withBody(jsonString.getBytes(StandardCharsets.UTF_8)) //消息内容 并指定编码
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .build();

        //将消息表中的id作为关联业务的id
        Long id = message.getId();
        //可在对象中传递一个业务id与消息进行关联 用于生产者确认消息 消息回调对象
        CorrelationData correlationData = new CorrelationData(id.toString());

        correlationData.getFuture().addCallback(
                result->{
                    if(result.isAck()){
                        //消息接收成功 这里是生产者确认机制
                        log.debug("消息接收成功:{}",message);
                        //从mq_message表中删除mq
                        mqMessageService.completed(id);
                    }else{
                        //消息接收失败 在这里进行失败处理
                        log.debug("消息接收失败:{}",message);
                    }

                },ex->{
                    //异常处理
                    log.info("发送消息异常:{}",ex.getMessage());

                }
        );
        //参数：交换机，路由键，消息，消息回调对象
        rabbitTemplate.convertAndSend(PayNotifyConfig.PAYNOTIFY_EXCHANGE_FANOUT, "", messageObj, correlationData);
    }
}
