# Mini Redis Server - Architecture Documentation

## System Architecture Overview

The Mini Redis Server is built with a modular, high-performance architecture designed for production use while maintaining simplicity for development and learning.

## Core Architecture Components

### 1. Server Core (`server/`)

**RedisServer.java** - Main Entry Point
- NIO-based event loop using `Selector`
- Non-blocking I/O for high concurrency
- Graceful shutdown handling
- Configuration management integration

**ClientConnectionHandler.java** - Connection Management
- Accepts new client connections
- Handles client disconnections
- Manages client request/response cycle
- Buffer management for efficient I/O

**ServerContext.java** - Shared Resources
- Centralized access to all server components
- Lifecycle management for subsystems
- Configuration distribution
- Resource cleanup coordination

**ServerConfiguration.java** - Configuration Management
- Immutable configuration records
- Command-line argument parsing
- Environment variable support
- Validation and defaults

### 2. Protocol Layer (`protocol/`)

**ProtocolParser.java** - RESP Protocol Implementation
- Redis Serialization Protocol (RESP) parser
- Handles arrays, bulk strings, simple strings
- Streaming parser for incomplete data
- Error handling for malformed commands

**CommandDispatcher.java** - Command Routing
- Routes parsed commands to implementations
- Context creation and management
- Error handling and response generation
- Command execution pipeline

**ResponseBuilder.java** - Response Generation
- RESP response formatting
- Type-safe response creation
- Efficient buffer management
- Error response standardization

### 3. Command System (`commands/`)

**Hierarchical Command Structure:**
```
commands/
├── core/                    # Core interfaces
│   ├── Command.java         # Base command interface
│   └── CommandType.java     # Command categorization
├── base/                    # Abstract base classes
│   ├── AbstractCommand.java
│   ├── WriteCommand.java
│   ├── ReadCommand.java
│   ├── BlockingCommand.java
│   └── ReplicationCommand.java
├── impl/                    # Command implementations
│   ├── strings/             # String operations
│   ├── lists/               # List operations
│   ├── sortedsets/          # Sorted set operations
│   ├── geo/                 # Geospatial operations
│   ├── streams/             # Stream operations
│   ├── pubsub/              # Pub/Sub operations
│   ├── transaction/         # Transaction operations
│   └── replication/         # Replication operations
└── registry/                # Command discovery
    ├── CommandRegistry.java
    └── CommandFactory.java
```

**Command Execution Pipeline:**
1. Protocol parsing → Command identification
2. Validation → Context creation
3. Execution → Result generation
4. Response formatting → Client transmission

### 4. Storage Engine (`storage/`)

**StorageService.java** - Unified Storage Interface
- Type-agnostic storage operations
- Event publishing for keyspace changes
- Expiration management integration
- Metrics collection hooks

**Repository Pattern:**
- `StringRepository` - String value operations
- `ListRepository` - List operations with QuickList
- `StreamRepository` - Stream operations with time-series data
- `ZSetRepository` - Sorted set operations with QuickZSet
- `GeoRepository` - Geospatial operations using sorted sets with geohash encoding

**Custom Data Structures:**
- `QuickList` - Memory-efficient doubly-linked list
- `QuickZSet` - Skip list-based sorted set implementation
- `StreamValue` - Time-series entries with efficient range queries

**Persistence Layer:**
- `AofRepository` - Append-Only File persistence
- `RdbRepository` - Redis Database File format
- `PersistentRepository` - Unified persistence interface

### 5. Replication System (`replication/`)

**Master-Side Components:**
- `ReplicationManager` - Manages replica connections
- Tracks replication offsets and acknowledgments
- Handles WAIT command for synchronous replication
- Command propagation to replicas

**Replica-Side Components:**
- `ReplicationClient` - Connects to master server
- Handles PSYNC protocol handshake
- Processes replication stream
- Maintains synchronization state

**Replication Protocol Flow:**
1. **Handshake**: PING → REPLCONF → PSYNC
2. **Full Sync**: RDB transfer → Command stream
3. **Incremental Sync**: Real-time command replication
4. **Heartbeat**: Regular REPLCONF GETACK commands

### 6. Pub/Sub System (`pubsub/`)

**PubSubManager.java** - Message Distribution
- Channel subscription management
- Pattern matching with glob support
- Message broadcasting to subscribers
- Client state tracking per connection

**Features:**
- Exact channel subscriptions (`SUBSCRIBE`)
- Pattern-based subscriptions (`PSUBSCRIBE`)
- Efficient pattern matching with caching
- Per-client subscription isolation

### 7. Transaction System (`transaction/`)

**TransactionManager.java** - ACID Transactions
- Command queuing during MULTI/EXEC
- Optimistic locking with WATCH
- Rollback on watched key modifications
- Transaction state per client connection

