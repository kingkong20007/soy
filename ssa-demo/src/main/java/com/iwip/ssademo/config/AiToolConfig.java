//package com.iwip.ssademo.config;
//
//import com.iwip.ssademo.utils.CommonUtil;
//import org.springframework.ai.tool.annotation.Tool;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Description;
//import java.util.function.Function;
//
//@Configuration
//public class AiToolConfig {
//
//    // 1. 定义入参结构 (对于不需要参数的工具，定义一个空的 record 接收 LLM 传来的空 JSON)
//    public record TimeRequest() {}
//
//
//    public Function<TimeRequest, String> currentTimeTool() {
//        return request -> CommonUtil.getCurrentTime();
//    }
//
//    // 2. 定义搜索工具的入参结构 (LLM 会自动提取 keyword 填充到这里)
//    public record SearchRequest(String keyword) {}
//
//
//    public Function<SearchRequest, String> searchWebTool() {
//        return request -> CommonUtil.searchWeb(request.keyword());
//    }
//}