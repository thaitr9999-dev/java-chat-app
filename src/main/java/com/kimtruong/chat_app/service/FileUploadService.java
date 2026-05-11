package com.kimtruong.chat_app.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class FileUploadService {

    // Đọc từ application.properties: file.upload-dir=uploads
    @Value("${file.upload-dir}")
    private String uploadDir;

    // Chỉ cho phép 3 loại image
    private static final List<String> ALLOWED_TYPES = List.of(
        "image/jpeg", "image/png", "image/gif"
    );

    /**
     * Save an uploaded image file to the filesystem.
     * Validates content type, file extension, and sanitizes the filename.
     * Only JPEG, PNG, and GIF formats are allowed.
     *
     * @param file the multipart file to upload
     * @return the relative URL path to access the uploaded file
     * @throws IllegalArgumentException if file type or extension is invalid
     * @throws RuntimeException if an I/O error occurs during file save
     */
    public String save(MultipartFile file) {
        // Lớp 1: Validate content type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Chỉ chấp nhận file JPEG, PNG, GIF");
        }

        // Lớp 2: Validate extension từ tên file gốc
        String originalName = file.getOriginalFilename();
        if (originalName == null || !hasImageExtension(originalName)) {
            throw new IllegalArgumentException("Extension file không hợp lệ");
        }

        // Lớp 3: Sanitize — dùng UUID thay tên gốc, chống path traversal
        String extension = originalName.substring(originalName.lastIndexOf("."));
        String newFilename = UUID.randomUUID().toString() + extension;

        try {
            // Tạo thư mục uploads/ nếu chưa có — sử dụng đường dẫn tuyệt đối từ working directory
            String workingDir = System.getProperty("user.dir");
            Path uploadPath = Paths.get(workingDir, uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Lưu file
            Path filePath = uploadPath.resolve(newFilename);
            file.transferTo(filePath.toFile());

            return "/uploads/" + newFilename;

        } catch (IOException e) {
            throw new RuntimeException("Không thể lưu file: " + e.getMessage());
        }
    }

    /**
     * Check if the given filename has a valid image extension.
     *
     * @param filename the filename to check
     * @return true if the filename ends with a valid image extension (jpg, jpeg, png, gif), false otherwise
     */
    private boolean hasImageExtension(String filename) {
        String lower = filename.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg")
            || lower.endsWith(".png") || lower.endsWith(".gif");
    }


    /* Day 19 - bước này chỉ làm service, controller sẽ gọi vào đây để xử lý. Cách tách này giúp code sạch hơn, dễ test hơn, controller chỉ lo chuyện HTTP request/response, còn service lo logic lưu file. 
 
 
 
    **🎯 Nó làm gì?**

Nhận `MultipartFile` từ controller, chạy qua 3 lớp validation theo thứ tự, nếu pass hết thì sanitize tên file và lưu vào disk, trả về URL.

---

**1. Keyword lạ**

`@Value("${file.upload-dir}")` — đọc giá trị từ `application.properties` và inject vào field. `${...}` là Expression Language của Spring. Nếu key không tồn tại trong properties → app crash khi start, không phải khi gọi method.

`Path` / `Paths` / `Files` — Java NIO (New I/O), cách hiện đại để làm việc với file system. Khác với `java.io.File` cũ: xử lý path an toàn hơn, cross-platform (Windows dùng `\`, Linux dùng `/` — `Path` tự handle).

`Paths.get(uploadDir)` — tạo Path object từ string `"uploads"` → đại diện cho thư mục uploads trên disk.

`Files.createDirectories(uploadPath)` — tạo thư mục kể cả thư mục cha nếu chưa có. Khác với `mkdir()` chỉ tạo 1 cấp.

`uploadPath.resolve(newFilename)` — nối path an toàn. Tương đương `"uploads/" + newFilename` nhưng không bị lỗi separator trên Windows.

`file.transferTo(filePath.toFile())` — ghi bytes của `MultipartFile` ra file trên disk. Spring handle buffer internally.

`UUID.randomUUID()` — sinh chuỗi random 36 ký tự dạng `f47ac10b-58cc-4372-a567-0e02b2c3d479`, xác suất trùng gần như bằng 0.

---

**2. ⚙️ Nó chạy như nào?**

```
file = MultipartFile{name="avatar.jpg", type="image/jpeg", size=2MB}
        ↓
Lớp 1: "image/jpeg" có trong ALLOWED_TYPES? → pass
        ↓
Lớp 2: "avatar.jpg" có đuôi .jpg? → pass
        ↓
Lớp 3: tên gốc "avatar.jpg" bị bỏ hoàn toàn
        extension = ".jpg"
        newFilename = "f47ac10b-58cc-4372-a567-0e02b2c3d479.jpg"
        ↓
Thư mục "uploads/" chưa có → createDirectories tạo mới
        ↓
filePath = "uploads/f47ac10b-58cc-4372-a567-0e02b2c3d479.jpg"
        ↓
transferTo → ghi file ra disk
        ↓
return "/uploads/f47ac10b-58cc-4372-a567-0e02b2c3d479.jpg"

---

Nếu client gửi file độc hại:
name="../../etc/passwd", type="image/jpeg"
        ↓
Lớp 2: không có đuôi .jpg/.png/.gif → reject
        ↓
throw IllegalArgumentException — không bao giờ chạm đến disk
```

---

**3. 🔥 So sánh với cách khác**

**Tại sao cần 2 lớp validate (content type + extension)?**

Content type do client tự khai báo trong request header — có thể fake:
```
# Hacker gửi file .exe nhưng khai type là image/jpeg
Content-Type: image/jpeg
file: malware.exe
```
Extension từ tên file gốc là lớp 2 độc lập — fake được content type nhưng khó fake cả extension cùng lúc theo đúng format.

Production thực tế còn thêm lớp 3: đọc magic bytes (4 byte đầu của file) để xác định file type thật sự — không thể fake. Với portfolio intern thì 2 lớp là đủ.

**Dùng UUID thay vì giữ tên gốc:**
```
# Tên gốc giữ nguyên — có thể bị path traversal:
"../../../etc/passwd.jpg" → lưu ra ngoài thư mục uploads

# UUID — tên gốc bị bỏ hoàn toàn, chỉ giữ extension:
"f47ac10b.jpg" → luôn nằm trong uploads/
```

**`transferTo()` vs tự đọc `InputStream`:**
```java
// Cách thủ công — phải tự handle buffer, close stream
try (InputStream is = file.getInputStream()) {
    Files.copy(is, filePath);
}

// transferTo() — Spring handle hết, ngắn hơn, ít lỗi hơn
file.transferTo(filePath.toFile());
```
`transferTo()` là cách được recommend trong Spring, dùng cho portfolio là đủ. */
}