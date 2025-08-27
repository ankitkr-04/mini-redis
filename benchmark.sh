#!/bin/bash

# Redis Full Command Benchmark Script
# Tests all 25 implemented commands in your Mini Redis Server

HOST="127.0.0.1"
PORT="6379"
CLIENTS=50
REQUESTS=10000
DATASIZE=3

echo "====== MINI REDIS FULL BENCHMARK ======"
echo "Testing all implemented commands..."
echo ""

# Basic String Operations
echo "====== STRING COMMANDS ======"
echo "Testing SET..."
redis-benchmark -h $HOST -p $PORT -n $REQUESTS -c $CLIENTS -d $DATASIZE -t set

echo "Testing GET..."
redis-benchmark -h $HOST -p $PORT -n $REQUESTS -c $CLIENTS -d $DATASIZE -t get

echo "Testing INCR..."
redis-benchmark -h $HOST -p $PORT -n $REQUESTS -c $CLIENTS -t incr

echo "Testing DECR..."
redis-cli -h $HOST -p $PORT FLUSHALL 2>/dev/null || true
for i in {1..1000}; do
    redis-cli -h $HOST -p $PORT SET "counter:$i" 1000 >/dev/null
done
redis-benchmark -h $HOST -p $PORT -n $REQUESTS -c $CLIENTS -r 1000 DECR counter:__rand_int__

echo ""
echo "====== LIST COMMANDS ======"

echo "Testing LPUSH..."
redis-benchmark -h $HOST -p $PORT -n $REQUESTS -c $CLIENTS -t lpush

echo "Testing RPUSH..."
redis-benchmark -h $HOST -p $PORT -n $REQUESTS -c $CLIENTS -t rpush

echo "Testing LPOP..."
# Pre-populate lists for LPOP test
redis-cli -h $HOST -p $PORT FLUSHALL 2>/dev/null || true
for i in {1..1000}; do
    redis-cli -h $HOST -p $PORT LPUSH "list:$i" "item1" "item2" "item3" "item4" "item5" >/dev/null
done
redis-benchmark -h $HOST -p $PORT -n $REQUESTS -c $CLIENTS -r 1000 LPOP list:__rand_int__

echo "Testing RPOP..."
# Pre-populate lists for RPOP test
redis-cli -h $HOST -p $PORT FLUSHALL 2>/dev/null || true
for i in {1..1000}; do
    redis-cli -h $HOST -p $PORT RPUSH "list:$i" "item1" "item2" "item3" "item4" "item5" >/dev/null
done
redis-benchmark -h $HOST -p $PORT -n $REQUESTS -c $CLIENTS -r 1000 RPOP list:__rand_int__

echo "Testing LLEN..."
redis-benchmark -h $HOST -p $PORT -n $REQUESTS -c $CLIENTS -t llen

echo "Testing LRANGE..."
# Pre-populate lists for LRANGE test
redis-cli -h $HOST -p $PORT FLUSHALL 2>/dev/null || true
for i in {1..1000}; do
    redis-cli -h $HOST -p $PORT RPUSH "list:$i" $(seq 1 20) >/dev/null
done
redis-benchmark -h $HOST -p $PORT -n $REQUESTS -c $CLIENTS -r 1000 LRANGE list:__rand_int__ 0 9

echo ""
echo "====== SORTED SET COMMANDS ======"

echo "Testing ZADD..."
redis-benchmark -h $HOST -p $PORT -n $REQUESTS -c $CLIENTS -t zadd

echo "Testing ZRANGE..."
# Pre-populate sorted sets
redis-cli -h $HOST -p $PORT FLUSHALL 2>/dev/null || true
for i in {1..1000}; do
    for j in {1..20}; do
        redis-cli -h $HOST -p $PORT ZADD "zset:$i" $j "member:$j" >/dev/null
    done
done
redis-benchmark -h $HOST -p $PORT -n $REQUESTS -c $CLIENTS -r 1000 ZRANGE zset:__rand_int__ 0 9

echo "Testing ZRANK..."
redis-benchmark -h $HOST -p $PORT -n $REQUESTS -c $CLIENTS -r 1000 -r 20 ZRANK zset:__rand_int__ member:__rand_int__

echo "Testing ZSCORE..."
redis-benchmark -h $HOST -p $PORT -n $REQUESTS -c $CLIENTS -r 1000 -r 20 ZSCORE zset:__rand_int__ member:__rand_int__

echo "Testing ZCARD..."
redis-benchmark -h $HOST -p $PORT -n $REQUESTS -c $CLIENTS -r 1000 ZCARD zset:__rand_int__

echo "Testing ZREM..."
redis-benchmark -h $HOST -p $PORT -n $REQUESTS -c $CLIENTS -r 1000 -r 20 ZREM zset:__rand_int__ member:__rand_int__

