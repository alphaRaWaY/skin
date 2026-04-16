package org.skinAI.controller;

import org.skinAI.pojo.Result;
import org.skinAI.services.OssService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/oss")
public class OssController {
    @Autowired
    private OssService ossService;

    /**
     * 从OSS获取资源临时密钥
     * @param objectKey 访问资源的名称
     * @return 返回对应资源的密钥
     */
    @PostMapping("/image-url")
    public Result<String> getImageUrl(@RequestBody String objectKey) {
        if (objectKey == null || objectKey.trim().isEmpty()) {
            return Result.error("objectKey不能为空");
        }
        // 去掉前后可能的引号或空格
        objectKey = objectKey.trim().replaceAll("^\"|\"$", "");

        String url = ossService.generatePresignedUrl("skin/" + objectKey);
        if (url == null) {
            return Result.error("获取密钥失败");
        }
        return Result.success(url);
    }

    /**
     * 往OSS上传资源
     * @param file 文件流
     * @return 返回文件在官网上的资源名称
     * @throws Exception
     */
    @PostMapping("/upload")
    public Result<String> upload(MultipartFile file) throws Exception {
        String  fileOriginalFilename = file.getOriginalFilename();
        String directory = "skin/";  // 你想指定的目录
        //保证文件名字唯一,随机UUID拼接文件后缀
        String filename = UUID.randomUUID() + fileOriginalFilename.substring(fileOriginalFilename.lastIndexOf("."));
//        String filename = UUID.randomUUID()+fileOriginalFilename.substring(fileOriginalFilename.lastIndexOf("."));
        //调用工具类
        String url = ossService.uploadFile(directory+filename,file.getInputStream());
        return Result.success(filename);
    }
}