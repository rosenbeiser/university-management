# 🎓 University Management System (Spring Boot 3.x)

A secure, production-grade RESTful API backend for managing university operations, students, teachers, and administrators. Built with **Java 21**, **Spring Boot 3.4.1**, **Spring Security 6**, **MySQL 8.4**, **Flyway**, and **Docker**.

---

## 🌟 Key Features

- **Role-Based Access Control (RBAC)**: Strict permission boundaries for `ADMIN`, `TEACHER`, and `STUDENT` roles.
- **Stateless JWT Authentication**: Access tokens (15-min expiry) + revocable DB-backed Refresh tokens (7-day expiry).
- **BCrypt Password Hashing**: Passwords are securely hashed; raw passwords never leak into responses.
- **Database Migrations with Flyway**: Versioned, reproducible database schema management (`V1` to `V5`).
- **Comprehensive CRUD Operations**:
  - Full management of Students, Teachers, and Admins.
  - Pagination, sorting, and field-level validation on all list endpoints.
  - Safety guardrails (e.g. preventing the deletion of the final active administrator).
- **Integrated Web Portal**: Full responsive web management frontend with dashboards, student directories, teacher management, modal forms, and dark/light mode running directly on `http://localhost:8081/`.
- **Interactive OpenAPI Documentation**: Swagger UI integrated out of the box with JWT Bearer auth support on `http://localhost:8081/swagger-ui/index.html`.
- **Docker Containerization**: Multi-stage `Dockerfile` and automated multi-service `compose.yaml`.
- **Automated Testing Suite**: Unit and integration tests covering security, JWT, and business logic.

---

## 🛠️ Technology Stack

| Component | Technology | Version |
|---|---|---|
| **Language** | Java (OpenJDK) | 21 / 25 |
| **Framework** | Spring Boot | 3.4.1 |
| **Security** | Spring Security + JJWT | 6.x / 0.12.6 |
| **Persistence** | Spring Data JPA (Hibernate) | 3.4.1 |
| **Database** | MySQL Server | 8.4 LTS |
| **Schema Migration** | Flyway MySQL | 10.x |
| **API Docs** | Springdoc OpenAPI (Swagger UI) | 2.7.0 |
| **Testing** | JUnit 5, Mockito, Testcontainers | Latest |
| **Containerization**| Docker & Docker Compose | Latest |

---

## 🏛️ Architecture & Project Structure

The project follows a standard layered clean architecture:

```
src/
├── main/
│   ├── java/com/university/management/
│   │   ├── UniversityManagementApplication.java  # Application Entry Point
│   │   ├── config/                               # Security, OpenAPI, Bootstrap Data
│   │   ├── controller/                           # REST API Endpoints
│   │   ├── dto/
│   │   │   ├── request/                          # Validated Request Payloads
│   │   │   └── response/                         # Sanitized Response Payloads
│   │   ├── entity/                               # JPA Domain Entities
│   │   ├── enums/                                # Role, AccountStatus
│   │   ├── exception/                            # Error Models & Global Exception Handler
│   │   ├── repository/                           # Spring Data Repositories
│   │   ├── security/                             # JWT Filter, JwtService, UserDetailsService
│   │   └── service/                              # Business Logic Layer
│   └── resources/
│       ├── application.yml                       # Configuration & Env variable bindings
│       └── db/migration/                         # Flyway SQL migrations (V1 to V5)
└── test/                                         # Comprehensive unit & integration tests
```

---

## 🔒 Role-Based Permissions Matrix
 
| Endpoint | Method | Permitted Roles | HTTP Status | Description |
|---|---|---|---|---|
| `/api/v1/auth/login` | `POST` | Public | `200 OK` | Authenticate and obtain JWT + Refresh token |
| `/api/v1/auth/refresh` | `POST` | Public | `200 OK` | Exchange refresh token for fresh access token |
| `/api/v1/auth/logout` | `POST` | Authenticated | `204 No Content` | Revoke user refresh token |
| `/api/v1/auth/me` | `GET` | Authenticated | `200 OK` | Safe profile information for current user |
| `/api/v1/admin/dashboard` | `GET` | `ADMIN` | `200 OK` | System metrics (total users, students, faculty, admins) |
| `/api/v1/admins` | `POST` | `ADMIN` | `201 Created` | Create new Administrator account |
| `/api/v1/admins` | `GET` | `ADMIN` | `200 OK` | List administrators (paginated) |
| `/api/v1/admins/{id}` | `GET` | `ADMIN` | `200 OK` / `404` | Retrieve admin details by ID |
| `/api/v1/admins/{id}` | `PUT`/`PATCH` | `ADMIN` | `200 OK` / `404` | Update administrator profile |
| `/api/v1/admins/{id}` | `DELETE` | `ADMIN` | `204 No Content` | Delete admin (guarded against last admin) |
| `/api/v1/teachers` | `POST` | **`ADMIN` only** | `201 Created` | **Appoint new Faculty / Teacher member** |
| `/api/v1/teachers` | `GET` | `ADMIN`, `TEACHER`, `STUDENT` | `200 OK` | List all faculty members (paginated) |
| `/api/v1/teachers/{id}` | `GET` | `ADMIN`, `TEACHER`, `STUDENT` | `200 OK` / `404` | Retrieve faculty member by ID |
| `/api/v1/teachers/{id}` | `PUT`/`PATCH` | `ADMIN` | `200 OK` / `404` | Update faculty member record |
| `/api/v1/teachers/{id}` | `DELETE` | `ADMIN` | `204 No Content` | Remove faculty member |
| `/api/v1/students` | `POST` | **`ADMIN` only** | `201 Created` | **Register new Student account** |
| `/api/v1/students` | `GET` | `ADMIN`, `TEACHER` | `200 OK` | List all enrolled students (paginated) |
| `/api/v1/students/{id}` | `GET` | `ADMIN`, `TEACHER`, `STUDENT` | `200 OK` / `404` | Retrieve student details by ID |
| `/api/v1/students/{id}` | `PUT`/`PATCH` | `ADMIN` | `200 OK` / `404` | Update student profile |
| `/api/v1/students/{id}` | `DELETE` | `ADMIN` | `204 No Content` | Delete student record |
| `/`, `/index.html` | `GET` | Public | `200 OK` | Integrated Web Portal UI (`http://localhost:8081/`) |
| `/swagger-ui/**`, `/api-docs/**`| `GET` | Public | `200 OK` | Interactive OpenAPI UI & schema |

