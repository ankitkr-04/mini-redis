# Redis Commands Reference

This document provides a comprehensive reference for all Redis commands implemented in Mini Redis Server. The implementation focuses on core Redis functionality for learning and demonstration purposes.

## 📋 Implemented Commands Overview

Based on the actual codebase, the following commands are implemented:

### ✅ Implemented Commands (25 Core Commands)

**🔤 String Operations (4 commands):**
- `GET` - Get string value  
- `SET` - Set string value
- `INCR` - Increment integer value
- `DECR` - Decrement integer value

**📝 List Operations (5 commands):**
- `LPUSH` / `RPUSH` - Push to list (left/right)
- `LPOP` / `RPOP` - Pop from list (left/right)  
- `LLEN` - Get list length
- `LRANGE` - Get list range
- `BLPOP` / `BRPOP` - Blocking pop operations

**🎯 Sorted Set Operations (6 commands):**
- `ZADD` - Add members with scores
- `ZCARD` - Get set cardinality
- `ZRANGE` - Get range by rank
- `ZRANK` - Get member rank
- `ZREM` - Remove members
- `ZSCORE` - Get member score

**🌊 Stream Operations (3 commands):**
- `XADD` - Add stream entry
- `XRANGE` - Get stream range
- `XREAD` - Read from streams (with blocking support)

**📡 Pub/Sub Operations (3 commands):**
- `PUBLISH` - Publish message to channel
- `SUBSCRIBE` / `PSUBSCRIBE` - Subscribe to channels/patterns
- `UNSUBSCRIBE` / `PUNSUBSCRIBE` - Unsubscribe from channels/patterns

**🔒 Transaction Operations (5 commands):**
- `MULTI` - Start transaction
- `EXEC` - Execute transaction
- `DISCARD` - Discard transaction
- `WATCH` - Watch keys for changes
- `UNWATCH` - Stop watching keys

**🔄 Replication Commands (3 commands):**
- `PSYNC` - Replica synchronization
- `REPLCONF` - Replication configuration
- `WAIT` - Wait for replica acknowledgment

**⚙️ Server & Basic Commands (7 commands):**
- `PING` - Ping server
- `ECHO` - Echo message
- `TYPE` - Get key type
- `KEYS` - Find keys by pattern
- `INFO` - Server information
- `CONFIG` - Configuration management
- `FLUSHALL` - Clear All Keys

---

## 📋 Command Details

### 🔤 String Commands

#### GET
**Syntax:** `GET key`  
**Description:** Get the value of a key  
**Returns:** String value or null if key doesn't exist  
**Example:**
```bash
redis-cli SET mykey "Hello World"
redis-cli GET mykey
# Returns: "Hello World"
```

#### SET
**Syntax:** `SET key value [PX milliseconds]`  
**Description:** Set string value of a key with optional expiration  
**Returns:** OK  
**Example:**
```bash
redis-cli SET mykey "Hello Redis"
redis-cli SET tempkey "expires soon" PX 5000  # Expires in 5 seconds
```

#### INCR
**Syntax:** `INCR key`  
**Description:** Increment the integer value of a key by one  
**Returns:** The new value after increment  
**Example:**
```bash
redis-cli SET counter 10
redis-cli INCR counter
# Returns: 11
```

#### DECR
**Syntax:** `DECR key`  
**Description:** Decrement the integer value of a key by one  
**Returns:** The new value after decrement  
**Example:**
```bash
redis-cli SET counter 10
redis-cli DECR counter
# Returns: 9
```

---

### 📝 List Commands

#### LPUSH
**Syntax:** `LPUSH key element [element ...]`  
**Description:** Insert elements at the head of the list  
**Returns:** The length of the list after the push operations  
**Example:**
```bash
redis-cli LPUSH mylist "world"
redis-cli LPUSH mylist "hello"
# List now contains: ["hello", "world"]
```

#### RPUSH
**Syntax:** `RPUSH key element [element ...]`  
**Description:** Insert elements at the tail of the list  
**Returns:** The length of the list after the push operations  
**Example:**
```bash
redis-cli RPUSH mylist "first"
redis-cli RPUSH mylist "second"
# List now contains: ["first", "second"]
```

