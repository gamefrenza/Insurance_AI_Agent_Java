# 🚀 Insurance AI Agent - Quick Start Guide

Get up and running in 5 minutes!

---

## ⚡ Fastest Way to Start

### Option 1: Using Docker Compose (Recommended)

```bash
# 1. Clone and navigate
git clone <repository-url>
cd insurance-ai-agent

# 2. Create template directory
mkdir -p templates output/documents

# 3. Start application
docker-compose up -d

# 4. Check health
curl http://localhost:8080/api/v1/insurance/health
```

**That's it!** The application is now running on `http://localhost:8080`

---

## 🎯 Alternative Methods

### Option 2: Using Maven

```bash
# 1. Build
mvn clean package

# 2. Run
mvn spring-boot:run
```

### Option 3: Using JAR

```bash
# 1. Build
mvn clean package

# 2. Run
java -jar target/insurance-agent-1.0.0.jar
```

### Option 4: Using Makefile

```bash
# Build and run
make deploy

# Or just run
make run
```

---

## 🧪 Test the Application

### 1. Health Check

```bash
curl http://localhost:8080/api/v1/insurance/health
```

**Expected Response:**
```json
{
  "status": "UP",
  "timestamp": "2024-10-04T12:00:00",
  "service": "Insurance AI Agent",
  "version": "1.0.0"
}
```

### 2. Process Insurance Request

```bash
curl -X POST http://localhost:8080/api/v1/insurance/process \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your-api-key-here" \
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

### 3. Run Test Scripts

**Windows:**
```powershell
.\test-integrated-api.ps1
```

**Linux/Mac:**
```bash
chmod +x test-integrated-api.sh
./test-integrated-api.sh
```

---

## 🔑 Configuration

### Set API Key

**Option 1: Environment Variable**

```bash
# Windows PowerShell
$env:API_KEY="your-secure-api-key"

# Linux/Mac
export API_KEY="your-secure-api-key"
```

**Option 2: In application.yml**

```yaml
insurance:
  security:
    api-key: your-secure-api-key
    api-key-enabled: true
```

**Option 3: Disable API Key (Development Only)**

```yaml
insurance:
  security:
    api-key-enabled: false
```

---

## 📦 Directory Structure Setup

```bash
# Create required directories
mkdir -p templates
mkdir -p output/documents

# Using Makefile
make setup-templates
```

---

## 🐳 Docker Commands

```bash
# Start
docker-compose up -d

# View logs
docker-compose logs -f

# Stop
docker-compose down

# Restart
docker-compose restart

# View status
docker-compose ps
```

---

## ☸️ Kubernetes Quick Deploy

```bash
# 1. Deploy
kubectl apply -f k8s-deployment.yml

# 2. Check status
kubectl get pods -n insurance-agent

# 3. Access
kubectl port-forward svc/insurance-agent-service 8080:80 -n insurance-agent

# Or use Makefile
make k8s-deploy
make k8s-status
```

---

## 🔧 Common Issues

### Port 8080 Already in Use

**Solution:**

```bash
# Find process
lsof -i :8080  # Mac/Linux
netstat -ano | findstr :8080  # Windows

# Change port in application.yml
server:
  port: 8081
```

### API Key Error

**Error:** `{"error": "API Key required"}`

**Solution:**
1. Disable API key for development:
   ```yaml
   insurance:
     security:
       api-key-enabled: false
   ```

2. Or provide valid API key:
   ```bash
   curl -H "X-API-Key: your-api-key" ...
   ```

### Docker Build Fails

**Solution:**

```bash
# Clean Docker cache
docker system prune -a

# Rebuild without cache
docker build --no-cache -t insurance-agent .
```

---

## 📚 Next Steps

1. **Explore APIs**
   - Read [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md)
   - Check [INTEGRATION_COMPLETE.md](INTEGRATION_COMPLETE.md)

2. **Configure Services**
   - Update `application.yml`
   - Set environment variables

3. **Deploy to Production**
   - Follow [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md)
   - Use Kubernetes or AWS ECS

4. **Run Tests**
   ```bash
   mvn test
   .\test-integrated-api.ps1
   ```

---

## 💡 Quick Commands Reference

| Task | Command |
|------|---------|
| Build | `mvn clean package` |
| Run | `mvn spring-boot:run` |
| Test | `mvn test` |
| Docker Start | `docker-compose up -d` |
| Docker Stop | `docker-compose down` |
| Health Check | `curl http://localhost:8080/api/v1/insurance/health` |
| K8s Deploy | `kubectl apply -f k8s-deployment.yml` |
| Using Make | `make deploy` |

---

## 🎉 You're Ready!

Your Insurance AI Agent is now running!

- **API Documentation**: Check the `/api/v1/insurance` endpoints
- **Health Check**: `http://localhost:8080/api/v1/insurance/health`
- **Full Docs**: See [README.md](README.md) and [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md)

**Need Help?** Check [COMPLETE_PROJECT_SUMMARY.md](COMPLETE_PROJECT_SUMMARY.md) or raise an issue.

---

**Happy Insuring! 🎊**

