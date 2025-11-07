# Camunda 7 + Kubling Virtual Database Federation

[![Java](https://img.shields.io/badge/java-21+-blue.svg)](#)
[![Kubling license](https://img.shields.io/badge/license-Apache%202.0-blue.svg?style=flat-square)](LICENSE)

---

## Overview

This repository demonstrates how **[Kubling](https://docs.kubling.com)** can serve as a **federated data layer** for **Camunda Platform 7.23.0**, enabling its persistence model to be split across multiple databases, while preserving full transactional consistency.

The goal is to establish a **production-grade persistence architecture** for process automation workloads that demand scalability, storage flexibility, and heterogeneous infrastructure.

---

## Before considering this solution

Camunda has invested significant effort in evolving its architecture, and Camunda 8 is now the default platform for new projects.

However, many existing users are not yet ready to migrate, or prefer to gain time and stability without the pressure of a full transition, especially when the main challenge is scaling Camunda 7’s data layer rather than its runtime logic.

If you are new to Camunda, you should carefully evaluate Camunda 8, as Camunda 7 is no longer actively maintained and will not receive new features.
But if after that evaluation you determine that Camunda 7 better fits your current business or operational constraints, we strongly recommend adopting this Kubling-based persistence model from day 1 as it will prevent future scalability and storage issues without forcing a disruptive migration later.

---

## Motivation

Camunda 7’s default design assumes a single relational database for all engine subsystems: runtime, history, identity, and repository.  
This monolithic persistence layer becomes a bottleneck for:

- **Horizontal scaling**
- **Mixed workloads** (transactional vs. analytical)
- **Cloud migrations** or **hybrid data strategies**

Kubling introduces **soft transactions** and **federated schemas**, letting Camunda’s components map to different storage engines transparently.

---

## Implementation

Since Camunda has limited database engine support, Kubling is not yet plug-and-play.  
The reason is simple: behind the scenes, all persistence operations executed by the engine’s APIs are managed through **MyBatis**, which dynamically constructs the SQL queries and commands for the configured database.

Although Kubling supports the PostgreSQL wire protocol, not all SQL dialect constructs used by MyBatis are currently compatible.  
For that reason, the native Kubling protocol must be used to ensure correct execution and transactional consistency.

This implies that certain **entity mapping files must be adapted or rewritten** so that Camunda’s persistence layer can operate transparently over the federated virtual database.

At the end of the day, that is precisely **the goal of this repository**: to explore and progressively document the adjustments needed to make Camunda 7 fully compatible with Kubling.

---

## Architecture

This project maps Camunda 7’s internal tables across three distinct storage backends:

| Subsystem | Database | Description |
|------------|-----------|-------------|
| Runtime / TX (`ACT_RU_*`) | **MySQL** | Fast, transactional storage |
| History (`ACT_HI_*`) | **PostgreSQL** | Scalable append-oriented history |
| Repository / Metadata / Identity (`ACT_RE_*`, `ACT_GE_*`, `ACT_ID_*`) | **Kubling Embedded DB** | Lightweight local storage for engine metadata |

All databases are exposed as a **single logical schema (`camunda`)** through Kubling’s virtualization layer.  
Transactions that span these systems are coordinated by Kubling’s soft transaction manager.

---

## Technology Stack

- Camunda Platform 7 (Spring Boot integration)
- Kubling Virtualization Engine
- MySQL 8 — Runtime data
- PostgreSQL 15 — Historical data
- Testcontainers — Reproducible integration environment
- JUnit 5 + AssertJ — Validation of distributed operations

---

## What’s implemented

- ✅ MyBatis mappings for a common API access.
- ✅ Base testing structure to spin up required instances.
- ✅ A dumb Camunda's `TransactionContext` only for monitoring.

---

## Getting started

### Prerequisites
- Java 21+
- Docker (Testcontainers will auto-pull images)
- Maven or Gradle

### Run all tests

```bash
./mvnw clean test
```

This will:
1. Generate the descriptor bundle module.
2. Start MySQL, PostgreSQL, and Kubling containers.
3. Initialize the schema for each subsystem.
4. Boot a Camunda 7 engine connected via Kubling.
5. Run simple distributed transaction and consistency tests.

---

## Project tests layout

```

src/test/java/com/kubling/samples/camunda/
.
├── AbstractCamundaIntegrationTest.java         # Shared container + engine setup
├── config
│   └── ContainersConfig.java             # Testcontainers orchestration
├── support
│   ├── CamundaDeploymentHelper.java      # Camunda API helper
│   └── KublingBundleBuilder.java         # Bundle Builder helper
└── tests
    ├── CamundaHistoryTest.java
    ├── CamundaQueryTest.java
    ├── CamundaRuntimeTest.java
    ├── CamundaVariableTest.java
    ├── DistributedTransactionIntegrationTest.java
    └── ListCompositeTest.java

src/test/resources
.
├── history-postgres.sql    # History DDL (Postgres)
├── junit-platform.properties
├── logback.xml
├── processes
│   ├── minimal-process.bpmn
│   └── minimal-process-with-wait.bpmn
└── runtime-mysql.sql   # Runtime DDL (MySQL)

vdb/
.
├── app-config.yaml     # Main app configuration
├── descriptor
│   ├── bundle-info.yaml                        
│   └── vdb
│       ├── CamundaVDB.yaml     # Virtual Database information file 
│       └── ddl
│           └── metadata-rep-kubling.sql        # Metadata DDL (Internal)
```

---

## Key concept: _Soft Transactions_

Kubling implements **soft transactions**, a lightweight coordination model designed for data virtualization.  
It guarantees *atomicity and consistency* across heterogeneous data sources, whether they natively support transactions or not.  
When a data source lacks native transactional behavior, **rollback compensation** is automatically applied based on metadata stored in Kubling’s internal transaction database.

In this setup:
- Inserts into **MySQL**, **PostgreSQL**, and **Kubling Embedded** occur under a single logical transaction.
- A failure in any participant triggers a coordinated rollback across all others.
- Transaction state is tracked in the soft transactions DB (queryable via `SOFT_TRANSACTIONS.TXDB`), ensuring deterministic recovery after errors or restarts.

### Do not confuse with “2PC” or “XA distributed transactions”

Kubling internally performs a **two-step commit sequence** (pre-commit>commit) to coordinate transactional participants (data sources), but this mechanism operates entirely *within the Kubling engine*, not as an external two-phase commit protocol between systems.

| Aspect | Classic 2PC / XA                                          | Kubling Soft Transactions                                                    |
|---------|-----------------------------------------------------------|------------------------------------------------------------------------------|
| **Coordinator** | External transaction manager (e.g. JTA)                   | Internal Kubling transaction orchestrator                                    |
| **Participants** | Independent systems, each with its own TX manager         | Local connectors managed by the same Kubling process                         |
| **Communication** | Network prepare/commit handshake                          | Local coordination; direct commit for TX-ready sources                       |
| **Commit model** | Typically blocking, waits for all participants to confirm | Sequential, local apply; transactional sources call `commit()`               |
| **Failure handling** | Heuristic or manual recovery                              | Native rollback mechanism for transactional sources; others use compensation |
| **Transaction state** | Distributed across systems                                | Centralized within the Kubling engine                                        |

In other words, Kubling’s model **borrows the structure of 2PC internally**, but removes the external coordination overhead, allowing it to provide transactional consistency across heterogeneous data sources as it was just a transaction against a single data source,
without the operational complexity of XA.

### Data Consistency

Kubling does **not** attempt to enforce strict physical consistency across all participants.  
Each data source may provide different durability or isolation guarantees. Some fully transactional, others only eventually consistent.

From Kubling’s perspective, **consistency is logical**, not physical.  
What matters is that each participant **acknowledges the operation** (through a successful return code or message) confirming that the command was accepted and persisted according to that data source’s own semantics.

Kubling then records this acknowledgment in its internal transaction db, ensuring that the overall virtual transaction maintains a coherent state from the engine’s point of view, even if underlying systems exhibit eventual consistency.

In other words, **Kubling guarantees transactional consistency of intent**, but does not override or redefine the physical consistency model of the underlying databases.

Keep this in mind when integrating new data sources: certain Camunda 7 entities (such as runtime or job-execution tables) may rely on **strong transactional consistency** to function correctly.  
Plan your data distribution (VDB/Schemas) carefully, and assign eventually consistent storages only to domains where relaxed consistency is acceptable, for example, historical or audit data.

---

## Roadmap

| Area                                                    | Status | Notes |
|---------------------------------------------------------|---------|-------|
| Core table mapping (`ACT_RU_*`, `ACT_HI_*`, `ACT_RE_*`) | ✅ Implemented | Tested with composite VDB |
| Camunda 7.24.0 compatibility                            | 🔄 In progress | Planned |
| Full identity (`ACT_ID_*`)                              | 🔄 In progress | Planned |
| Full Camunda 7 test coverage                            | 🔄 In progress | Goal: all official engine tests passing on Kubling |

---

## Target audience

Over the years, many teams have successfully adopted **Camunda 7** as an [*embedded workflow engine*](https://blog.bernd-ruecker.com/what-to-do-when-you-cant-quickly-migrate-to-camunda-8-754b1e176f0d), tightly integrated within their own applications.  

With **Camunda 8**, this architecture has changed significantly. The new engine operates as a **remote, service-based** component rather than an embeddable library, as described in [this article by Bernd Rücker](https://blog.bernd-ruecker.com/moving-from-embedded-to-remote-workflow-engines-8472992cc371).

This project is therefore aimed at **developers and architects still running (or maintaining) large Camunda 7 deployments** that rely on the embedded model.  
It provides them with a practical, forward-looking option to **scale their persistence layer**, without abandoning their existing architecture or forcing a full migration to Camunda 8.

---

## How to contribute

This is an open technical initiative.  
If you want to:
- Extend schema coverage,
- Incorporate Camunda [heavy tests](https://github.com/camunda/camunda-bpm-platform/),
- Or integrate alternative storages (Cassandra, Custom API, Redis, etc.),

please open a PR or discussion.  
The long-term goal is a **fully operational, production-ready Kubling-based persistence layer for Camunda 7**.

---

## License

Apache License 2.0 © Kubling Contributors

---

## Learn more

- [Kubling Documentation](https://docs.kubling.com)
- [Camunda Platform 7 Docs](https://docs.camunda.org/manual/7.x/)