#### LPOP
**Syntax:** `LPOP key`  
**Description:** Remove and return the first element of the list  
**Returns:** The removed element or null if list is empty  
**Example:**
```bash
redis-cli RPUSH mylist "a" "b" "c"
redis-cli LPOP mylist
# Returns: "a"
```

#### RPOP
**Syntax:** `RPOP key`  
**Description:** Remove and return the last element of the list  
**Returns:** The removed element or null if list is empty  
**Example:**
```bash
redis-cli RPUSH mylist "a" "b" "c"
redis-cli RPOP mylist
# Returns: "c"
```

#### LLEN
**Syntax:** `LLEN key`  
**Description:** Get the length of a list  
**Returns:** The length of the list  
**Example:**
```bash
redis-cli RPUSH mylist "a" "b" "c"
redis-cli LLEN mylist
# Returns: 3
```

#### LRANGE
**Syntax:** `LRANGE key start stop`  
**Description:** Get a range of elements from a list  
**Returns:** Array of elements in the specified range  
**Example:**
```bash
redis-cli RPUSH mylist "a" "b" "c" "d" "e"
redis-cli LRANGE mylist 0 2
# Returns: ["a", "b", "c"]
redis-cli LRANGE mylist -2 -1
# Returns: ["d", "e"]
```

#### BLPOP
**Syntax:** `BLPOP key [key ...] timeout`  
**Description:** Remove and get the first element in a list, block until available or timeout  
**Returns:** Array with key name and popped element, or null after timeout  
**Example:**
```bash
# Terminal 1
redis-cli BLPOP mylist 30  # Blocks for up to 30 seconds

# Terminal 2
redis-cli LPUSH mylist "hello"

# Terminal 1 receives: ["mylist", "hello"]
```

#### BRPOP
**Syntax:** `BRPOP key [key ...] timeout`  
**Description:** Remove and get the last element in a list, block until available or timeout  
**Returns:** Array with key name and popped element, or null after timeout  
**Example:**
```bash
# Terminal 1
redis-cli BRPOP mylist 30  # Blocks for up to 30 seconds

# Terminal 2
redis-cli RPUSH mylist "world"

# Terminal 1 receives: ["mylist", "world"]
```

---

### 🎯 Sorted Set Commands

#### ZADD
**Syntax:** `ZADD key score member [score member ...]`  
**Description:** Add members with scores to a sorted set  
**Returns:** The number of elements added (not counting score updates)  
**Example:**
```bash
redis-cli ZADD leaderboard 100 "alice" 200 "bob" 150 "charlie"
# Returns: 3
```

#### ZRANGE
**Syntax:** `ZRANGE key start stop [WITHSCORES]`  
**Description:** Return a range of members in a sorted set by index  
**Returns:** Array of members, optionally with scores  
**Example:**
```bash
redis-cli ZADD leaderboard 100 "alice" 200 "bob" 150 "charlie"
redis-cli ZRANGE leaderboard 0 -1
# Returns: ["alice", "charlie", "bob"]
redis-cli ZRANGE leaderboard 0 -1 WITHSCORES
# Returns: ["alice", "100", "charlie", "150", "bob", "200"]
```

#### ZRANK
**Syntax:** `ZRANK key member`  
**Description:** Return the rank of member in the sorted set  
**Returns:** The rank (0-based) or null if member doesn't exist  
**Example:**
```bash
redis-cli ZADD leaderboard 100 "alice" 200 "bob" 150 "charlie"
redis-cli ZRANK leaderboard "charlie"
# Returns: 1 (second position when sorted by score)
```

#### ZSCORE
**Syntax:** `ZSCORE key member`  
**Description:** Return the score of member in the sorted set  
**Returns:** The score or null if member doesn't exist  
**Example:**
```bash
redis-cli ZADD leaderboard 100 "alice" 200 "bob"
redis-cli ZSCORE leaderboard "alice"
# Returns: "100"
```

