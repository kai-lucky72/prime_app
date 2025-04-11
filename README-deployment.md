# Deploying Prime Management App

This guide explains how to deploy the Prime Management App backend to either Render.com or Vercel.

## Deploying to Render.com (Recommended)

Render is the recommended platform for deploying this Spring Boot application because it has better support for JVM applications and databases.

### Prerequisites

1. A Render.com account
2. Your application code pushed to a Git repository (GitHub, GitLab, or Bitbucket)

### Deployment Steps

1. Log in to your Render dashboard
2. Click on the "Blueprint" button in the dashboard
3. Connect your Git repository containing the application code
4. Select the repository and branch containing your application code
5. Render will detect the `render.yaml` file and ask you to confirm the resources to be created
6. Click "Apply" to start the deployment

Render will automatically:

- Create a MySQL database
- Deploy your Spring Boot application
- Set up the necessary environment variables

### Monitoring and Logs

1. Once deployed, you can monitor your application from the Render dashboard
2. View logs by clicking on your web service, then selecting the "Logs" tab
3. Check the status of your application using the health check endpoint: `/api/v1/actuator/health`

## Deploying to Vercel (Alternative)

Vercel is primarily designed for frontend applications, but it can be used to deploy Java applications with some limitations.

### Prerequisites

1. A Vercel account
2. Your application code pushed to a Git repository
3. An external MySQL database (Vercel doesn't provide managed databases)

### Deployment Steps

1. Install the Vercel CLI: `npm install -g vercel`
2. Login to Vercel: `vercel login`
3. Navigate to your project directory
4. Run: `vercel`
5. Follow the prompts to link your project to a Vercel project
6. Configure environment variables in the Vercel dashboard:
   - DB_HOST
   - DB_PORT
   - DB_NAME
   - DB_USERNAME
   - DB_PASSWORD
   - JWT_SECRET
   - FILE_UPLOAD_DIR

### Important Notes for Vercel Deployment

1. Vercel has a maximum execution time of 10 seconds for serverless functions, which may not be sufficient for some API operations
2. File uploads may not work as expected due to the serverless nature of Vercel
3. You'll need to set up and manage your own MySQL database (consider using PlanetScale, AWS RDS, or similar services)

## Environment Variables

Both deployment options require the following environment variables:

| Variable               | Description                              |
| ---------------------- | ---------------------------------------- |
| SPRING_PROFILES_ACTIVE | Set to 'prod' for production             |
| DB_HOST                | Database hostname                        |
| DB_PORT                | Database port (typically 3306 for MySQL) |
| DB_NAME                | Database name                            |
| DB_USERNAME            | Database username                        |
| DB_PASSWORD            | Database password                        |
| JWT_SECRET             | Secret key for JWT token generation      |
| FILE_UPLOAD_DIR        | Directory for file uploads               |

## Custom Domain Configuration

### Render

1. Go to your web service in the Render dashboard
2. Click on "Settings"
3. Scroll to "Custom Domain"
4. Click "Add Custom Domain" and follow the instructions

### Vercel

1. Go to your project in the Vercel dashboard
2. Click on "Settings" > "Domains"
3. Add your domain and follow the DNS configuration instructions
