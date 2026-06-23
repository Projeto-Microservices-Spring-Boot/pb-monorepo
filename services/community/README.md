# Community Service

Microservice providing Posts and Comments for the e-commerce platform community feed.

## Overview

| Aspect | Details |
|--------|---------|
| Language | Java 21 |
| Framework | Spring Boot 3.5 |
| Database | MongoDB (Bucket Pattern for embedded comments) |
| Messaging | AWS SNS fan-out → SQS (moderation + notification queues) |
| Auth | JWT validated with RSA public key (via `com.edu.infnet.pb:shared`), routed through Kong API Gateway |
| Observability | Structured JSON logging (logstash-logback-encoder), Prometheus metrics |

## Architecture Decisions

**Bucket Pattern**: Each Post embeds up to 10 most-recent Comments for fast feed reads. Full comment history is persisted in a separate `comments` collection for paginated queries beyond the bucket.

**Fan-out**: A single SNS topic (`community-events-topic`) fans out to two SQS queues via filter policies — one for content moderation (PostCreated + CommentCreated), one for post-author notifications (CommentCreated only).

**Soft deletes**: Posts and Comments are never physically removed. `deletedAt` field is set; repository queries exclude documents where `deletedAt != null`. Deleting a Post cascades soft-deletes to all its Comments.

**Authorization**: `POST /posts` requires `ROLE_SELLER` or `ROLE_ADMIN`. All write operations require ownership or `ROLE_ADMIN`. Read endpoints (`GET /posts`, `GET /posts/{id}`) are public.

**Inter-service communication**: All traffic between microservices (including users-service) is routed through the Kong API Gateway (`/community` → `community:8083`, `/users` → `users:8082`). The community-service does not call other services directly — it only validates the JWT issued by users-service using the shared RSA public key, so the user's `sub`, `name`, and `role` claims are read straight from the token without any synchronous lookup.

## Quick Start (Local)

```bash
# Start MongoDB + LocalStack + the service
docker compose up

# Create a post (requires a valid JWT from users-service, routed through Kong)
curl -X POST http://localhost:8000/community/posts \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"My First Post","content":"Hello community!"}'

# Browse the feed (no auth required)
curl http://localhost:8000/community/posts
```

> Requests in production always go through Kong (`http://kong:8000/community/**`).
> When running the service standalone for development (without Kong in front),
> it listens directly on port `8083`.

## API Reference

### Posts

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/posts` | SELLER or ADMIN | Create a new post |
| `GET` | `/posts?page=0&size=20` | None | Paginated feed (newest first) |
| `GET` | `/posts/{postId}?page=0&size=50` | None | Single post with paginated comments |
| `PUT` | `/posts/{postId}` | Owner or ADMIN | Update title and content |
| `DELETE` | `/posts/{postId}` | Owner or ADMIN | Soft-delete post (cascades to comments) |

### Comments

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/comments` | Any authenticated | Add a comment to a post |
| `DELETE` | `/comments/{commentId}` | Owner or ADMIN | Soft-delete a comment |

### Validation Rules

| Field | Constraint |
|-------|-----------|
| Post title | 5–200 characters, required |
| Post content | 10–10000 characters, required |
| Comment content | 1–2000 characters, required |

### Error Response Format

```json
{
  "timestamp": "2024-01-01T12:00:00Z",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Validation failed",
  "fieldErrors": {
    "title": "title is required"
  }
}
```

## Running Tests

```bash
# Unit tests only (no Docker required)
mvn test

# Unit + integration tests (requires Docker for Testcontainers)
mvn verify
```

## Observability

- **Health**: `GET /actuator/health` (liveness + readiness probes at `/actuator/health/liveness` and `/actuator/health/readiness`)
- **Metrics**: `GET /actuator/prometheus`
- **Logs**: Structured JSON to stdout — field `correlationId` propagated via MDC

## Authentication Setup

This service validates JWTs using an RSA public key — the same key pair used across all microservices and registered in `kong.yml`. See [docs/rsa-key-setup.md](docs/rsa-key-setup.md) for how to configure `src/main/resources/keys/public.pem` (or the `JWT_PUBLIC_KEY` environment variable) with the correct key.

## Production Setup

See [docs/aws-setup.md](docs/aws-setup.md) for SNS topic, SQS queue, DLQ, and IAM policy provisioning.
