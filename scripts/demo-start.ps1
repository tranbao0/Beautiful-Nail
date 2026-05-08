# demo-start.ps1 — start the app in normal demo mode
# Notification service is live. Use for Part 1 (booking) and Part 3 (boundary).

$ROOT     = Resolve-Path "$PSScriptRoot\.."
$MVN      = "C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.2\plugins\maven\lib\maven3\bin\mvn.cmd"
$JAVA_HOME = "C:\Program Files\Java\jdk-17"

Write-Host ""
Write-Host "=== Starting Beautiful Nail (DEMO MODE) ===" -ForegroundColor Cyan
Write-Host "Notification log: http://localhost:8080/api/external/notifications/log" -ForegroundColor Green
Write-Host ""

Set-Location $ROOT
$env:JAVA_HOME = $JAVA_HOME
& $MVN spring-boot:run
