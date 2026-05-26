#!/bin/bash
# ============================================
# OrderApp - Local Development Setup Script
# ============================================

set -e

echo "🚀 OrderApp Development Setup"
echo "=============================="
echo ""

# Color codes
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Check if .env file exists
if [ ! -f .env ]; then
    echo -e "${YELLOW}⚠️  .env file not found. Creating from .env.example${NC}"
    cp .env.example .env
    echo -e "${GREEN}✓ .env created${NC}"
    echo -e "${BLUE}ℹ️  Edit .env with your local configuration${NC}"
fi

# Check Docker installation
if ! command -v docker &> /dev/null; then
    echo -e "${RED}✗ Docker is not installed${NC}"
    echo "Install from: https://www.docker.com/products/docker-desktop"
    exit 1
fi
echo -e "${GREEN}✓ Docker is installed${NC}"

# Check Docker Compose installation
if ! command -v docker-compose &> /dev/null; then
    echo -e "${RED}✗ Docker Compose is not installed${NC}"
    echo "Install from: https://docs.docker.com/compose/install"
    exit 1
fi
echo -e "${GREEN}✓ Docker Compose is installed${NC}"

# Generate JWT Secret if not in .env
if grep -q "JWT_SECRET=\$" .env || grep -q "JWT_SECRET=$" .env; then
    echo -e "${YELLOW}⚠️  Generating secure JWT Secret...${NC}"
    JWT_SECRET=$(openssl rand -base64 32)
    sed -i '' "s/JWT_SECRET=.*/JWT_SECRET=$JWT_SECRET/" .env
    echo -e "${GREEN}✓ JWT Secret generated${NC}"
fi

echo ""
echo -e "${BLUE}📦 Starting services with Docker Compose...${NC}"

# Build images
docker-compose build

# Start services
docker-compose up -d

echo ""
echo -e "${GREEN}✓ Services are starting${NC}"
echo ""

# Wait for services to be ready
echo -e "${BLUE}⏳ Waiting for services to be ready...${NC}"
sleep 15

# Check services
BACKEND_READY=false
POSTGRES_READY=false
REDIS_READY=false

for i in {1..30}; do
    if curl -s http://localhost:8080/api/health > /dev/null 2>&1; then
        BACKEND_READY=true
    fi
    
    if docker-compose exec -T postgres pg_isready -U orderapp_user -d ordering_db > /dev/null 2>&1; then
        POSTGRES_READY=true
    fi
    
    if docker-compose exec -T redis redis-cli ping > /dev/null 2>&1; then
        REDIS_READY=true
    fi
    
    if [ "$BACKEND_READY" = true ] && [ "$POSTGRES_READY" = true ] && [ "$REDIS_READY" = true ]; then
        break
    fi
    
    echo -e "${YELLOW}Attempt $i/30...${NC}"
    sleep 2
done

echo ""
echo -e "${GREEN}✓ Services started${NC}"
echo ""

# Display service status
echo -e "${BLUE}📊 Service Status:${NC}"
echo -e "${GREEN}✓ PostgreSQL:${NC} postgresql://orderapp_user:orderapp_password_dev@localhost:5432/ordering_db"
echo -e "${GREEN}✓ Redis:${NC} redis://localhost:6379"
echo -e "${GREEN}✓ Backend:${NC} http://localhost:8080"
echo -e "${GREEN}✓ Backend Health:${NC} http://localhost:8080/api/health"
echo -e "${GREEN}✓ API Docs:${NC} http://localhost:8080/swagger-ui.html"
echo -e "${GREEN}✓ Mailhog:${NC} http://localhost:8025"
echo ""

# Wait for Angular to be ready (takes longer)
echo -e "${BLUE}⏳ Building Angular frontend...${NC}"
for i in {1..120}; do
    if curl -s http://localhost:4200 > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Frontend is ready${NC}"
        break
    fi
    if [ $((i % 10)) -eq 0 ]; then
        echo -e "${YELLOW}Still building... ($i/120s)${NC}"
    fi
    sleep 1
done

echo ""
echo -e "${GREEN}✅ Development environment is ready!${NC}"
echo ""
echo -e "${BLUE}🌐 Open in your browser:${NC}"
echo -e "   Frontend:     ${BLUE}http://localhost:4200${NC}"
echo -e "   Backend:      ${BLUE}http://localhost:8080/api/health${NC}"
echo -e "   Swagger UI:   ${BLUE}http://localhost:8080/swagger-ui.html${NC}"
echo -e "   Mailhog:      ${BLUE}http://localhost:8025${NC}"
echo ""
echo -e "${BLUE}🛑 To stop services:${NC}"
echo -e "   ${YELLOW}docker-compose down${NC}"
echo ""
echo -e "${BLUE}📝 To view logs:${NC}"
echo -e "   ${YELLOW}docker-compose logs -f backend${NC}"
echo ""
