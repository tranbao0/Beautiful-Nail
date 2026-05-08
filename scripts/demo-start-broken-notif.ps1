# demo-start-broken-notif.ps1 — start the app with a dead notification URL
# Bookings succeed; notifications fail and queue up. Shows graceful degradation.

$ROOT      = Resolve-Path "$PSScriptRoot\.."
$MVN       = "C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.2\plugins\maven\lib\maven3\bin\mvn.cmd"
$JAVA_HOME = "C:\Program Files\Java\jdk-17"

Write-Host ""
Write-Host "=== Starting Beautiful Nail (BROKEN NOTIFICATION MODE) ===" -ForegroundColor Cyan
Write-Host "Notification service: OFFLINE (dead port 9999)" -ForegroundColor Red
Write-Host "Health:   http://localhost:8080/health" -ForegroundColor Yellow
Write-Host "Failures: http://localhost:8080/metrics/bookings/failed" -ForegroundColor Yellow
Write-Host ""

Set-Location $ROOT
$env:JAVA_HOME = $JAVA_HOME
& $MVN spring-boot:run "-Dspring-boot.run.jvmArguments=-Dnotification.service.url=http://localhost:9999/api/external/notifications"
