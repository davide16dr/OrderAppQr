#!/bin/bash
# ============================================
# OrderApp - Deployment Helper Script
# ============================================

set -e

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}🚀 OrderApp Deployment Helper${NC}"
echo "=============================="
echo ""

# Menu
echo "Select deployment target:"
echo "1) Local Development (Docker)"
echo "2) Staging (Render + Vercel)"
echo "3) Production (Render + Vercel)"
echo "4) Pre-deployment checks"
echo ""

read -p "Enter choice [1-4]: " choice

case $choice in
    1)
        echo -e "${BLUE}Setting up local development...${NC}"
        chmod +x setup-local.sh
        ./setup-local.sh
        ;;
    2)
        echo -e "${BLUE}Preparing for staging deployment...${NC}"
        echo ""
        echo -e "${YELLOW}Steps to deploy to staging:${NC}"
        echo ""
        echo "1️⃣  Backend (Render):"
        echo "   - Go to https://render.com/dashboard"
        echo "   - New Web Service"
        echo "   - Connect GitHub repo"
        echo "   - Add render.yaml configuration"
        echo ""
        echo "2️⃣  Frontend (Vercel):"
        echo "   - Go to https://vercel.com/dashboard"
        echo "   - Import project"
        echo "   - Root: ordering-frontend"
        echo "   - Add ANGULAR_APP_API_URL environment variable"
        echo ""
        echo "See DEPLOYMENT_GUIDE.md for detailed instructions"
        ;;
    3)
        echo -e "${BLUE}Preparing for production deployment...${NC}"
        echo ""
        echo -e "${RED}⚠️  Production Checklist:${NC}"
        echo "   - [ ] JWT Secret is unique and secure"
        echo "   - [ ] Database backups enabled"
        echo "   - [ ] CORS restricted to your domain only"
        echo "   - [ ] Swagger UI disabled in prod"
        echo "   - [ ] SSL certificates configured"
        echo "   - [ ] Monitoring/alerting setup"
        echo "   - [ ] Uptime SLA in place (Render Pro)"
        echo ""
        read -p "All checks completed? (yes/no): " confirm
        if [ "$confirm" = "yes" ]; then
            echo -e "${GREEN}✓ Ready for production${NC}"
            echo "See DEPLOYMENT_GUIDE.md for detailed steps"
        else
            echo -e "${YELLOW}Aborted. Please complete all checks.${NC}"
        fi
        ;;
    4)
        echo -e "${BLUE}Running pre-deployment checks...${NC}"
        echo ""
        
        # Check Java
        if command -v java &> /dev/null; then
            JAVA_VERSION=$(java -version 2>&1 | grep 'version' | awk -F'"' '{print $2}')
            echo -e "${GREEN}✓ Java:${NC} $JAVA_VERSION"
        else
            echo -e "${RED}✗ Java not found${NC}"
        fi
        
        # Check Maven
        if command -v mvn &> /dev/null; then
            MVN_VERSION=$(mvn --version | head -1)
            echo -e "${GREEN}✓ Maven:${NC} $MVN_VERSION"
        else
            echo -e "${RED}✗ Maven not found${NC}"
        fi
        
        # Check Node
        if command -v node &> /dev/null; then
            NODE_VERSION=$(node --version)
            echo -e "${GREEN}✓ Node.js:${NC} $NODE_VERSION"
        else
            echo -e "${RED}✗ Node.js not found${NC}"
        fi
        
        # Check Git
        if command -v git &> /dev/null; then
            GIT_VERSION=$(git --version)
            echo -e "${GREEN}✓ Git:${NC} $GIT_VERSION"
        else
            echo -e "${RED}✗ Git not found${NC}"
        fi
        
        # Check Docker
        if command -v docker &> /dev/null; then
            DOCKER_VERSION=$(docker --version)
            echo -e "${GREEN}✓ Docker:${NC} $DOCKER_VERSION"
        else
            echo -e "${RED}✗ Docker not found${NC}"
        fi
        
        echo ""
        echo -e "${BLUE}Build checks:${NC}"
        
        # Backend build check
        echo -n "Checking backend build... "
        if mvn -v -f ordering-system/pom.xml clean compile -q 2>/dev/null; then
            echo -e "${GREEN}✓${NC}"
        else
            echo -e "${RED}✗${NC}"
        fi
        
        # Frontend build check
        echo -n "Checking frontend build... "
        cd ordering-frontend
        if npm run build 2>/dev/null; then
            echo -e "${GREEN}✓${NC}"
        else
            echo -e "${RED}✗${NC}"
        fi
        cd ..
        
        echo ""
        echo -e "${GREEN}Pre-deployment checks completed${NC}"
        ;;
    *)
        echo -e "${RED}Invalid choice${NC}"
        exit 1
        ;;
esac
