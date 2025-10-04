# PowerShell script to create sample PDF templates for testing

Write-Host "===== Creating PDF Template Directories =====" -ForegroundColor Cyan
Write-Host ""

# Create directories
$templateDir = ".\templates"
$outputDir = ".\output\documents"

if (!(Test-Path $templateDir)) {
    New-Item -ItemType Directory -Path $templateDir | Out-Null
    Write-Host "✓ Created templates directory: $templateDir" -ForegroundColor Green
} else {
    Write-Host "✓ Templates directory already exists" -ForegroundColor Yellow
}

if (!(Test-Path $outputDir)) {
    New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
    Write-Host "✓ Created output directory: $outputDir" -ForegroundColor Green
} else {
    Write-Host "✓ Output directory already exists" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "===== Setup Complete =====" -ForegroundColor Cyan
Write-Host ""
Write-Host "Next Steps:" -ForegroundColor Yellow
Write-Host "1. Place your PDF templates in: $templateDir" -ForegroundColor White
Write-Host "   Template naming convention:" -ForegroundColor White
Write-Host "     - auto_policy_template.pdf" -ForegroundColor Gray
Write-Host "     - home_application_template.pdf" -ForegroundColor Gray
Write-Host "     - life_quote_template.pdf" -ForegroundColor Gray
Write-Host "     - health_claim_template.pdf" -ForegroundColor Gray
Write-Host ""
Write-Host "2. Or use templates without form fields (system will add text directly)" -ForegroundColor White
Write-Host ""
Write-Host "3. Run the application:" -ForegroundColor White
Write-Host "   .\run.ps1" -ForegroundColor Gray
Write-Host ""
Write-Host "4. Test the document service:" -ForegroundColor White
Write-Host "   .\test-document-api.ps1" -ForegroundColor Gray
Write-Host ""

