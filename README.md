# 🏥 Patient Management Microservice Platform

[![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.4-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?logo=postgresql)](https://www.postgresql.org/)
[![Apache Kafka](https://img.shields.io/badge/Kafka-4.0.0_(KRaft)-black?logo=apachekafka)](https://kafka.apache.org/)
[![gRPC](https://img.shields.io/badge/gRPC-1.70.0-00B0FF?logo=grpc)](https://grpc.io/)
[![Docker](https://img.shields.io/badge/Docker-Enabled-2496ED?logo=docker)](https://www.docker.com/)
[![LocalStack](https://img.shields.io/badge/AWS-LocalStack-FF9900?logo=amazon-aws)](https://localstack.cloud/)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

A distributed microservice platform for managing patient records. Built with **Spring Boot 3**, **PostgreSQL**, **Apache Kafka (KRaft)**, **gRPC**, **JWT authentication**, and a **Spring Cloud Gateway** — containerized with **Docker** and deployable to **AWS / LocalStack via AWS CDK**.

---

## 📑 Table of Contents

- [Architecture Overview](#-architecture-overview)
- [Tech Stack](#-tech-stack)
- [Service Catalog](#-service-catalog)
- [API Reference](#-api-reference)
- [Data Models](#-data-models)
- [Prerequisites](#-prerequisites)
- [Getting Started](#-getting-started)
  - [Option 1: Docker (Step-by-Step Manual Run)](#option-1-docker-step-by-step-manual-run)
  - [Option 2: LocalStack / AWS CDK Infrastructure](#option-2-localstack--aws-cdk-infrastructure)
  - [Option 3: Local Development (IDE / Maven)](#option-3-local-development-ide--maven)
- [Configuration Reference](#-configuration-reference)
- [Docker Reference & Cheat Sheet](#-docker-reference--cheat-sheet)
- [Kafka Operations](#-kafka-operations)
- [PostgreSQL Operations](#-postgresql-operations)
- [Integration Testing](#-integration-testing)
- [Project Structure](#-project-structure)
- [Security](#-security)
- [Contributing](#-contributing)
- [License](#-license)

---

## 🏛 Architecture Overview

```
┌─────────────┐         ┌──────────────────┐         ┌──────────────────┐
│   Client    │────────▶│   API Gateway    │────────▶│  Auth Service    │
│  (Browser/  │         │   (Port 4004)    │◀────────│  (Port 4005)     │
│   Postman)  │         │                  │  JWT    │                  │
└─────────────┘         │  Spring Cloud    │  Valid. │  Spring Security │
                        │  Gateway         │         │  + JWT (HS256)   │
                        └──┬─────────┬─────┘         └────────┬─────────┘
                           │         │                        │
                    Routing│         │Routing                 │
                           ▼         ▼                        ▼
              ┌──────────────┐  ┌──────────────┐    ┌──────────────────┐
              │  Patient     │  │  Billing     │    │  PostgreSQL      │
              │  Service     │  │  Service     │    │  (Auth DB:5433)  │
              │  (Port 4000) │  │  (Port 4001) │    └──────────────────┘
              │              │  │  (gRPC:9001) │
              │  CRUD +      │──│              │
              │  Kafka Prod. │  │  gRPC Server │
              └──┬───────┬───┘  └──────┬───────┘
                 │       │             │
          ┌──────┘       │             │
          ▼              ▼             ▼
  ┌──────────────┐  ┌────────┐  ┌──────────────────┐
  │ PostgreSQL   │  │ Kafka  │  │  PostgreSQL       │
  │ (Patient     │  │ (9092) │  │  (Billing DB      │
  │  DB: 5432)   │  │ KRaft  │  │   shared: 5432)   │
  └──────────────┘  └───┬────┘  └──────────────────┘
                        │
                        ▼
              ┌──────────────────┐
              │ Analytics Service│
              │ (Port 4002)      │
              │ Kafka Consumer   │
              └──────────────────┘
```

### Request Flow

1. **Client** sends HTTP request to the **API Gateway** (`:4004`).
2. Gateway applies **JWT validation filter** — calls Auth Service to verify the token (except for `/auth/**` routes).
3. On successful validation, the request is **routed** to the appropriate downstream service.
4. **Patient Service** handles CRUD operations, persists data to **PostgreSQL**, publishes events to **Kafka** (`patient` topic), and calls **Billing Service** via **gRPC** to create a billing account on patient creation.
5. **Analytics Service** consumes messages from the Kafka `patient` topic for downstream processing.

---

## 🛠 Tech Stack

| Category | Technology | Version |
|---|---|---|
| **Language** | Java | 17 |
| **Framework** | Spring Boot | 3.4.4 |
| **API Gateway** | Spring Cloud Gateway | 2024.0.1 |
| **Database** | PostgreSQL | 16 |
| **ORM** | Spring Data JPA / Hibernate | — |
| **Messaging** | Apache Kafka (Bitnami / KRaft mode) | 4.0.0 |
| **RPC** | gRPC (Netty Shaded) | 1.70.0 |
| **Serialization** | Protocol Buffers (protobuf) | 3 |
| **Object Mapping** | MapStruct | 1.6.3 |
| **Authentication** | JWT (jjwt by io.jsonwebtoken) | 0.12.6 |
| **Security** | Spring Security | — |
| **Cloud IaC** | AWS CDK (Java) | — |
| **Local Cloud** | LocalStack (AWS in Docker) | Latest |
| **Containerization** | Docker | — |
| **Build Tool** | Apache Maven | 3.9.9 |
| **Runtime** | Eclipse Temurin JDK/JRE | 17 |
| **Integration Testing** | REST Assured | 5.5.1 |

---

## 📦 Service Catalog

### 1. Patient Service (`patient_service`) — Port `4000`

| Property | Value |
|---|---|
| **Database** | PostgreSQL on `:5432` / DB: `db` (or `patient_service`) |
| **Responsibilities** | Patient CRUD, Kafka event publishing, gRPC client to Billing Service |
| **Key Dependencies** | Spring Web, Data JPA, Validation, Actuator, Kafka, gRPC, MapStruct |

### 2. Billing Service (`billing-service`) — Port `4001` (HTTP) + `9001` (gRPC)

| Property | Value |
|---|---|
| **Database** | PostgreSQL on `:5432` / DB: `billing_service` (or `db`) |
| **Responsibilities** | Billing account CRUD, gRPC server for patient-triggered billing |
| **Key Dependencies** | Spring Web, Data JPA, Validation, Actuator, gRPC, MapStruct |

### 3. Analytics Service (`analytics-service`) — Port `4002`

| Property | Value |
|---|---|
| **Database** | None |
| **Responsibilities** | Kafka consumer — consumes `patient` topic events for analytics |
| **Key Dependencies** | Spring Web, Spring Kafka, Actuator |

### 4. Auth Service (`auth-service`) — Port `4005`

| Property | Value |
|---|---|
| **Database** | PostgreSQL on `:5433` (or `:5001`) / DB: `db` (or `auth_service`) |
| **Responsibilities** | User authentication, JWT token generation & validation |
| **Key Dependencies** | Spring Web, Data JPA, Security, Validation, JJWT |

### 5. API Gateway (`api-gateway`) — Port `4004`

| Property | Value |
|---|---|
| **Responsibilities** | Request routing, JWT validation filter, CORS handling |
| **Key Dependencies** | Spring Cloud Gateway, JJWT |
| **Routes** | `/api/patients/**` → Patient Service, `/auth/**` → Auth Service |

### Port Map

| Service | HTTP Port | gRPC Port | DB Port |
|---|---|---|---|
| API Gateway | 4004 | — | — |
| Patient Service | 4000 | — | 5432 / 5000 |
| Billing Service | 4001 | 9001 | 5432 |
| Analytics Service | 4002 | — | — |
| Auth Service | 4005 | — | 5433 / 5001 |
| Kafka | 9092 (internal), 9094 (external) | — | — |

---

## 📡 API Reference

All client requests should go through the **API Gateway** at `http://localhost:4004`.

### Authentication

| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| `POST` | `/auth/login` | Authenticate and get JWT token | ❌ No |
| `GET` | `/auth/validate` | Validate a JWT token | ❌ No |

**Login Request:**
```json
{
  "email": "admin@example.com",
  "password": "your_password"
}
```

**Login Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIs..."
}
```

### Patient Management

> All patient endpoints require a valid JWT token in the `Authorization` header:
> `Authorization: Bearer <token>`

| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| `GET` | `/api/patients` | List all patients | ✅ Yes |
| `POST` | `/api/patients` | Create a new patient | ✅ Yes |
| `PUT` | `/api/patients/{id}` | Update a patient by ID | ✅ Yes |
| `DELETE` | `/api/patients/{id}` | Delete a patient by ID | ✅ Yes |

**Create/Update Patient — Request Body:**
```json
{
  "name": "John Doe",
  "email": "john.doe@example.com",
  "dateOfBirth": "1990-01-15",
  "registeredDate": "2026-11-05",
  "address": {
    "street": "123 Main St",
    "city": "Springfield",
    "state": "IL",
    "zipCode": "62701"
  }
}
```

**Patient Response:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "John Doe",
  "email": "john.doe@example.com",
  "dateOfBirth": "1990-01-15",
  "registeredDate": "2026-11-05",
  "address": {
    "street": "123 Main St",
    "city": "Springfield",
    "state": "IL",
    "zipCode": "62701"
  }
}
```

### Billing Service (Direct Access)

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `http://localhost:4001/billing` | List all billing accounts |
| `POST` | `http://localhost:4001/billing` | Create a billing account |

---

## 🗃 Data Models

### Patient Entity

```
┌──────────────────────────────────────┐
│              Patient                 │
├──────────────────────────────────────┤
│ id            : UUID (PK, Auto)     │
│ name          : String (NOT NULL)   │
│ email         : String (UNIQUE)     │
│ dateOfBirth   : String              │
│ registeredDate: String              │
│ createdAt     : LocalDateTime (Auto)│
│ updatedAt     : LocalDateTime (Auto)│
│ ┌──────────────────────────────────┐ │
│ │      Address (Embedded)          │ │
│ │ street  : String                 │ │
│ │ city    : String                 │ │
│ │ state   : String                 │ │
│ │ zipCode : String                 │ │
│ └──────────────────────────────────┘ │
└──────────────────────────────────────┘
```

### BillingAccount Entity

```
┌──────────────────────────────────────┐
│          BillingAccount              │
├──────────────────────────────────────┤
│ id            : UUID (PK, Auto)     │
│ patientId     : String              │
│ accountNumber : String              │
│ status        : String              │
│ amount        : BigDecimal          │
│ createdAt     : LocalDateTime (Auto)│
│ updatedAt     : LocalDateTime (Auto)│
└──────────────────────────────────────┘
```

### User Entity (Auth Service)

```
┌──────────────────────────────────────┐
│                User                  │
├──────────────────────────────────────┤
│ id       : UUID (PK, Auto)          │
│ email    : String (UNIQUE)          │
│ password : String                   │
│ role     : String (e.g., "ADMIN")   │
└──────────────────────────────────────┘
```

### gRPC — Billing Service Contract

```protobuf
service BillingService {
  rpc createBillingAccount(BillingRequest) returns (BillingResponse);
}

message BillingRequest {
  string patientId = 1;
  string name      = 2;
  string email     = 3;
}

message BillingResponse {
  string billingId  = 1;
  string patientId  = 2;
  string status     = 3;
}
```

---

## ✅ Prerequisites

| Tool | Minimum Version | Purpose |
|---|---|---|
| **Java JDK** | 17 | Building services |
| **Maven** | 3.9+ | Dependency management & packaging |
| **Docker** | 20.10+ | Containerization |
| **Git** | 2.30+ | Version control |

---

## 🚀 Getting Started

### Option 1: Docker (Step-by-Step Manual Run)

Follow these steps to run all databases, messaging, and services using Docker.

#### 1. Pull Base Images

```bash
docker pull postgres:latest
docker pull bitnamilegacy/kafka:4.0.0-debian-12-r10
docker pull localstack/localstack:latest
```

#### 2. Tag Images (Optional)

```bash
docker tag postgres:latest patient-service-db:latest
docker tag bitnamilegacy/kafka:4.0.0-debian-12-r10 kafka:latest
```

#### 3. Create Docker Network

```bash
docker network create internal
```

#### 4. Run Databases

**Patient Service DB:**
```bash
docker run -d --name patient-service-db-container --network internal \
  -p 5000:5432 \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=db \
  -v patient-db-data:/var/lib/postgresql/data \
  postgres:latest
```

**Auth Service DB:**
```bash
docker run -d --name auth-service-db-container --network internal \
  -p 5001:5432 \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=db \
  -v auth-db-data:/var/lib/postgresql/data \
  postgres:latest
```

#### 5. Run Kafka (KRaft Mode)

```bash
docker run -d \
  --name kafka-container \
  --network internal \
  -p 9092:9092 \
  -p 9094:9094 \
  -e KAFKA_CFG_NODE_ID=0 \
  -e KAFKA_CFG_PROCESS_ROLES=controller,broker \
  -e KAFKA_CFG_LISTENERS=PLAINTEXT://:9092,CONTROLLER://:9093,EXTERNAL://:9094 \
  -e KAFKA_CFG_ADVERTISED_LISTENERS=PLAINTEXT://kafka-container:9092,EXTERNAL://localhost:9094 \
  -e KAFKA_CFG_CONTROLLER_LISTENER_NAMES=CONTROLLER \
  -e KAFKA_CFG_CONTROLLER_QUORUM_VOTERS=0@kafka-container:9093 \
  -e KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,EXTERNAL:PLAINTEXT,PLAINTEXT:PLAINTEXT \
  kafka:latest
```

#### 6. Package and Build Service Images

In each service directory, compile the JAR:
```bash
cd patient_service && mvn clean package -DskipTests && cd ..
cd billing-service && mvn clean package -DskipTests && cd ..
cd analytics-service && mvn clean package -DskipTests && cd ..
cd auth-service && mvn clean package -DskipTests && cd ..
cd api-gateway && mvn clean package -DskipTests && cd ..
```

Build Docker images from the root:
```bash
docker build -t patient-service:latest ./patient_service
docker build -t billing-service:latest ./billing-service
docker build -t analytics-service:latest ./analytics-service
docker build -t auth-service:latest ./auth-service
docker build -t api-gateway:latest ./api-gateway
```

#### 7. Run Service Containers

**Auth Service:**
```bash
docker run -d --name auth-service --network internal \
  -p 4005:4005 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://auth-service-db-container:5432/db \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=postgres \
  -e SPRING_JPA_HIBERNATE_DDL_AUTO=update \
  -e SPRING_SQL_INIT_MODE=always \
  -e SPRING_JPA_DEFER_DATASOURCE_INITIALIZATION=true \
  -e JWT_SECRET=c2VjcmV0S2V5Rm9ySldUVG9rZW5HZW5lcmF0aW9uMTIzNDU2Nzg5MDEyMzQ1Njc4OTA= \
  auth-service:latest
```

**Billing Service:**
```bash
docker run -d --name billing-service-container --network internal \
  -p 4001:4001 \
  -p 9001:9001 \
  billing-service:latest
```

**Patient Service:**
```bash
docker run -d --name patient-service-container --network internal \
  -p 4000:4000 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://patient-service-db-container:5432/db \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=postgres \
  -e SPRING_JPA_HIBERNATE_DDL_AUTO=update \
  -e SPRING_SQL_INIT_MODE=always \
  -e BILLING_SERVICE_ADDRESS=billing-service-container \
  -e BILLING_SERVICE_GRPC_PORT=9001 \
  patient-service:latest
```

**Analytics Service:**
```bash
docker run -d --name analytics-service-container --network internal \
  -p 4002:4002 \
  -e SERVER_PORT=4002 \
  -e SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka-container:9092 \
  analytics-service:latest
```

**API Gateway:**
```bash
docker run -d --name api-gateway --network internal \
  -p 4004:4004 \
  -e AUTH_SERVICE_URL=http://auth-service:4005 \
  api-gateway:latest
```

---

### Option 2: LocalStack / AWS CDK Infrastructure

The `infrastructure/` directory contains an **AWS CDK** stack written in Java (`LocalStack.java`) that provisions cloud resources locally using LocalStack:
- **VPC** with public & isolated subnets
- **RDS PostgreSQL** instances (`AuthServiceDb`, `PatientServiceDb`)
- **Amazon MSK** (Kafka Cluster)
- **ECS Fargate Services** for all 5 microservices
- **Route53 Health Checks**

To deploy via LocalStack:
```bash
# 1. Start LocalStack
docker run -d --name localstack -p 4566:4566 localstack/localstack:latest

# 2. Deploy using the provided script
cd infrastructure
./localstack-deploy.sh
```

---

### Option 3: Local Development (IDE / Maven)

Run each service directly on your host machine:

```bash
# Terminal 1 — Auth Service
cd auth-service && mvn spring-boot:run

# Terminal 2 — Billing Service
cd billing-service && mvn spring-boot:run

# Terminal 3 — Patient Service
cd patient_service && mvn spring-boot:run

# Terminal 4 — Analytics Service
cd analytics-service && mvn spring-boot:run

# Terminal 5 — API Gateway
cd api-gateway && mvn spring-boot:run
```

---

## ⚙ Configuration Reference

### Patient Service (`patient_service/src/main/resources/application.properties`)

| Property | Default Value | Description |
|---|---|---|
| `server.port` | `4000` | HTTP server port |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/patient_service` | PostgreSQL JDBC URL |
| `spring.datasource.username` | `admin_user` | Database username |
| `spring.datasource.password` | `admin_password` | Database password |
| `spring.jpa.hibernate.ddl-auto` | `update` | Auto schema migration |
| `spring.kafka.bootstrap-servers` | `localhost:9092` | Kafka broker address |
| `billing.service.address` | `localhost` | gRPC billing service host |
| `billing.service.grpc.port` | `9001` | gRPC billing service port |

### Billing Service (`billing-service/src/main/resources/application.properties`)

| Property | Default Value | Description |
|---|---|---|
| `server.port` | `4001` | HTTP server port |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/billing_service` | PostgreSQL JDBC URL |
| `grpc.server.port` | `9001` | gRPC server port |

### Analytics Service (`analytics-service/src/main/resources/application.properties`)

| Property | Default Value | Description |
|---|---|---|
| `server.port` | `4002` | HTTP server port |
| `spring.kafka.bootstrap-servers` | `localhost:9092` | Kafka broker address |
| `spring.kafka.consumer.group-id` | `analytics-service-group` | Kafka consumer group |

### Auth Service (`auth-service/src/main/resources/application.properties`)

| Property | Default Value | Description |
|---|---|---|
| `server.port` | `4005` | HTTP server port |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5433/auth_service` | PostgreSQL JDBC URL |

### API Gateway (`api-gateway/src/main/resources/application.yml`)

| Property | Default Value | Description |
|---|---|---|
| `server.port` | `4004` | Gateway port |
| `auth.service.url` | `http://localhost:4005` | Auth service URL for JWT validation |
| Route: `/api/patients/**` | `http://localhost:4000` | Forwards to Patient Service |
| Route: `/auth/**` | `http://localhost:4005` | Forwards to Auth Service |

---

## 🐳 Docker Reference & Cheat Sheet

```bash
# ── Images ──
docker images                           # List all images
docker build -t <name>:latest .         # Build image (run from Dockerfile dir)
docker rmi <image>                      # Remove an image

# ── Containers ──
docker ps                               # Running containers
docker ps -a                            # All containers (including stopped)
docker start <container>                # Start a stopped container
docker stop <container>                 # Stop a running container
docker restart <container>              # Restart a container
docker rm <container>                   # Remove a container
docker rm -f <container>                # Force remove (even if running)

# ── Logs ──
docker logs <container>                 # View container logs
docker logs -f <container>              # Follow/stream logs
docker logs --tail 100 <container>      # Last 100 lines

# ── Exec & Network ──
docker exec -it <container> bash        # Shell into container
docker network create internal          # Create bridge network
docker network ls                       # List networks
docker network inspect internal         # Inspect network details
```

---

## 📨 Kafka Operations

### Access Kafka Container

```bash
docker exec -it kafka-container bash
```

### Manage Topics

```bash
# List topics
kafka-topics.sh --bootstrap-server localhost:9092 --list

# Create patient topic
kafka-topics.sh --bootstrap-server localhost:9092 --create --topic patient

# Describe topic
kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic patient
```

### Console Producer & Consumer

```bash
# Read messages from beginning
kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic patient --from-beginning

# Produce messages manually
kafka-console-producer.sh --bootstrap-server localhost:9092 --topic patient
```

---

## 🐘 PostgreSQL Operations

### Access Databases

```bash
# Patient DB
docker exec -it patient-service-db-container psql -U postgres -d db

# Auth DB
docker exec -it auth-service-db-container psql -U postgres -d db
```

### Common Commands

```sql
\dt                        -- List tables
\d patient                 -- Describe table
SELECT * FROM patient;     -- View records
\q                         -- Exit
```

---

## 🧪 Integration Testing

Integration tests use **REST Assured** in `integration-tests/`:

```bash
cd integration-tests
mvn test
```

Tests run against the API Gateway at `http://localhost:4004` to verify the authentication handshake and patient CRUD lifecycles.

---

## 📁 Project Structure

```
Patient_Management/
├── README.md                           # Documentation
│
├── api-gateway/                        # Spring Cloud Gateway (Port 4004)
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/pm/api_gateway/
│       └── resources/application.yml
│
├── auth-service/                       # Authentication & JWT (Port 4005)
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/pm/auth_service/
│       └── resources/application.properties
│
├── patient_service/                    # Core Patient CRUD (Port 4000)
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/pm/patient_service/
│       ├── proto/billing.proto
│       └── resources/application.properties
│
├── billing-service/                    # Billing & gRPC Server (Port 4001 / 9001)
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/pm/billing_service/
│       ├── proto/billing.proto
│       └── resources/application.properties
│
├── analytics-service/                  # Kafka Consumer (Port 4002)
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/pm/analytics_service/
│       └── resources/application.properties
│
├── integration-tests/                  # REST Assured Integration Tests
│   ├── pom.xml
│   └── src/test/java/com/pm/
│
├── infrastructure/                     # AWS CDK / LocalStack IaC
│   ├── pom.xml
│   ├── localstack-deploy.sh
│   └── src/main/java/com/pm/stack/LocalStack.java
│
├── api-requests/                       # HTTP request files (.http)
└── grpc-requests/                      # gRPC request files
```

---

## 🔒 Security

### JWT Authentication Flow

1. Client sends `POST /auth/login` with email and password.
2. Auth Service validates credentials against the database.
3. On success, a **JWT token** is returned (HS256, 30-minute expiry).
4. Client includes the token in subsequent requests: `Authorization: Bearer <token>`.
5. API Gateway intercepts the request, extracts the token, and calls Auth Service's `/auth/validate`.
6. On successful validation, the request is forwarded to the downstream service.

---

## 🤝 Contributing

1. **Fork** the repository
2. Create your feature branch: `git checkout -b feature/my-feature`
3. Commit your changes: `git commit -m 'Add my feature'`
4. Push to the branch: `git push origin feature/my-feature`
5. Open a **Pull Request**

---

## 📄 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

---

<p align="center">
  Built with ❤️ using Spring Boot, Kafka, gRPC & Docker
</p>
