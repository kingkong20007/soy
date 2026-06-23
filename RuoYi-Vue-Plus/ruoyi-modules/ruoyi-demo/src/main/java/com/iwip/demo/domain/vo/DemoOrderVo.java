package com.iwip.demo.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import com.iwip.demo.domain.DemoOrder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 演示订单视图对象
 *
 * @author kingkong
 * @date 2026-06-18
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = DemoOrder.class)
public class DemoOrderVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @ExcelProperty(value = "订单ID")
    private Long id;

    /**
     * 订单号
     */
    @ExcelProperty(value = "订单号")
    private String orderNo;

    /**
     * 用户ID
     */
    @ExcelProperty(value = "用户ID")
    private Long userId;

    /**
     * 商品ID
     */
    @ExcelProperty(value = "商品ID")
    private Long goodsId;

    /**
     * 商品数量
     */
    @ExcelProperty(value = "商品数量")
    private Integer goodsCount;

    /**
     * 订单金额
     */
    @ExcelProperty(value = "订单金额")
    private BigDecimal amount;

    /**
     * 订单状态 (0: 待支付, 1: 已支付, 2: 已取消, 3: 已失效, 4: 待审批, 5: 审批退回/驳回)
     */
    @ExcelProperty(value = "订单状态")
    private Integer status;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;
}
