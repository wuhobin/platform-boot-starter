package com.aurora.starter.oss.template;

import com.aurora.starter.oss.model.OssUploadResult;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Consumer;

/**
 * OSS 文件操作模板.
 * <p>
 * 封装 x-file-storage FileStorageService 的常用操作，
 * 提供快捷上传/下载/删除/复制/移动方法。
 * 需要完整能力时，可直接注入 {@link FileStorageService} 使用。
 *
 * @author whb
 */
public class OssTemplate {

    private final FileStorageService fileStorageService;

    public OssTemplate(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    // ==================== 快捷上传 ====================

    /** 上传 MultipartFile */
    public OssUploadResult upload(MultipartFile file) {
        return OssUploadResult.from(fileStorageService.of(file).upload());
    }

    /** 上传 File */
    public OssUploadResult upload(File file) {
        return OssUploadResult.from(fileStorageService.of(file).upload());
    }

    /** 上传字节数组 */
    public OssUploadResult upload(byte[] bytes, String originalFilename) {
        return OssUploadResult.from(
                fileStorageService.of(bytes).setOriginalFilename(originalFilename).upload());
    }

    /** 上传输入流 */
    public OssUploadResult upload(InputStream in, String originalFilename) {
        return OssUploadResult.from(
                fileStorageService.of(in).setOriginalFilename(originalFilename).upload());
    }

    /** 上传到指定路径 */
    public OssUploadResult upload(MultipartFile file, String path) {
        return OssUploadResult.from(
                fileStorageService.of(file).setPath(path).upload());
    }

    /** 上传到指定路径并设置文件名 */
    public OssUploadResult upload(MultipartFile file, String path, String saveFilename) {
        return OssUploadResult.from(
                fileStorageService.of(file).setPath(path).setSaveFilename(saveFilename).upload());
    }

    /** 上传到指定平台、路径、文件名 */
    public OssUploadResult upload(MultipartFile file, String path, String saveFilename, String platform) {
        return OssUploadResult.from(
                fileStorageService.of(file)
                        .setPath(path)
                        .setSaveFilename(saveFilename)
                        .setPlatform(platform)
                        .upload());
    }

    /**
     * 上传图片并缩放 + 生成缩略图.
     *
     * @param file   图片文件
     * @param width  目标宽度
     * @param height 目标高度
     */
    public OssUploadResult uploadImage(MultipartFile file, int width, int height) {
        return OssUploadResult.from(
                fileStorageService.of(file)
                        .image(img -> img.size(width, height))
                        .thumbnail(th -> th.size(width, height))
                        .upload());
    }

    // ==================== 链式上传 ====================

    /** 链式上传：MultipartFile */
    public OssUploadBuilder of(MultipartFile file) {
        return new OssUploadBuilder(fileStorageService.of(file));
    }

    /** 链式上传：File */
    public OssUploadBuilder of(File file) {
        return new OssUploadBuilder(fileStorageService.of(file));
    }

    /** 链式上传：byte[] */
    public OssUploadBuilder of(byte[] bytes, String originalFilename) {
        return new OssUploadBuilder(
                fileStorageService.of(bytes).setOriginalFilename(originalFilename));
    }

    /** 链式上传：InputStream */
    public OssUploadBuilder of(InputStream in, String originalFilename) {
        return new OssUploadBuilder(
                fileStorageService.of(in).setOriginalFilename(originalFilename));
    }

    // ==================== 下载 ====================

    /** 通过 URL 下载为字节数组 */
    public byte[] download(String url) {
        return download(toFileInfo(url));
    }

    /** 通过 FileInfo 下载为字节数组 */
    public byte[] download(FileInfo fileInfo) {
        return fileStorageService.download(fileInfo).bytes();
    }

    /** 通过 URL 下载到 OutputStream */
    public void download(String url, OutputStream out) {
        fileStorageService.download(toFileInfo(url)).outputStream(out);
    }

    /** 通过 URL 下载，以 Consumer 方式处理 InputStream */
    public void download(String url, Consumer<InputStream> consumer) {
        fileStorageService.download(toFileInfo(url)).inputStream(consumer);
    }

    // ==================== 删除 ====================

    /** 通过 URL 删除文件 */
    public boolean delete(String url) {
        return delete(toFileInfo(url));
    }

    /** 通过 FileInfo 删除文件 */
    public boolean delete(FileInfo fileInfo) {
        return fileStorageService.delete(fileInfo);
    }

    // ==================== 文件信息 ====================

    /** 通过 URL 获取文件信息 */
    public FileInfo getFileInfo(String url) {
        return fileStorageService.getFileInfoByUrl(url);
    }

    /** 判断文件是否存在 */
    public boolean exists(String url) {
        return fileStorageService.exists(toFileInfo(url));
    }

    // ==================== 复制/移动 ====================

    /** 同平台复制文件 */
    public OssUploadResult copy(String sourceUrl, String targetPath, String targetFilename) {
        return copy(sourceUrl, targetPath, targetFilename, null);
    }

    /** 指定平台复制文件 */
    public OssUploadResult copy(String sourceUrl, String targetPath, String targetFilename, String platform) {
        FileInfo sourceFile = fileStorageService.getFileInfoByUrl(sourceUrl);
        FileInfo result = fileStorageService.copy(sourceFile)
                .setPath(targetPath)
                .setFilename(targetFilename)
                .setPlatform(platform)
                .copy();
        return OssUploadResult.from(result);
    }

    /** 同平台移动/重命名文件 */
    public OssUploadResult move(String sourceUrl, String targetPath, String targetFilename) {
        return move(sourceUrl, targetPath, targetFilename, null);
    }

    /** 指定平台移动文件 */
    public OssUploadResult move(String sourceUrl, String targetPath, String targetFilename, String platform) {
        FileInfo sourceFile = fileStorageService.getFileInfoByUrl(sourceUrl);
        FileInfo result = fileStorageService.move(sourceFile)
                .setPath(targetPath)
                .setFilename(targetFilename)
                .setPlatform(platform)
                .move();
        return OssUploadResult.from(result);
    }

    /** 检查平台是否支持同平台复制 */
    public boolean isSupportCopy(String platform) {
        return fileStorageService.isSupportSameCopy(platform);
    }

    /** 检查平台是否支持同平台移动 */
    public boolean isSupportMove(String platform) {
        return fileStorageService.isSupportSameMove(platform);
    }

    // ==================== 原生服务暴露 ====================

    /** 获取底层的 FileStorageService，用于需要完整 API 的场景 */
    public FileStorageService getFileStorageService() {
        return fileStorageService;
    }

    // ==================== 内部辅助 ====================

    private FileInfo toFileInfo(String url) {
        return new FileInfo().setUrl(url);
    }
}
