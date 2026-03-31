package com.iwip.ssademo1.controller;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.google.genai.types.Blob;
import com.google.genai.types.SpeechConfig;
import com.google.genai.types.VoiceConfig;
import com.google.genai.types.PrebuiltVoiceConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/voice")
public class VoiceController {

    @Autowired
    private Client genAiClient;

    private static final String AUDIO_MODEL = "gemini-2.5-flash-preview-tts";

    @GetMapping("/tts")
    public ResponseEntity<byte[]> textToSpeech(@RequestParam(value = "text", defaultValue = "你好，很高兴为你服务！") String text) {
        try {
            GenerateContentConfig config = GenerateContentConfig.builder()
                    .responseModalities(List.of("AUDIO"))
                    .speechConfig(SpeechConfig.builder()
                            .voiceConfig(VoiceConfig.builder()
                                    .prebuiltVoiceConfig(PrebuiltVoiceConfig.builder()
                                            .voiceName("Aoede")
                                            .build())
                                    .build())
                            .build())
                    .build();

            GenerateContentResponse response = genAiClient.models.generateContent(
                    AUDIO_MODEL,
                    text,
                    config
            );

            if (response.candidates().isPresent() && !response.candidates().get().isEmpty()) {
                var candidate = response.candidates().get().get(0);
                if (candidate.content().isPresent() && candidate.content().get().parts().isPresent()) {
                    List<Part> parts = candidate.content().get().parts().get();

                    for (Part part : parts) {
                        if (part.inlineData().isPresent()) {
                            Blob blob = part.inlineData().get();

                            if (blob.data().isPresent()) {
                                // 1. 获取原始 PCM 数据
                                byte[] pcmData = blob.data().get();

                                // 2. 将 PCM 包装成标准的 WAV 格式
                                byte[] wavData = addWavHeader(pcmData);

                                // 3. 设置 HTTP 响应头，告诉浏览器这是一个可播放的 wav 音频
                                HttpHeaders headers = new HttpHeaders();
                                headers.setContentType(MediaType.valueOf("audio/wav"));
                                // inline 表示在浏览器中直接播放，如果是 attachment 则会触发下载
                                headers.setContentDispositionFormData("inline", "output.wav");

                                return new ResponseEntity<>(wavData, headers, HttpStatus.OK);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ResponseEntity<>(new byte[0], HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 工具方法：为裸 PCM 数据添加 44 字节的 WAV 文件头
     * Gemini TTS 默认输出格式为：24000 Hz 采样率, 16 bit 采样位数, 单声道 (Mono)
     */
    private byte[] addWavHeader(byte[] pcmData) {
        int sampleRate = 24000;
        int channels = 1;
        int bitsPerSample = 16;
        int byteRate = sampleRate * channels * bitsPerSample / 8;
        int totalDataLen = pcmData.length + 36;

        byte[] header = new byte[44];
        header[0] = 'R'; header[1] = 'I'; header[2] = 'F'; header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W'; header[9] = 'A'; header[10] = 'V'; header[11] = 'E';
        header[12] = 'f'; header[13] = 'm'; header[14] = 't'; header[15] = ' ';
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0;
        header[20] = 1; header[21] = 0;
        header[22] = (byte) channels; header[23] = 0;
        header[24] = (byte) (sampleRate & 0xff);
        header[25] = (byte) ((sampleRate >> 8) & 0xff);
        header[26] = (byte) ((sampleRate >> 16) & 0xff);
        header[27] = (byte) ((sampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (channels * bitsPerSample / 8); header[33] = 0;
        header[34] = 16; header[35] = 0;
        header[36] = 'd'; header[37] = 'a'; header[38] = 't'; header[39] = 'a';
        header[40] = (byte) (pcmData.length & 0xff);
        header[41] = (byte) ((pcmData.length >> 8) & 0xff);
        header[42] = (byte) ((pcmData.length >> 16) & 0xff);
        header[43] = (byte) ((pcmData.length >> 24) & 0xff);

        // 将请求头和音频数据拼接
        byte[] wavData = new byte[header.length + pcmData.length];
        System.arraycopy(header, 0, wavData, 0, header.length);
        System.arraycopy(pcmData, 0, wavData, header.length, pcmData.length);

        return wavData;
    }
}