package com.iwip.ssalocalmcpclient.controller;


import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/localMcpClient")
public class LocalMcpClientController {

    //配置了tool工具
    @Autowired
    private ChatClient chatClient;

    //未配置tool工具
    @Autowired
    private ChatModel chatModel;



    /**
     * 通过 MCP 客户端调用远程 MCP 服务器提供的工具（如天气查询）
     *
     * @param message 用户输入的查询内容，例如 "北京天气怎么样？"
     * @return 大模型结合 MCP 工具返回的回答
     */
    @GetMapping("/chat")
    public String chat(@RequestParam(value = "message", defaultValue = "北京天气怎么样？") String message) {
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }

    /**
     * 直接使用 ChatModel 进行对话（不通过 ChatClient 预设的工具）
     *
     * @param message 用户输入的查询内容
     * @return 大模型直接返回的回答
     */
    @GetMapping("/directChat")
    public String directChat(@RequestParam(value = "message", defaultValue = "北京天气怎么样") String message) {
        return chatModel.call(message);
    }


}
