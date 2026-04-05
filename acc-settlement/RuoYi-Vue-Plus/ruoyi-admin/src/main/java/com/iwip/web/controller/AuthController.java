package com.iwip.web.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.ObjectUtil;
import com.iwip.common.core.constant.SystemConstants;
import com.iwip.common.core.domain.R;
import com.iwip.common.core.domain.model.LoginBody;
import com.iwip.common.core.domain.model.RegisterBody;

import com.iwip.common.core.utils.MessageUtils;
import com.iwip.common.core.utils.StringUtils;
import com.iwip.common.core.utils.ValidatorUtils;
import com.iwip.common.json.utils.JsonUtils;

import com.iwip.system.domain.vo.SysClientVo;
import com.iwip.system.service.ISysClientService;
import com.iwip.system.service.ISysConfigService;

import com.iwip.web.domain.vo.LoginVo;
import com.iwip.web.service.IAuthStrategy;
import com.iwip.web.service.SysLoginService;
import com.iwip.web.service.SysRegisterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

/**
 * 认证
 *
 * @author Lion Li
 */
@Slf4j
@SaIgnore
@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final SysLoginService loginService;
    private final SysRegisterService registerService;
    private final ISysConfigService configService;
    private final ISysClientService clientService;
    private final ScheduledExecutorService scheduledExecutorService;


    /**
     * 登录方法
     *
     * @param body 登录信息
     * @return 结果
     */
    @PostMapping("/login")
    public R<LoginVo> login(@RequestBody String body) {
        LoginBody loginBody = JsonUtils.parseObject(body, LoginBody.class);
        ValidatorUtils.validate(loginBody);
        // 授权类型和客户端id
        String clientId = loginBody.getClientId();
        String grantType = loginBody.getGrantType();
        SysClientVo client = clientService.queryByClientId(clientId);
        // 查询不到 client 或 client 内不包含 grantType
        if (ObjectUtil.isNull(client) || !StringUtils.contains(client.getGrantType(), grantType)) {
            log.info("客户端id: {} 认证类型：{} 异常!.", clientId, grantType);
            return R.fail(MessageUtils.message("auth.grant.type.error"));
        } else if (!SystemConstants.NORMAL.equals(client.getStatus())) {
            return R.fail(MessageUtils.message("auth.grant.type.blocked"));
        }
        // 登录
        LoginVo loginVo = IAuthStrategy.login(body, client, grantType);

        return R.ok(loginVo);
    }




    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public R<Void> logout() {
        loginService.logout();
        return R.ok("退出成功");
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public R<Void> register(@Validated @RequestBody RegisterBody user) {
        if (!configService.selectRegisterEnabled()) {
            return R.fail("当前系统没有开启注册功能！");
        }
        registerService.register(user);
        return R.ok();
    }

}
