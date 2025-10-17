# Demo Multiple Transaction Manager Order Service

A Spring Boot microservice demonstrating **distributed Two-Phase Commit (2PC) transactions** using ScalarDB for order management in a distributed system.

## Overview

This application showcases how to implement distributed transactions across multiple microservices using ScalarDB's Two-Phase Commit protocol. It serves as both a participant service (Order Service) and a coordinator (BFF - Backend For Frontend) in a distributed transaction system.

### Key Features

- **Distributed 2PC Transactions**: Coordinate atomic operations across multiple microservices
- **Transaction ID Propagation**: Pass transaction context between services via HTTP headers
- **Dual Transaction Support**: Both standard transactions and 2PC transactions
- **RESTful API**: Complete CRUD operations with 2PC lifecycle management
- **Error Recovery**: Automatic rollback coordination on failure
- **API Documentation**: Built-in Swagger/OpenAPI documentation

## Prerequisites

- **Java 17** or higher
- **Gradle** (wrapper included)
- **ScalarDB Cluster** (configured endpoint)
- **Inventory Service** running on port 8081 (for distributed transaction demos)

## Getting Started

### 1. Clone the Repository

```bash
git clone <repository-url>
cd demo-multiple-tm-order
```

### 2. Configure ScalarDB

Update the ScalarDB configuration files with your cluster details:

**`scalardb.properties`**:
```properties
scalar.db.cluster.auth.enabled=true
scalar.db.contact_points=indirect:order.ms.example.com
scalar.db.password=admin
scalar.db.transaction_manager=cluster
scalar.db.username=admin
```

**`scalardb_sql.properties`**:
```properties
scalar.db.sql.cluster_mode.username=admin
scalar.db.cluster.tls.enabled=false
scalar.db.sql.connection_mode=cluster
scalar.db.sql.cluster_mode.contact_points=indirect:order.ms.example.com
scalar.db.sql.cluster_mode.password=admin
scalar.db.cluster.auth.enabled=true
```

### 3. Build the Application

```bash
./gradlew build
```

### 4. Run the Application

```bash
./gradlew bootRun
```

The application will start on **port 8080** by default.

## API Documentation

Once the application is running, access the interactive API documentation:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8080/v3/api-docs

## Usage Examples

### Standard Order Operations

#### Create an Order (2PC Participant)

```bash
curl -X POST http://localhost:8080/order-two-pc \
  -H "Content-Type: application/json" \
  -H "ScalarDB-Transaction-ID: <transaction-id>" \
  -d '{
    "orderId": "ORDER001",
    "customerId": "CUST001",
    "productId": "PROD001",
    "orderQty": 5
  }'
```

#### Prepare Transaction

```bash
curl -X GET http://localhost:8080/order-two-pc/prepare \
  -H "ScalarDB-Transaction-ID: <transaction-id>"
```

#### Commit Transaction

```bash
curl -X GET http://localhost:8080/order-two-pc/commit \
  -H "ScalarDB-Transaction-ID: <transaction-id>"
```

#### Rollback Transaction

```bash
curl -X GET http://localhost:8080/order-two-pc/rollback \
  -H "ScalarDB-Transaction-ID: <transaction-id>"
```

### Distributed Transaction (BFF Coordinator)

#### Place Order with Inventory Check

The BFF endpoint orchestrates a distributed transaction across Order and Inventory services:

```bash
curl -X POST http://localhost:8080/place-order-two-pc-bff \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ORDER001",
    "customerId": "CUST001",
    "productId": "PROD001",
    "orderQty": 3
  }'
```

This single request will:
1. Start a 2PC transaction
2. Check product inventory (Inventory Service)
3. Deduct stock if available
4. Create the order (Order Service)
5. Coordinate prepare and commit across both services
6. Rollback all services on any failure

## Two-Phase Commit Flow

