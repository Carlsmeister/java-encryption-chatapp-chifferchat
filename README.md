# ChifferChat

> End-to-end encrypted web chat application built with Spring Boot and React

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-blue.svg)](https://reactjs.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## 🔐 Overview

ChifferChat is a secure messaging platform that prioritizes user privacy through **client-side end-to-end encryption**.
The server never has access to private keys or plaintext messages, ensuring complete confidentiality.

### Key Features

- ✅ **End-to-End Encryption** - RSA-2048 + AES-256-GCM encryption
- ✅ **Real-Time Messaging** - WebSocket (STOMP) for instant delivery
- ✅ **Group Chats** - Secure multi-user conversations
- ✅ **Modern UI** - Discord-inspired interface with dark/light themes
- ✅ **JWT Authentication** - Secure token-based auth with refresh tokens
- ✅ **Privacy First** - Server cannot decrypt messages

## 🚀 Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.8+
- Node.js 18+ (for frontend)
- PostgreSQL 15+ (or use H2 for development)

### Backend Setup

```bash
# Clone the repository
git clone https://github.com/yourusername/ChifferChat.git
cd ChifferChat

# Run with H2 database (development)
mvn spring-boot:run

# Or run with PostgreSQL (production)
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

Backend will start on `http://localhost:8080`

### Frontend Setup

```bash
cd frontend

# Install dependencies
npm install

# Start development server
npm start
```

Frontend will start on `http://localhost:3000`

## 📚 Documentation

Comprehensive documentation is available in the `/ai` directory:

- **[Project Context](context/context.md)** - Technology stack and current status
- **[Architecture](context/architecture.md)** - System design and component relationships
- **[Conventions](context/conventions.md)** - Coding standards and best practices
- **[Tasks](context/tasks.md)** - Development roadmap and priorities
- **[Testing](context/testing.md)** - Testing strategy and guidelines
- **[Design System](context/design.md)** - UI/UX guidelines and component library
- **[Security](context/security.md)** - Security requirements and implementation

## 🏗️ Architecture

```
┌─────────────────┐         ┌─────────────────┐         ┌─────────────────┐
│  React Frontend │ ◄─────► │  Spring Boot    │ ◄─────► │  PostgreSQL     │
│                 │  HTTPS  │  Backend        │  JDBC   │  Database       │
│  • Web Crypto   │  WSS    │  • REST API     │         │  • Encrypted    │
│  • STOMP Client │         │  • WebSocket    │         │    messages     │
│  • Material-UI  │         │  • Spring Sec.  │         │  • User data    │
└─────────────────┘         └─────────────────┘         └─────────────────┘
```

### Technology Stack

**Backend:**

- Spring Boot 3.2.2
- Spring Security + JWT
- Spring Data JPA + Hibernate
- PostgreSQL / H2
- WebSocket (STOMP over SockJS)
- Flyway (database migrations)

**Frontend:**

- React 18
- Material-UI (MUI)
- Web Crypto API
- STOMP.js + SockJS
- Axios

**Security:**

- RSA-2048 for key exchange
- AES-256-GCM for message encryption
- BCrypt for password hashing
- JWT (HS512) for authentication

## 🔒 How Encryption Works

### 1. Registration

```
User registers → Generate RSA keypair in browser
              → Store private key locally (IndexedDB)
              → Send public key to server
```

### 2. Sending a Message

```
Type message → Generate AES session key
            → Encrypt message with AES
            → Encrypt AES key with recipient's RSA public key
            → Send encrypted payload to server
```

### 3. Receiving a Message

```
Receive encrypted payload → Decrypt AES key with my RSA private key
                         → Decrypt message with AES key
                         → Display plaintext
```

**The server never has access to:**

- Private keys (stored only in client browser)
- Plaintext messages (encrypted before transmission)
- AES session keys (encrypted with recipient's public key)

## 🗂️ Project Structure

```
ChifferChat/
├── src/main/
│   ├── java/se/mau/chifferchat/
│   │   ├── config/           # Spring Boot configuration
│   │   ├── controller/       # REST API controllers
│   │   ├── websocket/        # WebSocket handlers
│   │   ├── service/          # Business logic
│   │   ├── repository/       # Data access layer
│   │   ├── model/            # JPA entities
│   │   ├── dto/              # Data Transfer Objects
│   │   ├── security/         # JWT, authentication
│   │   ├── crypto/           # Encryption utilities
│   │   └── exception/        # Error handling
│   └── resources/
│       ├── db/migration/     # Flyway SQL scripts
│       └── application.yml   # Configuration
├── frontend/
│   ├── src/
│   │   ├── components/       # React components
│   │   ├── services/         # API clients, crypto
│   │   ├── hooks/            # Custom React hooks
│   │   └── store/            # State management
│   └── public/
├── ai/                       # AI context documentation
└── pom.xml                   # Maven configuration
```

## 🧪 Testing

```bash
# Run backend tests
mvn test

# Run backend tests with coverage
mvn test jacoco:report

# Run frontend tests
cd frontend
npm test

# Run E2E tests
npm run test:e2e
```

## 📦 Building for Production

```bash
# Build backend JAR
mvn clean package -DskipTests

# Build frontend
cd frontend
npm run build

# Run production build
java -jar target/ChifferChat-1.0-SNAPSHOT.jar --spring.profiles.active=prod
```

## 🚢 Deployment

### Docker

```bash
# Build Docker image
docker build -t chifferchat .

# Run with docker-compose
docker-compose up -d
```

### Environment Variables

Required for production:

```bash
DATABASE_URL=jdbc:postgresql://localhost:5432/chifferchat
DATABASE_USERNAME=your_username
DATABASE_PASSWORD=your_password
JWT_SECRET=your_secret_key_min_512_bits
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'feat: add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

See [conventions.md](context/conventions.md) for coding standards.

## 📋 Development Roadmap

- [x] Backend architecture setup
- [x] JWT authentication
- [x] WebSocket real-time messaging
- [x] End-to-end encryption
- [ ] React frontend implementation
- [ ] Group chat functionality
- [ ] File sharing with encryption
- [ ] Voice/video calls (WebRTC)
- [ ] Mobile apps (React Native)

See [tasks.md](context/tasks.md) for detailed task breakdown.

## 🔐 Security

Security is our top priority. ChifferChat implements:

- End-to-end encryption (E2EE)
- Zero-knowledge architecture
- JWT with refresh token rotation
- BCrypt password hashing
- Rate limiting
- CORS protection
- SQL injection prevention
- XSS sanitization

For detailed security information, see [security.md](context/security.md).

### Reporting Security Issues

Please report security vulnerabilities to: security@chifferchat.com

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👏 Acknowledgments

- Design inspired by Discord
- Encryption implementation follows Signal Protocol principles
- Built with Spring Boot and React

## 📞 Contact

- **Project Maintainer**: Carl Lundholm
- **Email**: carl_0221@hotmail.se
- **Website**: https://chifferchat.example.com

---

**⚠️ Disclaimer**: This project is currently in development. While we implement industry-standard encryption, a full
security audit has not been completed. Use at your own risk for production deployments.
