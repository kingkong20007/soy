package com.iwip.demo.service;

import com.iwip.demo.domain.DemoOrder;
import com.iwip.demo.domain.bo.DemoOrderBo;
import com.iwip.demo.domain.vo.DemoOrderVo;

/**
 * 演示订单服务接口
 *
 * @author Antigravity
 * @date 2026-06-18
 */
public interface IDemoOrderService {

    /**
     * 下单接口 (外部调用入口，发送事务消息)
     *
     * @param bo 订单业务信息
     * @return 订单编号
     */
    String placeOrder(DemoOrderBo bo);

    /**
     * 执行本地下单事务 (保存订单记录到数据库，供 RocketMQ 事务监听器回调)
     *
     * @param order 订单实体对象
     * @param transactionId 事务ID
     */
    void saveOrderLocal(DemoOrder order, String transactionId);

    /**
     * 根据订单号查询订单
     *
     * @param orderNo 订单号
     * @return 订单实体
     */
    DemoOrder selectByOrderNo(String orderNo);

    /**
     * 根据订单ID获取订单详情
     *
     * @param id 订单ID
     * @return 订单Vo
     */
    DemoOrderVo selectById(Long id);
}
