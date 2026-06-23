package com.iwip.common.mq.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * RocketMQ 消息队列相关常量定义
 *
 * @author kingkong
 * @date 2026-06-19
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MqConstants {

    /**
     * 订单生产者组
     */
    public static final String ORDER_PRODUCER_GROUP = "order-producer-group";

    /**
     * 订单消费者组
     */
    public static final String ORDER_CONSUMER_GROUP = "demo_order_consumer_group";

    /**
     * 订单相关 Topic
     */
    public static final String ORDER_TOPIC = "demo_order_topic";

    /**
     * 订单创建消息 Tag
     */
    public static final String ORDER_CREATE_TAG = "create";

    /**
     * 订单创建消息目的地 (Topic:Tag)
     */
    public static final String ORDER_CREATE_DESTINATION = ORDER_TOPIC + ":" + ORDER_CREATE_TAG;

    /**
     * 消息头属性：订单号
     */
    public static final String HEADER_ORDER_NO = "orderNo";

    /**
     * 顺序消息：订单步骤相关 Topic
     */
    public static final String ORDER_STEP_TOPIC = "demo_order_step_topic";

    /**
     * 顺序消息：订单步骤消费者组
     */
    public static final String ORDER_STEP_CONSUMER_GROUP = "demo_order_step_consumer_group";

    /**
     * 延迟消息：订单超时自动关单 Topic
     */
    public static final String ORDER_TIMEOUT_TOPIC = "demo_order_timeout_topic";

    /**
     * 延迟消息：订单超时自动关单消费者组
     */
    public static final String ORDER_TIMEOUT_CONSUMER_GROUP = "demo_order_timeout_consumer_group";
}
