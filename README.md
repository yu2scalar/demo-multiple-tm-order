# Demo Multiple Transaction Manager - Order Service

> A Spring Boot microservice demonstrating distributed Two-Phase Commit (2PC) transactions using ScalarDB

## Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
- [API Documentation](#api-documentation)
- [Usage Examples](#usage-examples)
- [Development](#development)
- [Testing](#testing)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)

## Overview

This application demonstrates how to implement **distributed Two-Phase Commit (2PC) transactions** across multiple microservices using ScalarDB. It functions as both:

- **Participant Service**: Handles order operations within distributed transactions
- **Coordinator (BFF)**: Orchestrates distributed transactions across Order and Inventory services

### What is Two-Phase Commit?

2PC is a distributed algorithm that ensures all participants in a distributed transaction either commit or rollback together, maintaining ACID properties across multiple databases/services.

## Key Features

- **Distributed Transactions**: Atomic operations spanning multiple microservices
- **Transaction ID Propagation**: Seamless transaction context sharing via HTTP headers
- **Dual Transaction Support**: Standard ScalarDB transactions and 2PC transactions
- **RESTful API**: Complete CRUD operations with 2PC lifecycle management
- **Automatic Rollback**: Coordinated rollback across all services on failure
- **Interactive API Docs**: Built-in Swagger/OpenAPI documentation
- **Error Handling**: Comprehensive error codes and exception handling

## Architecture

### System Components

```
┌─────────────────────────────────────────────────────────────┐
│                    Client Application                        │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│              BFF Service (Coordinator)                       │
│  - Starts 2PC transaction                                    │
│  - Propagates transaction ID                                 │
│  - Orchestrates prepare/commit/rollback                      │
└──────────────┬───────────────────────┬──────────────────────┘
               │                       │
               ▼                       ▼
┌──────────────────────┐  ┌──────────────────────────────────┐
│  Order Service       │  │  Inventory Service               │
│  (Port 8080)         │  │  (Port 8081)                     │
│  - Join transaction  │  │  - Join transaction              │
│  - Manage orders     │  │  - Manage inventory              │
│  - Prepare/Commit    │  │  - Prepare/Commit                │
└──────────┬───────────┘  └───────────┬──────────────────────┘
           │                          │
           ▼                          ▼
      ┌────────────────────────────────────┐
      │        ScalarDB Cluster            │
      └────────────────────────────────────┘
```

### Transaction Flow

```
1. BFF starts transaction → generates transaction ID
2. BFF calls Order Service (passes transaction ID via header)
3. BFF calls Inventory Service (passes transaction ID via header)
4. BFF calls prepare on all services
5. BFF calls commit on all services (or rollback on error)
```

## Prerequisites

| Requirement | Version |
|------------|---------|
| Java | 17+ |
| Gradle | 7.x+ (wrapper included) |
| ScalarDB Cluster | 3.16.1+ |
| Inventory Service | Running on port 8081 |

## Quick Start

### 1. Clone and Navigate

```bash
git clone <repository-url>
cd demo-multiple-tm-order
```

### 2. Configure ScalarDB Connection

Edit `scalardb.properties`:

```properties
scalar.db.cluster.auth.enabled=true
scalar.db.contact_points=indirect:your-scalardb-cluster.com
scalar.db.password=your-password
scalar.db.transaction_manager=cluster
scalar.db.username=your-username
```

Edit `scalardb_sql.properties`:

```properties
scalar.db.sql.cluster_mode.username=your-username
scalar.db.cluster.tls.enabled=false
scalar.db.sql.connection_mode=cluster
scalar.db.sql.cluster_mode.contact_points=indirect:your-scalardb-cluster.com
scalar.db.sql.cluster_mode.password=your-password
scalar.db.cluster.auth.enabled=true
```

### 3. Build

```bash
./gradlew clean build
```

### 4. Run

```bash
./gradlew bootRun
```

Application starts at: **http://localhost:8080**

## Configuration

### Application Properties

`src/main/resources/application.properties`:

```properties
spring.application.name=demo-multiple-tm-order
scalardb.config.file=scalardb.properties
server.port=8080
```

### Environment Variables

You can override configuration with environment variables:

```bash
export SERVER_PORT=8090
export SCALARDB_CONFIG_FILE=/path/to/custom/scalardb.properties
./gradlew bootRun
```

### Multi-Service Setup

| Service | Port | Configuration Location |
|---------|------|------------------------|
| Order Service | 8080 | `application.properties` |
| Inventory Service | 8081 | `PlaceOrderTwoPCBffService.java` |

To change service URLs, edit `PlaceOrderTwoPCBffService`:

```java
private static final String BASE_URL4ORDER = "http://localhost:8080";
private static final String BASE_URL4INVENTORY = "http://localhost:8081";
```

## API Documentation

### Interactive Documentation

Once running, access:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8080/v3/api-docs

### Key Endpoints

#### 2PC Participant Endpoints

```
POST   /order-two-pc              # Create order (within transaction)
GET    /order-two-pc/{id}         # Get order
PUT    /order-two-pc              # Update order
DELETE /order-two-pc/{id}         # Delete order

GET    /order-two-pc/prepare      # Prepare transaction
GET    /order-two-pc/validate     # Validate transaction
GET    /order-two-pc/commit       # Commit transaction
GET    /order-two-pc/rollback     # Rollback transaction
```

#### BFF Coordinator Endpoints

```
POST   /place-order-two-pc-bff    # Place order (orchestrates distributed transaction)
```

## Usage Examples

### Example 1: Distributed Order Placement (Recommended)

This is the typical use case - a single API call that handles the entire distributed transaction:

```bash
curl -X POST http://localhost:8080/place-order-two-pc-bff \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ORD-2025-001",
    "customerId": "CUST-12345",
    "productId": "PROD-789",
    "orderQty": 5
  }'
```

**What happens internally:**
1. Starts 2PC transaction
2. Checks inventory availability
3. Deducts stock from inventory
4. Creates order record
5. Prepares both services
6. Commits both services atomically

**Success Response:**
```json
{
  "success": true,
  "data": null,
  "message": "",
  "errorCode": null
}
```

**Error Response (e.g., out of stock):**
```json
{
  "success": false,
  "data": null,
  "message": "We are out of stock.",
  "errorCode": 9400
}
```

### Example 2: Manual 2PC Operations (Advanced)

For custom orchestration, you can manually control the 2PC lifecycle:

#### Step 1: Start Transaction (in your coordinator)

```java
TwoPhaseCommitTransaction transaction = manager.start();
String transactionId = transaction.getId();
```

#### Step 2: Perform Operations

```bash
# Create order
curl -X POST http://localhost:8080/order-two-pc \
  -H "Content-Type: application/json" \
  -H "ScalarDB-Transaction-ID: <transaction-id>" \
  -d '{
    "orderId": "ORD-2025-002",
    "customerId": "CUST-12345",
    "productId": "PROD-789",
    "orderQty": 3
  }'
```

#### Step 3: Prepare Phase

```bash
curl -X GET http://localhost:8080/order-two-pc/prepare \
  -H "ScalarDB-Transaction-ID: <transaction-id>"
```

#### Step 4: Commit or Rollback

**Commit:**
```bash
curl -X GET http://localhost:8080/order-two-pc/commit \
  -H "ScalarDB-Transaction-ID: <transaction-id>"
```

**Rollback:**
```bash
curl -X GET http://localhost:8080/order-two-pc/rollback \
  -H "ScalarDB-Transaction-ID: <transaction-id>"
```

### Example 3: Query Operations

```bash
# Get specific order
curl -X GET http://localhost:8080/order-two-pc/ORD-2025-001 \
  -H "ScalarDB-Transaction-ID: <transaction-id>"

# Get all orders
curl -X GET http://localhost:8080/order-two-pc/all \
  -H "ScalarDB-Transaction-ID: <transaction-id>"
```

## Development

### Project Structure

```
demo-multiple-tm-order/
├── src/
│   ├── main/
│   │   ├── java/com/example/demo_multiple_tm_order/
│   │   │   ├── config/              # Configuration beans
│   │   │   │   ├── ScalarDbConfig.java
│   │   │   │   ├── ScalarDbTwoPCConfig.java
│   │   │   │   └── RestTemplateConfig.java
│   │   │   ├── controller/          # REST endpoints
│   │   │   │   ├── BaseTwoPCController.java
│   │   │   │   ├── OrderTwoPCController.java
│   │   │   │   └── PlaceOrderTwoPCBffController.java
│   │   │   ├── service/             # Business logic
│   │   │   │   ├── BaseTwoPCService.java
│   │   │   │   ├── BaseTwoPCBffService.java
│   │   │   │   ├── OrderTwoPCService.java
│   │   │   │   └── PlaceOrderTwoPCBffService.java
│   │   │   ├── repository/          # Data access
│   │   │   ├── dto/                 # Data transfer objects
│   │   │   ├── model/               # Domain entities
│   │   │   ├── mapper/              # DTO-Model mapping
│   │   │   ├── util/                # Utilities
│   │   │   └── exception/           # Custom exceptions
│   │   └── resources/
│   │       ├── application.properties
│   │       └── static/
│   └── test/
├── scalardb.properties
├── scalardb_sql.properties
├── build.gradle
├── settings.gradle
├── CLAUDE.md                         # Architecture guide for AI
└── README.md                         # This file
```

### Building

```bash
# Clean build
./gradlew clean build

# Build without tests
./gradlew build -x test

# Create executable JAR
./gradlew bootJar

# Build output location
ls -la build/libs/demo-multiple-tm-order-0.0.1-SNAPSHOT.jar
```

### Running

```bash
# Run with Gradle
./gradlew bootRun

# Run with custom port
./gradlew bootRun --args='--server.port=8090'

# Run JAR directly
java -jar build/libs/demo-multiple-tm-order-0.0.1-SNAPSHOT.jar

# Run with custom config
java -jar build/libs/demo-multiple-tm-order-0.0.1-SNAPSHOT.jar \
  --scalardb.config.file=/path/to/custom/scalardb.properties
```

### Technology Stack

| Technology | Version | Purpose |
|-----------|---------|---------|
| Spring Boot | 3.5.6 | Application framework |
| Java | 17 | Programming language |
| ScalarDB | 3.16.1 | Distributed transaction manager |
| ScalarDB Cluster SDK | 3.16.1 | Cluster client library |
| Gradle | 8.x | Build automation |
| Lombok | Latest | Reduce boilerplate |
| SpringDoc OpenAPI | 2.3.0 | API documentation |
| ModelMapper | 3.2.2 | Object mapping |
| Apache Commons Text | 1.13.0 | Text utilities |

### Adding New 2PC Entities

To add a new entity (e.g., `Payment`) that participates in 2PC:

1. **Create Model**: `model/Payment.java`
2. **Create Repository**: `repository/PaymentTwoPCRepository.java`
3. **Create Service** extending `BaseTwoPCService`:
   ```java
   @Service
   public class PaymentTwoPCService extends BaseTwoPCService {
       public PaymentTwoPCService(TwoPhaseCommitTransactionManager manager) {
           super(manager);
       }
       // Add CRUD methods using manager.join(transactionId)
   }
   ```
4. **Create Controller** extending `BaseTwoPCController`:
   ```java
   @RestController
   @RequestMapping("/payment-two-pc")
   public class PaymentTwoPCController extends BaseTwoPCController {
       @Autowired
       private PaymentTwoPCService service;

       @Override
       protected BaseTwoPCService getService() {
           return service;
       }
       // Add CRUD endpoints
   }
   ```
5. **Update BFF Service** to include payment operations in distributed transactions

## Testing

### Run All Tests

```bash
./gradlew test
```

### Run Specific Test

```bash
./gradlew test --tests "DemoMultipleTmOrderApplicationTests"
```

### Test with Coverage

```bash
./gradlew test jacocoTestReport
open build/reports/jacoco/test/html/index.html
```

### Integration Testing

For testing distributed transactions, you'll need:
1. ScalarDB cluster running
2. Inventory service running on port 8081
3. Order service running on port 8080

```bash
# Terminal 1: Start inventory service
cd ../inventory-service && ./gradlew bootRun

# Terminal 2: Start order service
./gradlew bootRun

# Terminal 3: Run integration tests
curl -X POST http://localhost:8080/place-order-two-pc-bff \
  -H "Content-Type: application/json" \
  -d '{"orderId":"TEST-001","customerId":"CUST-001","productId":"PROD-001","orderQty":1}'
```

## Troubleshooting

### Connection Issues

**Problem**: Cannot connect to ScalarDB cluster

**Solutions**:
- Verify `scalardb.properties` has correct `contact_points`
- Check network connectivity: `ping order.ms.example.com`
- Verify credentials are correct
- Check if authentication is enabled: `scalar.db.cluster.auth.enabled=true`
- Review logs for connection errors

### Transaction Failures

**Problem**: Transactions fail or timeout

**Solutions**:
- Check error code in response (see [Error Codes](#error-codes))
- Verify all participant services are running and healthy
- Check logs for specific exception messages
- Ensure transaction IDs are properly propagated in headers
- Review ScalarDB cluster status

### Out of Stock Errors

**Problem**: `"We are out of stock"` error

**This is expected behavior!** The system correctly:
1. Checks inventory before creating order
2. Returns error if insufficient stock
3. Rolls back the transaction automatically

**To test successfully**:
- Ensure product exists in inventory
- Verify product has sufficient stock
- Check inventory service logs

### Port Conflicts

**Problem**: Port 8080 already in use

**Solutions**:
```bash
# Option 1: Use different port
./gradlew bootRun --args='--server.port=8090'

# Option 2: Stop process using port 8080
lsof -ti:8080 | xargs kill -9

# Option 3: Change in application.properties
echo "server.port=8090" >> src/main/resources/application.properties
```

### Build Failures

**Problem**: Build fails with dependency issues

**Solutions**:
```bash
# Clear Gradle cache
rm -rf ~/.gradle/caches/

# Rebuild
./gradlew clean build --refresh-dependencies

# Check Java version
java -version  # Should be 17+
```

## Error Codes

| Code | Exception Type | Description | Action |
|------|---------------|-------------|--------|
| 9100 | `UnsatisfiedConditionException` | Condition not met (e.g., insufficient stock, optimistic lock failure) | Check business logic conditions |
| 9200 | `UnknownTransactionStatusException` | Transaction status unclear | Check ScalarDB cluster status |
| 9300 | `TransactionException` | General transaction error | Review transaction logs |
| 9400 | `RuntimeException` | Runtime error (e.g., out of stock) | Check application logic |
| 9500 | Other | Unexpected error | Check application logs |

## Contributing

When adding new features:

1. **Follow 2PC Patterns**: Use the established BFF → Service → Controller architecture
2. **Extend Base Classes**: Inherit from `BaseTwoPCService` and `BaseTwoPCController`
3. **Consistent Error Handling**: Use error codes 9100-9500 consistently
4. **API Documentation**: Add Swagger/OpenAPI annotations to new endpoints
5. **Write Tests**: Include unit and integration tests
6. **Update Documentation**: Keep README.md and CLAUDE.md current

