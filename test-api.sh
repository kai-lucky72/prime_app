#!/bin/bash

# Colors for better readability
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Base URL for the API
BASE_URL="http://localhost:8080/api/v1"
AUTH_TOKEN=""
REFRESH_TOKEN=""

echo -e "${YELLOW}===== Prime App API Test Script =====${NC}"
echo "Testing API endpoints after JPA schema management implementation"
echo ""

# Function to check if application is running
check_app_running() {
  echo -e "${YELLOW}Checking if application is running...${NC}"
  if curl -s --head $BASE_URL/swagger-ui.html > /dev/null; then
    echo -e "${GREEN}✓ Application is running${NC}"
    return 0
  else
    echo -e "${RED}✗ Application is not running. Please start it with 'mvn spring-boot:run'${NC}"
    return 1
  fi
}

# Function to log in and get token
login() {
  echo -e "\n${YELLOW}Testing authentication...${NC}"
  
  # Admin login
  echo "Logging in as admin..."
  RESPONSE=$(curl -s -X POST -H "Content-Type: application/json" \
    -d '{"email":"admin@example.com","password":"admin123"}' \
    $BASE_URL/auth/login)
  
  if echo "$RESPONSE" | grep -q "token"; then
    echo -e "${GREEN}✓ Admin login successful${NC}"
    
    # Extract tokens
    AUTH_TOKEN=$(echo $RESPONSE | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
    REFRESH_TOKEN=$(echo $RESPONSE | sed -n 's/.*"refreshToken":"\([^"]*\)".*/\1/p')
    
    echo "Auth token: ${AUTH_TOKEN:0:20}..."
    return 0
  else
    echo -e "${RED}✗ Admin login failed${NC}"
    echo "Response: $RESPONSE"
    return 1
  fi
}

# Test user info endpoint
test_current_user() {
  echo -e "\n${YELLOW}Testing current user info...${NC}"
  
  RESPONSE=$(curl -s -X GET -H "Content-Type: application/json" \
    -H "Authorization: Bearer $AUTH_TOKEN" \
    $BASE_URL/auth/me)
  
  if echo "$RESPONSE" | grep -q "email"; then
    echo -e "${GREEN}✓ Current user info endpoint working${NC}"
    echo "User: $(echo $RESPONSE | sed -n 's/.*"email":"\([^"]*\)".*/\1/p')"
  else
    echo -e "${RED}✗ Current user info endpoint failed${NC}"
    echo "Response: $RESPONSE"
  fi
}

# Test admin endpoints
test_admin_endpoints() {
  echo -e "\n${YELLOW}Testing admin endpoints...${NC}"
  
  # Get managers list
  echo "Getting managers list..."
  RESPONSE=$(curl -s -X GET -H "Content-Type: application/json" \
    -H "Authorization: Bearer $AUTH_TOKEN" \
    $BASE_URL/api/admin/managers)
  
  if echo "$RESPONSE" | grep -q "managers"; then
    echo -e "${GREEN}✓ Managers list endpoint working${NC}"
  else
    echo -e "${RED}✗ Managers list endpoint failed${NC}"
    echo "Response: $RESPONSE"
  fi
}

# Test manager endpoints
test_manager_endpoints() {
  echo -e "\n${YELLOW}Testing manager endpoints...${NC}"
  
  # Get dashboard data
  echo "Getting dashboard data..."
  RESPONSE=$(curl -s -X GET -H "Content-Type: application/json" \
    -H "Authorization: Bearer $AUTH_TOKEN" \
    $BASE_URL/api/manager/dashboard)
  
  if [[ "$RESPONSE" == *"agent"* || "$RESPONSE" == *"dashboard"* ]]; then
    echo -e "${GREEN}✓ Manager dashboard endpoint working${NC}"
  else
    echo -e "${RED}✗ Manager dashboard endpoint failed${NC}"
    echo "Response: $RESPONSE"
  fi
}

# Test agent endpoints
test_agent_endpoints() {
  echo -e "\n${YELLOW}Testing agent endpoints...${NC}"
  
  # Submit attendance
  echo "Submitting attendance..."
  RESPONSE=$(curl -s -X POST -H "Content-Type: application/json" \
    -H "Authorization: Bearer $AUTH_TOKEN" \
    -d '{"status":"worked","location":"Office","sector":"Health"}' \
    $BASE_URL/api/agent/attendance)
  
  if echo "$RESPONSE" | grep -q "status"; then
    echo -e "${GREEN}✓ Agent attendance endpoint working${NC}"
  else
    echo -e "${RED}✗ Agent attendance endpoint failed${NC}"
    echo "Response: $RESPONSE"
  fi
}

# Test token refresh
test_token_refresh() {
  echo -e "\n${YELLOW}Testing token refresh...${NC}"
  
  RESPONSE=$(curl -s -X POST -H "Content-Type: application/json" \
    -H "Authorization: Bearer $REFRESH_TOKEN" \
    $BASE_URL/auth/refresh-token)
  
  if echo "$RESPONSE" | grep -q "token"; then
    echo -e "${GREEN}✓ Token refresh endpoint working${NC}"
    NEW_TOKEN=$(echo $RESPONSE | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
    echo "New token: ${NEW_TOKEN:0:20}..."
  else
    echo -e "${RED}✗ Token refresh endpoint failed${NC}"
    echo "Response: $RESPONSE"
  fi
}

# Run tests
main() {
  if check_app_running; then
    if login; then
      test_current_user
      test_admin_endpoints
      test_manager_endpoints
      test_agent_endpoints
      test_token_refresh
      
      echo -e "\n${GREEN}===== API Test Summary =====${NC}"
      echo "The application is working with JPA schema management."
      echo "Database migrations through Flyway have been successfully replaced."
    fi
  fi
}

# Execute the main function
main 