# Insurance AI Agent - Deployment Guide

## 📦 Complete Deployment Guide

This guide covers deploying the Insurance AI Agent application using Docker, Kubernetes, and AWS ECS.

---

## 🚀 Quick Start (Local Development)

### Prerequisites
- Java 17+
- Maven 3.6+
- Docker (optional)
- Docker Compose (optional)

### 1. Build the Application

```bash
# Clean and package
mvn clean package -DskipTests

# Or with tests
mvn clean package
```

### 2. Run Locally

```bash
# Using Maven
mvn spring-boot:run

# Or using JAR
java -jar target/insurance-agent-1.0.0.jar
```

### 3. Test the Application

```bash
# Health check
curl http://localhost:8080/api/v1/insurance/health

# Test with API (replace with your API key)
curl -X POST http://localhost:8080/api/v1/insurance/process \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your-api-key-here" \
  -d @example-request.json
```

---

## 🐳 Docker Deployment

### Build Docker Image

```bash
# Build the image
docker build -t insurance-agent:latest .

# Tag for registry
docker tag insurance-agent:latest your-registry/insurance-agent:1.0.0
```

### Run with Docker

```bash
docker run -d \
  --name insurance-agent \
  -p 8080:8080 \
  -e INSURANCE_SECURITY_API_KEY=your-api-key \
  -e AES_SECRET_KEY=YourSecretKey32CharactersLong! \
  -e OPENAI_API_KEY=your-openai-key \
  -v $(pwd)/templates:/app/templates:ro \
  -v $(pwd)/output:/app/output \
  insurance-agent:latest
```

### Run with Docker Compose

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

### Docker Compose Configuration

Create `.env` file:

```bash
# API Keys
API_KEY=your-secure-api-key-here
AES_SECRET_KEY=YourSecretKey32CharactersLong!
OPENAI_API_KEY=your-openai-key

# External APIs
GUIDEWIRE_API_KEY=your-guidewire-key
CREDIT_SCORE_API_KEY=your-experian-key
DOCUSIGN_API_KEY=your-docusign-key
```

---

## ☸️ Kubernetes Deployment

### Prerequisites
- Kubernetes cluster (1.20+)
- kubectl configured
- Docker registry access

### 1. Build and Push Image

```bash
# Build
docker build -t your-registry/insurance-agent:1.0.0 .

# Push to registry
docker push your-registry/insurance-agent:1.0.0
```

### 2. Create Secrets

```bash
# Create namespace
kubectl create namespace insurance-agent

# Create secrets
kubectl create secret generic insurance-agent-secrets \
  --from-literal=api-key=your-api-key \
  --from-literal=aes-secret-key=YourSecretKey32CharactersLong! \
  --from-literal=openai-api-key=your-openai-key \
  --from-literal=guidewire-api-key=your-guidewire-key \
  --from-literal=credit-score-api-key=your-experian-key \
  --from-literal=docusign-api-key=your-docusign-key \
  -n insurance-agent
```

### 3. Deploy Application

```bash
# Apply deployment
kubectl apply -f k8s-deployment.yml

# Check status
kubectl get pods -n insurance-agent
kubectl get svc -n insurance-agent

# View logs
kubectl logs -f deployment/insurance-agent -n insurance-agent
```

### 4. Access Application

```bash
# Get service URL
kubectl get svc insurance-agent-service -n insurance-agent

# Port forward (for testing)
kubectl port-forward svc/insurance-agent-service 8080:80 -n insurance-agent
```

### 5. Scale Application

```bash
# Manual scaling
kubectl scale deployment insurance-agent --replicas=5 -n insurance-agent

# HPA (Horizontal Pod Autoscaler) is automatically configured
# View HPA status
kubectl get hpa -n insurance-agent
```

---

## ☁️ AWS ECS Deployment

### Prerequisites
- AWS CLI configured
- ECR repository created
- ECS cluster created
- IAM roles configured

### 1. Push to AWS ECR

```bash
# Login to ECR
aws ecr get-login-password --region YOUR_REGION | docker login --username AWS --password-stdin YOUR_ACCOUNT_ID.dkr.ecr.YOUR_REGION.amazonaws.com

# Tag image
docker tag insurance-agent:latest YOUR_ACCOUNT_ID.dkr.ecr.YOUR_REGION.amazonaws.com/insurance-agent:latest

# Push to ECR
docker push YOUR_ACCOUNT_ID.dkr.ecr.YOUR_REGION.amazonaws.com/insurance-agent:latest
```

### 2. Create Secrets in AWS Secrets Manager

```bash
# API Key
aws secretsmanager create-secret \
  --name insurance/api-key \
  --secret-string "your-api-key" \
  --region YOUR_REGION

# AES Key
aws secretsmanager create-secret \
  --name insurance/aes-key \
  --secret-string "YourSecretKey32CharactersLong!" \
  --region YOUR_REGION

# OpenAI Key
aws secretsmanager create-secret \
  --name insurance/openai-key \
  --secret-string "your-openai-key" \
  --region YOUR_REGION
```

### 3. Register Task Definition

```bash
# Update k8s-aws-ecs.yml with your values
# Register task definition
aws ecs register-task-definition --cli-input-json file://k8s-aws-ecs.yml --region YOUR_REGION
```

### 4. Create ECS Service

```bash
# Create service
aws ecs create-service \
  --cluster insurance-cluster \
  --service-name insurance-agent-service \
  --task-definition insurance-agent-task \
  --desired-count 2 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={subnets=[subnet-xxx],securityGroups=[sg-xxx],assignPublicIp=ENABLED}" \
  --region YOUR_REGION
```

