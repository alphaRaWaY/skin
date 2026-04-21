package org.skinAI.controller;

import org.skinAI.pojo.Result;
import org.skinAI.services.OssService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/oss")
public class OssController {

    private final OssService ossService;

    public OssController(OssService ossService) {
        this.ossService = ossService;
    }

    @PostMapping("/image-url")
    public Result<String> getImageUrl(@RequestBody String objectKey) {
        if (objectKey == null || objectKey.trim().isEmpty()) {
            return Result.error("objectKey is empty");
        }

        String url = ossService.generatePresignedUrl(objectKey);
        if (url == null) {
            return Result.error("failed to generate image url");
        }
        return Result.success(url);
    }

    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) throws Exception {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            return Result.error("invalid file name");
        }

        String objectKey = "skinAI/" + UUID.randomUUID() + originalFilename.substring(originalFilename.lastIndexOf("."));
        String storedKey = ossService.uploadFile(objectKey, file.getInputStream());
        return Result.success(storedKey);
    }

    @DeleteMapping("/object")
    public Result deleteObject(@RequestParam("objectKey") String objectKey) {
        if (objectKey == null || objectKey.trim().isEmpty()) {
            return Result.error("objectKey is empty");
        }
        ossService.deleteFile(objectKey);
        return Result.success();
    }
}
