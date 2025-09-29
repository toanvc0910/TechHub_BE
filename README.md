**TechHub Backend — Project Description**
- TechHub is a programming knowledge sharing and learning platform that combines video courses, hands‑on practice, and a community ecosystem. The backend is being built as a microservices system for scalability and flexibility.

**Purpose & Goals**
- Address the shortage of industry‑ready developers with a practical, course‑plus‑practice model tailored to Vietnam.
- Blend Udemy‑style course delivery with Viblo‑style knowledge sharing and roadmap‑guided learning.
- Enable future integration of AI assistants (RAG/LLM) for personalized guidance and 24/7 support.

**High‑Level Architecture**
- Microservices with service discovery and an API gateway.
- Services (work‑in‑progress): user management/auth, course management, learning paths, blogs, payments, and shared/common utilities.
- Observability and distributed tracing planned via Zipkin; messaging and caching will be considered as features grow.

**Core Capabilities (Planned)**
- User management with roles (Learner/Instructor/Admin), authentication (email/OAuth/OTP), and profiles.
- Course domain: courses, chapters, lessons, exercises; progress tracking, comments, and ratings.
- Community features: blogs, nested comments, tags, and discussions.
- Learning paths: curated roadmaps (Frontend/Backend/DevOps, etc.) with progress tracking.
- E‑commerce: cart, checkout, transactions, and local e‑wallet integrations (e.g., VNPAY/Momo).
- AI integration: chatbot for Q&A, content retrieval (RAG), and code assistance.

**Technology Stack (Planned/Current)**
- Java 11, Spring Boot 2.x, Spring Cloud (Eureka, Gateway), Spring Data JPA, Validation.
- PostgreSQL as primary database. The schema (enums, arrays, JSONB) is defined in `techhub.sql` and is the source of truth.
- Containerization with Docker; local orchestration via Docker Compose.

**Project Status**
- Active development. Many services and APIs are not yet complete and will be delivered iteratively.
- Current code includes scaffolding and initial implementations; interfaces and contracts may change as features mature.

**Roadmap (Next Steps)**
- Flesh out Course domain (chapters, lessons, exercises) following `techhub.sql`.
- Complete user auth flows (JWT/OAuth/OTP), role‑based access, and email flows.
- Implement learning paths, blog CRUD with comments, and payments.
- Add search, recommendations, gamification, and analytics.
- Integrate AI chatbot and web IDE in later milestones.

**Notes**
- This repository is a multi‑module Maven project. Each service has its own configuration and build profile.
- The database must be initialized with `techhub.sql` in environments that run DB‑backed services.
