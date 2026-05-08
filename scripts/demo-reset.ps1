# demo-reset.ps1 — wipe and reseed the database for a clean demo run
# Run from anywhere; paths resolve relative to the project root.

$ROOT = Resolve-Path "$PSScriptRoot\.."
$DB   = "$ROOT\db\beautiful_nail.db"

Write-Host ""
Write-Host "=== Beautiful Nail Demo Reset ===" -ForegroundColor Cyan

if (Test-Path $DB) {
    Remove-Item $DB -Force
    Write-Host "Removed existing database." -ForegroundColor Yellow
}

Write-Host "Running migrations..." -ForegroundColor Cyan
Get-ChildItem "$ROOT\db\migrations\V*.sql" | Sort-Object Name | ForEach-Object {
    Write-Host "  $($_.Name)"
    Get-Content $_.FullName | sqlite3 $DB
}

Write-Host "Seeding data..." -ForegroundColor Cyan
Get-Content "$ROOT\db\seeds\seed_data.sql" | sqlite3 $DB

Write-Host ""
Write-Host "Done. Database is clean and seeded." -ForegroundColor Green
Write-Host "Run .\demo-start.ps1 to start the app." -ForegroundColor White
Write-Host ""