**Transaction Lifecycle:**
1. `MULTI` - Start transaction, begin queuing
2. Commands - Queue for later execution
3. `WATCH` - Monitor keys for changes
4. `EXEC` - Execute queued commands atomically
5. `DISCARD` - Abort transaction, clear queue

### 8. Blocking Operations (`blocking/`)

**BlockingManager.java** - Client Blocking Support
- Thread-safe blocking client management
- Timeout handling with automatic cleanup
- Multiple data structure support (lists, streams)
- Fair queuing for blocked clients

**Blocking Flow:**
1. Client issues blocking command (BLPOP, XREAD BLOCK)
2. If no data available, client enters blocking state
3. When data becomes available, blocked clients notified
4. First blocked client gets the data (FIFO)

### 9. Monitoring & Metrics (`metrics/`, `server/http/`)

**MetricsCollector.java** - Performance Monitoring
- Micrometer integration for metrics collection
- Redis Enterprise compatible metrics
- Real-time performance tracking
- Memory, command, and connection metrics

**HTTP Server Integration:**
- Prometheus metrics endpoint (`/metrics`)
- Health check endpoint (`/health`)
- Server info endpoint (`/info`)
- RESTful API for monitoring

### 10. Scheduling & Cleanup (`scheduler/`)

**TimeoutScheduler.java** - Background Tasks
- Expired key cleanup
- Blocked client timeout handling
- Periodic maintenance tasks
- Resource cleanup scheduling

## Data Flow Architecture

### Request Processing Flow

```
Client Request → ProtocolParser → CommandDispatcher → Command Implementation
                                                    ↓
Client Response ← ResponseBuilder ← CommandResult ← Storage/Business Logic
```

### Write Operation Flow

```
Write Command → Validation → Storage Update → AOF Append → Replica Propagation
                                          ↓
                               Event Publishing → Blocking Client Notification
```

### Read Operation Flow

```
Read Command → Validation → Storage Lookup → Response Generation
                        ↓
                   Metrics Update
```

## Concurrency Model

### Thread Safety Strategy

1. **Immutable Objects** - Configuration, commands, responses
2. **ConcurrentHashMap** - For thread-safe storage operations
3. **Atomic Operations** - For counters and simple state
4. **Synchronized Blocks** - For complex state changes only
5. **Lock-Free Design** - Minimize blocking operations

### NIO Event Loop

- Single-threaded event loop for I/O operations
- Non-blocking channel operations
- Selector-based multiplexing
- Efficient memory management with ByteBuffers

## Memory Management

### Storage Efficiency

- **Custom Data Structures** - Optimized for Redis use cases
- **Memory Pooling** - ByteBuffer reuse where possible
- **Lazy Loading** - Load data structures on demand
- **Expiration** - Automatic cleanup of expired keys

### Configurable Limits

- `--maxmemory` - Global memory limit
- Automatic eviction when limit reached
- Memory usage tracking and reporting
- Per-data-type memory optimization

## Performance Characteristics

### Throughput Optimization

- **Non-blocking I/O** - Handle thousands of concurrent connections
- **Zero-copy Operations** - Efficient buffer handling
- **Batch Processing** - Multiple commands per I/O cycle
- **Connection Reuse** - Persistent client connections

### Latency Optimization

- **In-memory Operations** - Sub-millisecond response times
- **Efficient Data Structures** - O(1) or O(log n) operations
- **Minimal Allocations** - Reduce garbage collection pressure
- **Direct Buffer Access** - Avoid unnecessary copying

## Extensibility Points

### Adding New Commands

1. Create command class extending appropriate base class
2. Implement `execute()` and `validate()` methods
3. Register in `CommandFactory`
4. Add unit tests

### Adding New Data Types

1. Create value type class implementing `StoredValue`
2. Create repository class implementing storage operations
3. Add to `StorageService` type routing
4. Implement persistence methods

### Adding New Persistence Formats

1. Implement `Repository` interface
2. Add format detection logic
3. Register in `PersistentRepository`
4. Add configuration options

## Configuration Architecture

### Hierarchical Configuration

1. **Hard-coded Defaults** - In `ServerConfig.java`
2. **Command-line Arguments** - Parsed by `ConfigurationParser`
3. **Environment Variables** - System environment
4. **Runtime Configuration** - `CONFIG SET` commands

### Configuration Validation

- Type checking (ports, memory sizes, paths)
- Range validation (1-65535 for ports)
- Dependency checking (replica requires master info)
- Security validation (password requirements)

This architecture provides a solid foundation for a production-ready Redis implementation while maintaining clarity and extensibility for continued development.
