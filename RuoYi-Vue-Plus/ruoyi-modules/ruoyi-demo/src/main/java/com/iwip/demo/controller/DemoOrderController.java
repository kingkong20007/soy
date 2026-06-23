package com.iwip.demo.controller;

import com.iwip.common.core.domain.R;
import com.iwip.common.web.core.BaseController;
import com.iwip.demo.domain.bo.DemoOrderBo;
import com.iwip.demo.domain.vo.DemoOrderVo;
import com.iwip.demo.service.IDemoOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 演示订单Controller (下单接口)
 *
 * @author kingkong
 * @date 2026-06-18
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/demo/order")
public class DemoOrderController extends BaseController {

    private final IDemoOrderService orderService;

    /**
     * 下单 (RocketMQ 事务消息演示入口)
     *
     * @param bo 下单业务参数
     * @return 订单号
     */
    @PostMapping("/place")
    public R<String> placeOrder(@Validated @RequestBody DemoOrderBo bo) {
        String orderNo = orderService.placeOrder(bo);
        return R.ok("下单请求已受理", orderNo);
    }

    /**
     * 下单并启动审批流 (Vip/大额订单审批演示入口)
     *
     * @param bo 下单业务参数
     * @return 订单号
     * @author kingkong
     */
    @PostMapping("/place-with-approval")
    public R<String> placeOrderWithApproval(@Validated @RequestBody DemoOrderBo bo) {
        String orderNo = orderService.placeOrderWithApproval(bo);
        return R.ok("下单成功，已启动审批流程，待系统管理员审批", orderNo);
    }

    /**
     * 根据订单ID查询详情
     *
     * @param id 订单ID
     * @return 订单详情
     */
    @GetMapping("/{id}")
    public R<DemoOrderVo> getOrder(@PathVariable Long id) {
        DemoOrderVo vo = orderService.selectById(id);
        if (vo == null) {
            return R.fail("订单不存在");
        }
        return R.ok(vo);
    }

    /**
     * 测试发送顺序消息 (模拟订单状态步骤执行)
     *
     * @param orderNo 订单号
     * @return 响应结果
     */
    @PostMapping("/steps/{orderNo}")
    public R<Void> sendOrderSteps(@PathVariable String orderNo) {
        orderService.sendOrderStepMessages(orderNo);
        return R.ok("顺序消息发送成功，请观察消费端控制台输出顺序");
    }

    /**
     * 下单并设置超时自动关单 (RocketMQ 延迟消息演示入口)
     *
     * @param bo 下单业务参数
     * @return 订单号
     */
    @PostMapping("/place-with-timeout")
    public R<String> placeOrderWithTimeout(@Validated @RequestBody DemoOrderBo bo) {
        String orderNo = orderService.placeOrderWithTimeout(bo);
        return R.ok("下单成功，已启动超时判定延迟消息", orderNo);
    }
}
