#!/bin/bash

# Insurance AI Agent Deployment Script

set -e

echo "========================================="
echo "Insurance AI Agent - Deployment Script"
echo "========================================="
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
IMAGE_NAME="insurance-agent"
IMAGE_TAG="${1:-latest}"
REGISTRY="${2:-}"

# Functions
function print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

function print_error() {
    echo -e "${RED}✗ $1${NC}"
}

function print_info() {
    echo -e "${YELLOW}→ $1${NC}"
}

# Check prerequisites
print_info "Checking prerequisites..."

if ! command -v java &> /dev/null; then
    print_error "Java not found. Please install Java 17+"
    exit 1
fi
print_success "Java found"

if ! command -v mvn &> /dev/null; then
    print_error "Maven not found. Please install Maven 3.6+"
    exit 1
fi
print_success "Maven found"

if ! command -v docker &> /dev/null; then
    print_error "Docker not found. Please install Docker"
    exit 1
fi
print_success "Docker found"

echo ""
print_info "Building application..."

# Clean and build
mvn clean package -DskipTests
if [ $? -eq 0 ]; then
    print_success "Application built successfully"
else
    print_error "Build failed"
    exit 1
fi

echo ""
print_info "Building Docker image..."

# Build Docker image
docker build -t $IMAGE_NAME:$IMAGE_TAG .
if [ $? -eq 0 ]; then
    print_success "Docker image built: $IMAGE_NAME:$IMAGE_TAG"
else
    print_error "Docker build failed"
    exit 1
fi

# Tag and push if registry is specified
if [ ! -z "$REGISTRY" ]; then
    echo ""
    print_info "Tagging image for registry..."
    docker tag $IMAGE_NAME:$IMAGE_TAG $REGISTRY/$IMAGE_NAME:$IMAGE_TAG
    
    print_info "Pushing to registry..."
    docker push $REGISTRY/$IMAGE_NAME:$IMAGE_TAG
    
    if [ $? -eq 0 ]; then
        print_success "Image pushed to registry: $REGISTRY/$IMAGE_NAME:$IMAGE_TAG"
    else
        print_error "Push to registry failed"
        exit 1
    fi
fi

echo ""
print_success "Deployment preparation complete!"
echo ""
echo "Next steps:"
echo "  Local:       docker-compose up -d"
echo "  Kubernetes:  kubectl apply -f k8s-deployment.yml"
echo "  AWS ECS:     aws ecs register-task-definition --cli-input-json file://k8s-aws-ecs.yml"
echo ""

