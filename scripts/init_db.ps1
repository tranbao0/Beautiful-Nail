# Run all migrations then seed data against the SQLite DB
$DB = "db\beautiful_nail.db"

Write-Host "Running migrations..."
Get-ChildItem "db\migrations\V*.sql" | Sort-Object Name | ForEach-Object {
    Write-Host "  Applying $($_.Name)"
    Get-Content $_.FullName | sqlite3 $DB
}

Write-Host "Seeding data..."
Get-Content "db\seeds\seed_data.sql" | sqlite3 $DB

Write-Host "Done. DB is at $DB"
