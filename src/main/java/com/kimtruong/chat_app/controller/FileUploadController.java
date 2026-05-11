package com.kimtruong.chat_app.controller;

import com.kimtruong.chat_app.service.AuditLogService;
import com.kimtruong.chat_app.service.FileUploadService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;


@RestController
@RequestMapping("/api")
public class FileUploadController {

    private final FileUploadService fileUploadService;
    private final AuditLogService auditLogService;

    public FileUploadController(FileUploadService fileUploadService, AuditLogService auditLogService) {
        this.fileUploadService = fileUploadService;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest servletRequest) {

        String url = fileUploadService.save(file);
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        String ip = servletRequest.getRemoteAddr();
        auditLogService.log(username, "FILE_UPLOAD", ip, "File: " + file.getOriginalFilename());
        return ResponseEntity.ok(Map.of("url", url));
    }

 /* Day 19 _ bước 1 : File Upload
    
 **🎯 Nó làm gì?**

Mở 1 REST endpoint `POST /api/upload` nhận file từ client, chuyển cho service xử lý, trả về URL của file vừa lưu dưới dạng JSON.

---

**1. Keyword lạ**

`MultipartFile` — interface của Spring đại diện cho file được upload lên qua HTTP. Không phải file thông thường trên disk — nó là wrapper chứa: tên file gốc, content type, bytes của file, size. Spring tự parse request và inject vào method khi có `@RequestParam("file")`.

`@RequestParam("file")` — báo Spring lấy field tên `"file"` từ form-data của request. Client phải gửi đúng key này, không phải JSON body.

`Map.of("url", url)` — tạo Map bất biến 1 cặp key-value ngay tại chỗ, không cần tạo class riêng. Jackson tự convert thành `{"url": "/uploads/abc.jpg"}`.

`ResponseEntity<Map<String, String>>` — wrapper cho HTTP response, cho phép control cả status code lẫn body. `ResponseEntity.ok(...)` = status 200 + body.

---
**2. ⚙️ Nó chạy như nào?**
```
Client gửi POST /api/upload
  Content-Type: multipart/form-data
  Body: file=<binary data>
        ↓
Spring parse request → tạo MultipartFile object
        ↓
@RequestParam("file") inject vào method
        ↓
fileUploadService.save(file) → validate + lưu → trả về URL string
        ↓
Map.of("url", "/uploads/abc-uuid.jpg")
        ↓
Jackson convert → {"url": "/uploads/abc-uuid.jpg"}
        ↓
Client nhận JSON, dùng URL để hiển thị ảnh
```

--
**3. 🔥 So sánh với cách khác**

**Cách này — `@RequestParam MultipartFile`:**
- Chuẩn cho upload đơn giản, Spring handle parse tự động
- Client gửi `multipart/form-data` — đúng với HTML form và fetch API

**Cách khác — nhận `@RequestBody byte[]`:**
- Phải tự parse binary, không có metadata (tên file, content type)
- Không dùng được trong thực tế

**Cách khác — tạo class `UploadResponse` thay vì `Map.of()`:**
```java
public class UploadResponse {
    private String url;
}
```
- Tốt hơn khi response có nhiều field hoặc cần Swagger documentation
- Với 1 field đơn giản như này thì `Map.of()` đủ dùng, không cần tạo thêm class

**`@RequestMapping("/api")` ở class vs đặt thẳng vào method:**
- Đặt ở class → tất cả method trong controller đều có prefix `/api`
- Nhất quán với các controller khác đang dùng `/api/messages`, `/auth/...`*/
}