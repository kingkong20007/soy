package com.iwip.system.controller.system;

import com.iwip.common.core.domain.R;
import com.iwip.common.log.annotation.Log;
import com.iwip.common.log.enums.BusinessType;
import com.iwip.common.minio.utils.MinioUtils;
import com.iwip.common.web.core.BaseController;
import com.iwip.system.domain.vo.SysFileVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.iwip.common.minio.config.MinioProperties;

/**
 * 文件请求处理
 *
 * @author iwip
 */
@Slf4j
@RestController
@RequestMapping("/resource/file")
@RequiredArgsConstructor
public class SysFileController extends BaseController {

    private final MinioUtils minioUtils;

    /**
     * 通用上传请求（单一）
     */
    @Log(title = "通用上传", businessType = BusinessType.INSERT)
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<SysFileVo> upload(@RequestPart("file") MultipartFile file) {
        try {
            // 上传并返回对象名称 (e.g. yyyy/MM/dd/uuid.jpg)
            String objectName = minioUtils.uploadWebFile(minioUtils.getBucketName(), file, "upload");

            // 存入数据库的是：桶名 + / + 相对路径
            String url = minioUtils.getRelativePath(minioUtils.getBucketName(), objectName);

            SysFileVo sysFileVo = new SysFileVo();
            sysFileVo.setFileName(objectName);
            sysFileVo.setOriginalName(file.getOriginalFilename());
            sysFileVo.setUrl(url);
            sysFileVo.setSize(file.getSize());

            return R.ok(sysFileVo);
        } catch (Exception e) {
            log.error("上传文件失败", e);
            return R.fail(e.getMessage());
        }
    }

}
