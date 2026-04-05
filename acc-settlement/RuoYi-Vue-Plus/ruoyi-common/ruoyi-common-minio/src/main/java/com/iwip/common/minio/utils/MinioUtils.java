package com.iwip.common.minio.utils;

import com.iwip.common.minio.config.MinioProperties;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.Bucket;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class MinioUtils {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private static final String SEPARATOR = "/";

    // region ==================== 基础与存储桶操作 ====================

    /**
     * 获取基础访问地址
     *
     * @param bucketName 存储桶名称
     * @return 基础 URL 格式：url/bucketName/
     */
    public String getBasisUrl(String bucketName) {
        return minioProperties.getUrl() + SEPARATOR + bucketName + SEPARATOR;
    }

    /**
     * 获取存储桶名称
     *
     * @return 存储桶名称
     */
    public String getBucketName() {
        return minioProperties.getBucketName();
    }

    /**
     * 获取相对存储路径（存入数据库的格式：bucketName/objectName）
     *
     * @param objectName 对象名
     * @return 相对存储路径
     */
    public String getRelativePath(String objectName) {
        return getBucketName() + SEPARATOR + objectName;
    }

    /**
     * 获取相对存储路径（存入数据库的格式：bucketName/objectName）
     *
     * @param bucketName 存储桶名称
     * @param objectName 对象名
     * @return 相对存储路径
     */
    public String getRelativePath(String bucketName, String objectName) {
        return bucketName + SEPARATOR + objectName;
    }

    /**
     * 存储桶是否存在
     *
     * @param bucketName
     * @return
     * @throws Exception
     */
    public boolean bucketExists(String bucketName) throws Exception {
        return minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
    }

    public void createBucket(String bucketName) throws Exception {
        if (!bucketExists(bucketName)) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            log.info("Bucket [{}] created successfully.", bucketName);
        }
    }

    public List<Bucket> getAllBuckets() throws Exception {
        return minioClient.listBuckets();
    }

    public Optional<Bucket> getBucket(String bucketName) throws Exception {
        return minioClient.listBuckets().stream().filter(b -> b.name().equals(bucketName)).findFirst();
    }

    public void removeBucket(String bucketName) throws Exception {
        minioClient.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
    }



    // endregion

    // region ==================== 对象与目录查询操作 ====================

    public boolean doesObjectExist(String bucketName, String objectName) {
        try {
            minioClient.statObject(StatObjectArgs.builder().bucket(bucketName).object(objectName).build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean doesFolderExist(String bucketName, String objectName) {
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder().bucket(bucketName).prefix(objectName).recursive(false).build());
            for (Result<Item> result : results) {
                Item item = result.get();
                if (item.isDir() && objectName.equals(item.objectName())) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("Check folder exist failed for {}/{}", bucketName, objectName, e);
        }
        return false;
    }

    public List<Item> getAllObjectsByPrefix(String bucketName, String prefix, boolean recursive) throws Exception {
        List<Item> list = new ArrayList<>();
        Iterable<Result<Item>> objectsIterator = minioClient.listObjects(
            ListObjectsArgs.builder().bucket(bucketName).prefix(prefix).recursive(recursive).build());
        for (Result<Item> o : objectsIterator) {
            list.add(o.get());
        }
        return list;
    }

    public List<Item> listObjects(int limit, String bucketName) {
        List<Item> objects = new ArrayList<>();
        Iterable<Result<Item>> results = minioClient.listObjects(
            ListObjectsArgs.builder().bucket(bucketName).maxKeys(limit).includeVersions(true).build());
        try {
            for (Result<Item> result : results) {
                objects.add(result.get());
            }
        } catch (Exception e) {
            log.error("List objects failed", e);
            throw new RuntimeException("获取文件列表失败", e);
        }
        return objects;
    }

    // endregion

    // region ==================== 核心上传操作 ====================


    /**
     * 通用的 Web 文件上传（带日期分包策略）--- 使用默认存储桶名称
     *
     * @param file       文件对象
     * @param customPath 自定义路径前缀 (可为空，默认 yyyy/MM/dd/)
     * @return 返回 MinIO 中的完整 ObjectName
     */
    public String uploadWebFile(MultipartFile file, String customPath) throws Exception {
        return uploadWebFile(getBucketName(), file, customPath);
    }

    /**
     * 通用的 Web 文件上传（带日期分包策略）--- 使用指定存储桶名称
     *
     * @param bucketName 桶名称
     * @param file       文件对象
     * @param customPath 自定义路径前缀 (可为空，默认 yyyy/MM/dd/)
     * @return 返回 MinIO 中的完整 ObjectName
     */
    public String uploadWebFile(String bucketName, MultipartFile file, String customPath) throws Exception {
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String fileName = UUID.randomUUID().toString().replace("-", "")
            + (StringUtils.hasText(extension) ? "." + extension : "");

        String objectName = buildFilePath(fileName, customPath);
        putObject(bucketName, file, objectName);
        return objectName;
    }

    /**
     * 底层流上传（适用于后端生成文件直接上传，如 EasyExcel 导出）
     */
    public String uploadInputStream(String bucketName, InputStream inputStream, String originalFilename, String customPath) throws Exception {
        String extension = StringUtils.getFilenameExtension(originalFilename);
        String fileName = UUID.randomUUID().toString().replace("-", "")
            + (StringUtils.hasText(extension) ? "." + extension : "");

        String objectName = buildFilePath(fileName, customPath);
        String contentType = getContentType(originalFilename);

        try (inputStream) { // JDK9+ 自动关闭流
            minioClient.putObject(
                PutObjectArgs.builder().bucket(bucketName).object(objectName)
                    .contentType(contentType)
                    .stream(inputStream, inputStream.available(), -1).build());
        }
        return objectName;
    }

    /**
     * 字节数组上传（适用于生成图片海报等）
     */
    public String uploadByteArray(String bucketName, byte[] data, String originalFilename, String customPath) throws Exception {
        return uploadInputStream(bucketName, new ByteArrayInputStream(data), originalFilename, customPath);
    }

    /**
     * 上传 MultipartFile 到指定 ObjectName
     */
    public ObjectWriteResponse putObject(String bucketName, MultipartFile file, String objectName) throws Exception {
        return putObject(bucketName, file, objectName, getContentType(file.getOriginalFilename()));
    }

    /**
     * 基础：上传 MultipartFile，指定 ContentType
     */
    public ObjectWriteResponse putObject(String bucketName, MultipartFile file, String objectName, String contentType) throws Exception {
        try (InputStream inputStream = file.getInputStream()) {
            return minioClient.putObject(
                PutObjectArgs.builder().bucket(bucketName).object(objectName).contentType(contentType)
                    .stream(inputStream, inputStream.available(), -1).build());
        }
    }

    /**
     * 基础：将服务器本地文件上传至 MinIO
     *
     * @param localFilePath 服务器本地的绝对路径
     */
    public ObjectWriteResponse putLocalFile(String bucketName, String objectName, String localFilePath) throws Exception {
        return minioClient.uploadObject(
            UploadObjectArgs.builder().bucket(bucketName).object(objectName).filename(localFilePath).build());
    }

    /**
     * 创建空目录
     */
    public ObjectWriteResponse putDirObject(String bucketName, String objectName) throws Exception {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(new byte[]{})) {
            return minioClient.putObject(
                PutObjectArgs.builder().bucket(bucketName).object(objectName)
                    .stream(bais, 0, -1).build());
        }
    }

    /**
     * 将网络 URL 资源转储至 Minio
     */
    public String netToMinio(String httpUrl, String bucketName, String customPath) {
        String extension = StringUtils.getFilenameExtension(httpUrl);
        String fileName = UUID.randomUUID().toString().replace("-", "")
            + (StringUtils.hasText(extension) ? "." + extension : "");
        String objectName = buildFilePath(fileName, customPath);

        try {
            URL url = new URL(httpUrl);
            URLConnection connection = url.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            // 优化：直接流式传输，不生成本地临时文件
            try (InputStream in = connection.getInputStream()) {
                long contentLength = connection.getContentLengthLong();
                minioClient.putObject(
                    PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .contentType(getContentType(httpUrl))
                        // 如果获取不到 contentLength，则传入 -1，由 MinIO 自动处理分片 (默认 10MB/part)
                        .stream(in, contentLength > 0 ? contentLength : -1, 10485760)
                        .build());
            }
            return objectName;
        } catch (Exception e) {
            log.error("Network file to Minio failed for URL: {}", httpUrl, e);
            throw new RuntimeException("网络文件转储失败", e);
        }
    }

    // endregion

    // region ==================== 下载与流读取操作 ====================

    /**
     * Web 下载文件（增强了请求头防缓存处理）
     */
    public void downloadFile(String bucketName, String objectName, String downloadName, HttpServletRequest request, HttpServletResponse response) {
        try (InputStream fileStream = getObject(bucketName, objectName);
             ServletOutputStream outputStream = response.getOutputStream()) {

            String useragent = request.getHeader("USER-AGENT").toLowerCase();
            String encodedFilename;
            if (useragent.contains("msie") || useragent.contains("like gecko") || useragent.contains("trident")) {
                encodedFilename = URLEncoder.encode(downloadName, StandardCharsets.UTF_8);
            } else {
                encodedFilename = new String(downloadName.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
            }

            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(getContentType(downloadName));
            // 禁止缓存，防止同名文件更新后浏览器走本地缓存
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setDateHeader("Expires", 0);
            response.setHeader("Content-Disposition", "attachment;filename=" + encodedFilename);

            fileStream.transferTo(outputStream);
            outputStream.flush();

        } catch (Exception e) {
            log.error("File download failed. Bucket: {}, Object: {}", bucketName, objectName, e);
            throw new RuntimeException("文件下载失败", e);
        }
    }

    public InputStream getObject(String bucketName, String objectName) throws Exception {
        return minioClient.getObject(GetObjectArgs.builder().bucket(bucketName).object(objectName).build());
    }

    public InputStream getObject(String bucketName, String objectName, long offset, long length) throws Exception {
        return minioClient.getObject(GetObjectArgs.builder().bucket(bucketName).object(objectName).offset(offset).length(length).build());
    }

    public byte[] fileToBytes(String localPath) {
        try {
            return Files.readAllBytes(Paths.get(localPath));
        } catch (IOException e) {
            log.error("Read local file to bytes failed: {}", localPath, e);
            throw new RuntimeException("读取本地文件失败", e);
        }
    }

    // endregion

    // region ==================== 删除操作 ====================

    public void removeObject(String bucketName, String objectName) throws Exception {
        minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
    }

    public void removeObjects(String bucketName, List<String> objectNames) {
        if (objectNames == null || objectNames.isEmpty()) return;

        List<DeleteObject> objects = objectNames.stream().map(DeleteObject::new).collect(Collectors.toList());
        try {
            Iterable<Result<DeleteError>> results = minioClient.removeObjects(
                RemoveObjectsArgs.builder().bucket(bucketName).objects(objects).build());
            for (Result<DeleteError> result : results) {
                DeleteError error = result.get();
                log.error("Error deleting object {}: {}", error.objectName(), error.message());
            }
        } catch (Exception e) {
            log.error("Batch delete failed", e);
            throw new RuntimeException("批量删除文件失败", e);
        }
    }

    // endregion

    // region ==================== 预览与辅助方法 ====================

    public List<String> getPreviewUrlByData(String data) {
        if (!StringUtils.hasText(data)) return Collections.emptyList();

        List<String> list = new ArrayList<>();
        String[] infos = data.split(",");
        for (String info : infos) {
            String previewUrl = getPreviewUrl(getBucketName(), info, 10, TimeUnit.MINUTES);
            // 替换外网域名映射
            if (StringUtils.hasText(minioProperties.getPreviewUrl()) && StringUtils.hasText(previewUrl)) {
                previewUrl = previewUrl.replace(minioProperties.getUrl(), minioProperties.getPreviewUrl());
            }
            list.add(previewUrl);
        }
        return list;
    }

    public String getPreviewUrl(String bucketName, String objectName, Integer validTime, TimeUnit timeUnit) {
        try {
            Map<String, String> reqParams = new HashMap<>();
            reqParams.put("response-content-type", getContentType(objectName));

            return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucketName)
                    .object(objectName)
                    .expiry(validTime, timeUnit)
                    .extraQueryParams(reqParams)
                    .build());
        } catch (Exception e) {
            log.error("Get preview URL failed for object: {}", objectName, e);
            throw new RuntimeException("获取预览链接失败", e);
        }
    }

    public String getPreviewUrl(String bucketName, String objectName) {
        return getPreviewUrl(bucketName, objectName, null, null);
    }

    public ObjectWriteResponse copyObject(String bucketName, String objectName, String srcBucketName, String srcObjectName) throws Exception {
        return minioClient.copyObject(
            CopyObjectArgs.builder()
                .source(CopySource.builder().bucket(srcBucketName).object(srcObjectName).build())
                .bucket(bucketName)
                .object(objectName)
                .build());
    }

    public String getUtf8ByURLDecoder(String str) throws UnsupportedEncodingException {
        String url = str.replaceAll("%(?![0-9a-fA-F]{2})", "%25");
        return URLDecoder.decode(url, StandardCharsets.UTF_8);
    }

    //endregion

    // region ==================== 私有工具方法 ====================

    /**
     * 获取文件类型 MimeType
     */
    private String getContentType(String filename) {
        String extension = StringUtils.getFilenameExtension(filename);
        if (!StringUtils.hasText(extension)) return "application/octet-stream";

        return switch (extension.toLowerCase()) {
            case "mp4" -> "video/mp4";
            case "jpeg", "jpg" -> "image/jpeg";
            case "png" -> "image/png";
            case "pdf" -> "application/pdf";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "xls" -> "application/vnd.ms-excel";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default -> "application/octet-stream";
        };
    }

    /**
     * 构建按日期的层级路径: 优先使用 customPath，否则默认 yyyy/MM/dd/
     */
    private String buildFilePath(String fileName, String customPath) {
        String prefix;
        if (StringUtils.hasText(customPath)) {
            prefix = customPath.endsWith("/") ? customPath : customPath + "/";
        } else {
            prefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd/"));
        }
        return prefix + fileName;
    }

    // endregion
}