### 5. Configure Load Balancer

```bash
# Create Application Load Balancer
aws elbv2 create-load-balancer \
  --name insurance-agent-alb \
  --subnets subnet-xxx subnet-yyy \
  --security-groups sg-xxx \
  --region YOUR_REGION

# Create target group
aws elbv2 create-target-group \
  --name insurance-agent-tg \
  --protocol HTTP \
  --port 8080 \
  --vpc-id vpc-xxx \
  --target-type ip \
  --health-check-path /api/v1/insurance/health \
  --region YOUR_REGION
```

---

## 🔐 Security Configuration

### API Key Authentication

#### Generate Secure API Key

```bash
# Generate random API key
openssl rand -base64 32
```

#### Configure API Key

```yaml
# application.yml
insurance:
  security:
    api-key: ${API_KEY}
    api-key-enabled: true
```

#### Use API Key

```bash
curl -X POST http://localhost:8080/api/v1/insurance/process \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your-api-key-here" \
  -d @request.json
```

### TLS/SSL Configuration

#### For Kubernetes (with cert-manager)

```bash
# Install cert-manager
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.0/cert-manager.yaml

# Create ClusterIssuer
kubectl apply -f - <<EOF
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: your-email@example.com
    privateKeySecretRef:
      name: letsencrypt-prod
    solvers:
    - http01:
        ingress:
          class: nginx
EOF
```

#### For AWS ECS (with ACM)

```bash
# Request certificate
aws acm request-certificate \
  --domain-name insurance-agent.yourdomain.com \
  --validation-method DNS \
  --region YOUR_REGION
```

---

## 📊 Monitoring and Logging

### Health Checks

```bash
# Application health
curl http://localhost:8080/api/v1/insurance/health

# Kubernetes health
kubectl get pods -n insurance-agent

# Docker health
docker ps --filter name=insurance-agent
```

### Logs

#### Docker Logs

```bash
docker logs -f insurance-agent
```

#### Kubernetes Logs

```bash
kubectl logs -f deployment/insurance-agent -n insurance-agent
```

#### AWS CloudWatch

```bash
aws logs tail /ecs/insurance-agent --follow --region YOUR_REGION
```

### Metrics

#### Enable Spring Boot Actuator

Add to `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

Access metrics:

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/metrics
```

---

## 🔄 CI/CD Pipeline

### GitHub Actions Example

```yaml
name: Build and Deploy

on:
  push:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Build with Maven
      run: mvn clean package
    
    - name: Build Docker image
      run: docker build -t insurance-agent:${{ github.sha }} .
    
    - name: Push to Registry
      run: |
        echo "${{ secrets.DOCKER_PASSWORD }}" | docker login -u "${{ secrets.DOCKER_USERNAME }}" --password-stdin
        docker push insurance-agent:${{ github.sha }}
    
    - name: Deploy to Kubernetes
      run: |
        kubectl set image deployment/insurance-agent insurance-agent=insurance-agent:${{ github.sha }} -n insurance-agent
```

---

## 🧪 Testing Deployment

### Integration Tests

```bash
# Run all tests
mvn test

# Run integration tests only
mvn test -Dtest=IntegrationTest
```

### API Tests

```bash
# Health check
curl http://localhost:8080/api/v1/insurance/health

# Complete workflow
curl -X POST http://localhost:8080/api/v1/insurance/process \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your-api-key" \
  -d '{
    "customerId": "TEST-001",
    "customerName": "John Doe",
    "age": 30,
    "gender": "male",
    "address": "123 Main St, New York",
    "email": "john@example.com",
    "insuranceType": "auto",
    "vehicleModel": "Tesla Model 3",
    "creditScore": 750,
    "claimsHistory": 0
  }'
```

---

## 🔧 Troubleshooting

### Common Issues

#### Port Already in Use

```bash
# Find process using port 8080
lsof -i :8080

# Kill process
kill -9 <PID>
```

#### Docker Build Fails

```bash
# Clean Docker cache
docker system prune -a

# Rebuild without cache
docker build --no-cache -t insurance-agent:latest .
```

#### Kubernetes Pod Not Starting

```bash
# Describe pod
kubectl describe pod <pod-name> -n insurance-agent

# Check logs
kubectl logs <pod-name> -n insurance-agent

# Check events
kubectl get events -n insurance-agent --sort-by='.lastTimestamp'
```

#### Out of Memory

```bash
# Increase Java heap size
export JAVA_OPTS="-Xms1024m -Xmx2048m"

# Or in Dockerfile/K8s
ENV JAVA_OPTS="-Xms1024m -Xmx2048m"
```

---

## 📝 Production Checklist

- [ ] Change default API keys
- [ ] Configure TLS/SSL certificates
- [ ] Set up proper logging (CloudWatch, ELK, etc.)
- [ ] Configure monitoring (Prometheus, Datadog, etc.)
- [ ] Set up backup strategy
- [ ] Configure auto-scaling
- [ ] Enable health checks
- [ ] Set resource limits
- [ ] Configure secrets management
- [ ] Set up CI/CD pipeline
- [ ] Document runbooks
- [ ] Configure alerts
- [ ] Perform load testing
- [ ] Security audit

---

## 📚 Additional Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Docker Documentation](https://docs.docker.com/)
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [AWS ECS Documentation](https://docs.aws.amazon.com/ecs/)

---

**For questions or issues, see [README.md](README.md) or [COMPLETE_PROJECT_SUMMARY.md](COMPLETE_PROJECT_SUMMARY.md)**

