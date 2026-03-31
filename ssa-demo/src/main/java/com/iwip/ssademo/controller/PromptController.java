package com.iwip.ssademo.controller;

import com.iwip.ssademo.records.StudtentRecord;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

@RestController
@RequestMapping("/prompt")
public class PromptController {

    @Autowired
    @Qualifier("aliMemoryClient")
    private ChatClient aliClient;

    @Autowired
    @Qualifier("geminiMemoryClient")
    private ChatClient geminiClient;

    @Autowired
    @Qualifier("ollamaMemoryClient")
    private ChatClient ollamaClient;

    @Value("classpath:promptemplate/ai-template.txt")
    private org.springframework.core.io.Resource userTemplate;

    /**
     * spring ai的四大角色：system(系统提示消息)、user(用户输入的消息)、assistant(大模型回复的消息)、tool(外部调用的工具的消息)
     */

    /**
     * 基础训练使用
     *
     * @param msg
     * @return
     */
    @GetMapping("/stream/chat01")
    public Flux<String> call(@RequestParam(value = "msg", defaultValue = "请问，你叫什么") String msg) {
        return geminiClient.prompt()
                .system("你是一个 系统架构师，熟悉Python、Java、Go、C++等常见语言，你只回复关于开发技术相关的内容，暂不支持其他内容。")
                .system("请以html格式的语句进行输出")
                .user(msg)
                .stream()
                .content();
    }

    /**
     * 自定义提示词-案例
     *
     */
    @GetMapping("/stream/chat02")
    public Flux<String> stream2(@RequestParam(value = "id", defaultValue = "0001") Long id,
                                @RequestParam(value = "sname", defaultValue = "张三") String sname,
                                @RequestParam(value = "major", defaultValue = "计算机科学与技术") String major,
                                @RequestParam(value = "phone", defaultValue = "13800000000") String phone,
                                @RequestParam(value = "msg", defaultValue = "请问，你叫什么") String msg) {
        //创建promptTemplate(这里是给AI的提示词)
        PromptTemplate promptTemplate = new PromptTemplate("""
                        你的学号{id}，，你叫{sname}，你是一名{major}专业的学生，你的手机号是{phone}，你正在使用spring ai进行对话。
                        """);

        //使用prompt将promptTemplate中的占位符替换为实际的值
        Prompt prompt = promptTemplate.create(Map.of(
                        "id", id,
                        "sname", sname,
                        "major", major,
                        "phone", phone
                )
        );
        return geminiClient.prompt(prompt) //将处理过的prompt的提示词，传递过来
                .system("请以html格式的语句进行输出")
                .user(msg)
                .stream()
                .content();
    }


    /**
     * 使用配置文件提示词(使提示词和代码分离)-案例
     *
     */
    @GetMapping("/stream/chat03")
    public Flux<String> stream3(@RequestParam(value = "id", defaultValue = "0001") Long id,
                                @RequestParam(value = "sname", defaultValue = "张三") String sname,
                                @RequestParam(value = "major", defaultValue = "计算机科学与技术") String major,
                                @RequestParam(value = "phone", defaultValue = "13800000000") String phone,
                                @RequestParam(value = "msg", defaultValue = "请问，你叫什么") String msg) {

        //1.使用配置文件提示词
        PromptTemplate promptTemplate = new PromptTemplate(userTemplate);

        //填充promptTemplate内的占位符
        Prompt prompt = promptTemplate.create(
                Map.of(
                        "id", id,
                        "sname", sname,
                        "major", major,
                        "phone", phone
                )

        );

        return geminiClient.prompt(prompt)
                .system("请以html格式的语句进行输出")
                .user(msg)
                .stream()
                .content();
    }


    /**
     * 使用配置文件提示词(使提示词和代码分离)-案例
     *
     */
    @GetMapping("/stream/chat04")
    public Stream<ChatClientResponse> stream4(@RequestParam(value = "id", defaultValue = "0001") Long id,
                                              @RequestParam(value = "sname", defaultValue = "张三") String sname,
                                              @RequestParam(value = "major", defaultValue = "计算机科学与技术") String major,
                                              @RequestParam(value = "phone", defaultValue = "13800000000") String phone,
                                              @RequestParam(value = "msg", defaultValue = "请问，你叫什么") String msg) {

        //1.使用配置文件提示词
        PromptTemplate promptTemplate = new PromptTemplate(userTemplate);

        //填充promptTemplate内的占位
        Prompt prompt = promptTemplate.create(
                Map.of(
                        "id", id,
                        "sname", sname,
                        "major", major,
                        "phone", phone
                )

        );

        return geminiClient.prompt(prompt)
                .system("请以html格式的语句进行输出")
                .user(msg)
                .stream()
                .chatClientResponse()
                .toStream();
    }

    //
    @GetMapping("/stream/chat05")
    public StudtentRecord stream5(@RequestParam(value = "id", defaultValue = "0001") Long id,
                                  @RequestParam(value = "sname", defaultValue = "张三") String sname,
                                  @RequestParam(value = "major", defaultValue = "计算机科学与技术") String major,
                                  @RequestParam(value = "phone", defaultValue = "13800000000") String phone,
                                  @RequestParam(value = "msg", defaultValue = "请问，你叫什么") String msg) {
        String stringTemplate = "你的学号{id}，，你叫{sname}，你是一名{major}专业的学生，你的手机号是{phone}，你正在使用spring ai进行对话。";
        return geminiClient.prompt()
                .system("请以html格式的语句进行输出")
                .user(new Consumer<ChatClient.PromptUserSpec>() {
                    @Override
                    public void accept(ChatClient.PromptUserSpec promptUserSpec) {
                        promptUserSpec.text(stringTemplate).param("id", id).param("sname", sname).param("major", major).param("phone", phone);
                    }
                })
                .call()
                .entity(StudtentRecord.class);
    }







}