```
Client → BFF Service → [Order Service, Inventory Service]
                ↓
         1. Start Transaction
                ↓
         2. Business Operations
            - Check inventory
            - Update stock
            - Create order
                ↓
         3. Prepare Phase
            - Lock resources
            - Validate changes
                ↓
         4. Commit/Rollback Phase
            - Atomic commit on success
            - Coordinated rollback on failure
```

## Project Structure

```
src/main/java/com/example/demo_multiple_tm_order/
├── config/              # ScalarDB and Spring configuration
├── controller/          # REST API endpoints
│   ├── BaseTwoPCController.java          # 2PC lifecycle endpoints
│   ├── OrderTwoPCController.java         # Order participant endpoints
│   └── PlaceOrderTwoPCBffController.java # BFF coordinator endpoints
├── service/             # Business logic
│   ├── BaseTwoPCService.java             # Base 2PC operations
│   ├── BaseTwoPCBffService.java          # Base BFF coordination
│   ├── OrderTwoPCService.java            # Order service logic
│   └── PlaceOrderTwoPCBffService.java    # Place order orchestration
├── repository/          # Data access layer
├── dto/                 # Data transfer objects
├── model/               # Domain models
├── mapper/              # Object mapping utilities
├── util/                # Helper utilities
└── exception/           # Custom exceptions
```

## Technology Stack

- **Spring Boot 3.5.6** - Application framework
- **Java 17** - Programming language
- **ScalarDB 3.16.1** - Distributed transaction management
- **ScalarDB Cluster Java Client SDK 3.16.1** - Client library
- **Gradle** - Build tool
- **Lombok** - Boilerplate reduction
- **SpringDoc OpenAPI** - API documentation
- **ModelMapper** - Object mapping

## Multi-Service Architecture

This service is designed to work in a microservices architecture:

| Service | Port | Role |
|---------|------|------|
| Order Service | 8080 | This service - manages orders |
| Inventory Service | 8081 | Manages product inventory |

Both services participate in distributed 2PC transactions coordinated by BFF services.

## Error Codes

| Code | Description |
|------|-------------|
| 9100 | Unsatisfied condition (e.g., optimistic lock failure, insufficient stock) |
| 9200 | Unknown transaction status |
| 9300 | Transaction exception |
| 9400 | Runtime exception |
| 9500 | Other exceptions |

## Running Tests

```bash
# Run all tests
./gradlew test

# Run a specific test class
./gradlew test --tests "DemoMultipleTmOrderApplicationTests"

# Run tests with detailed output
./gradlew test --info
```

## Development

### Building

```bash
# Clean and build
./gradlew clean build

# Build without tests
./gradlew build -x test

# Create bootable JAR
./gradlew bootJar
```

### Running Locally

```bash
# Run with Gradle
./gradlew bootRun

# Run the JAR directly
java -jar build/libs/demo-multiple-tm-order-0.0.1-SNAPSHOT.jar
```

## Configuration

### Application Properties

Edit `src/main/resources/application.properties`:

```properties
spring.application.name=demo-multiple-tm-order
scalardb.config.file=scalardb.properties
```

### Changing Ports

To run on a different port:

```bash
./gradlew bootRun --args='--server.port=8082'
```

Or set in `application.properties`:
```properties
server.port=8082
```

## Troubleshooting

### Connection Issues

If you cannot connect to ScalarDB:
1. Verify `scalardb.properties` has correct contact points
2. Check network connectivity to ScalarDB cluster
3. Verify authentication credentials

### Transaction Failures

If transactions fail:
1. Check logs for specific error codes
2. Verify all participant services are running
3. Ensure transaction IDs are being propagated correctly
4. Check for resource conflicts or locking issues

### Port Conflicts

If port 8080 is already in use:
```bash
./gradlew bootRun --args='--server.port=8090'
```

## Contributing

When adding new features:
1. Follow the established 2PC patterns (BFF → Service → Controller)
2. Extend appropriate base classes
3. Maintain consistent error handling
4. Add API documentation annotations
5. Write tests for new functionality

