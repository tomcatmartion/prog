package com.shechubbb.smdc.controller.admin;

import com.shechubbb.smdc.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * 文件上传控制器
 */
@Slf4j
@RestController
@RequestMapping("/admin/upload")
public class AdminUploadController {

    @Value("${smdc.upload-path}")
    private String uploadPath;

    /**
     * 图片上传
     */
    @PostMapping("/image")
    public Result<String> uploadImage(MultipartFile file, HttpServletRequest request) {
        log.info("图片文件上传：{}", file.getOriginalFilename());
        
        // 创建目录
        File dir = new File(uploadPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        // 获取原始文件名和扩展名
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        
        // 使用UUID重命名文件，防止重名
        String fileName = UUID.randomUUID().toString() + suffix;
        
        try {
            // 保存文件
            file.transferTo(new File(dir, fileName));
            
            // 返回完整的访问路径
            String url = "/upload/" + fileName;
            return Result.success(url);
        } catch (IOException e) {
            log.error("文件上传失败：", e);
            return Result.error("文件上传失败");
        }
    }
} 