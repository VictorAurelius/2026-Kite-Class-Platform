# Skill: Required Knowledge

Kiến thức kỹ thuật cần nắm vững cho dự án KiteClass Platform V3.1.

## Mô tả

Tài liệu yêu cầu kỹ thuật bao gồm:
- Tech Stack Overview
- Backend Development (Java, Spring Boot)
- Frontend Development (React, Next.js)
- Database & Data (PostgreSQL)
- DevOps & Deployment (Docker, Kubernetes)
- Security & Testing

## Trigger phrases

- "kiến thức cần học"
- "tech stack"
- "required knowledge"
- "learning roadmap"
- "skill matrix"

## Files

| File | Path |
|------|------|
| Tài liệu chính | `documents/plans/required-knowledge.md` |

## Tech Stack Overview

### KiteHub
- Backend: Java Spring Boot (Modular Monolith)
- Frontend: Next.js 14 + TypeScript + TailwindCSS
- Database: PostgreSQL
- Cache: Redis
- Queue: RabbitMQ
- AI: OpenAI GPT-4, Stability AI SDXL

### KiteClass Instance
- User+Gateway Service: Java Spring Boot + Spring Cloud Gateway
- Core Service: Java Spring Boot
- Engagement Service: Java Spring Boot (Optional)
- Media Service: Node.js + FFmpeg (Optional)
- Frontend: Next.js 14 + TypeScript

### Infrastructure
- Container: Docker + Docker Compose
- Orchestration: Kubernetes (Production)
- CI/CD: GitHub Actions
- Cloud: AWS / DigitalOcean / Vultr
- CDN: CloudFlare
- Monitoring: Prometheus + Grafana

## Skill Matrix theo Role

| Role | Primary Skills | Level |
|------|----------------|-------|
| Backend Dev | Java, Spring Boot, PostgreSQL | Mid-Senior |
| Frontend Dev | React, Next.js, TypeScript | Mid-Senior |
| Full-stack Dev | Java + React/Next.js | Senior |
| DevOps Engineer | Docker, Kubernetes, CI/CD | Mid-Senior |

## Key Knowledge Areas

### Backend (P0 - Bắt buộc)
- Java Core: OOP, Collections, Streams, Lambda
- Spring Boot: IoC, DI, REST API, JPA
- Spring Security: JWT, OAuth2, RBAC
- Database: JPA/Hibernate, N+1 problem
- API Design: REST principles, Error handling

### Frontend (P0 - Bắt buộc)
- TypeScript: Types, Interfaces, Generics
- React: Hooks, Custom Hooks, State Management
- Next.js 14: App Router, Server Components
- TailwindCSS + Shadcn/UI

### DevOps (P1 - Quan trọng)
- Docker: Multi-stage builds, Compose
- Kubernetes: Pods, Deployments, Services
- CI/CD: GitHub Actions

## Learning Roadmap

### Junior (0-2 years) - 3-6 tháng
- Java Core, Spring Boot basics
- SQL fundamentals
- React/Next.js basics
- Git, Docker basics

### Mid-Level (2-4 years) - 3-6 tháng
- Spring Security, JWT
- Advanced JPA
- React Query, State management
- Testing, CI/CD basics

### Senior (4+ years) - 6-12 tháng
- System Design
- Microservices patterns
- Kubernetes
- Performance optimization
- Team leadership

## Actions

### Xem chi tiết knowledge
Đọc file `documents/plans/required-knowledge.md` cho code examples và chi tiết.

### Xem testing strategies
Xem phần "7. Testing" trong tài liệu.

### Xem tài liệu tham khảo
Xem phần "10. Tài liệu tham khảo" cho books, courses, platforms.
