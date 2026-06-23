package com.iwip.demo.mq.consumer;

import com.iwip.common.mq.constant.MqConstants;
import com.iwip.demo.domain.DemoOrderStepMsg;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 订单步骤消费者 (演示顺序消息消费)
 *
 * @author kingkong
 * @date 2026-06-21
 */
@Slf4j
@Component
@RocketMQMessageListener(
    topic = MqConstants.ORDER_STEP_TOPIC,
    consumerGroup = MqConstants.ORDER_STEP_CONSUMER_GROUP,
    consumeMode = ConsumeMode.ORDERLY // 启用顺序消费模式
)
public class DemoOrderStepConsumer implements RocketMQListener<DemoOrderStepMsg> {

    @Override
    public void onMessage(DemoOrderStepMsg message) {
        log.info("[订单步骤消费者] 收到消息 - 线程: {}, 订单号: {}, 步骤: [{}], 顺序号: {}",
            Thread.currentThread().getName(),
            message.getOrderNo(),
            message.getStepDesc(),
            message.getStepIndex());

        // 模拟执行耗时，验证顺序处理
        try {
            long delay = (long) (Math.random() * 500) + 100;
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("[订单步骤消费者] 处理完成 - 订单号: {}, 步骤: [{}]", message.getOrderNo(), message.getStepDesc());
    }
}
