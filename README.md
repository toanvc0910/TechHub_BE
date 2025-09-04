# TechHub – Programming Knowledge Sharing and Learning Platform

## 📌 Introduction
**TechHub** is an advanced online learning platform specializing in **Information Technology (IT)**.  
It combines the features of **Udemy** (online courses) and **Viblo** (community knowledge sharing) to meet Vietnam’s growing demand for programming education and professional development.

The platform is built with **microservices architecture** to ensure scalability and flexibility, integrating modern technologies such as **AI chatbot**, **web IDE**, and **gamification** to enhance the learning experience.

---

## 🎯 Objectives
- Build a **comprehensive learning platform** tailored to the needs of Vietnamese students and programmers.
- Provide **high-quality courses** with video lectures, quizzes, and hands-on practice via a **web-based IDE**.
- Integrate a **blog system, forums, and collaboration tools** to create a strong programming community.
- Apply **Microservices architecture** for scalability and easy integration of future technologies (AI, blockchain, IoT).
- Develop an **AI Chatbot** powered by LLM, NLP, and RAG for **personalized learning support**.
- Enhance the **learning experience** with gamification, learning analytics, and personalized recommendations.

---

## ⚙️ Functional Features

### 👤 User Management
- Registration/Login via Email, Google, Facebook, GitHub (OTP & OAuth2).
- Roles:
    - **Learners**: Access purchased courses, comment, track progress.
    - **Instructors**: Create courses, blogs, code content, and interact with learners.
    - **Admins**: Approve content, manage users and transactions.
- User Profile with learning history, purchased courses, and paths.

### 🎓 Course Management
- Course creation with videos, exercises, and documents.
- Progress-based **content unlocking** (≥70% completion required).
- Ratings & comments (nested replies).
- **Learning progress tracking** with completion percentages.
- Exercises: Multiple-choice, coding (auto-graded with test cases).
- **Integrated Web IDE** for practice (HTML, CSS, JS, Python, etc.), with code saving & syncing.

### 💰 E-Commerce
- Payments via **Momo, ZaloPay, credit cards, bank transfer**.
- Shopping cart & promotions.
- Transaction history and refund management.

### 📝 Blog & Community
- Blog posts with markdown, images, and tags.
- Nested comments and replies.

### 🛣️ Learning Paths
- Roadmaps (Frontend, Backend, DevOps, etc.).
- Progress tracking with milestones.

### 🤖 AI Chatbot
- Answers based on **course content, blogs, and code** (RAG).
- Supports **open-ended LLM queries**.
- Markdown + code snippet support in chat.

### 🏆 Extended Features
- Gamification (badges, leaderboards, reward points).
- Course recommendations based on history & preferences.
- Group chat & forums for collaboration.
- Notifications (email & push).
- Multilingual support: **Vietnamese, English, Japanese**.
- Learning analytics dashboard.

---

## 🛠️ Technologies

- **Frontend**: React.js / Next.js, Tailwind CSS
- **Backend**: Java Spring Boot / Python
- **Architecture**: Microservices
- **Database**: PostgreSQL (structured), MongoDB (unstructured)
- **Web IDE**: Monaco Editor (VS Code-like)
- **AI Chatbot**: GPT model + RAG + Vector Database
- **Message Broker**: Kafka, Redis
- **Monitoring & Logging**: Zipkin, ELK Stack
- **Deployment**: Docker, Kubernetes
- **Payments**: VNPAY, Momo

---

## 🔐 Non-Functional Requirements
- **Performance**: Handle high concurrency.
- **Scalability**: Extendable IDE languages, new payment methods.
- **Security**: SSL/TLS, content protection, 2FA.
- **Usability**: Responsive, accessible (WCAG 2.1).
- **Compatibility**: Latest versions of Chrome, Firefox, Safari, Edge + Mobile (iOS, Android).

---

## 🚧 Challenges
- Technical complexity (web IDE integration, microservices scaling).
- Content security (preventing unauthorized downloads).
- High-performance requirements with many concurrent users.

---

## 👥 Contributors
- **Văn Công Toàn** – Student ID: 22110079
- **Trần Văn Tuyến** – Student ID: 22110086
- **Lý Đăng Triều** – Student ID: 22110080

---

## 📄 License
This project is for **academic and research purposes**. Future development may adapt licensing based on contributions and community usage.  
