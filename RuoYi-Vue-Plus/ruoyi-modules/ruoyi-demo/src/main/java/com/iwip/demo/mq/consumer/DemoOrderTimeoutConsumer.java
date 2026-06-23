package com.iwip.demo.mq.consumer;

import com.iwip.common.mq.constant.MqConstants;
import com.iwip.demo.service.IDemoOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 订单超时自动取消消费者 (处理延迟消息)
 *
 * @author kingkong
 * @date 2026-06-21
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
    topic = MqConstants.ORDER_TIMEOUT_TOPIC,
    consumerGroup = MqConstants.ORDER_TIMEOUT_CONSUMER_GROUP
)
public class DemoOrderTimeoutConsumer implements RocketMQListener<Long> {

    private final IDemoOrderService orderService;

    @Override
    public void onMessage(Long orderId) {
        log.info("[订单超时取消消费者] 收到超时判定延迟消息，订单ID: {}", orderId);
        try {
            // 调用服务层逻辑执行判定与关闭
            orderService.closeTimeoutOrder(orderId);
        } catch (Exception e) {
            log.error("[订单超时取消消费者] 处理超时判定异常，订单ID: {}", orderId, e);
            throw e;
        }
    }
}
