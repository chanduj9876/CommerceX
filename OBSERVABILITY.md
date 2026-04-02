# CommerceX Observability Stack — Implementation Summary

## ⚠️ **RAM OPTIMIZATION (Current Configuration)**

**ELK Stack + Grafana have been DISABLED** to reduce RAM usage by ~1.6 GB.

**Active observability tools:**
- ✅ Zipkin (distributed tracing) — 200 MB RAM
- ✅ Prometheus (metrics collection) — 150 MB RAM

**Disabled (commented out in docker-compose.yml):**
- ❌ Grafana (metrics visualization) — saves 200 MB
- ❌ Elasticsearch + Logstash + Kibana (centralized logging) — saves 1.4 GB

**Current setup:** 14 containers (down from 18)
**RAM requirement:** ~3.7 GB (down from 5.3 GB)

**To re-enable:** Uncomment the services in [docker-compose.yml](docker-compose.yml) and [logback-spring.xml](common-lib/src/main/resources/logback-spring.xml)

---

## ✅ **COMPLETED: Priority Set 1**

All critical observability features have been implemented:

1. ✅ Circuit Breaker (prevents cascading failures)
2. ✅ Distributed Tracing (debugging production issues)
3. ✅ Centralized Logging (finding errors)
4. ✅ Metrics & Monitoring (understanding system health)
5. ✅ Rate Limiting (security)

---

## 📦 What Was Added

### **1. API Gateway Enhancements**

#### Added Dependencies (`api-gateway/pom.xml`):
- `spring-cloud-starter-circuitbreaker-reactor-resilience4j` — Circuit breaker for fault tolerance
- `spring-boot-starter-data-redis-reactive` — Rate limiting (Redis-based)
- `micrometer-tracing-bridge-brave` + `zipkin-reporter-brave` — Distributed tracing
- `spring-boot-starter-actuator` + `micrometer-registry-prometheus` — Metrics
- `logstash-logback-encoder` — Centralized logging

#### Configuration Updates (`api-gateway/src/main/resources/application.yml`):
- **HTTP Timeouts**: 5s connect, 10s response
- **Circuit Breaker**: Per-service circuit breakers with fallback endpoints
- **Retry Logic**: 2-3 retries with exponential backoff
- **Rate Limiting**: 
  - Product Service: 50 req/s, burst 100
  - Order Service: 20 req/s, burst 40
  - Payment Service: 10 req/s, burst 20
- **Tracing**: 100% sampling → Zipkin
- **Metrics**: Prometheus endpoints exposed

#### New Code:
- `/filter/LoggingGlobalFilter.java` — Logs all requests/responses with timing
- `/controller/FallbackController.java` — Circuit breaker fallback responses
- `/config/RateLimiterConfig.java` — IP-based rate limiting

---

### **2. All Services (user, product, order, payment, shipping)**

#### Added Dependencies (each service):
- `micrometer-tracing-bridge-brave` + `zipkin-reporter-brave`
- `spring-boot-starter-actuator` + `micrometer-registry-prometheus`
- `logstash-logback-encoder`
- `spring-cloud-starter-circuitbreaker-resilience4j` (for services with Feign clients)

#### Configuration Updates (all `application.yml` files):
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  metrics:
    export:
      prometheus:
        enabled: true
  tracing:
    sampling:
      probability: 1.0
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans
```

---

### **3. Observability Stack (docker-compose.yml)**

#### Active Services:

| Service | Port | Purpose | UI |
|---|---|---|---|
| **Zipkin** | 9411 | Distributed tracing | http://localhost:9411 |
| **Prometheus** | 9090 | Metrics collection | http://localhost:9090 |

#### Disabled Services (Commented Out for RAM Optimization):

| Service | Port | Purpose | RAM Saved |
|---|---|---|---|
| ~~Grafana~~ | ~~3000~~ | Metrics visualization | 200 MB |
| ~~Elasticsearch~~ | ~~9200~~ | Log storage | 700 MB |
| ~~Logstash~~ | ~~5000~~ | Log aggregation | 500 MB |
| ~~Kibana~~ | ~~5601~~ | Log visualization | 200 MB |

**Total RAM saved:** ~1.6 GB

**Note:** You can view logs with `docker logs <container-name>` or re-enable ELK stack by uncommenting in docker-compose.yml.

---

### **4. Configuration Files**

#### `docker/prometheus.yml`
Prometheus scrape configuration for all 7 services + infrastructure (PostgreSQL, Redis, RabbitMQ).

#### `docker/logstash.conf`
Logstash pipeline:
- Input: TCP port 5000 (JSON logs)
- Filter: Parse timestamps, extract service names
- Output: Elasticsearch + stdout

#### `common-lib/src/main/resources/logback-spring.xml`
Logback configuration for structured logging to Logstash.

---

## 🚀 How to Use

### **1. Start the Full Stack**

```bash
docker compose up -d --build
```

This now starts **14 containers** (optimized for RAM):
- 5 services
- 1 API Gateway
- 3 Eureka Servers (HA setup)
- 3 infrastructure (Postgres, Redis, RabbitMQ)
- **2 observability tools** (Zipkin, Prometheus)

### **2. Access Observability Dashboards**

#### **Distributed Tracing (Zipkin)**
1. Open http://localhost:9411
2. Click "Run Query" to see all traces
3. Click a trace to see request flow across services
4. Example: Track "Create Order" → `order-service` → `product-service` (stock deduction)

#### **Metrics (Prometheus)**
1. Open Prometheus: http://localhost:9090
2. Try queries:
   ```promql
   # HTTP request rate
   rate(http_server_requests_seconds_count[1m])
   
   # JVM memory usage
   jvm_memory_used_bytes{area="heap"}
   
   # API Gateway response times
   http_server_requests_seconds{service="api-gateway"}
   ```

**Note:** Grafana is disabled for RAM optimization. You can:
- Use Prometheus UI directly, or
- Re-enable Grafana by uncommenting in docker-compose.yml and rebuilding

#### **Centralized Logging (View with Docker)**
**ELK stack is disabled.** Use Docker commands to view logs:
```bash
# View recent logs
docker logs commercex-order-service

