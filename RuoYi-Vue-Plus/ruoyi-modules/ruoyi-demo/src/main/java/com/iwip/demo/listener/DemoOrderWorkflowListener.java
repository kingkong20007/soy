package com.iwip.demo.listener;

import cn.hutool.core.convert.Convert;
import com.iwip.common.core.domain.event.ProcessEvent;
import com.iwip.common.core.enums.BusinessStatusEnum;
import com.iwip.common.mq.constant.MqConstants;
import com.iwip.demo.domain.DemoOrder;
import com.iwip.demo.mapper.DemoOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 订单审批工作流监听器
 *
 * @author kingkong
 * @date 2026-06-23
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class DemoOrderWorkflowListener {

    private final DemoOrderMapper orderMapper;
    private final RocketMQTemplate rocketMQTemplate;

    /**
     * 监听订单审批流程的总体流程变化 (例如: 提交，撤销，退回，终止，已完成等)
     *
     * @param processEvent 流程事件参数
     */
    @EventListener(condition = "#processEvent.flowCode == 'order_approval'")
    @Transactional(rollbackFor = Exception.class)
    public void processHandler(ProcessEvent processEvent) {
        log.info("[订单审批监听器] 收到流程事件, 业务ID: {}, flowCode: {}, 状态: {}", 
            processEvent.getBusinessId(), processEvent.getFlowCode(), processEvent.getStatus());

        Long orderId = Convert.toLong(processEvent.getBusinessId());
        DemoOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            log.warn("[订单审批监听器] 订单不存在, ID: {}", orderId);
            return;
        }

        String status = processEvent.getStatus();
        // 1. 流程已完成，代表审批通过
        if (BusinessStatusEnum.FINISH.getStatus().equals(status)) {
            // 更新订单状态为 0 (待支付)
            order.setStatus(0);
            orderMapper.updateById(order);
            log.info("[订单审批监听器] 订单审批通过! 订单号: {}, 状态更新为待支付(0)", order.getOrderNo());

            // 审批通过后，发送 RocketMQ 消息通知下游（例如库存扣减，或供超时自动关单定时任务）
            sendMQMessage(order);
        } 
        // 2. 流程撤销、作废、终止，代表审批不通过
        else if (BusinessStatusEnum.CANCEL.getStatus().equals(status) 
                || BusinessStatusEnum.INVALID.getStatus().equals(status) 
                || BusinessStatusEnum.TERMINATION.getStatus().equals(status)) {
            // 更新订单状态为 2 (已取消)
            order.setStatus(2);
            orderMapper.updateById(order);
            log.info("[订单审批监听器] 订单审批不通过(撤销/作废/终止)! 订单号: {}, 状态更新为已取消(2)", order.getOrderNo());
        } 
        // 3. 流程退回，代表被驳回
        else if (BusinessStatusEnum.BACK.getStatus().equals(status)) {
            // 更新订单状态为 5 (审批退回/驳回)
            order.setStatus(5);
            orderMapper.updateById(order);
            log.info("[订单审批监听器] 订单审批被退回! 订单号: {}, 状态更新为审批退回(5)", order.getOrderNo());
        }
    }

    /**
     * 发送 RocketMQ 消息到下游通知系统
     *
     * @param order 订单信息
     */
    private void sendMQMessage(DemoOrder order) {
        try {
            log.info("[订单审批监听器] 开始发送下单成功MQ消息, 订单号: {}", order.getOrderNo());
            // 发送到订单创建目的Topic
            rocketMQTemplate.convertAndSend(MqConstants.ORDER_CREATE_DESTINATION, order.getOrderNo());
            log.info("[订单审批监听器] 下单成功MQ消息发送成功, 订单号: {}", order.getOrderNo());
        } catch (Exception e) {
            log.error("[订单审批监听器] 发送MQ消息异常, 订单号: {}", order.getOrderNo(), e);
        }
    }
}