#### ZCARD
**Syntax:** `ZCARD key`  
**Description:** Get the number of members in a sorted set  
**Returns:** The cardinality (number of elements)  
**Example:**
```bash
redis-cli ZADD leaderboard 100 "alice" 200 "bob" 150 "charlie"
redis-cli ZCARD leaderboard
# Returns: 3
```

#### ZREM
**Syntax:** `ZREM key member [member ...]`  
**Description:** Remove members from a sorted set  
**Returns:** The number of members removed  
**Example:**
```bash
redis-cli ZADD leaderboard 100 "alice" 200 "bob" 150 "charlie"
redis-cli ZREM leaderboard "alice" "bob"
# Returns: 2
```

---

### 🌊 Stream Commands

#### XADD
**Syntax:** `XADD key ID field value [field value ...]`  
**Description:** Append a new entry to a stream  
**Returns:** The ID of the added entry  
**Example:**
```bash
redis-cli XADD mystream * name "Alice" age "30"
# Returns: "1693123456789-0" (timestamp-sequence)
```

#### XRANGE
**Syntax:** `XRANGE key start end [COUNT count]`  
**Description:** Return a range of elements in a stream  
**Returns:** Array of stream entries  
**Example:**
```bash
redis-cli XADD mystream * name "Alice" age "30"
redis-cli XADD mystream * name "Bob" age "25"
redis-cli XRANGE mystream - +
# Returns entries with their IDs and field-value pairs
```

#### XREAD
**Syntax:** `XREAD [COUNT count] [BLOCK milliseconds] STREAMS key [key ...] id [id ...]`  
**Description:** Read data from streams, optionally blocking  
**Returns:** Array of stream data  
**Example:**
```bash
# Non-blocking read
redis-cli XREAD COUNT 2 STREAMS mystream 0

# Blocking read (waits for new entries)
redis-cli XREAD BLOCK 5000 STREAMS mystream $
```

**Note:** The following stream commands are NOT implemented:
- `XLEN` - Get stream length
- `XDEL` - Delete stream entries  
- `XTRIM` - Trim stream to size

---

### 📡 Pub/Sub Commands

#### PUBLISH
**Syntax:** `PUBLISH channel message`  
**Description:** Post a message to a channel  
**Returns:** The number of clients that received the message  
**Example:**
```bash
redis-cli PUBLISH news "Breaking: Redis is awesome!"
# Returns: 2 (if 2 clients are subscribed to 'news')
```

#### SUBSCRIBE
**Syntax:** `SUBSCRIBE channel [channel ...]`  
**Description:** Subscribe to channels  
**Returns:** Subscription confirmations and messages  
**Example:**
```bash
redis-cli SUBSCRIBE news sports
# Client enters subscription mode and receives messages
```

#### UNSUBSCRIBE
**Syntax:** `UNSUBSCRIBE [channel ...]`  
**Description:** Unsubscribe from channels  
**Returns:** Unsubscription confirmations  
**Example:**
```bash
redis-cli UNSUBSCRIBE news
# Unsubscribes from 'news' channel
redis-cli UNSUBSCRIBE
# Unsubscribes from all channels
```

#### PSUBSCRIBE
**Syntax:** `PSUBSCRIBE pattern [pattern ...]`  
**Description:** Subscribe to channels matching patterns  
**Returns:** Pattern subscription confirmations and matching messages  
**Example:**
```bash
redis-cli PSUBSCRIBE news:*
# Subscribes to all channels starting with 'news:'
```

#### PUNSUBSCRIBE
**Syntax:** `PUNSUBSCRIBE [pattern ...]`  
**Description:** Unsubscribe from patterns  
**Returns:** Pattern unsubscription confirmations  
**Example:**
```bash
redis-cli PUNSUBSCRIBE news:*
# Unsubscribes from pattern 'news:*'
```

---

### 🔒 Transaction Commands

#### MULTI
**Syntax:** `MULTI`  
**Description:** Start a transaction  
**Returns:** OK  
**Example:**
```bash
redis-cli MULTI
redis-cli SET key1 "value1"
redis-cli SET key2 "value2"
redis-cli EXEC
# Executes both SET commands atomically
```

