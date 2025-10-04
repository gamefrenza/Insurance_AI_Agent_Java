# Makefile for Insurance AI Agent

.PHONY: help build test run docker-build docker-run docker-compose-up docker-compose-down k8s-deploy k8s-delete clean

# Default target
.DEFAULT_GOAL := help

# Variables
IMAGE_NAME := insurance-agent
IMAGE_TAG := latest
REGISTRY := 
NAMESPACE := insurance-agent

help: ## Show this help message
	@echo "Insurance AI Agent - Make Commands"
	@echo ""
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

build: ## Build the application with Maven
	@echo "Building application..."
	mvn clean package -DskipTests

test: ## Run all tests
	@echo "Running tests..."
	mvn test

run: ## Run the application locally
	@echo "Starting application..."
	mvn spring-boot:run

docker-build: ## Build Docker image
	@echo "Building Docker image..."
	docker build -t $(IMAGE_NAME):$(IMAGE_TAG) .

docker-run: ## Run Docker container
	@echo "Running Docker container..."
	docker run -d \
		--name $(IMAGE_NAME) \
		-p 8080:8080 \
		-e INSURANCE_SECURITY_API_KEY=demo-api-key \
		-e AES_SECRET_KEY=YourSecretKey32CharactersLong! \
		$(IMAGE_NAME):$(IMAGE_TAG)

docker-stop: ## Stop Docker container
	@echo "Stopping Docker container..."
	docker stop $(IMAGE_NAME) || true
	docker rm $(IMAGE_NAME) || true

docker-compose-up: ## Start with Docker Compose
	@echo "Starting with Docker Compose..."
	docker-compose up -d

docker-compose-down: ## Stop Docker Compose
	@echo "Stopping Docker Compose..."
	docker-compose down

docker-compose-logs: ## View Docker Compose logs
	docker-compose logs -f

k8s-deploy: ## Deploy to Kubernetes
	@echo "Deploying to Kubernetes..."
	kubectl apply -f k8s-deployment.yml

k8s-delete: ## Delete from Kubernetes
	@echo "Deleting from Kubernetes..."
	kubectl delete -f k8s-deployment.yml

k8s-status: ## Check Kubernetes status
	@echo "Checking Kubernetes status..."
	kubectl get all -n $(NAMESPACE)

k8s-logs: ## View Kubernetes logs
	kubectl logs -f deployment/insurance-agent -n $(NAMESPACE)

clean: ## Clean build artifacts
	@echo "Cleaning..."
	mvn clean
	docker-compose down -v || true
	rm -rf target/ logs/

setup-templates: ## Create template directories
	@echo "Creating template directories..."
	mkdir -p templates output/documents

push: ## Push Docker image to registry
	@echo "Pushing to registry..."
	docker tag $(IMAGE_NAME):$(IMAGE_TAG) $(REGISTRY)/$(IMAGE_NAME):$(IMAGE_TAG)
	docker push $(REGISTRY)/$(IMAGE_NAME):$(IMAGE_TAG)

all: clean build docker-build ## Clean, build, and create Docker image

health: ## Check application health
	@echo "Checking health..."
	@curl -s http://localhost:8080/api/v1/insurance/health | json_pp || echo "Application not running"

test-api: ## Run API tests
	@echo "Running API tests..."
	@if [ -f test-integrated-api.ps1 ]; then \
		powershell -ExecutionPolicy Bypass -File test-integrated-api.ps1; \
	elif [ -f test-integrated-api.sh ]; then \
		./test-integrated-api.sh; \
	else \
		echo "No test script found"; \
	fi

deploy: build docker-build docker-compose-up ## Build, dockerize, and deploy locally

full-deploy: clean build test docker-build docker-compose-up health ## Complete deployment pipeline
	@echo "Full deployment complete!"

# Development shortcuts
dev: run ## Alias for run

dev-docker: docker-compose-up docker-compose-logs ## Run in Docker and follow logs

# Production deployment
prod-k8s: build docker-build push k8s-deploy ## Build and deploy to Kubernetes
	@echo "Production deployment to Kubernetes complete!"

