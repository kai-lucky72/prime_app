#!/bin/bash

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}Prime App Setup Script${NC}"
echo "=============================="

# Check Java version
echo -e "\n${YELLOW}Checking Java version...${NC}"
if ! command -v java &> /dev/null; then
    echo -e "${RED}Java not found. Please install Java 17 or higher${NC}"
    exit 1
fi

java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | awk -F. '{print $1}')
if [ "$java_version" -lt 17 ]; then
    echo -e "${RED}Java version must be 17 or higher. Current version: $java_version${NC}"
    exit 1
fi
echo -e "${GREEN}Java version check passed${NC}"

# Check Maven
echo -e "\n${YELLOW}Checking Maven...${NC}"
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}Maven not found. Please install Maven 3.8 or higher${NC}"
    exit 1
fi
echo -e "${GREEN}Maven check passed${NC}"

# Check MySQL
echo -e "\n${YELLOW}Checking MySQL...${NC}"
if ! command -v mysql &> /dev/null; then
    echo -e "${RED}MySQL not found. Please install MySQL 8.0 or higher${NC}"
    exit 1
fi
echo -e "${GREEN}MySQL check passed${NC}"

# Create .env file if it doesn't exist
echo -e "\n${YELLOW}Setting up environment variables...${NC}"
if [ ! -f .env ]; then
    cp .env.example .env
    echo -e "${GREEN}Created .env file from template${NC}"
    
    # Generate secure JWT secret
    jwt_secret=$(openssl rand -hex 32)
    sed -i "s/your_secure_jwt_secret_key_min_64_characters_long/$jwt_secret/" .env
    
    # Generate random database password
    db_password=$(openssl rand -base64 12)
    sed -i "s/your_secure_password/$db_password/" .env
    
    echo -e "${YELLOW}Please update the following in your .env file:${NC}"
    echo "1. DB_USERNAME (if different from 'root')"
    echo "2. DB_HOST (if different from 'localhost')"
    echo "3. REDIS_HOST and REDIS_PASSWORD (if using Redis)"
    echo "4. CORS_ALLOWED_ORIGINS (for production)"
else
    echo -e "${YELLOW}.env file already exists. Skipping...${NC}"
fi

# Create database if it doesn't exist
echo -e "\n${YELLOW}Setting up database...${NC}"
if mysql -u root -e "CREATE DATABASE IF NOT EXISTS prime_app_db;"; then
    echo -e "${GREEN}Database setup complete${NC}"
else
    echo -e "${RED}Failed to create database. Please check MySQL credentials${NC}"
    exit 1
fi

# Build the project
echo -e "\n${YELLOW}Building project...${NC}"
if mvn clean install; then
    echo -e "${GREEN}Build successful${NC}"
else
    echo -e "${RED}Build failed. Please check the errors above${NC}"
    exit 1
fi

echo -e "\n${GREEN}Setup complete!${NC}"
echo -e "To run the application:"
echo -e "1. Update the .env file with your configurations"
echo -e "2. Run: ${YELLOW}mvn spring-boot:run${NC}"
echo -e "3. Access the API at: ${YELLOW}http://localhost:8080/api/v1${NC}"
echo -e "4. Swagger UI at: ${YELLOW}http://localhost:8080/api/v1/swagger-ui.html${NC}"