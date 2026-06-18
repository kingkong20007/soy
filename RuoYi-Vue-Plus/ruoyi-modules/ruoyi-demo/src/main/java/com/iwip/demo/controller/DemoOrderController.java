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
 * @author Antigravity
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
}
