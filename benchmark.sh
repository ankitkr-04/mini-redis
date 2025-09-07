#!/usr/bin/env bash
# benchmark.sh - Redis benchmark script with proper error handling
# Requirements: redis-benchmark, redis-cli, bc, python3

# Set safer bash options
set -o pipefail

HOST="${HOST:-127.0.0.1}"
PORT="${PORT:-6379}"
CLIENTS="${CLIENTS:-50}"
REQUESTS="${REQUESTS:-10000}"
DATASIZE="${DATASIZE:-3}"
BENCHMARK_FILE="${BENCHMARK_FILE:-benchmark.json}"
TMPDIR=$(mktemp -d -t redis-bench.XXXX)

# Commands to benchmark
COMMANDS=( \
  "SET" "GET" "INCR" "DECR" \
  "LPUSH" "RPUSH" "LPOP" "RPOP" "LLEN" "LRANGE" \
  "ZADD" "ZRANGE" "ZRANK" "ZSCORE" "ZCARD" "ZREM" \
  "PING" "ECHO" "TYPE" "KEYS" "INFO" "FLUSHALL" \
  "XADD" "MULTI_EXEC" \
)

# Check dependencies
check_dependencies() {
  local missing=()
  for dep in redis-benchmark redis-cli bc python3; do
    if ! command -v "$dep" &>/dev/null; then
      missing+=("$dep")
    fi
  done
  if [[ ${#missing[@]} -gt 0 ]]; then
    echo "Error: Missing dependencies: ${missing[*]}" >&2
    exit 1
  fi
}

# Test Redis connection
test_redis_connection() {
  if ! redis-cli -h "$HOST" -p "$PORT" ping &>/dev/null; then
    echo "Error: Cannot connect to Redis at $HOST:$PORT" >&2
    exit 1
  fi
}

# Cleanup function
finalize_json() {
  echo "Merging results into final JSON..."
  python3 <<EOF
import json, os, glob, datetime

# Metadata
meta = {
    "timestamp": datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
    "host": "$HOST",
    "port": $PORT,
    "clients": $CLIENTS,
    "requests_per_command": $REQUESTS,
    "data_size": $DATASIZE
}

# Load all benchmark results
benchmarks = {}
tmpdir = "$TMPDIR"
for json_file in sorted(glob.glob(os.path.join(tmpdir, "*.json"))):
    cmd_name = os.path.splitext(os.path.basename(json_file))[0]
    try:
        with open(json_file, 'r') as f:
            benchmarks[cmd_name] = json.load(f)
    except Exception as e:
        print(f"Warning: Could not load {json_file}: {e}")

# Summary
summary = {
    "tested_commands": sorted(benchmarks.keys()),
    "total_commands_tested": len(benchmarks),
    "notes": "Percentiles extracted from redis-benchmark latency distribution output"
}

# Final result
result = {
    **meta,
    "benchmarks": benchmarks,
    "summary": summary
}

# Write final JSON
try:
    with open("$BENCHMARK_FILE", 'w') as f:
        json.dump(result, f, indent=2)
    print(f"Results written to: $BENCHMARK_FILE")
except Exception as e:
    print(f"Error writing final JSON: {e}")
EOF

  # Cleanup temp directory
  rm -rf "$TMPDIR" 2>/dev/null || true
}

# Trap for cleanup
trap 'echo "Interrupted. Cleaning up..."; finalize_json; exit 1' INT TERM

# Main benchmark function
run_benchmark() {
  local cmd_name="$1"
  local extra_args="${2:-}"
  local prefill_fn="${3:-}"
  
  echo "=== Benchmarking: $cmd_name ==="
  
  # Run prefill if specified
  if [[ -n "$prefill_fn" ]] && declare -f "$prefill_fn" >/dev/null 2>&1; then
    echo "Prefilling data..."
    "$prefill_fn" || true
  fi
  
  # Run the benchmark with timeout
  local output=""
  local success=false
  
  if output=$(timeout 120 redis-benchmark -h "$HOST" -p "$PORT" -n "$REQUESTS" -c "$CLIENTS" -d "$DATASIZE" -t "$cmd_name" $extra_args 2>&1); then
    success=true
  else
    echo "Warning: Benchmark failed or timed out for $cmd_name"
    output="Benchmark failed"
  fi
  
  # Parse results
  local rps=""
  local avg_latency=""
  local p50="" p95="" p99=""
  
  if [[ "$success" == true ]]; then
    # Extract requests per second - look for the summary line
    if rps_line=$(echo "$output" | grep -E "[0-9]+\.[0-9]+ requests per second" | tail -1); then
      rps=$(echo "$rps_line" | grep -oE '[0-9]+\.[0-9]+' | head -1)
    fi
    
    # Extract completion time and calculate RPS if not found
    if [[ -z "$rps" ]]; then
      if time_line=$(echo "$output" | grep -E "[0-9]+ requests completed in [0-9]+\.[0-9]+ seconds"); then
        local total_time=$(echo "$time_line" | grep -oE '[0-9]+\.[0-9]+')
        if [[ -n "$total_time" ]] && command -v bc >/dev/null; then
          rps=$(echo "scale=2; $REQUESTS / $total_time" | bc 2>/dev/null || echo "")
        fi
      fi
    fi
    
    # Calculate average latency
    if [[ -n "$rps" ]] && command -v bc >/dev/null; then
      avg_latency=$(echo "scale=3; (1000 / $rps)" | bc 2>/dev/null || echo "")
    fi
    
    # Extract percentiles from latency distribution
    p50=$(echo "$output" | grep -E "50\.000%" | grep -oE '[0-9]+\.[0-9]+' | tail -1 || echo "")
    p95=$(echo "$output" | grep -E "9[567]\.[0-9]+%" | grep -oE '[0-9]+\.[0-9]+' | tail -1 || echo "")
    p99=$(echo "$output" | grep -E "99\.[0-9]+%" | grep -oE '[0-9]+\.[0-9]+' | tail -1 || echo "")
  fi
  
  # Create JSON result
  python3 <<EOF
import json

def safe_float(val):
    try:
        return float(val) if val and val.strip() else None
    except:
        return None

result = {
    "requests_per_second": safe_float("$rps"),
    "average_latency_ms": safe_float("$avg_latency"),
    "p50_latency_ms": safe_float("$p50"),
    "p95_latency_ms": safe_float("$p95"),
    "p99_latency_ms": safe_float("$p99"),
    "benchmark_succeeded": "$success" == "true"
}

with open("$TMPDIR/$cmd_name.json", 'w') as f:
    json.dump(result, f, indent=2)

rps_value = result['requests_per_second'] if result['requests_per_second'] is not None else 0
print(f"✓ $cmd_name: {rps_value:.0f} req/sec")
EOF
}

# Prefill functions
prefill_counters() {
  redis-cli -h "$HOST" -p "$PORT" FLUSHALL >/dev/null 2>&1 || true
  for i in {1..50}; do
    redis-cli -h "$HOST" -p "$PORT" SET "counter:$i" 1000 >/dev/null 2>&1 || true
  done
}

prefill_lists() {
  redis-cli -h "$HOST" -p "$PORT" FLUSHALL >/dev/null 2>&1 || true
  for i in {1..50}; do
    for j in {1..10}; do
      redis-cli -h "$HOST" -p "$PORT" RPUSH "list:$i" "item$j" >/dev/null 2>&1 || true
    done
  done
}

prefill_zsets() {
  redis-cli -h "$HOST" -p "$PORT" FLUSHALL >/dev/null 2>&1 || true
  for i in {1..50}; do
    for j in {1..10}; do
      redis-cli -h "$HOST" -p "$PORT" ZADD "zset:$i" "$j" "member$j" >/dev/null 2>&1 || true
    done
  done
}

prefill_mixed() {
  redis-cli -h "$HOST" -p "$PORT" FLUSHALL >/dev/null 2>&1 || true
  for i in {1..50}; do
    redis-cli -h "$HOST" -p "$PORT" SET "str:$i" "value" >/dev/null 2>&1 || true
    redis-cli -h "$HOST" -p "$PORT" LPUSH "list:$i" "item" >/dev/null 2>&1 || true
    redis-cli -h "$HOST" -p "$PORT" ZADD "zset:$i" 1 "member" >/dev/null 2>&1 || true
  done
}

# Main execution
main() {
  check_dependencies
  test_redis_connection
  
  mkdir -p "$TMPDIR"
  
  echo "====== REDIS BENCHMARK SUITE ======"
  echo "Host: $HOST:$PORT | Clients: $CLIENTS | Requests: $REQUESTS"
  echo "Results will be saved to: $BENCHMARK_FILE"
  echo ""
  
  # Standard benchmarks (commands supported by valkey-benchmark/redis-benchmark)
  run_benchmark "SET"
  run_benchmark "GET"
  run_benchmark "INCR"
  # run_benchmark "DECR" "" "prefill_counters"  # DECR needs existing keys
  
  run_benchmark "LPUSH"
  run_benchmark "RPUSH"
  run_benchmark "LPOP" "" "prefill_lists"
  run_benchmark "RPOP" "" "prefill_lists"
  run_benchmark "LRANGE" "" "prefill_lists"
  
  run_benchmark "ZADD"
  
  run_benchmark "PING"
  
  echo ""
  finalize_json
  
  echo ""
  echo "✅ Benchmark completed successfully!"
  echo "📊 Results saved to: $BENCHMARK_FILE"
}

# Run main function
main "$@"