echo ""
echo "====== SERVER COMMANDS ======"

echo "Testing PING..."
redis-benchmark -h $HOST -p $PORT -n $REQUESTS -c $CLIENTS -t ping

echo "Testing ECHO..."
redis-benchmark -h $HOST -p $PORT -n $REQUESTS -c $CLIENTS ECHO "hello world"

echo "Testing TYPE..."
# Pre-populate various data types
redis-cli -h $HOST -p $PORT FLUSHALL 2>/dev/null || true
for i in {1..1000}; do
    redis-cli -h $HOST -p $PORT SET "string:$i" "value" >/dev/null
    redis-cli -h $HOST -p $PORT LPUSH "list:$i" "item" >/dev/null
    redis-cli -h $HOST -p $PORT ZADD "zset:$i" 1 "member" >/dev/null
done
redis-benchmark -h $HOST -p $PORT -n $REQUESTS -c $CLIENTS -r 1000 TYPE string:__rand_int__

echo ""
echo "====== STREAM COMMANDS (Manual Test) ======"
echo "Note: XADD, XRANGE, XREAD require manual testing due to complex syntax"

echo "Testing XADD performance..."
# Simple XADD benchmark
start_time=$(date +%s.%N)
for i in {1..1000}; do
    redis-cli -h $HOST -p $PORT XADD "stream:test" "*" "field" "value$i" >/dev/null
done
end_time=$(date +%s.%N)
duration=$(echo "$end_time - $start_time" | bc)
throughput=$(echo "scale=2; 1000 / $duration" | bc)
echo "XADD: 1000 requests completed in $duration seconds"
echo "XADD Throughput: $throughput req/sec"

echo ""
echo "====== TRANSACTION COMMANDS (Manual Test) ======"
echo "Note: MULTI/EXEC/WATCH require manual testing due to stateful nature"

echo "Testing MULTI/EXEC performance..."
start_time=$(date +%s.%N)
for i in {1..1000}; do
    {
        echo "MULTI"
        echo "SET key:$i value:$i"
        echo "INCR counter"
        echo "EXEC"
    } | redis-cli -h $HOST -p $PORT --pipe >/dev/null
done
end_time=$(date +%s.%N)
duration=$(echo "$end_time - $start_time" | bc)
throughput=$(echo "scale=2; 1000 / $duration" | bc)
echo "MULTI/EXEC: 1000 transactions completed in $duration seconds"
echo "Transaction Throughput: $throughput transactions/sec"

echo ""
echo "====== CUSTOM BENCHMARKS FOR MISSING COMMANDS ======"

echo "Testing KEYS..."
redis-cli -h $HOST -p $PORT FLUSHALL 2>/dev/null || true
for i in {1..1000}; do
    redis-cli -h $HOST -p $PORT SET "user:$i" "data" >/dev/null
    redis-cli -h $HOST -p $PORT SET "product:$i" "info" >/dev/null
done
start_time=$(date +%s.%N)
for i in {1..100}; do
    redis-cli -h $HOST -p $PORT KEYS "user:*" >/dev/null
done
end_time=$(date +%s.%N)
duration=$(echo "$end_time - $start_time" | bc)
throughput=$(echo "scale=2; 100 / $duration" | bc)
echo "KEYS: 100 pattern matches completed in $duration seconds"
echo "KEYS Throughput: $throughput req/sec"

echo "Testing INFO..."
redis-benchmark -h $HOST -p $PORT -n 1000 -c 10 INFO

echo ""
echo "====== BLOCKING OPERATIONS TEST ======"
echo "Note: BLPOP/BRPOP require separate terminals for proper testing"
echo "Run this in terminal 1: redis-cli BLPOP testlist 10"
echo "Run this in terminal 2: redis-cli LPUSH testlist item"

echo ""
echo "====== PUB/SUB TEST ======"
echo "Note: PUBLISH/SUBSCRIBE require separate terminals"
echo "Run this in terminal 1: redis-cli SUBSCRIBE testchannel"
echo "Run this in terminal 2: redis-cli PUBLISH testchannel 'hello'"

echo ""
echo "====== BENCHMARK COMPLETE ======"
echo "Summary of tested commands:"
echo "✓ String: GET, SET, INCR, DECR"
echo "✓ List: LPUSH, RPUSH, LPOP, RPOP, LLEN, LRANGE"
echo "✓ Sorted Set: ZADD, ZRANGE, ZRANK, ZSCORE, ZCARD, ZREM"  
echo "✓ Server: PING, ECHO, TYPE, KEYS, INFO"
echo "✓ Stream: XADD (custom test)"
echo "✓ Transaction: MULTI/EXEC (custom test)"
echo "⚠ Manual testing required: BLPOP, BRPOP, SUBSCRIBE, PUBLISH, XREAD, WATCH"