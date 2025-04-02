# Prime Management App

![Java](https://img.shields.io/badge/Java-17-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.0-green)
![MySQL](https://img.shields.io/badge/MySQL-8.0-orange)
![License](https://img.shields.io/badge/License-MIT-yellow)

A comprehensive management system for Agents, Managers, and Administrators built with Spring Boot and React. Features include attendance tracking, performance management, and real-time notifications.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Setup and Installation](#setup-and-installation)
- [Features](#features)
- [API Documentation](#api-documentation)
- [Authentication](#authentication)
- [Security](#security)
- [Caching](#caching)
- [Rate Limiting](#rate-limiting)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [License](#license)

## Prerequisites

- Java 17 or higher
- Maven 3.8+
- MySQL 8.0+ (or PostgreSQL 13+)
- Node.js 18+ and npm (for frontend)
- Redis (optional, for caching and rate limiting)

## Setup and Installation

### Clone the Repository

```bash
git clone https://github.com/yourusername/prime-app.git
cd prime-app
```

### Configure Environment Variables

Create a `.env` file in the project root with the following variables:

```
DB_URL=jdbc:mysql://localhost:3306/prime_db
DB_USERNAME=your_username
DB_PASSWORD=your_password
JWT_SECRET=your_jwt_secret
MAIL_HOST=smtp.example.com
MAIL_PORT=587
MAIL_USERNAME=your_email
MAIL_PASSWORD=your_email_password
REDIS_ENABLED=false
```

### Database Setup

1. Create a MySQL database:

```sql
CREATE DATABASE prime_db;
```

2. The application will automatically create the tables on first run.

### Build and Run

#### Backend

```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

Alternatively, you can run the JAR file:

```bash
java -jar target/prime-app-0.0.1-SNAPSHOT.jar
```

#### Frontend (if applicable)

```bash
cd frontend
npm install
npm start
```

## Features

### User Management

- **User Roles**: Admin, Manager, and Agent roles with appropriate permissions
- **Profile Management**: Users can update their profiles and contact information

### Authentication

- **Secure Login**: Work ID and email-based authentication with optional password
- **JWT Tokens**: Secure session management using JSON Web Tokens
- **Role-Based Redirection**: Users are redirected to appropriate dashboards based on roles

### Attendance Management

- **Clock In/Out**: Agents can clock in and out for shifts
- **Attendance Records**: Managers can view attendance records for their teams
- **Reports**: Generate attendance reports for selected time periods

### Performance Tracking

- **Key Metrics**: Track agent performance through configurable metrics
- **Analytics**: Visual analytics for performance trends
- **Evaluations**: Regular performance evaluations and feedback

### Notifications

- **Real-time Updates**: Get notified about important events
- **Email Notifications**: Receive emails for urgent matters and reminders
- **Custom Settings**: Configure notification preferences

## API Documentation

### Authentication Endpoints

- `POST /api/auth/login` - Authenticate user
- `POST /api/auth/logout` - Logout user
- `POST /api/auth/request-help` - Request login help

### Admin Endpoints

- `GET /api/admin/users` - List all users
- `POST /api/admin/users` - Create a new user
- `PUT /api/admin/users/{id}` - Update a user
- `DELETE /api/admin/users/{id}` - Delete a user
- `GET /api/admin/reports` - Generate system reports

### Manager Endpoints

- `GET /api/manager/agents` - List agents under a manager
- `GET /api/manager/attendance` - View team attendance
- `POST /api/manager/notifications` - Send notifications to agents
- `GET /api/manager/performance` - View team performance metrics

### Agent Endpoints

- `POST /api/agent/attendance/clock-in` - Clock in for shift
- `POST /api/agent/attendance/clock-out` - Clock out from shift
- `GET /api/agent/notifications` - View notifications
- `GET /api/agent/performance` - View personal performance metrics

## Security

The application implements several security measures:

- **JWT Authentication**: Secure token-based authentication
- **Role-Based Access Control**: Endpoints are secured by role
- **Password Encryption**: Passwords are stored using BCrypt
- **HTTPS Support**: Configure SSL for production environments
- **CORS Configuration**: Controlled cross-origin requests

## Caching

Redis caching is optionally supported for enhanced performance:

```properties
# Enable Redis caching
spring.data.redis.enabled=true
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

When Redis is not available, the application falls back to in-memory caching.

## Rate Limiting

API rate limiting is implemented to prevent abuse:

```properties
# Configure rate limiting
rate.limit.enabled=true
rate.limit.requests=100
rate.limit.time-window=60
```

## Troubleshooting

### Common Issues

1. **Database Connection Failed**:

   - Verify database credentials in `.env` or application properties
   - Check if MySQL service is running

2. **Authentication Issues**:

   - Clear browser cookies and try again
   - Check if the JWT secret is properly configured

3. **Redis Connection Failed**:
   - If Redis is not available, set `spring.data.redis.enabled=false`
   - Check if Redis server is running when enabled

### Logs

Application logs are located in:

- `./logs/application.log` (default location)

## Contributing

We welcome contributions to improve the Prime Management App!

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Coding Standards

- Follow Java code conventions
- Write unit tests for new features
- Update documentation as necessary

## License

This project is licensed under the MIT License - see the LICENSE file for details.

---

© 2023 Prime Management App. All rights reserved.
