package com.iwip.ssademo.controller;

import com.iwip.ssademo.utils.CommonUtil;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/toolCalling")
public class TooCallingController {

    @Autowired
    @Qualifier("geminiMemoryClient")
    private ChatClient chatClient;


    /**
     * 工具调用（函数调用）示例接口
     * 允许 AI 根据用户问题自动选择并调用在配置类中注册的 Bean 工具
     *
     * @param question 用户的问题，例如：“帮我查询一下订单号为 12345 的物流状态”
     * @return AI 处理后的结果
     */
    @GetMapping("/chat")
    public String toolChat(@RequestParam String question) {
        return chatClient.prompt()
                .user(question)
                // 🌟 核心：通过 function 指定在配置类中定义的 Bean 名称
                // AI 会根据语义自动判断是否需要调用该方法，并提取参数
                .tools(new CommonUtil())
                .call()
                .content();
    }

}
