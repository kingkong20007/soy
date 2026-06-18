package com.iwip.demo.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.iwip.common.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * 演示订单对象 demo_order
 *
 * @author Antigravity
 * @date 2026-06-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("demo_order")
public class DemoOrder extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 商品ID
     */
    private Long goodsId;

    /**
     * 商品数量
     */
    private Integer goodsCount;

    /**
     * 订单金额
     */
    private BigDecimal amount;

    /**
     * 订单状态 (0: 待支付, 1: 已支付, 2: 已取消, 3: 已失效)
     */
    private Integer status;

    /**
     * 乐观锁版本号
     */
    @Version
    private Long version;

    /**
     * 删除标志 (0代表存在 2代表删除)
     */
    @TableLogic
    private Long delFlag;
}
