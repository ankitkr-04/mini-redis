# Mini Redis Server

A Redis-compatible server implementation in Java demonstrating advanced data structures, system design, and distributed systems concepts. Built for learning Redis internals and exploring high-performance server architecture.

## 🎯 Project Goals

This project showcases:
- **Data Structures & Algorithms**: Custom QuickList, SkipList, and Redis data structure implementations
- **System Design**: High-performance, concurrent server architecture
- **Network Programming**: Non-blocking I/O and protocol implementation
- **Distributed Systems**: Master-replica replication and consistency
- **Software Engineering**: Clean code, comprehensive testing, and documentation

## 🚀 Quick Start

```bash
# One-command setup
git clone <repository> && cd mini-redis-clean
./run.sh start

# Interactive demo
./run.sh interactive

# Test with Redis CLI
redis-cli -p 6379 SET demo "Hello Redis!"
redis-cli -p 6379 GET demo
```

## 📋 Requirements

- **Java 24+** (Latest OpenJDK recommended)
- **Maven 3.6+** (automatically handled by run.sh)
- **Linux/macOS/Windows** (WSL for Windows)

Optional:
- `redis-cli` for command-line interaction
- `curl` for monitoring endpoints

## 🛠️ Essential Commands

| Command | Description | Example |
|---------|-------------|---------|
| `./run.sh start` | Start Redis server | `./run.sh start --port 6379` |
| `./run.sh cluster` | Start full cluster | `./run.sh cluster` |
| `./run.sh status` | Check all servers | `./run.sh status` |
| `./run.sh test` | Run test suite | `./run.sh test` |
| `./run.sh benchmark` | Performance test | `./run.sh benchmark` |
| `./run.sh interactive` | Interactive mode | `./run.sh interactive` |

**📋 Full Command Reference:** See [Run Tool Guide](docs/run-tool-guide.md)

## 📊 Supported Redis Commands (29+)

### Core Data Types
- **🔤 Strings**: `GET`, `SET`, `INCR`, `DECR`
- **📝 Lists**: `LPUSH`, `RPUSH`, `LPOP`, `RPOP`, `LLEN`, `LRANGE`, `BLPOP`, `BRPOP`
- **🎯 Sorted Sets**: `ZADD`, `ZRANGE`, `ZRANK`, `ZSCORE`, `ZCARD`, `ZREM`
- **🌊 Streams**: `XADD`, `XRANGE`, `XREAD`
- **🗺️ Geospatial**: `GEOADD`, `GEODIST`, `GEOPOS`, `GEOSEARCH`

### Advanced Features
- **📡 Pub/Sub**: `PUBLISH`, `SUBSCRIBE`, `UNSUBSCRIBE`, `PSUBSCRIBE`, `PUNSUBSCRIBE`
- **🔒 Transactions**: `MULTI`, `EXEC`, `DISCARD`, `WATCH`, `UNWATCH`
- **🔄 Replication**: `PSYNC`, `REPLCONF`, `WAIT`
- **⚙️ Server**: `PING`, `ECHO`, `TYPE`, `KEYS`, `INFO`, `CONFIG`

**📝 Complete Command Reference:** [Commands Documentation](docs/commands.md)

## 🏗️ Architecture & Features

### Core Features
| Feature | Status | Description |
|---------|--------|-------------|
| **Redis Protocol** | ✅ Complete | Full RESP compatibility |
| **Data Structures** | ✅ Complete | Strings, Lists, Streams, Sorted Sets |
| **Persistence** | ✅ Complete | AOF and RDB snapshot support |
| **Replication** | ✅ Complete | Master-replica with auto-sync |
| **Pub/Sub** | ✅ Complete | Channel and pattern messaging |
| **Transactions** | ✅ Complete | MULTI/EXEC with WATCH support |
| **Monitoring** | ✅ Complete | Metrics, health checks, HTTP API |
| **Security** | 🔧 Planned | Authentication and authorization |

