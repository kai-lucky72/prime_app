# Prime App - Insurance Agent Management System

![Prime App Logo](https://via.placeholder.com/150x50?text=Prime+App)

A comprehensive backend system for managing insurance agents, tracking their performance, clients, and attendance.

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [API Documentation](#api-documentation)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Installation](#installation)
  - [Configuration](#configuration)
- [Authentication](#authentication)
- [Frontend Integration Guide](#frontend-integration-guide)
- [Database Schema](#database-schema)
- [Security](#security)
- [Caching](#caching)
- [Monitoring](#monitoring)
- [Testing](#testing)
- [Deployment](#deployment)
- [License](#license)

## 🔍 Overview

Prime App is a robust insurance agent management system built with Spring Boot. It provides a complete solution for insurance companies to manage their agents, track performance metrics, monitor attendance, and handle client information.

## ✨ Features

- **Authentication and Authorization**

  - JWT-based secure authentication
  - Role-based access control (Admin, Manager, Agent)
  - Token refresh mechanism

- **User Management**

  - User registration and profile management
  - Role assignment and management
  - Hierarchical management structure

- **Performance Tracking**

  - KPI monitoring (sales targets, achievement percentages)
  - Performance trends over time
  - Scoring and rating system

- **Attendance Management**

  - Check-in/check-out tracking
  - Attendance status monitoring
  - Working hours calculation

- **Client Management**
  - Client information storage
  - Policy tracking and renewals
  - Client-agent relationship management

## 🏗️ Architecture

Prime App is built on a modern, scalable architecture:

- **Backend Framework**: Spring Boot 3.4.4
- **Database**: MySQL with JPA/Hibernate
- **Security**: Spring Security with JWT authentication
- **API Documentation**: OpenAPI/Swagger
- **Caching**: Redis (configurable)
- **Database Migration**: Flyway
- **Build Tool**: Maven
- **Java Version**: Java 21

### Component Structure

- **Controllers**: Handle HTTP requests and define API endpoints
- **Services**: Implement business logic
- **Repositories**: Data access layer for entity persistence
- **DTOs**: Data transfer objects for API requests/responses
- **Entities**: JPA entities mapped to database tables
- **Security**: JWT authentication, authorization, and security config
- **Exception Handling**: Global exception handler for consistent error responses

## 📚 API Documentation

The API is fully documented using OpenAPI/Swagger.

- **Swagger UI**: Access at `/api/v1/swagger-ui.html` when the application is running
- **API Docs**: Raw OpenAPI specification available at `/api/v1/api-docs`

## 🚀 Getting Started

### Prerequisites

- JDK 21
- MySQL 8.0+
- Maven 3.8+
- Redis (optional - for caching)

### Installation

1. Clone the repository:

   ```bash
   git clone https://github.com/yourusername/prime-app.git
   cd prime-app
   ```

2. Build the application:

   ```bash
   mvn clean install
   ```

3. Run the application:
   ```bash
   mvn spring-boot:run
   ```

### Configuration

The application is configured via `application.properties`. Key configuration parameters:

- **Database**:

  ```properties
  spring.datasource.url=jdbc:mysql://localhost:3306/prime_app_db?createDatabaseIfNotExist=true
  spring.datasource.username=root
  spring.datasource.password=your_password
  ```

- **JWT**:

  ```properties
  app.jwt.secret=your_secret_key
  app.jwt.expiration=86400000
  app.jwt.refresh-token.expiration=604800000
  ```

- **Server**:
  ```properties
  server.port=8080
  server.servlet.context-path=/api/v1
  ```

## 🔐 Authentication

The system uses JWT (JSON Web Tokens) for authentication.

### Authentication Flow

1. **Register** a new user at `/auth/register`
2. **Login** with credentials at `/auth/login` to receive JWT tokens
3. Use the **Access Token** in the `Authorization` header for API requests
4. When the access token expires, use the **Refresh Token** at `/auth/refresh-token` to get a new one

### Token Format

- **Authorization Header**: `Bearer eyJhbGciOiJIUzI1NiJ9...`
- **Access Token Validity**: 24 hours
- **Refresh Token Validity**: 7 days

## 🔄 Frontend Integration Guide

### Authentication Integration

1. **User Registration**:

   ```javascript
   fetch('http://localhost:8080/api/v1/auth/register', {
   	method: 'POST',
   	headers: { 'Content-Type': 'application/json' },
   	body: JSON.stringify({
   		firstName: 'John',
   		lastName: 'Doe',
   		email: 'john.doe@example.com',
   		password: 'SecurePass123@',
   		phoneNumber: '+1234567890',
   	}),
   });
   ```

2. **User Login**:

   ```javascript
   fetch('http://localhost:8080/api/v1/auth/login', {
   	method: 'POST',
   	headers: { 'Content-Type': 'application/json' },
   	body: JSON.stringify({
   		email: 'john.doe@example.com',
   		password: 'SecurePass123@',
   	}),
   });
   ```

3. **Making Authenticated Requests**:

   ```javascript
   fetch('http://localhost:8080/api/v1/agent/clients', {
   	method: 'GET',
   	headers: {
   		Authorization: 'Bearer ' + accessToken,
   		'Content-Type': 'application/json',
   	},
   });
   ```

4. **Token Refresh Logic**:

   ```javascript
   async function refreshToken() {
   	const response = await fetch(
   		'http://localhost:8080/api/v1/auth/refresh-token',
   		{
   			method: 'POST',
   			headers: {
   				Authorization: 'Bearer ' + refreshToken,
   			},
   		}
   	);

   	if (response.ok) {
   		const data = await response.json();
   		// Update tokens in your storage
   		localStorage.setItem('accessToken', data.accessToken);
   		localStorage.setItem('refreshToken', data.refreshToken);
   		return data.accessToken;
   	} else {
   		// Redirect to login page if refresh fails
   		window.location.href = '/login';
   	}
   }
   ```

### Error Handling

The API returns consistent error responses with the following structure:

```json
{
	"timestamp": "2025-03-29T20:15:30.1243798",
	"status": 401,
	"error": "Authentication Error",
	"message": "Bad credentials"
}
```

Frontend should handle these error responses appropriately.

### CORS Considerations

The server has CORS enabled for all origins in development. In production, you should configure specific allowed origins.

### API Endpoints Overview

| Feature               | Endpoint                     | Method | Description                   |
| --------------------- | ---------------------------- | ------ | ----------------------------- |
| **Authentication**    | `/auth/register`             | POST   | Register a new user           |
|                       | `/auth/login`                | POST   | Login and get tokens          |
|                       | `/auth/refresh-token`        | POST   | Refresh access token          |
|                       | `/auth/validate-token`       | GET    | Validate token                |
| **Client Management** | `/clients`                   | GET    | List all clients (paginated)  |
|                       | `/clients/{id}`              | GET    | Get client details            |
|                       | `/clients`                   | POST   | Create new client             |
|                       | `/clients/{id}`              | PUT    | Update client                 |
|                       | `/clients/{id}`              | DELETE | Delete client                 |
|                       | `/clients/upcoming-renewals` | GET    | Get upcoming policy renewals  |
| **Performance**       | `/performance`               | GET    | Get agent performance records |
|                       | `/performance/trend`         | GET    | Get performance trends        |
|                       | `/performance/metrics`       | GET    | Get performance metrics       |
| **Attendance**        | `/attendance/check-in`       | POST   | Record check-in               |
|                       | `/attendance/check-out`      | POST   | Record check-out              |
|                       | `/attendance`                | GET    | Get attendance records        |

### Client Management Integration

1. **Fetching Client List (with pagination):**

```javascript
// Example: Fetch paginated client list
async function fetchClients(page = 0, size = 10) {
	const response = await fetch(
		`http://localhost:8080/api/v1/clients?page=${page}&size=${size}`,
		{
			method: 'GET',
			headers: {
				Authorization: 'Bearer ' + accessToken,
				'Content-Type': 'application/json',
			},
		}
	);

	if (response.ok) {
		return await response.json();
	} else {
		handleApiError(response);
	}
}
```

2. **Creating a New Client:**

```javascript
async function createClient(clientData) {
	const response = await fetch('http://localhost:8080/api/v1/clients', {
		method: 'POST',
		headers: {
			Authorization: 'Bearer ' + accessToken,
			'Content-Type': 'application/json',
		},
		body: JSON.stringify({
			firstName: clientData.firstName,
			lastName: clientData.lastName,
			email: clientData.email,
			phoneNumber: clientData.phoneNumber,
			address: clientData.address,
			dateOfBirth: clientData.dateOfBirth,
			insuranceType: clientData.insuranceType, // LIFE, HEALTH, AUTO, PROPERTY, BUSINESS
			policyNumber: clientData.policyNumber,
			policyStartDate: clientData.policyStartDate,
			policyEndDate: clientData.policyEndDate,
			premiumAmount: clientData.premiumAmount,
			policyStatus: clientData.policyStatus, // ACTIVE, PENDING, EXPIRED, CANCELLED
		}),
	});

	if (response.ok) {
		return await response.json();
	} else {
		handleApiError(response);
	}
}
```

### Performance Tracking Integration

1. **Fetching Performance Trend Data:**

```javascript
async function fetchPerformanceTrend(startDate, endDate, agentId = null) {
	let url = `http://localhost:8080/api/v1/performance/trend?startDate=${startDate}&endDate=${endDate}`;

	// If agentId is provided (for managers viewing agent performance)
	if (agentId) {
		url += `&agentId=${agentId}`;
	}

	const response = await fetch(url, {
		method: 'GET',
		headers: {
			Authorization: 'Bearer ' + accessToken,
			'Content-Type': 'application/json',
		},
	});

	if (response.ok) {
		const trendData = await response.json();
		// Format data for charts (e.g., Chart.js)
		return trendData.map((item) => ({
			period: `${item.year}-${item.month}`,
			averageScore: item.averageScore,
		}));
	} else {
		handleApiError(response);
	}
}
```

2. **Visualizing Performance Data:**

```javascript
// Example using Chart.js
function renderPerformanceChart(trendData) {
	const ctx = document.getElementById('performanceChart').getContext('2d');
	new Chart(ctx, {
		type: 'line',
		data: {
			labels: trendData.map((item) => item.period),
			datasets: [
				{
					label: 'Performance Score',
					data: trendData.map((item) => item.averageScore),
					borderColor: 'rgba(75, 192, 192, 1)',
					backgroundColor: 'rgba(75, 192, 192, 0.2)',
					tension: 0.1,
				},
			],
		},
		options: {
			responsive: true,
			scales: {
				y: {
					beginAtZero: true,
					max: 100,
				},
			},
		},
	});
}
```

### Attendance Management Integration

1. **Recording Check-in:**

```javascript
async function recordCheckIn(latitude, longitude) {
	const response = await fetch(
		'http://localhost:8080/api/v1/attendance/check-in',
		{
			method: 'POST',
			headers: {
				Authorization: 'Bearer ' + accessToken,
				'Content-Type': 'application/json',
			},
			body: JSON.stringify({
				latitude: latitude,
				longitude: longitude,
				notes: 'Checked in via mobile app',
			}),
		}
	);

	if (response.ok) {
		return await response.json();
	} else {
		handleApiError(response);
	}
}
```

2. **Recording Check-out:**

```javascript
async function recordCheckOut(latitude, longitude) {
	const response = await fetch(
		'http://localhost:8080/api/v1/attendance/check-out',
		{
			method: 'POST',
			headers: {
				Authorization: 'Bearer ' + accessToken,
				'Content-Type': 'application/json',
			},
			body: JSON.stringify({
				latitude: latitude,
				longitude: longitude,
				notes: 'Checked out via mobile app',
			}),
		}
	);

	if (response.ok) {
		return await response.json();
	} else {
		handleApiError(response);
	}
}
```

3. **Fetching Monthly Attendance Summary:**

```javascript
async function fetchMonthlyAttendance(year, month) {
	const response = await fetch(
		`http://localhost:8080/api/v1/attendance/summary?year=${year}&month=${month}`,
		{
			method: 'GET',
			headers: {
				Authorization: 'Bearer ' + accessToken,
				'Content-Type': 'application/json',
			},
		}
	);

	if (response.ok) {
		return await response.json();
	} else {
		handleApiError(response);
	}
}
```

### Error Handling Utility

```javascript
async function handleApiError(response) {
	const errorData = await response.json();

	// Log error for debugging
	console.error('API Error:', errorData);

	// Handle based on status code
	switch (response.status) {
		case 401:
			// Unauthorized - token might be expired
			const refreshed = await attemptTokenRefresh();
			if (!refreshed) {
				// Redirect to login if token refresh fails
				window.location.href = '/login?error=session_expired';
			}
			break;
		case 403:
			// Forbidden - user doesn't have permission
			showErrorNotification(
				'You do not have permission to perform this action'
			);
			break;
		case 404:
			// Not found
			showErrorNotification('The requested resource was not found');
			break;
		case 400:
			// Bad request - validation errors
			if (errorData.details) {
				// Display specific validation errors
				Object.entries(errorData.details).forEach(([field, message]) => {
					showFieldError(field, message);
				});
			} else {
				showErrorNotification(errorData.message || 'Invalid input');
			}
			break;
		default:
			showErrorNotification(
				'An unexpected error occurred. Please try again later.'
			);
	}

	// Re-throw to allow function-specific handling
	throw new Error(errorData.message || 'API Error');
}
```

### State Management Recommendations

For managing application state in your frontend:

1. **Authentication State**: Store tokens securely using:

   - Browser storage (localStorage/sessionStorage) for SPAs
   - HTTP-only cookies for server-rendered applications
   - Secure storage for mobile applications

2. **Application State**:

   - Use Redux or Context API for React applications
   - Use Vuex for Vue.js applications
   - Use NgRx for Angular applications

3. **API Integration Layer**:
   - Create a dedicated API service layer to abstract API calls
   - Implement request/response interceptors for token handling
   - Use caching strategies for appropriate endpoints

### Real-time Notifications (Optional)

For real-time features, consider implementing WebSocket integration:

```javascript
function connectToNotifications() {
	const socket = new WebSocket(
		`ws://localhost:8080/api/v1/ws?token=${accessToken}`
	);

	socket.onopen = () => {
		console.log('WebSocket connection established');
	};

	socket.onmessage = (event) => {
		const notification = JSON.parse(event.data);
		showNotification(notification);
	};

	socket.onclose = () => {
		// Attempt to reconnect after delay
		setTimeout(connectToNotifications, 5000);
	};

	socket.onerror = (error) => {
		console.error('WebSocket error:', error);
	};

	return socket;
}
```

## 📊 Database Schema

The database consists of several key tables:

- **users**: Stores user information including authentication details
- **roles**: Role definitions (ADMIN, MANAGER, AGENT)
- **user_roles**: Junction table for user-role relationships
- **clients**: Client information and policies
- **attendances**: Agent attendance records
- **performances**: Agent performance metrics and evaluations

## 🔒 Security

The application implements several security measures:

- **Password Encryption**: BCrypt password encoding
- **JWT Security**: Signed tokens with expiration time
- **Role-Based Access Control**: Different APIs accessible based on user roles
- **Rate Limiting**: API rate limiting using Bucket4j
- **CSRF Protection**: CSRF protection is disabled for REST APIs

## 💾 Caching

Redis caching is configured but disabled by default. Enable it by setting:

```properties
spring.cache.type=redis
```

Cached entities include:

- Performance records
- Client information
- Attendance records

## 📈 Monitoring

The application includes Spring Boot Actuator for monitoring:

- **Health Endpoint**: `/api/v1/actuator/health`
- **Metrics Endpoint**: `/api/v1/actuator/metrics`
- **Prometheus Integration**: `/api/v1/actuator/prometheus`

## 🧪 Testing

Run tests with Maven:

```bash
mvn test
```

## 🚢 Deployment

The application can be deployed as a JAR file:

```bash
java -jar target/prime-app-0.0.1-SNAPSHOT.jar
```

For production deployment, consider using:

- Docker containerization
- Environment-specific configuration
- Database connection pooling
- Proper logging configuration

## 📄 License

This project is licensed under the [MIT License](LICENSE).

---

Built with ❤️ using Spring Boot and Java.
