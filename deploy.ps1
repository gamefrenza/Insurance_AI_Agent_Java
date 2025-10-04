# PowerShell Deployment Script for Insurance AI Agent

param(
    [string]$ImageTag = "latest",
    [string]$Registry = ""
)

$ErrorActionPreference = "Stop"

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Insurance AI Agent - Deployment Script" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

$ImageName = "insurance-agent"

# Check prerequisites
Write-Host "Checking prerequisites..." -ForegroundColor Yellow

try {
    $null = java -version 2>&1
    Write-Host "✓ Java found" -ForegroundColor Green
} catch {
    Write-Host "✗ Java not found. Please install Java 17+" -ForegroundColor Red
    exit 1
}

try {
    $null = mvn -version 2>&1
    Write-Host "✓ Maven found" -ForegroundColor Green
} catch {
    Write-Host "✗ Maven not found. Please install Maven 3.6+" -ForegroundColor Red
    exit 1
}

try {
    $null = docker --version 2>&1
    Write-Host "✓ Docker found" -ForegroundColor Green
} catch {
    Write-Host "✗ Docker not found. Please install Docker" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Building application..." -ForegroundColor Yellow

# Clean and build
mvn clean package -DskipTests
if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ Application built successfully" -ForegroundColor Green
} else {
    Write-Host "✗ Build failed" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Building Docker image..." -ForegroundColor Yellow

# Build Docker image
docker build -t "${ImageName}:${ImageTag}" .
if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ Docker image built: ${ImageName}:${ImageTag}" -ForegroundColor Green
} else {
    Write-Host "✗ Docker build failed" -ForegroundColor Red
    exit 1
}

# Tag and push if registry is specified
if ($Registry) {
    Write-Host ""
    Write-Host "Tagging image for registry..." -ForegroundColor Yellow
    docker tag "${ImageName}:${ImageTag}" "${Registry}/${ImageName}:${ImageTag}"
    
    Write-Host "Pushing to registry..." -ForegroundColor Yellow
    docker push "${Registry}/${ImageName}:${ImageTag}"
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ Image pushed to registry: ${Registry}/${ImageName}:${ImageTag}" -ForegroundColor Green
    } else {
        Write-Host "✗ Push to registry failed" -ForegroundColor Red
        exit 1
    }
}

Write-Host ""
Write-Host "✓ Deployment preparation complete!" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Cyan
Write-Host "  Local:       docker-compose up -d" -ForegroundColor White
Write-Host "  Kubernetes:  kubectl apply -f k8s-deployment.yml" -ForegroundColor White
Write-Host "  AWS ECS:     aws ecs register-task-definition --cli-input-json file://k8s-aws-ecs.yml" -ForegroundColor White
Write-Host ""

