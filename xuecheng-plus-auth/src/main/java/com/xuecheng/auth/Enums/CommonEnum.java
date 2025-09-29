package com.xuecheng.auth.Enums;

import lombok.AllArgsConstructor;

import lombok.Getter;


/**
 * @author huang
 * @version 1.0
 * @description
 * @date 2025/9/29
 */


@AllArgsConstructor
@Getter
public enum CommonEnum {

    // ================== 使用状态 ==================
    USE_STATUS("1", "使用状态"),
    DELETE_STATUS("0", "删除状态"),
    TEMP_STATUS("-1", "暂时状态"),

    // ================== 审核状态 ==================
    AUDIT_NOT_APPROVED("002001", "审核未通过"),
    AUDIT_PENDING("002002", "未审核"),
    AUDIT_APPROVED("002003", "审核通过"),

    // ================== 媒体类型 ==================
    MEDIA_IMAGE("001001", "图片"),
    MEDIA_VIDEO("001002", "视频"),
    MEDIA_OTHER("001003", "其它"),

    // ================== 提交状态 ==================
    SUBMIT_NOT_SUBMITTED("202001", "未提交"),
    SUBMIT_SUBMITTED("202002", "已提交"),
    SUBMIT_APPROVED("202004", "审核通过"),

    // ================== 课程费用 ==================
    COURSE_FREE("201001", "免费"),
    COURSE_PAID("201002", "收费"),

    // ================== 课程等级 ==================
    COURSE_LEVEL_PRIMARY("204001", "初级"),
    COURSE_LEVEL_INTERMEDIATE("204002", "中级"),
    COURSE_LEVEL_ADVANCED("204003", "高级"),

    // ================== 授课方式 ==================
    TEACHING_RECORDED("200001", "录播"),
    TEACHING_LIVE("200002", "直播"),

    // ================== 发布状态 ==================
    PUBLISH_UNPUBLISHED("203001", "未发布"),
    PUBLISH_PUBLISHED("203002", "已发布"),
    PUBLISH_OFFLINE("203003", "下线"),

    // ================== 支付状态 ==================
    PAY_PENDING("600001", "未支付"),
    PAY_PAID("600002", "已支付"),
    PAY_CLOSED("600003", "已关闭"),
    PAY_REFUND("600004", "已退款"),
    PAY_PART_REFUND("600005", "部分退款"),

    // ================== 审核结果 ==================
    REVIEW_PENDING("306001", "未提交"),
    REVIEW_PENDING_SPECIAL("306002", "待批改"),
    REVIEW_APPROVED("306003", "已批改"),

    // ================== 消息通知 ==================
    NOTICE_UNREAD("003001", "未通知"),
    NOTICE_SENT("003002", "已通知"),

    // ================== 订单状态 ==================
    ORDER_PENDING("601001", "未支付"),
    ORDER_PAID("601002", "已支付"),
    ORDER_REFUNDED("601003", "已退款"),

    // ================== 学习资源 ==================
    RESOURCE_PURCHASED("602001", "购买资料"),
    RESOURCE_ACCESSED("602002", "学习资料"),

    // ================== 支付渠道 ==================
    PAY_ALIPAY("603001", "支付宝"),
    PAY_WECHAT("603002", "微信支付"),

    // ================== 课程类型 ==================
    COURSE_FREE_TYPE("700001", "免费课程"),
    COURSE_PAID_TYPE("700002", "收费课程"),

    // ================== 学习进度 ==================
    STUDY_SELECTION("701001", "选课成功"),
    STUDY_PENDING_PAYMENT("701002", "待支付"),

    // ================== 学习状态 ==================
    STUDY_NORMAL("702001", "正常学习"),
    STUDY_NO_COURSE("702002", "没有选课或选课后没有支付"),
    STUDY_EXPIRED("702003", "已过期需申请续期或重新支付"),

    //用户类型

    STUDENT("101001", "学生"),
    TEACHER("101002", "教师"),
    ADMIN("101003", "管理员"),
    STUDENT_ROLE_ID("17", "学生角色ID"),
    ;

    private final String value;
    private final String desc;
}
