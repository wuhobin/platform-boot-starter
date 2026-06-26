package com.aurora.starter.oss.template;

import com.aurora.starter.oss.model.OssUploadResult;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.UploadPretreatment;

/**
 * 链式上传 Builder.
 * <p>
 * 封装 x-file-storage 的链式上传 API，提供 setPath/setPlatform/thumbnail 等
 * 常用配置方法，最终通过 {@link #upload()} 或 {@link #uploadRaw()} 执行上传。
 *
 * @author whb
 */
public class OssUploadBuilder {

    private final UploadPretreatment pretreatment;

    public OssUploadBuilder(UploadPretreatment pretreatment) {
        this.pretreatment = pretreatment;
    }

    /** 设置保存路径 */
    public OssUploadBuilder setPath(String path) {
        pretreatment.setPath(path);
        return this;
    }

    /** 设置保存文件名 */
    public OssUploadBuilder setSaveFilename(String filename) {
        pretreatment.setSaveFilename(filename);
        return this;
    }

    /** 指定存储平台 */
    public OssUploadBuilder setPlatform(String platform) {
        pretreatment.setPlatform(platform);
        return this;
    }

    /** 关联对象 ID */
    public OssUploadBuilder setObjectId(String objectId) {
        pretreatment.setObjectId(objectId);
        return this;
    }

    /** 关联对象类型 */
    public OssUploadBuilder setObjectType(String objectType) {
        pretreatment.setObjectType(objectType);
        return this;
    }

    /** 保存自定义属性 */
    public OssUploadBuilder putAttr(String key, Object value) {
        pretreatment.putAttr(key, value);
        return this;
    }

    /** 设置缩略图后缀 */
    public OssUploadBuilder setThumbnailSuffix(String suffix) {
        pretreatment.setThumbnailSuffix(suffix);
        return this;
    }

    /** 保存时使用的缩略图文件名 */
    public OssUploadBuilder setSaveThFilename(String thFilename) {
        pretreatment.setSaveThFilename(thFilename);
        return this;
    }

    /**
     * 缩放原图.
     *
     * @param width  目标宽度
     * @param height 目标高度
     */
    public OssUploadBuilder image(int width, int height) {
        pretreatment.image(img -> img.size(width, height));
        return this;
    }

    /**
     * 生成缩略图.
     *
     * @param width  缩略图宽度
     * @param height 缩略图高度
     */
    public OssUploadBuilder thumbnail(int width, int height) {
        pretreatment.thumbnail(th -> th.size(width, height));
        return this;
    }

    /** 执行上传，返回 OssUploadResult */
    public OssUploadResult upload() {
        FileInfo fileInfo = pretreatment.upload();
        return OssUploadResult.from(fileInfo);
    }

    /** 执行上传，返回原生 FileInfo */
    public FileInfo uploadRaw() {
        return pretreatment.upload();
    }
}
