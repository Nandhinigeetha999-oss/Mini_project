Write-Host "========================================================" -ForegroundColor Cyan
Write-Host " Starting Smart Road Maintenance System (SENTHAMIZH NAGAR)" -ForegroundColor Green
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "Frontend & Road API: http://localhost:8080/`n" -ForegroundColor Yellow

java -cp "classes;lib\sqlite-jdbc-3.46.1.3.jar" Main