---

## 🚀 Getting Started

### Prerequisites

- **Java 21** or later installed
- **Maven 3.9+** (or bundled IDE Maven)
- **Docker** and **Docker Compose** (for containerized execution)
- **MySQL 8.0+** (if running locally without Docker)

---

### Option 1: Running with Docker Compose (Recommended)

1. Clone or navigate to the project directory:
   ```bash
   cd "d:/University Management Project"
   ```

2. Copy the environment template:
   ```bash
   cp .env.example .env
   ```

3. Launch MySQL and the application:
   ```bash
   docker compose up --build -d
   ```

4. View logs:
   ```bash
   docker compose logs -f app
   ```

5. Once started, open **Swagger UI** in your browser:
   👉 **http://localhost:8080/swagger-ui.html**

---

### Option 2: Running Locally with Maven

1. Ensure MySQL is running on port `3306` with database `university_db`.
2. Configure `.env` or set environment variables:
   ```bash
   export DB_URL="jdbc:mysql://localhost:3306/university_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
   export DB_USERNAME="root"
   export DB_PASSWORD="your_password"
   export ADMIN_BOOTSTRAP_ENABLED=true
   export ADMIN_BOOTSTRAP_USERNAME=admin
   export ADMIN_BOOTSTRAP_EMAIL=admin@university.edu
   export ADMIN_BOOTSTRAP_PASSWORD=Admin@123456
   ```
3. Run the application:
   ```bash
   mvn spring-boot:run
   ```

---

## 🧪 Running Automated Tests

Run all unit tests:
```bash
mvn test "-Dtest=JwtServiceTest,AdminServiceTest,TeacherServiceTest,StudentServiceTest"
```

Run all tests including container integration tests:
```bash
mvn test
```

---

## 📡 API Usage Examples (cURL)

### 1. Authenticate (Login)
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "Admin@123456"
  }'
```

*Response:*
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "48b6c4e0-...",
  "tokenType": "Bearer",
  "expiresIn": 900000,
  "role": "ADMIN",
  "username": "admin"
}
```

### 2. Access Protected Endpoints with Bearer Token
```bash
curl -X GET http://localhost:8080/api/v1/dashboard/summary \
  -H "Authorization: Bearer <YOUR_ACCESS_TOKEN>"
```

### 3. Create a New Teacher (Admin Only)
```bash
curl -X POST http://localhost:8080/api/v1/teachers \
  -H "Authorization: Bearer <YOUR_ACCESS_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "prof_smith",
    "email": "smith@university.edu",
    "password": "Password@123",
    "fullName": "Dr. John Smith",
    "department": "Computer Science",
    "designation": "Associate Professor",
    "phone": "+1-555-0199"
  }'
```

### 4. Create a New Student
```bash
curl -X POST http://localhost:8080/api/v1/students \
  -H "Authorization: Bearer <YOUR_ACCESS_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "student_jane",
    "email": "jane@university.edu",
    "password": "Password@123",
    "fullName": "Jane Doe",
    "studentNumber": "STU2026-001",
    "department": "Computer Science",
    "semester": 3,
    "phone": "+1-555-0144"
  }'
```

### 5. Refresh Access Token
```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "<YOUR_REFRESH_TOKEN>"
  }'
```

---

## 🛡️ Security Best Practices Implemented

1. **Defense in Depth**: URLs are secured in both `SecurityConfig.java` and with method-level `@PreAuthorize` annotations on controllers.
2. **Password Hygiene**: Password hash is generated via BCrypt with strength 12. Password fields use `@JsonProperty(access = Access.WRITE_ONLY)` to guarantee no accidental password disclosure.
3. **Auditability & Integrity**: Soft and strict constraints with foreign keys (`ON DELETE CASCADE` between profile and credential accounts). Last-admin deletion protection prevents locking out the system.
4. **Standardized Error Responses**: All API errors conform to RFC-style timestamps, status codes, error classifications, and descriptive messages without leaking internal stack traces.

---

## 📄 License
This project is licensed under the Apache 2.0 License.
