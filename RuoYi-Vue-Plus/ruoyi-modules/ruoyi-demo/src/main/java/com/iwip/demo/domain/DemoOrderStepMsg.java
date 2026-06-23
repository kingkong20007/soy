package com.iwip.demo.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 订单步骤消息实体 (用于顺序消息演示)
 *
 * @author kingkong
 * @date 2026-06-21
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DemoOrderStepMsg implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 步骤描述 (如: 创建、支付、发货、完成)
     */
    private String stepDesc;

    /**
     * 步骤顺序号 (从小到大执行)
     */
    private Integer stepIndex;
}
