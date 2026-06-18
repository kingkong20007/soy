package com.iwip.demo.mq.producer;

import com.iwip.demo.domain.DemoOrder;
import com.iwip.demo.service.IDemoOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * 订单事务消息监听器 (执行本地事务 & 事务状态回查)
 *
 * @author Antigravity
 * @date 2026-06-18
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQTransactionListener
public class DemoOrderTransactionListener implements RocketMQLocalTransactionListener {

    private final IDemoOrderService orderService;

    /**
     * 执行本地事务
     *
     * @param msg 消息对象
     * @param arg 发送消息时传入的自定义参数 (此处为 DemoOrder 实体)
     * @return 事务状态 (COMMIT / ROLLBACK / UNKNOWN)
     */
    @Override
    public RocketMQLocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        log.info("[事务监听器] 收到半消息发送成功回调，开始执行本地事务");
        try {
            DemoOrder order = (DemoOrder) arg;
            String transactionId = (String) msg.getHeaders().get(RocketMQHeaders.TRANSACTION_ID);
            
            // 执行本地数据库写入
            orderService.saveOrderLocal(order, transactionId);
            
            log.info("[事务监听器] 本地事务执行成功，准备 COMMIT 事务消息. 订单号: {}", order.getOrderNo());
            return RocketMQLocalTransactionState.COMMIT;
        } catch (Exception e) {
            log.error("[事务监听器] 本地事务执行异常，回滚事务消息", e);
            return RocketMQLocalTransactionState.ROLLBACK;
        }
    }

    /**
     * 事务状态回查 (当半消息确认超时，或者返回了 UNKNOWN 时，Broker会主动发起回查)
     *
     * @param msg 消息对象
     * @return 事务状态 (COMMIT / ROLLBACK / UNKNOWN)
     */
    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message msg) {
        String orderNo = (String) msg.getHeaders().get("orderNo");
        log.info("[事务监听器] 收到 Broker 事务回查请求，订单号: {}", orderNo);
        try {
            DemoOrder order = orderService.selectByOrderNo(orderNo);
            if (order != null) {
                log.info("[事务监听器] 回查结果：订单记录已存在数据库，提交(COMMIT)消息. 订单号: {}", orderNo);
                return RocketMQLocalTransactionState.COMMIT;
            } else {
                log.warn("[事务监听器] 回查结果：订单记录不存在，回滚(ROLLBACK)消息. 订单号: {}", orderNo);
                return RocketMQLocalTransactionState.ROLLBACK;
            }
        } catch (Exception e) {
            log.error("[事务监听器] 回查操作执行异常，返回 UNKNOWN 等待下一次回查. 订单号: {}", orderNo, e);
            return RocketMQLocalTransactionState.UNKNOWN;
        }
    }
}