### Performance Characteristics
- **Throughput**: 100K+ ops/sec (single-threaded)
- **Latency**: Sub-millisecond response times
- **Concurrency**: Non-blocking I/O with thousands of connections
- **Memory**: Efficient data structures with minimal overhead

## 💡 Usage Examples

### Data Persistence
```bash
./run.sh start --appendonly
redis-cli SET user:1 "John Doe"
./run.sh restart
redis-cli GET user:1  # Data persisted: "John Doe"
```

### Master-Replica Replication
```bash
./run.sh cluster
redis-cli -p 6379 SET key "value"
redis-cli -p 6380 GET key  # Returns: "value"
```

### Pub/Sub Messaging
```bash
# Terminal 1
redis-cli SUBSCRIBE news

# Terminal 2
redis-cli PUBLISH news "Breaking news!"
```

### Transactions
```bash
redis-cli MULTI
redis-cli SET account:a 100
redis-cli SET account:b 50
redis-cli EXEC
```

### Geospatial Operations
```bash
# Add locations to a geospatial set
redis-cli GEOADD cities 13.361389 38.115556 Palermo 15.087269 37.502669 Catania

# Calculate distance between cities
redis-cli GEODIST cities Palermo Catania km
# Returns: "166.227564"

# Find cities within radius
redis-cli GEOSEARCH cities FROMMEMBER Palermo BYRADIUS 200 km
# Returns: 1) "Catania"  2) "Palermo"

# Get coordinates of cities
redis-cli GEOPOS cities Palermo Catania
```

## 📊 Monitoring

### HTTP Endpoints
- **Health**: `http://localhost:8080/health`
- **Metrics**: `http://localhost:8080/metrics` (Prometheus format)
- **Info**: `http://localhost:8080/info`

### Key Metrics
- Server: Uptime, connections, memory usage
- Commands: Execution count, latency, errors
- Replication: Offset, lag, connected replicas
- Pub/Sub: Active channels, subscribers, messages

## 🧪 Testing & Development

### Testing
```bash
./run.sh test           # Run test suite
redis-benchmark -h 127.0.0.1 -p 6379 -n 10000 -c 50
```

### Development
```bash
./run.sh dev --debug   # Development mode
./run.sh clean         # Clean build artifacts
./run.sh logs 6379     # View server logs
```

## 🏆 Technical Achievements

### Skills Demonstrated
- **Advanced Java**: Modern Java 24 features, NIO programming, concurrent collections
- **System Design**: Distributed systems, replication, persistence
- **Performance**: 100K+ ops/sec with optimized algorithms
- **Testing**: 85%+ test coverage with comprehensive integration tests
- **Documentation**: Technical writing and user experience design

### Architecture Highlights
- **Modular Design**: Clean separation across 10+ major components
- **High Performance**: NIO-based event loops, zero-copy operations
- **Production Ready**: Logging, metrics, health monitoring, graceful shutdown

## 📚 Documentation

- **[🛠️ Run Tool Guide](docs/run-tool-guide.md)** - Comprehensive management tool reference
- **[🏗️ Architecture Guide](docs/architecture.md)** - System design and components
- **[📝 Commands Reference](docs/commands.md)** - Complete Redis command documentation

## 🔧 Troubleshooting

### Common Issues
```bash
# Port conflicts
./run.sh status && ./run.sh stop-all

# Build issues
./run.sh clean && java -version

# Check logs
./run.sh logs 6379
```

## 📄 License

This project is open source under the [MIT License](LICENSE).

**🎯 Purpose**: Personal learning project exploring Redis internals, data structures, and distributed systems while strengthening Java programming skills.

---

**🚀 Ready to explore?**
- **Quick Start**: `./run.sh interactive` for guided setup
- **Deep Dive**: Check [documentation](docs/) for architecture details
- **Experiment**: Clone, run, and explore the codebase!