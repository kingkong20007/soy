package com.iwip.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.iwip.common.core.exception.ServiceException;
import com.iwip.common.core.utils.MapstructUtils;
import com.iwip.common.mq.constant.MqConstants;
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
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import com.iwip.common.core.service.WorkflowService;
import com.iwip.common.core.domain.dto.StartProcessDTO;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 演示订单服务实现类
 *
 * @author kingkong
 * @date 2026-06-18
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DemoOrderServiceImpl implements IDemoOrderService {

    private final DemoOrderMapper baseMapper;
    private final RocketMQTemplate rocketMQTemplate;
    private final WorkflowService workflowService;

    /**
     * 订单号前缀
     */
    private static final String ORDER_NO_PREFIX ="ORD-";

    @Override
    public String placeOrder(DemoOrderBo bo) {
        // 1. 生成全局唯一订单号
        String orderNo =  ORDER_NO_PREFIX + IdWorker.getIdStr();

        // 2. 转换并初始化订单实体
        DemoOrder order = MapstructUtils.convert(bo, DemoOrder.class);
        order.setOrderNo(orderNo);
        order.setStatus(0); // 待支付状态

        // 3. 构建 RocketMQ 事务消息，Payload 为订单号
        Message<String> message = MessageBuilder.withPayload(orderNo)
            .setHeader(MqConstants.HEADER_ORDER_NO, orderNo) // 消息头
            .setHeader(RocketMQHeaders.KEYS, orderNo) // dashboard搜素使用
            .build();

        // 4. 发送事务消息
        String destination = MqConstants.ORDER_CREATE_DESTINATION;

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

    @Override
    public void sendOrderStepMessages(String orderNo) {
        log.info("[顺序消息] 开始向 Topic: {} 发送订单步骤消息, 订单号: {}", MqConstants.ORDER_STEP_TOPIC, orderNo);

        java.util.List<com.iwip.demo.domain.DemoOrderStepMsg> stepList = new java.util.ArrayList<>();
        stepList.add(new com.iwip.demo.domain.DemoOrderStepMsg(orderNo, "创建订单", 1));
        stepList.add(new com.iwip.demo.domain.DemoOrderStepMsg(orderNo, "订单支付", 2));
        stepList.add(new com.iwip.demo.domain.DemoOrderStepMsg(orderNo, "商品发货", 3));
        stepList.add(new com.iwip.demo.domain.DemoOrderStepMsg(orderNo, "订单完成", 4));

        for (com.iwip.demo.domain.DemoOrderStepMsg stepMsg : stepList) {
            try {
                org.apache.rocketmq.client.producer.SendResult sendResult = rocketMQTemplate.syncSendOrderly(
                    MqConstants.ORDER_STEP_TOPIC,
                    stepMsg,
                    orderNo
                );
                log.info("[顺序消息] 发送成功 - 步骤: [{}], 队列ID: {}, 发送状态: {}",
                    stepMsg.getStepDesc(), sendResult.getMessageQueue().getQueueId(), sendResult.getSendStatus());
            } catch (Exception e) {
                log.error("[顺序消息] 发送异常 - 步骤: [{}], 订单号: {}", stepMsg.getStepDesc(), orderNo, e);
                throw new ServiceException("顺序消息发送失败: " + e.getMessage());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String placeOrderWithTimeout(com.iwip.demo.domain.bo.DemoOrderBo bo) {
        String orderNo = "ORD-" + IdWorker.getIdStr();

        DemoOrder order = MapstructUtils.convert(bo, DemoOrder.class);
        order.setOrderNo(orderNo);
        order.setStatus(0); // 待支付状态
        int inserted = baseMapper.insert(order);
        if (inserted <= 0) {
            throw new ServiceException("创建订单数据失败");
        }

        // 构建延迟消息，传递订单 ID 作为 Payload
        org.springframework.messaging.Message<Long> message = MessageBuilder.withPayload(order.getId()).build();

        // 延迟消息等级：3 代表 10s，用于开发调试快速验证
        int delayLevel = 3;
        try {
            log.info("[订单超时取消] 发送延迟消息, 订单ID: {}, 订单号: {}, 延迟等级: {}", order.getId(), orderNo, delayLevel);
            rocketMQTemplate.syncSend(
                MqConstants.ORDER_TIMEOUT_TOPIC,
                message,
                3000,
                delayLevel
            );
        } catch (Exception e) {
            log.error("[订单超时取消] 发送延迟消息异常, 订单号: {}", orderNo, e);
            throw new ServiceException("系统繁忙，下单失败: " + e.getMessage());
        }

        return orderNo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean closeTimeoutOrder(Long orderId) {
        log.info("[订单超时取消] 开始处理超时判定，订单ID: {}", orderId);

        DemoOrder order = baseMapper.selectById(orderId);
        if (order == null) {
            log.warn("[订单超时取消] 订单不存在，终止判定. 订单ID: {}", orderId);
            return false;
        }

        if (order.getStatus() != 0) {
            log.info("[订单超时取消] 订单状态非待支付，不进行自动关单. 订单号: {}, 当前状态: {}", order.getOrderNo(), order.getStatus());
            return false;
        }

        // 状态机更新为 2 (已取消)，并做乐观锁校验
        order.setStatus(2);
        LambdaQueryWrapper<DemoOrder> updateWrapper = Wrappers.lambdaQuery();
        updateWrapper.eq(DemoOrder::getId, orderId)
            .eq(DemoOrder::getStatus, 0);

        int updated = baseMapper.update(order, updateWrapper);
        if (updated > 0) {
            log.info("[订单超时取消] 自动关单成功，订单号: {}", order.getOrderNo());
            log.info("[下游逻辑] 成功补偿释放订单 {} 的库存和优惠券", order.getOrderNo());
            return true;
        } else {
            log.warn("[订单超时取消] 自动关单失败，状态已被修改. 订单号: {}", order.getOrderNo());
            return false;
        }
    }

    /**
     * 下单并启动审批流
     *
     * @param bo 订单业务信息
     * @return 订单编号
     * @author kingkong
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String placeOrderWithApproval(DemoOrderBo bo) {
        // 1. 生成全局唯一订单号
        String orderNo = ORDER_NO_PREFIX + IdWorker.getIdStr();

        // 2. 转换并初始化订单实体
        DemoOrder order = MapstructUtils.convert(bo, DemoOrder.class);
        order.setOrderNo(orderNo);
        order.setStatus(4); // 4 代表 待审批 状态

        // 3. 保存订单到数据库
        int inserted = baseMapper.insert(order);
        if (inserted <= 0) {
            throw new ServiceException("保存订单数据失败");
        }

        // 4. 启动 Warm-Flow 工作流流程 (flowCode = "order_approval")
        StartProcessDTO startProcess = new StartProcessDTO();
        startProcess.setBusinessId(order.getId().toString());
        startProcess.setFlowCode("order_approval");
        // 后端发起，忽略审批人权限校验
        startProcess.getVariables().put("ignore", true);

        log.info("[下单审批] 启动审批工作流流程, 订单号: {}, 业务ID: {}", orderNo, order.getId());
        boolean flag = workflowService.startCompleteTask(startProcess);
        if (!flag) {
            throw new ServiceException("启动审批流程失败");
        }

        return orderNo;
    }
}
