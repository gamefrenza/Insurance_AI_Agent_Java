# PowerShell script to run Insurance AI Agent

Write-Host "===== Insurance AI Agent Startup =====" -ForegroundColor Cyan
Write-Host ""

# Check if Java is installed
Write-Host "Checking Java installation..." -ForegroundColor Yellow
try {
    $javaVersion = java -version 2>&1 | Select-Object -First 1
    Write-Host "✓ Java found: $javaVersion" -ForegroundColor Green
} catch {
    Write-Host "✗ Java not found. Please install Java 17 or higher." -ForegroundColor Red
    exit 1
}
Write-Host ""

# Check if Maven is installed
Write-Host "Checking Maven installation..." -ForegroundColor Yellow
try {
    $mvnVersion = mvn -version 2>&1 | Select-Object -First 1
    Write-Host "✓ Maven found: $mvnVersion" -ForegroundColor Green
} catch {
    Write-Host "✗ Maven not found. Please install Maven 3.6 or higher." -ForegroundColor Red
    exit 1
}
Write-Host ""

# Set environment variables if not already set
if (-not $env:OPENAI_API_KEY) {
    Write-Host "Warning: OPENAI_API_KEY not set. AI features will use fallback behavior." -ForegroundColor Yellow
    Write-Host "To set it, run: `$env:OPENAI_API_KEY='your-api-key-here'" -ForegroundColor Yellow
    Write-Host ""
}

if (-not $env:AES_SECRET_KEY) {
    Write-Host "Setting default AES_SECRET_KEY..." -ForegroundColor Yellow
    $env:AES_SECRET_KEY = "MySecretKey12345MySecretKey12345"
    Write-Host ""
}

# Build and run the application
Write-Host "Building and starting application..." -ForegroundColor Yellow
Write-Host ""

mvn spring-boot:run

