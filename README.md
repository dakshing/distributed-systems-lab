# Distributed Systems Lab

A collection of **production-ready** distributed services implemented from scratch.

This repository moves beyond the theoretical "boxes and arrows" of system design interviews to provide **working, end-to-end implementations** of complex distributed challenges. It bridges the gap between high-level architecture diagrams and the low-level engineering reality.

Instead of assuming a database scales, we **choose the DB**, implement the schema and prove it with **load tests** and **observability** metrics.

>**Core Philosophy:** It's not a valid design until it runs in Docker, passes CI/CD, and handles real traffic.

## Modules & Status

| Module                                         | Description                                              | Key Tech                          | Status                |
|:-----------------------------------------------|:---------------------------------------------------------|:----------------------------------|:----------------------|
| **[Common Libraries](java/dsl-common)** (Java) | Shared utility libraries for distributed systems.        | Distributed ID Generator          | ✅ **Completed**      |
| **[Observability (lib)](java/dsl-observability)**    | Central monitoring infrastructure & shared metrics lib.  | Prometheus, Grafana               | ✅ **Completed**      |
| **[URL Shortener](java/url-shortener)**        | Distributed URL Shortener service                        | Netty, ScyllaDB                   | ✅ **Completed**      |
| **[Rate Limiter](java/rate-limiter)**          | Distributed sliding window rate limiter.                 | Redis (Lua), Token Bucket         | 🚧 **In Progress**    |

