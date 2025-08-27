# Mini Redis Server - Management Tool Documentation

This document provides comprehensive documentation for the `run.sh` management tool, which is the primary interface for managing your Mini Redis server instances.

## Table of Contents

- [Overview](#overview)
- [Installation & Setup](#installation--setup)
- [Basic Usage](#basic-usage)
- [Command Reference](#command-reference)
- [Configuration Options](#configuration-options)
- [Development Workflow](#development-workflow)
- [Troubleshooting](#troubleshooting)
- [Examples](#examples)

## Overview

The `run.sh` script is an all-in-one management tool that provides:

- **Zero-Configuration Setup** - Works out of the box with sensible defaults
- **Multi-Instance Management** - Run multiple Redis servers on different ports
- **Built-in Development Tools** - Interactive development mode with auto-restart
- **Redis CLI Integration** - Direct connection to any server instance
- **Comprehensive Monitoring** - Status, logs, and health checking
- **Master-Replica Support** - Easy cluster setup and management
- **Production Ready** - Process management, logging, and persistence

## Installation & Setup

### Prerequisites

- **Java 24+** (required)
- **Maven 3.6+** (for building)
- **redis-cli** (optional, for enhanced CLI experience)

### Quick Setup

```bash
# Clone the repository
git clone https://github.com/ankitkr-04/mini-redis.git
cd mini-redis

# Make the script executable
chmod +x run.sh

# Build the project
./run.sh package

# Start your first server
./run.sh start
```

## Basic Usage

### Interactive Mode

```bash
# Start interactive mode
./run.sh

# Menu options:
# 1) start     - Start Redis master server (port 6379)
# 2) replica   - Start Redis replica server (port 6380)  
# 3) cluster   - Start master + 2 replicas cluster
# 4) status    - Show server status
# 5) stop-all  - Stop all servers
# 6) logs      - View server logs
# 7) cli       - Connect with Redis CLI
# 8) test      - Test connectivity
# 9) dev       - Development mode
```

### Direct Commands

```bash
# Start a server
./run.sh start [PORT] [OPTIONS...]

# Connect with Redis CLI
./run.sh cli [PORT]

# Check status
./run.sh status
```

## Command Reference

### Server Management Commands

#### `start [PORT] [ARGS...]`
Starts a Redis server instance.

```bash
# Start on default port (6379)
./run.sh start

# Start on custom port
./run.sh start 6380

# Start with AOF persistence
./run.sh start 6379 --appendonly

# Start replica
./run.sh start 6380 --replicaof "127.0.0.1 6379"
```

#### `stop [PORT]`
Stops a specific Redis server.

```bash
# Stop server on default port
./run.sh stop

# Stop server on specific port
./run.sh stop 6380
```

#### `restart [PORT] [ARGS...]`
Restarts a Redis server with optional new configuration.

```bash
# Restart with same configuration
./run.sh restart 6379

# Restart with persistence enabled
./run.sh restart 6379 --appendonly
```

#### `status`
Shows the status of all Redis server instances.

```bash
./run.sh status

# Output example:
[INFO] Redis Server Status:
  Port 6379: RUNNING (PID: 12345)
  Port 6380: RUNNING (PID: 12346)
  Port 6381: STOPPED (stale PID file)
```

### Cluster Management

#### `cluster [MASTER_PORT] [REPLICA_COUNT] [BASE_REPLICA_PORT]`
Starts a complete master-replica cluster.

```bash
# Default: master on 6379, 2 replicas starting from 6380
./run.sh cluster

# Custom configuration
./run.sh cluster 6379 3 6380  # 1 master + 3 replicas

# Different master port
./run.sh cluster 7000 2 7001  # Master on 7000, replicas on 7001, 7002
```

### Client Connection Commands

#### `cli [PORT]`
Opens an interactive Redis CLI session.

```bash
# Connect to default port
./run.sh cli

# Connect to specific port
./run.sh cli 6380
```

#### `test [PORT]`
Tests server connectivity and basic operations.

```bash
# Test default port
./run.sh test

# Test specific port
./run.sh test 6380
```

### Development Commands

#### `dev [PORT] [ARGS...]`
Starts development mode with interactive controls.

```bash
# Start development mode
./run.sh dev

# Development mode with persistence
./run.sh dev 6379 --appendonly
```

**Development Console Commands:**
- `r` + Enter - Restart server
- `s` + Enter - Show status  
- `l` + Enter - Show logs (last 20 lines)
- `t` + Enter - Test connectivity
- `c` + Enter - Connect with Redis CLI
- `q` + Enter - Quit development mode

### Build Commands

#### `compile`
Compiles the Java source code.

```bash
./run.sh compile
```

#### `package`
Builds the complete JAR with dependencies.

```bash
./run.sh package
```

#### `clean`
Cleans up build artifacts, old logs, and stale PID files.

```bash
./run.sh clean
```

### Monitoring Commands

#### `logs [PORT]`
Shows real-time logs for a server.

```bash
# Follow logs for default port
./run.sh logs

# Follow logs for specific port
./run.sh logs 6380
```

## Configuration Options

Currently supported Redis server configuration options:

### Core Configuration
- `--port PORT` - Server port (default: 6379)
- `--bind ADDRESS` - Bind address (default: 127.0.0.1)
- `--dir PATH` - Data directory (default: /var/lib/redis)
- `--dbfilename NAME` - RDB filename (default: dump.rdb)

### Persistence
- `--appendonly` - Enable AOF persistence

### Replication
- `--replicaof "HOST PORT"` - Configure as replica of master

### Configuration Examples

```bash
# Master with AOF persistence
./run.sh start 6379 --appendonly --dir ./data

# Replica server
./run.sh start 6380 --replicaof "127.0.0.1 6379"

# Custom data directory
./run.sh start 6379 --dir ./custom-data --appendonly
```

## Development Workflow

### Daily Development

```bash
# Start development mode
./run.sh dev

# Make code changes in your editor
# Press 'r' + Enter to restart with changes
# Press 't' + Enter to test functionality
# Press 'c' + Enter to test with Redis CLI
```

### Testing Changes

```bash
# Quick connectivity test
./run.sh test

# Full cluster test
./run.sh cluster
./run.sh test 6379  # Test master
./run.sh test 6380  # Test replica 1
./run.sh test 6381  # Test replica 2
```

### Debugging

```bash
# Check server status
./run.sh status

# Monitor logs in real-time
./run.sh logs 6379

# Test specific functionality
./run.sh cli 6379
redis> info replication
redis> keys *
```

## Troubleshooting

### Common Issues

#### "Port already in use"
```bash
# Check what's using the port
./run.sh status
netstat -tuln | grep 6379

# Stop conflicting server
./run.sh stop 6379

# Or use different port
./run.sh start 6380
```

#### "Server failed to start"
```bash
# Check logs for errors
./run.sh logs 6379

# Common solutions:
# 1. Check Java version
java -version

# 2. Rebuild project
./run.sh clean
./run.sh package

# 3. Check file permissions
ls -la target/mini-redis-server.jar
```

#### "Can't connect with CLI"
```bash
# Verify server is running
./run.sh status
./run.sh test

# Install redis-cli if missing
sudo apt-get install redis-tools  # Ubuntu/Debian
```

## Examples

### Example 1: Basic Development Setup

```bash
# Start development environment
./run.sh dev

# In development console:
dev:6379> t  # Test connectivity
dev:6379> c  # Connect with CLI

# In Redis CLI:
127.0.0.1:6379> set name "Redis Developer"
OK
127.0.0.1:6379> get name
"Redis Developer"
127.0.0.1:6379> exit

# Back in development console:
dev:6379> r  # Restart server
dev:6379> q  # Quit development mode
```

### Example 2: Master-Replica Cluster

```bash
# Start master with persistence
./run.sh start 6379 --appendonly --dir ./master-data

# Start replicas
./run.sh start 6380 --replicaof "127.0.0.1 6379"
./run.sh start 6381 --replicaof "127.0.0.1 6379"

# Test replication
./run.sh cli 6379
redis> set test-key "master-value"
redis> exit

./run.sh cli 6380
redis> get test-key
"master-value"
redis> exit

# Verify cluster
./run.sh status
```

### Example 3: Data Persistence Testing

```bash
# Start server with AOF persistence
./run.sh start 6379 --appendonly --dir ./persist-test

# Add some data
./run.sh cli 6379
redis> set user:1 "John Doe"
redis> set user:2 "Jane Smith"
redis> lpush logs "event1" "event2" "event3"
redis> exit

# Stop and restart server
./run.sh stop 6379
./run.sh start 6379 --appendonly --dir ./persist-test

# Verify data persistence
./run.sh cli 6379
redis> get user:1
"John Doe"
redis> lrange logs 0 -1
1) "event3"
2) "event2" 
3) "event1"
```

### Example 4: Development with Hot Reload

```bash
# Terminal 1: Start development mode
./run.sh dev 6379 --appendonly

# Terminal 2: Make code changes
# Edit source files...

# Terminal 1: Restart to see changes
dev:6379> r  # Restart server
dev:6379> l  # Check logs
dev:6379> t  # Test functionality
```

---

This documentation covers the management tool for the current feature set. As new features are added to Mini Redis Server, this documentation will be updated accordingly.