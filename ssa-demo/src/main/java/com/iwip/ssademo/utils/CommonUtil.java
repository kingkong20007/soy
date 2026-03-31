package com.iwip.ssademo.utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.ai.tool.annotation.Tool;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.Duration;

public class CommonUtil {

    /**
     * 获取当前的系统时间与日期。
     * 当用户询问当前时间、今天星期几、当前日期时，大模型应当调用此工具。
     * * @return 包含时区信息的当前时间格式化字符串，例如："2026-03-30 20:07:39 WIB"
     */
    @Tool(description = "获取当前的系统时间与日期。当用户询问当前时间、今天星期几、当前日期时调用此工具。",returnDirect = true)
    public String getCurrentTime() {
        // 设定为印尼雅加达时间，贴合你当前所在的时区环境
        ZoneId zoneId = ZoneId.of("Asia/Seoul");
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
        return now.format(formatter);
    }

    /**
     * 联网搜索引擎工具。
     * 当你需要查询最新的知识、新闻、或者你不知道的客观事实时，请提取核心关键词并调用此工具获取外部信息。
     * * @param keyword 搜索的关键词，必须是精简的词语或短语
     * @return 搜索引擎返回的相关内容摘要或 JSON 数据
     */
    @Tool(description = "联网搜索引擎工具。当需要查询最新的知识、新闻或不知道的客观事实时调用此工具获取外部信息。",returnDirect = false)
    public  String searchWeb(String keyword) {
        // 🌟 第一步：一定要在控制台打印日志，看看 AI 到底传了什么词！
        System.out.println("========== 触发联网工具 ==========");
        System.out.println("【AI 提取的搜索词】: " + keyword);

        if (keyword == null || keyword.trim().isEmpty()) {
            return "搜索关键词不能为空";
        }

        try {
            // 改用百度搜索，国内数码资讯抓取更准
            String encodedKeyword = URLEncoder.encode(keyword.trim(), StandardCharsets.UTF_8);
            String url = "https://www.baidu.com/s?wd=" + encodedKeyword;

            // 增强伪装，加上 Referer 和 Accept，防止被百度识别为机器
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .header("Referer", "https://www.baidu.com/")
                    .timeout(10000)
                    .get();

            // 百度的搜索结果通常在 class 包含 c-container 的 div 中
            Elements results = doc.select("div.c-container");
            StringBuilder summaryBuilder = new StringBuilder();

            int count = 0;
            for (Element result : results) {
                if (count >= 3) break;

                // 提取标题和摘要（百度摘要的 class 经常变，这里多兼容几个）
                String title = result.select("h3").text();
                String snippet = result.select(".c-abstract, .content-right_8Zs40, .c-span-last").text();

                if (!title.isEmpty() && !snippet.isEmpty()) {
                    summaryBuilder.append("【标题】: ").append(title).append("\n");
                    summaryBuilder.append("【内容】: ").append(snippet).append("\n\n");
                    count++;
                }
            }

            String finalResult = summaryBuilder.toString();
            System.out.println("【抓取到的内容长度】: " + finalResult.length() + " 字符");

            if (finalResult.isEmpty()) {
                return "未找到相关结果，可能是搜索引擎限制了抓取，或者该关键词没有有效新闻。请提示用户换个问题。";
            }
            return "以下是最新搜索到的网络信息，请根据这些信息回答用户：\n" + finalResult;

        } catch (Exception e) {
            System.out.println("【抓取异常】: " + e.getMessage());
            return "执行联网搜索时发生异常: " + e.getMessage();
        }
    }
}