package com.iwip.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.iwip.common.core.exception.ServiceException;
import com.iwip.common.core.utils.MapstructUtils;
import com.iwip.demo.domain.DemoOrder;
import com.iwip.demo.domain.bo.DemoOrderBo;
import com.iwip.demo.domain.vo.DemoOrderVo;
import com.iwip.demo.mapper.DemoOrderMapper;
import com.iwip.demo.service.IDemoOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 演示订单服务实现类
 *
 * @author Antigravity
 * @date 2026-06-18
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DemoOrderServiceImpl implements IDemoOrderService {

    private final DemoOrderMapper baseMapper;
    private final RocketMQTemplate rocketMQTemplate;

    @Override
    public String placeOrder(DemoOrderBo bo) {
        // 1. 生成全局唯一订单号
        String orderNo = "ORD-" + IdWorker.getIdStr();

        // 2. 转换并初始化订单实体
        DemoOrder order = MapstructUtils.convert(bo, DemoOrder.class);
        order.setOrderNo(orderNo);
        order.setStatus(0); // 待支付状态

        // 3. 构建 RocketMQ 事务消息，Payload 为订单号
        Message<String> message = MessageBuilder.withPayload(orderNo)
            .setHeader("orderNo", orderNo)
            .build();

        // 4. 发送事务消息
        String destination = "demo_order_topic:create";

        try {
            log.info("[下单服务] 准备发送事务半消息, 订单号: {}", orderNo);
            TransactionSendResult sendResult = rocketMQTemplate.sendMessageInTransaction(
                destination,
                message,
                order // 传入 order 实体作为参数，供本地事务执行器使用
            );

            log.info("[下单服务] 半消息发送结果: {}, 事务状态: {}", sendResult.getSendStatus(), sendResult.getLocalTransactionState());
            if (sendResult.getLocalTransactionState() == LocalTransactionState.ROLLBACK_MESSAGE) {
                throw new ServiceException("本地事务执行失败，下单回滚");
            }
        } catch (Exception e) {
            log.error("[下单服务] 发送 RocketMQ 事务消息异常", e);
            throw new ServiceException("下单系统繁忙，请稍后再试: " + e.getMessage());
        }

        return orderNo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOrderLocal(DemoOrder order, String transactionId) {
        log.info("[下单本地事务] 开始执行本地事务, 订单号: {}, 事务ID: {}", order.getOrderNo(), transactionId);
        // 保存订单记录
        int inserted = baseMapper.insert(order);
        if (inserted <= 0) {
            throw new ServiceException("保存订单数据失败");
        }
        log.info("[下单本地事务] 本地事务保存成功, 订单ID: {}", order.getId());
    }

    @Override
    public DemoOrder selectByOrderNo(String orderNo) {
        LambdaQueryWrapper<DemoOrder> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(DemoOrder::getOrderNo, orderNo);
        return baseMapper.selectOne(queryWrapper);
    }

    @Override
    public DemoOrderVo selectById(Long id) {
        return baseMapper.selectVoById(id);
    }
}