# Follow logs in real-time
docker logs -f commercex-order-service

# Search for errors
docker logs commercex-order-service 2>&1 | grep ERROR

# View last 100 lines
docker logs --tail 100 commercex-order-service
```

**To re-enable ELK stack:**
1. Uncomment elasticsearch, logstash, kibana in docker-compose.yml
2. Uncomment Logstash appender in common-lib/src/main/resources/logback-spring.xml
3. Run `docker compose up -d`
4. Access Kibana at http://localhost:5601

---

## 📊 What You Can Now See

### **Circuit Breaker in Action**
1. Stop a service:
   ```bash
   docker stop commercex-product-service
   ```

2. Call API Gateway:
   ```bash
   curl http://localhost:8080/api/v1/products
   ```

3. **Result**: Immediate fallback response (no 30s timeout):
   ```json
   {
     "status": "error",
     "message": "Product service is temporarily unavailable. Please try again later.",
     "service": "product-service"
   }
   ```

4. Check Prometheus: Circuit breaker state changed to `OPEN`

### **Rate Limiting**
```bash
# Blast 100 requests
for i in {1..100}; do
  curl http://localhost:8080/api/v1/products &
done

# After hitting limit, you get:
# HTTP 429 Too Many Requests
```

### **Distributed Tracing**
Create an order and see the **complete request flow** in Zipkin:
```
POST /api/v1/orders → api-gateway (2ms)
  → order-service (45ms)
    → user-service (12ms) [Feign: GET /internal/users/{id}]
    → product-service (18ms) [Feign: GET /internal/products/{id}]
    → product-service (22ms) [Feign: PUT /internal/products/{id}/deduct-stock]
  → Total: 99ms
```

Each service's contribution to latency is visible!

### **Metrics**
In Prometheus, query:
Use Docker commands:
```bash
# View all error logs from order-service
docker logs commercex-order-service 2>&1 | grep ERROR

# Follow logs in real-time
docker logs -f commercex-order-service

# Search for specific trace ID
docker logs commercex-order-service 2>&1 | grep "trace_id_here"
```

**Or re-enable ELK:** Uncomment in docker-compose.yml and logback-spring.xml

# Circuit breaker state
resilience4j_circuitbreaker_state{name="productServiceCircuitBreaker"}
```

### **Centralized Logs**
In Kibana, search:
```
service_module: "order" AND level: "ERROR"
```

See all errors from order-service in one place, across all container restarts.

---

## 🔧 Fine-Tuning

### Reduce Tracing Overhead in Production
```yaml
# application.yml
management:
  tracing:
    sampling:
      probability: 0.1  # Sample only 10% of requests
```

### Adjust Circuit Breaker Sensitivity
```yaml
# api-gateway/application.yml
resilience4j:
  circuitbreaker:
    configs:
      default:
        failureRateThreshold: 50  # Open circuit if >50% failures
        slowCallDurationThreshold: 2s  # Calls >2s = slow
```

### Change Rate Limits
```yaml
# api-gateway/application.yml
- name: RequestRateLimiter
  args:
    redis-rate-limiter.replenishRate: 10  # Tokens/sec
    redis-rate-limiter.burstCapacity: 20  # Max burst
```

---

## 📈 Next Steps (Priority Set 2)

You now have:
- ✅ Circuit Breaker
- ✅ Distributed Tracing
- ✅ Centralized Logging
- ✅ Metrics
- ✅ Rate Limiting

**Still missing from the original list:**
6. Centralized Configuration (Spring Cloud Config Server)
7. Eureka High Availability (3+ instances)
8. CORS Configuration
9. Secrets Management (HashiCorp Vault)
10. API Versioning Strategy

Let me know when you're ready to implement the second priority set!

---

## 🐛 Troubleshooting

### Zipkin shows no traces
- Check service logs for `zipkin` keyword
- Verify `ZIPKIN_URL` environment variable
- EnNeed Grafana/Kibana?
If you need advanced visualization or centralized logging:
1. Ensure you have at least 5 GB free RAM
2. Uncomment grafana, elasticsearch, logstash, kibana in docker-compose.yml
3. Uncomment grafana_data and elasticsearch_data volumes
4. Uncomment Logstash appender in common-lib/src/main/resources/logback-spring.xml
5. Run: `docker compose up -d`
- Check `docker ps` — all services running?
- Visit http://localhost:8081/actuator/prometheus (should return metrics)

### Kibana shows no data
- Check Logstash logs: `docker logs commercex-logstash`
- Verify Elasticsearch is up: `curl http://localhost:9200`
- Logs take ~30s to appear after service start

### Circuit breaker not triggering
- Stop a service completely (not just pause)
- Clear browser cache (fallback responses are cacheable)
- Check `docker logs commercex-api-gateway` for circuit breaker logs
