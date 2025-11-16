# 🏦 Koinsave - Digital Wallet API

![Java](https://img.shields.io/badge/Java-21-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-green)
![Spring Security](https://img.shields.io/badge/Spring%20Security-6.2.0-red)
![Build](https://img.shields.io/badge/Build-Maven-orange)

A production-ready REST API for digital wallet operations with JWT authentication, secure money transfers, and comprehensive transaction management.

## 🎯 Features

- **🔐 Secure Authentication** - JWT tokens with BCrypt password hashing
- **💸 Atomic Money Transfers** - Pessimistic locking & transaction safety
- **🛡️ Enterprise Security** - Rate limiting, input validation, SQL injection protection
- **📊 Transaction History** - Complete audit trail with balance tracking
- **🚀 Production Ready** - Environment configuration, error handling, logging

## 🚀 Quick Start

### Prerequisites
- Java 21+
- Maven 3.6+

### Installation & Run (3 minutes)
```bash
# Clone and run
git clone https://github.com/Abdulrasaq1515/koinsave.git
cd koinsave
mvn clean install
mvn spring-boot:run

# Application runs on http://localhost:8080