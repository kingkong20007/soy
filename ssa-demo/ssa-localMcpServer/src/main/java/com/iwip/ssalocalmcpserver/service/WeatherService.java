package com.iwip.ssalocalmcpserver.service;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

@Service
public class WeatherService {


    /**
     * 获取指定城市的实时天气信息
     *
     * @param city 城市名称，例如 "北京"
     * @return 模拟的天气描述字符串
     */
    @Tool(name = "getWeather", description = "获取指定城市的实时天气信息")
    public String getWeather(String city) {
        // 在实际生产环境中，这里应该调用第三方天气 API（如高德、和风天气等）
        // 此处为 MCP 演示目的，返回模拟数据
        return switch (city) {
            case "北京" -> "北京今天晴，气温 15°C 到 25°C，空气质量优。";
            case "上海" -> "上海今天多云转阴，气温 18°C 到 22°C，有微风。";
            case "广州" -> "广州今天阵雨，气温 22°C 到 28°C，湿度较大。";
            case "深圳" -> "深圳今天多云，气温 23°C 到 29°C，适合户外活动。";
            default -> city + "目前天气状况良好，气温适宜。";
        };
    }

}
