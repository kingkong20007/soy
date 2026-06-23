package com.iwip.demo.mq.consumer;

import com.iwip.common.redis.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.redisson.api.RLock;
import org.springframework.stereotype.Component;
import com.iwip.common.mq.constant.MqConstants;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 订单消费者 (演示幂等性、并发控制、可靠性设计)
 *
 * @author kingkong
 * @date 2026-06-18
 */
@Slf4j
@Component
@RocketMQMessageListener(
    topic = MqConstants.ORDER_TOPIC,
    consumerGroup = MqConstants.ORDER_CONSUMER_GROUP,
    selectorExpression = MqConstants.ORDER_CREATE_TAG // 只消费 tag 为 create 的下单成功消息
)
public class DemoOrderConsumer implements RocketMQListener<MessageExt> {

    /**
     * 分布式锁 key 前缀
     */
    private static final String LOCK_PREFIX = "mq:lock:order_consume:";
    /**
     * 幂等状态表 key 前缀
     */
    private static final String IDEMPOTENT_PREFIX = "mq:idempotent:order_consume:";

    @Override
    public void onMessage(MessageExt messageExt) {
        String msgId = messageExt.getMsgId();
        String orderNo = new String(messageExt.getBody(), StandardCharsets.UTF_8);
        int reconsumeTimes = messageExt.getReconsumeTimes();

        log.info("[订单消费者] 开始消费消息, MsgId: {}, 订单号: {}, 重试次数: {}", msgId, orderNo, reconsumeTimes);

        // 1. 防御性校验
        if (orderNo.isBlank()) {
            log.error("[订单消费者] 消息内容为空，拒不消费. MsgId: {}", msgId);
            return; // 格式错误，直接丢弃（不抛异常，避免重试）
        }
        // 2. 基于 Redis 分布式锁，解决【高并发消息重复投递】的并发竞争问题
        String lockKey = LOCK_PREFIX + orderNo;
        RLock lock = RedisUtils.getClient().getLock(lockKey);

        try {
            // tryLock 参数：等待获取锁0秒（不等待立即返回），锁持有时间15秒
            // 采用 0秒 等待：如果是并发重复投递的消息，另一个消费线程已经拿到了锁，当前线程应直接获取锁失败。
            if (!lock.tryLock(0, 15, TimeUnit.SECONDS)) {
                log.warn("[订单消费者] 获取分布式锁失败，可能其他线程正在消费该订单, 订单号: {}, MsgId: {}", orderNo, msgId);
                // 抛出异常，让 RocketMQ 进行下一次重试投递
                throw new RuntimeException("获取分布式锁失败，触发重试");
            }

            // 3. 基于“幂等状态表”设计，解决【消息重复/幂等性】问题
            // 双重校验：1) 校验Redis状态；2) 校验数据库状态（这里仅演示Redis校验，真实生产还可以查询下游业务单据是否存在，以达到双重保险，既达到执行的效率，也保证了安全性）
            String idempotentKey = IDEMPOTENT_PREFIX + orderNo;
            String consumeStatus = RedisUtils.getCacheObject(idempotentKey);
            if ("SUCCESS".equals(consumeStatus)) {
                log.info("[订单消费者] 检测到该订单已消费成功，跳过重复处理. 订单号: {}, MsgId: {}", orderNo, msgId);
                return; // 幂等拦截，直接成功返回
            }

            // 4. 执行下游核心业务逻辑
            // 模拟真实生产操作：扣减库存、发放积分、发送支付提醒等
            executeDownstreamBusiness(orderNo, msgId);

            // 5. 业务执行成功，标记消费状态 (缓存7天，防止一段时间内的重复消息)
            RedisUtils.setCacheObject(idempotentKey, "SUCCESS", Duration.ofDays(7));
            log.info("[订单消费者] 消息消费成功! 订单号: {}, MsgId: {}", orderNo, msgId);

        } catch (InterruptedException e) {
            log.error("[订单消费者] 获取分布式锁被中断, 订单号: {}", orderNo, e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("消费被中断，等待重试", e);
        } catch (Exception e) {
            log.error("[订单消费者] 消费发生异常，需要重试. 订单号: {}, MsgId: {}, 当前重试次数: {}", orderNo, msgId, reconsumeTimes, e);
            // 抛出异常，触发 RocketMQ 的消费重试机制
            throw e;
        } finally {
            // 只释放自己持有的锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 模拟执行下游业务逻辑
     */
    private void executeDownstreamBusiness(String orderNo, String msgId) {
        log.info("[下游业务] 正在为订单 {} 扣减预留库存...", orderNo);
        try {
            // 模拟业务处理耗时
            TimeUnit.MILLISECONDS.sleep(400);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("[下游业务] 订单 {} 的预留库存扣减成功! (MsgId: {})", orderNo, msgId);
    }
}
