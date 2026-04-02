# Eureka High Availability Implementation Guide

## ✅ **What Was Implemented**

Eureka Server now runs with **3 replicated instances** for high availability. If one or even two Eureka servers fail, service discovery continues working.

---

## 🏗️ **Architecture**

### **Before (Single Instance)**
```
eureka-server (8761) ← All services connect here
           ↓
     [Single Point of Failure]
```

If Eureka crashes → **Entire system stops** (services can't find each other)

### **After (High Availability)**
```
eureka-server-1 (8761) ←───┐
       ↕                    │
eureka-server-2 (8762) ←───┼── All services connect to all 3
       ↕                    │
eureka-server-3 (8763) ←───┘

Peer Replication: Each Eureka knows about the other two
```

If 1 Eureka crashes → System keeps running ✅  
If 2 Eurekas crash → System keeps running ✅  
If all 3 crash → System stops (but very unlikely)

---

## 📦 **Changes Made**

### **1. Eureka Server Configuration**

**File:** `eureka-server/src/main/resources/application.yml`

Added **Spring profiles** for 3 different Eureka instances:

```yaml
# Profile: peer1 (runs on port 8761)
spring.profiles.active: peer1
eureka:
  instance:
    hostname: eureka-server-1
  client:
    service-url:
      defaultZone: http://eureka-server-2:8762/eureka/,http://eureka-server-3:8763/eureka/

# Profile: peer2 (runs on port 8762)
spring.profiles.active: peer2
eureka:
  instance:
    hostname: eureka-server-2
  client:
    service-url:
      defaultZone: http://eureka-server-1:8761/eureka/,http://eureka-server-3:8763/eureka/

# Profile: peer3 (runs on port 8763)
spring.profiles.active: peer3
eureka:
  instance:
    hostname: eureka-server-3
  client:
    service-url:
      defaultZone: http://eureka-server-1:8761/eureka/,http://eureka-server-2:8762/eureka/
```

**Key Settings:**
- `register-with-eureka: true` — Each Eureka registers itself with peers
- `fetch-registry: true` — Each Eureka fetches registry from peers
- `enable-self-preservation: true` — Prevents eviction during network issues

### **2. Docker Compose**

**File:** `docker-compose.yml`

Replaced single `eureka-server` with **3 instances**:

```yaml
eureka-server-1:
  hostname: eureka-server-1
  ports: ["8761:8761"]
  environment:
    SPRING_PROFILES_ACTIVE: peer1

eureka-server-2:
  hostname: eureka-server-2
  ports: ["8762:8762"]
  environment:
    SPRING_PROFILES_ACTIVE: peer2

eureka-server-3:
  hostname: eureka-server-3
  ports: ["8763:8763"]
  environment:
    SPRING_PROFILES_ACTIVE: peer3
```

### **3. All Services Updated**

**Changed:** All 6 services now connect to **all 3 Eureka instances**:

```yaml
# Before (single Eureka)
EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka/

# After (3 Eurekas)
EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server-1:8761/eureka/,http://eureka-server-2:8762/eureka/,http://eureka-server-3:8763/eureka/
```

**Services updated:**
- ✅ user-service
- ✅ product-service
- ✅ order-service
- ✅ payment-service
- ✅ shipping-service
- ✅ api-gateway

**Dependency Changes:**
```yaml
# Before
depends_on:
  eureka-server:
    condition: service_started

# After
depends_on:
  eureka-server-1:
    condition: service_started
  eureka-server-2:
    condition: service_started
  eureka-server-3:
    condition: service_started
```

---

## 🚀 **How to Use**

### **1. Start the System**

```bash
docker compose up -d --build
```

**Now starting:**
- 3 Eureka servers (instead of 1)
- 5 application services (all connect to all 3 Eurekas)
- 1 API Gateway (connects to all 3 Eurekas)
- Total: **18 containers** (was 16)

### **2. Access Eureka Dashboards**

All 3 Eureka instances have their own UI:

```
http://localhost:8761   # Eureka Server 1 (Primary)
http://localhost:8762   # Eureka Server 2 (Secondary)
http://localhost:8763   # Eureka Server 3 (Tertiary)
```

**What you'll see:**
- Each dashboard shows **all registered services**
- Under "DS Replicas": You'll see the other 2 Eureka instances
- Peer replication happens every ~30 seconds

### **3. Test High Availability**

#### **Scenario 1: Stop Eureka Server 1**
```bash
docker stop commercex-eureka-1

# Check: All services still work!
curl http://localhost:8080/api/v1/products

# Check Eureka 2 dashboard
open http://localhost:8762
# → Shows all services still registered
```

Services automatically failover to Eureka 2 and 3. **No downtime!**

#### **Scenario 2: Stop 2 Eureka Servers**
```bash
docker stop commercex-eureka-1
docker stop commercex-eureka-2

# System still works with just Eureka 3!
curl http://localhost:8080/api/v1/products
```

#### **Scenario 3: Restart a Stopped Eureka**
```bash
docker start commercex-eureka-1

# Wait 30s for peer sync
# Check Eureka 1 dashboard
open http://localhost:8761
# → All services automatically re-register
```

---

## 📊 **How Peer Replication Works**

### **Registration Flow**

```
1. product-service starts
   ↓
2. Connects to eureka-server-1:8761
   ↓
3. Registers itself: "I'm product-service at 172.18.0.5:8082"
   ↓
4. eureka-server-1 replicates to eureka-server-2 and eureka-server-3
   ↓
5. All 3 Eurekas now know about product-service
   ↓
6. order-service queries ANY Eureka → gets product-service location
```

### **Heartbeat & Health**

```
Every 30 seconds:
  - Each service sends heartbeat to ALL 3 Eurekas
  - Each Eureka syncs its registry with peers
  - If a service misses 3 heartbeats (90s) → evicted from registry
```

### **Failover Behavior**

```
Client (order-service) has connection list:
  [eureka-1:8761, eureka-2:8762, eureka-3:8763]

Try eureka-1 → ❌ Connection timeout
Try eureka-2 → ✅ Success! Get registry
Cache registry locally for 30s
```

**Even if all 3 Eurekas are down:** Services use their **cached registry** for up to 30 seconds, giving you time to restart Eureka.

---

## 🔧 **Configuration Details**

### **Self-Preservation Mode**

```yaml
eureka:
  server:
    enable-self-preservation: true  # Now enabled (was false)
```

**What it does:**
- Prevents mass eviction during network partitions
- If <85% of services send heartbeats → Eureka assumes network issue (not service failures)
- Stops evicting services until heartbeats resume

**Production recommended:** `true`  
**Local dev:** Can set to `false` for faster eviction

### **Replication Settings (Implicit Defaults)**

```yaml
eureka:
  server:
    peer-eureka-nodes-update-interval-ms: 600000  # Re-discover peers every 10 min
    peer-node-connect-timeout-ms: 200
    peer-node-read-timeout-ms: 200
    max-threads-for-peer-replication: 20
```

These are Spring Cloud defaults — no need to change unless you have specific requirements.

---

## 🐛 **Troubleshooting**

### **Problem: Eureka dashboard shows "UNAVAILABLE REPLICAS"**

**Cause:** Eureka instances haven't discovered each other yet.

**Fix:**
```bash
# Wait 30-60 seconds for peer discovery
# Check logs:
docker logs commercex-eureka-1 | grep "replica"
```

You should see:
```
Replica node added: eureka-server-2:8762
Replica node added: eureka-server-3:8763
```

### **Problem: Services show up 3 times in Eureka**

**Cause:** Service registered with all 3 Eurekas independently (this is normal!).

**Fix:** Not a problem — each Eureka shows its own view. The replicas will sync and deduplicate.

### **Problem: After stopping an Eureka, services take 90s to appear in another Eureka**

**Cause:** Default heartbeat interval (30s) + eviction timer (60s default).

**Fix:** This is normal behavior. For faster failover, reduce in `application.yml`:
```yaml
eureka:
  instance:
    lease-renewal-interval-in-seconds: 10  # Heartbeat every 10s (default: 30s)
    lease-expiration-duration-in-seconds: 30  # Evict after 30s (default: 90s)
```

**Trade-off:** More frequent heartbeats = more network traffic.

### **Problem: All 3 Eurekas show different registered services**

**Cause:** Peer replication delayed or failed.

**Fix:**
```bash
# Restart all Eurekas in sequence:
docker restart commercex-eureka-1
sleep 30
docker restart commercex-eureka-2
sleep 30
docker restart commercex-eureka-3
```

---

## 📈 **Production Recommendations**

### **For Cloud/Kubernetes Deployments**

If deploying to AWS/Azure/GCP, use **5 Eureka instances** (odd number for quorum):

```yaml
eureka-server-1: us-east-1a
eureka-server-2: us-east-1b
eureka-server-3: us-east-1c
eureka-server-4: us-west-2a
eureka-server-5: us-west-2b
```

Split across **availability zones** and **regions** for true HA.

### **Alternative: Use Cloud-Native Discovery**

Instead of Eureka:
- **AWS:** Use AWS Cloud Map
- **Azure:** Use Azure Service Fabric
- **GCP:** Use GCP Service Directory
- **Kubernetes:** Use Kubernetes Service Discovery (no Eureka needed)

### **Monitoring**

Track these metrics in Prometheus:

```promql
# Number of registered eureka replicas
eureka_server_replicas

# Registry size consistency
eureka_server_registry_size_total

# Replication failures
eureka_server_replication_failed_total
```

---

## 🎯 **Benefits You Now Have**

✅ **No single point of failure** — Lose 1-2 Eurekas, system keeps running  
✅ **Zero-downtime Eureka updates** — Rolling restart without service disruption  
✅ **Regional resilience** — Deploy across data centers  
✅ **Production-ready** — Matches Netflix OSS best practices  
✅ **Load distribution** — Services spread requests across 3 Eurekas  

---

## 🔄 **Rollback to Single Eureka (For Local Dev)**

If you want to simplify for local development:

**Option 1: Use standalone profile**

```yaml
# docker-compose.yml
eureka-server:
  environment:
    SPRING_PROFILES_ACTIVE: standalone  # Single instance mode
```

**Option 2: Comment out eureka-2 and eureka-3**

```yaml
# eureka-server-2:
#   ...
# eureka-server-3:
#   ...
```

Services will still work with just 1 Eureka (they'll retry the unreachable instances and fail over).

---

## 📚 **References**

- [Eureka High Availability](https://github.com/Netflix/eureka/wiki/Eureka-at-a-glance)
- [Spring Cloud Netflix Eureka](https://cloud.spring.io/spring-cloud-netflix/reference/html/)
- [Peer Awareness in Eureka](https://cloud.spring.io/spring-cloud-static/spring-cloud-netflix/2.2.1.RELEASE/reference/html/#spring-cloud-eureka-server-peer-awareness)

---

**Your system is now production-ready from a service discovery perspective!** 🎉
