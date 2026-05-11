# Chat App

![CI/CD](https://github.com/thaitr9999-dev/java-chat-app/actions/workflows/ci-cd.yml/badge.svg)

Real-time chat application with Spring Boot + WebSocket + JWT + Docker

## CI/CD Pipeline

Every push to `main` triggers:
1. **Test** - Run unit tests with H2 in-memory database
2. **Build** - Build Docker image and push to Docker Hub
3. **Deploy** - Auto-deploy to Render (production)

View workflow: https://github.com/thaitr9999-dev/java-chat-app/actions

## Deployment

### Production (Render)
- **URL**: https://your-app.onrender.com
- **Auto-deploy**: Every push to `main` triggers deployment
- **Database**: PostgreSQL 15 (managed by Render)

## Environment Variables
- `JWT_SECRET`: Production secret key
- `SPRING_DATASOURCE_URL`: PostgreSQL connection string
- `SPRING_PROFILES_ACTIVE`: `prod`