#### EXEC
**Syntax:** `EXEC`  
**Description:** Execute all commands issued after MULTI  
**Returns:** Array of results from executed commands  
**Example:**
```bash
redis-cli MULTI
redis-cli INCR counter
redis-cli INCR counter
redis-cli EXEC
# Returns: [1, 2] (results of both INCR commands)
```

#### DISCARD
**Syntax:** `DISCARD`  
**Description:** Discard all commands issued after MULTI  
**Returns:** OK  
**Example:**
```bash
redis-cli MULTI
redis-cli SET key "value"
redis-cli DISCARD
# Transaction is cancelled, SET command not executed
```

#### WATCH
**Syntax:** `WATCH key [key ...]`  
**Description:** Watch keys to determine execution of the MULTI/EXEC block  
**Returns:** OK  
**Example:**
```bash
redis-cli WATCH mykey
redis-cli MULTI
redis-cli SET mykey "newvalue"
redis-cli EXEC
# EXEC returns null if mykey was modified between WATCH and EXEC
```

#### UNWATCH
**Syntax:** `UNWATCH`  
**Description:** Forget about all watched keys  
**Returns:** OK  
**Example:**
```bash
redis-cli WATCH key1 key2
redis-cli UNWATCH
# No longer watching any keys
```

---

### 🔄 Replication Commands

#### PSYNC
**Syntax:** `PSYNC replicationid offset`  
**Description:** Internal command used for replication synchronization  
**Returns:** Synchronization response (used internally by replicas)  
**Note:** This is primarily used internally by replica servers

#### REPLCONF
**Syntax:** `REPLCONF option value`  
**Description:** Configuration command for replication  
**Returns:** OK or configuration response  
**Example:**
```bash
# Used internally during replication handshake
REPLCONF listening-port 6380
REPLCONF GETACK *
```

#### WAIT
**Syntax:** `WAIT numreplicas timeout`  
**Description:** Block until the specified number of replicas acknowledge writes  
**Returns:** The number of replicas that acknowledged the writes  
**Example:**
```bash
redis-cli SET important-data "critical-value"
redis-cli WAIT 2 1000
# Returns: 2 (if 2 replicas acknowledged within 1 second)
```

---

### ⚙️ Server Commands

#### PING
**Syntax:** `PING [message]`  
**Description:** Ping the server  
**Returns:** PONG or the provided message  
**Example:**
```bash
redis-cli PING
# Returns: PONG
redis-cli PING "Hello Redis"
# Returns: "Hello Redis"
```

#### ECHO
**Syntax:** `ECHO message`  
**Description:** Echo the given string  
**Returns:** The message  
**Example:**
```bash
redis-cli ECHO "Hello World"
# Returns: "Hello World"
```

#### TYPE
**Syntax:** `TYPE key`  
**Description:** Determine the type stored at key  
**Returns:** The type of the key  
**Example:**
```bash
redis-cli SET mystring "hello"
redis-cli LPUSH mylist "item"
redis-cli TYPE mystring
# Returns: "string"
redis-cli TYPE mylist
# Returns: "list"
```

#### KEYS
**Syntax:** `KEYS pattern`  
**Description:** Find all keys matching the given pattern  
**Returns:** Array of matching keys  
**Example:**
```bash
redis-cli SET user:1 "Alice"
redis-cli SET user:2 "Bob"
redis-cli SET product:1 "Laptop"
redis-cli KEYS "user:*"
# Returns: ["user:1", "user:2"]
```

#### INFO
**Syntax:** `INFO [section]`  
**Description:** Get information and statistics about the server  
**Returns:** Server information  
**Example:**
```bash
redis-cli INFO
# Returns comprehensive server information
redis-cli INFO replication
# Returns only replication-related information
```

#### CONFIG
**Syntax:** `CONFIG GET parameter` / `CONFIG SET parameter value`  
**Description:** Get or set configuration parameters  
**Returns:** Configuration values or OK  
**Example:**
```bash
redis-cli CONFIG GET "*port*"
# Returns configuration parameters matching the pattern
```

#### FLUSHALL`
**Syntax:** `FLUSHALL` / `FLUSHALL SYNC` / `FLUSHALL ASYNC`

