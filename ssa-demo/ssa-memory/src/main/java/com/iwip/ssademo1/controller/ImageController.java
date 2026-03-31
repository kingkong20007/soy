package com.iwip.ssademo1.controller;

import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImageOptionsBuilder;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/image")
public class ImageController {

    @Autowired
    private ImageModel imageModel;

    private static final String IMAGE_MODEL = "wanx-v1";

    /**
     * 使用通义万相生成图片
     *
     * @param prompt 提示词
     * @return 图片的 URL 地址
     */
    @RequestMapping("/dashscope")
    public String dashScopeImage(@RequestParam(value = "prompt", defaultValue = "cat") String prompt) {
        // 调用图片模型生成图片，并获取第一张图片的 URL
        return imageModel.call(
                        new ImagePrompt(prompt, ImageOptionsBuilder.builder()

                                .model(IMAGE_MODEL).build())
                )
                .getResult().getOutput().getUrl();
    }


}
