# Register trước (ignore lỗi nếu đã tồn tại)
try {
    Invoke-RestMethod -Uri "http://localhost:8080/auth/register" -Method POST -ContentType "application/json" -Body '{"username":"kim","password":"123456"}'
} catch {}

# Login lấy token
$response = Invoke-RestMethod -Uri "http://localhost:8080/auth/login" -Method POST -ContentType "application/json" -Body '{"username":"kim","password":"123456"}'
$token = $response.token
Write-Host "Token: $token"

# Upload
$boundary = [System.Guid]::NewGuid().ToString()
$filePath = "C:\Users\ADMIN\KNTT\Project\chat-app\truong.jpg"
$fileBytes = [System.IO.File]::ReadAllBytes($filePath)
$body = "--$boundary`r`nContent-Disposition: form-data; name=`"file`"; filename=`"truong.jpg`"`r`nContent-Type: image/jpeg`r`n`r`n"
$bodyBytes = [System.Text.Encoding]::UTF8.GetBytes($body)
$endBytes = [System.Text.Encoding]::UTF8.GetBytes("`r`n--$boundary--`r`n")
$fullBody = $bodyBytes + $fileBytes + $endBytes
Invoke-RestMethod -Uri "http://localhost:8080/api/upload" -Method POST -Headers @{Authorization = "Bearer $token"} -ContentType "multipart/form-data; boundary=$boundary" -Body $fullBody