**Description:** Removes all Keys Synchronously or Asynchronously

**Returns:**  OK  
**Example:**
```bash
redis-cli FLUSHALL 
# Returns "OK"
```


**Note:** The following commonly used commands are NOT implemented:
- `EXISTS` - Check if key exists
- `DEL` - Delete keys  
- `EXPIRE` - Set key expiration
- `TTL` - Get time to live
- `APPEND` - Append to string
- `STRLEN` - Get string length

---

## 🎯 Command Usage Patterns

### Basic Key-Value Operations
```bash
# Set and get values
redis-cli SET user:1:name "Alice Johnson"
redis-cli GET user:1:name

# Work with counters
redis-cli SET page:views 1000
redis-cli INCR page:views
redis-cli INCR page:views
redis-cli GET page:views  # Returns: "1002"
```

### List Operations for Queues
```bash
# Producer adds items to queue
redis-cli LPUSH task:queue "process-payment"
redis-cli LPUSH task:queue "send-email"

# Consumer processes items
redis-cli BRPOP task:queue 0  # Blocks until item available
```

### Sorted Sets for Leaderboards
```bash
# Add player scores
redis-cli ZADD game:leaderboard 1500 "player1"
redis-cli ZADD game:leaderboard 1200 "player2"
redis-cli ZADD game:leaderboard 1800 "player3"

# Get top players
redis-cli ZRANGE game:leaderboard 0 2 WITHSCORES
```

### Streams for Event Logging
```bash
# Log events
redis-cli XADD events:login * user "alice" timestamp "2023-08-27T10:30:00"
redis-cli XADD events:login * user "bob" timestamp "2023-08-27T10:31:00"

# Read recent events
redis-cli XREAD COUNT 10 STREAMS events:login 0
```

### Pub/Sub for Real-time Messages
```bash
# Terminal 1: Subscribe to notifications
redis-cli SUBSCRIBE user:notifications

# Terminal 2: Send notification
redis-cli PUBLISH user:notifications "You have a new message!"
```

### Transactions for Atomic Operations
```bash
# Transfer credits between accounts
redis-cli WATCH account:alice account:bob
redis-cli MULTI
redis-cli DECRBY account:alice 100
redis-cli INCRBY account:bob 100
redis-cli EXEC
```

---

## 🔧 Implementation Notes

### Data Type Compatibility
- **Strings**: Redis-compatible string operations with binary safety.
- **Lists**: Implemented using QuickList (segmented linked list of arrays) for memory efficiency and O(1) amortized push/pop at both ends.
- **Sorted Sets**: Implemented using a thread-safe skip list structure (`QuickZSet`), which combines a `ConcurrentSkipListMap<Double, ConcurrentSkipListSet<String>>` for score ordering and a `ConcurrentHashMap<String, Double>` for fast member lookups. This provides O(log n) operations for add, remove, and range queries, and supports efficient rank and score retrieval.
- **Streams**: Time-series data with microsecond precision.
- **Pub/Sub**: Pattern matching with glob-style wildcards.

### Performance Characteristics
- **String Operations**: O(1) for GET/SET, O(k) for INCR/DECR where k is the number of digits.
- **List Operations**: O(1) amortized for LPUSH/RPUSH/LPOP/RPOP (QuickList segments minimize pointer overhead), O(n) for LRANGE where n is the range size.
- **Sorted Set Operations**: O(log n) for ZADD/ZREM, O(log n + m) for range operations (where m is the number of returned elements). `QuickZSet` ensures thread safety and efficient concurrent access.
- **Stream Operations**: O(1) for XADD, O(n) for range queries where n is the number of returned entries.

**QuickList Notes:**  
- Each node is a fixed-size array (default size from config), linked for efficient memory usage.
- Push/pop at either end is O(1) amortized; splitting/merging nodes is rare and fast.
- LRANGE and iteration are O(n) where n is the number of elements traversed.
- Memory usage is compact due to array packing and minimal per-node overhead.
- Thread-safe via StampedLock for concurrent access.
- No random access (LINDEX/LSET not implemented).
