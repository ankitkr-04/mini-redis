#!/bin/bash

# Mini Redis Server - Management Script
# All-in-one script for compilation, server management, and development

set -e  # Exit on any error

# Configuration
PROJECT_NAME="mini-redis-server"
MAIN_CLASS="server.RedisServer"
JAR_NAME="mini-redis-server.jar"
DEFAULT_PORT=6379
PID_DIR="./pids"
LOG_DIR="./logs"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Create necessary directories
mkdir -p "$PID_DIR" "$LOG_DIR"

# Utility functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if Java is available
check_java() {
    if ! command -v java &> /dev/null; then
        log_error "Java is not installed or not in PATH"
        exit 1
    fi
    
    local java_version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
    log_info "Using Java version: $java_version"
}

# Check if Maven is available
check_maven() {
    if ! command -v mvn &> /dev/null; then
        log_error "Maven is not installed or not in PATH"
        exit 1
    fi
}

# Compile the project
compile() {
    log_info "Compiling project..."
    check_maven
    
    if mvn clean compile -q; then
        log_success "Compilation successful"
    else
        log_error "Compilation failed"
        exit 1
    fi
}

# Package the project
package() {
    log_info "Packaging project..."
    check_maven
    
    if mvn package -q; then
        log_success "Packaging successful"
    else
        log_error "Packaging failed"
        exit 1
    fi
}

# Check if server is running on a port
is_port_in_use() {
    local port=$1
    if command -v netstat &> /dev/null; then
        netstat -tuln | grep -q ":$port "
    elif command -v ss &> /dev/null; then
        ss -tuln | grep -q ":$port "
    else
        # Fallback: try to connect to the port
        (echo > /dev/tcp/localhost/$port) &>/dev/null
    fi
}

# Get PID file path for a given port
get_pid_file() {
    local port=$1
    echo "$PID_DIR/redis-server-$port.pid"
}

# Get log file path for a given port
get_log_file() {
    local port=$1
    echo "$LOG_DIR/redis-server-$port.log"
}

# Start a Redis server instance
start_server() {
    local port=$1
    local args=("${@:2}")  # All arguments except the first
    
    check_java
    
    # Check if JAR exists, compile if not
    if [ ! -f "target/$JAR_NAME" ]; then
        log_warning "JAR file not found, compiling project..."
        package
    fi
    
    # Check if port is already in use
    if is_port_in_use "$port"; then
        log_error "Port $port is already in use"
        return 1
    fi
    
    local pid_file=$(get_pid_file "$port")
    local log_file=$(get_log_file "$port")
    
    # Check if PID file exists and process is running
    if [ -f "$pid_file" ]; then
        local existing_pid=$(cat "$pid_file")
        if kill -0 "$existing_pid" 2>/dev/null; then
            log_error "Server already running on port $port (PID: $existing_pid)"
            return 1
        else
            log_warning "Removing stale PID file for port $port"
            rm -f "$pid_file"
        fi
    fi
    
    log_info "Starting Redis server on port $port..."
    
    # Prepare command arguments
    local cmd_args=("--port" "$port")
    cmd_args+=("${args[@]}")
    
    # Start the server in background
    nohup java -cp "target/$JAR_NAME" "$MAIN_CLASS" "${cmd_args[@]}" > "$log_file" 2>&1 &
    local pid=$!
    
    # Save PID
    echo "$pid" > "$pid_file"
    
    # Wait a moment and check if process is still running
    sleep 2
    if kill -0 "$pid" 2>/dev/null; then
        log_success "Server started successfully on port $port (PID: $pid)"
        log_info "Logs: tail -f $log_file"
    else
        log_error "Server failed to start on port $port"
        log_info "Check logs: cat $log_file"
        rm -f "$pid_file"
        return 1
    fi
}

# Stop a Redis server instance
stop_server() {
    local port=$1
    local pid_file=$(get_pid_file "$port")
    
    if [ ! -f "$pid_file" ]; then
        log_warning "No PID file found for port $port"
        return 0
    fi
    
    local pid=$(cat "$pid_file")
    
    if kill -0 "$pid" 2>/dev/null; then
        log_info "Stopping server on port $port (PID: $pid)..."
        kill "$pid"
        
        # Wait for graceful shutdown
        local count=0
        while kill -0 "$pid" 2>/dev/null && [ $count -lt 10 ]; do
            sleep 1
            ((count++))
        done
        
        # Force kill if still running
        if kill -0 "$pid" 2>/dev/null; then
            log_warning "Force killing server on port $port"
            kill -9 "$pid"
        fi
        
        log_success "Server on port $port stopped"
    else
        log_warning "Process $pid not running"
    fi
    
    rm -f "$pid_file"
}

# Stop all Redis servers
stop_all() {
    log_info "Stopping all Redis servers..."
    
    for pid_file in "$PID_DIR"/redis-server-*.pid; do
        if [ -f "$pid_file" ]; then
            local port=$(basename "$pid_file" .pid | sed 's/redis-server-//')
            stop_server "$port"
        fi
    done
    
    log_success "All servers stopped"
}

# Show status of all servers
status() {
    log_info "Redis Server Status:"
    echo
    
    local running_count=0
    
    for pid_file in "$PID_DIR"/redis-server-*.pid; do
        if [ -f "$pid_file" ]; then
            local port=$(basename "$pid_file" .pid | sed 's/redis-server-//')
            local pid=$(cat "$pid_file")
            
            if kill -0 "$pid" 2>/dev/null; then
                echo -e "  Port $port: ${GREEN}RUNNING${NC} (PID: $pid)"
                running_count=$((running_count + 1))
            else
                echo -e "  Port $port: ${RED}STOPPED${NC} (stale PID file)"
                rm -f "$pid_file"
            fi
        fi
    done
    
    if [ $running_count -eq 0 ]; then
        echo -e "  ${YELLOW}No servers running${NC}"
    fi
    echo
}

# Show logs for a specific port
logs() {
    local port=$1
    local log_file=$(get_log_file "$port")
    
    if [ -f "$log_file" ]; then
        tail -f "$log_file"
    else
        log_error "No log file found for port $port"
        exit 1
    fi
}

# Start master-replica cluster
start_cluster() {
    local master_port=${1:-6379}
    local replica_count=${2:-2}
    local base_replica_port=${3:-6380}
    
    log_info "Starting Redis cluster: 1 master + $replica_count replicas"
    
    # Start master
    start_server "$master_port"
    
    # Wait for master to be ready
    sleep 3
    
    # Start replicas
    for ((i=0; i<replica_count; i++)); do
        local replica_port=$((base_replica_port + i))
        start_server "$replica_port" --replicaof "127.0.0.1 $master_port"
        sleep 2
    done
    
    log_success "Cluster started successfully"
    echo
    status
}

# Clean up old logs and PID files
clean() {
    log_info "Cleaning up old files..."
    
    # Clean build artifacts
    if [ -d "target" ]; then
        mvn clean -q 2>/dev/null || rm -rf target/
        log_info "Cleaned build artifacts"
    fi
    
    # Remove old logs (older than 7 days)
    find "$LOG_DIR" -name "*.log" -mtime +7 -delete 2>/dev/null || true
    
    # Remove stale PID files
    for pid_file in "$PID_DIR"/redis-server-*.pid; do
        if [ -f "$pid_file" ]; then
            local pid=$(cat "$pid_file")
            if ! kill -0 "$pid" 2>/dev/null; then
                rm -f "$pid_file"
                log_info "Removed stale PID file: $(basename "$pid_file")"
            fi
        fi
    done
    
    log_success "Cleanup completed"
}

# Start Redis CLI session
start_cli() {
    local port=${1:-$DEFAULT_PORT}
    
    if ! command -v redis-cli &> /dev/null; then
        log_error "redis-cli not found. Install redis-tools package:"
        echo "  Ubuntu/Debian: sudo apt-get install redis-tools"
        echo "  CentOS/RHEL:   sudo yum install redis"
        echo "  macOS:         brew install redis"
        return 1
    fi
    
    log_info "Connecting to Redis server on port $port..."
    log_info "Type 'exit' or press Ctrl+C to quit"
    echo
    
    redis-cli -p "$port"
}

# Test server connectivity and basic operations
test_server() {
    local port=${1:-$DEFAULT_PORT}
    
    log_info "Testing Redis server on port $port..."
    
    if ! is_port_in_use "$port"; then
        log_error "No server running on port $port"
        return 1
    fi
    
    if command -v redis-cli &> /dev/null; then
        echo -e "${BLUE}Testing PING command:${NC}"
        if redis-cli -p "$port" ping 2>/dev/null; then
            log_success "Server responding to PING"
        else
            log_error "Server not responding to PING"
            return 1
        fi
        
        echo -e "\n${BLUE}Testing SET/GET commands:${NC}"
        local test_key="test:$(date +%s)"
        local test_value="mini-redis-test-value"
        
        if redis-cli -p "$port" set "$test_key" "$test_value" >/dev/null 2>&1; then
            log_success "SET command successful"
            
            local retrieved_value=$(redis-cli -p "$port" get "$test_key" 2>/dev/null)
            if [ "$retrieved_value" = "$test_value" ]; then
                log_success "GET command successful - value matches"
            else
                log_error "GET command failed - value mismatch"
            fi
            
            # Cleanup test key
            redis-cli -p "$port" del "$test_key" >/dev/null 2>&1
        else
            log_error "SET command failed"
        fi
        
        echo -e "\n${BLUE}Testing replication info:${NC}"
        redis-cli -p "$port" info replication 2>/dev/null | head -5 || true
        
    else
        log_warning "redis-cli not available, using basic connectivity test"
        if nc -z localhost "$port" 2>/dev/null; then
            log_success "Port $port is reachable"
        else
            log_error "Port $port is not reachable"
        fi
    fi
}

# Development mode with manual controls
dev_mode() {
    local port=${1:-$DEFAULT_PORT}
    local args=("${@:2}")
    
    log_info "Starting development mode on port $port"
    echo
    
    # Initial start
    start_server "$port" "${args[@]}"
    
    echo
    log_info "${PURPLE}Development Commands:${NC}"
    echo "  ${CYAN}r${NC} + Enter  - Restart server"
    echo "  ${CYAN}s${NC} + Enter  - Show status"
    echo "  ${CYAN}l${NC} + Enter  - Show logs (last 20 lines)"
    echo "  ${CYAN}t${NC} + Enter  - Test connectivity"
    echo "  ${CYAN}c${NC} + Enter  - Connect with Redis CLI"
    echo "  ${CYAN}q${NC} + Enter  - Quit development mode"
    echo
    
    while true; do
        read -p "${PURPLE}dev:$port>${NC} " cmd
        case "$cmd" in
            r|restart)
                log_info "Restarting server..."
                stop_server "$port" 2>/dev/null || true
                sleep 1
                start_server "$port" "${args[@]}"
                ;;
            s|status)
                status
                ;;
            l|logs)
                local log_file=$(get_log_file "$port")
                if [ -f "$log_file" ]; then
                    echo -e "${BLUE}Last 20 lines of logs:${NC}"
                    tail -20 "$log_file"
                else
                    log_warning "No log file found for port $port"
                fi
                ;;
            t|test)
                test_server "$port"
                ;;
            c|cli)
                start_cli "$port"
                ;;
            q|quit|exit)
                log_info "Stopping development server..."
                stop_server "$port"
                log_info "Development mode ended"
                exit 0
                ;;
            "")
                # Empty input, just continue
                ;;
            *)
                echo -e "${YELLOW}Unknown command: $cmd${NC}"
                echo "Available commands: r, s, l, t, c, q"
                ;;
        esac
    done
}

# Interactive management menu
interactive_mode() {
    log_info "${CYAN}Mini Redis Server - Interactive Management${NC}"
    
    while true; do
        echo
        echo -e "${YELLOW}Available Commands:${NC}"
        echo "  ${CYAN}1${NC}) start     - Start Redis master server (port $DEFAULT_PORT)"
        echo "  ${CYAN}2${NC}) replica   - Start Redis replica server (port 6380)"
        echo "  ${CYAN}3${NC}) cluster   - Start master + 2 replicas cluster"
        echo "  ${CYAN}4${NC}) status    - Show server status"
        echo "  ${CYAN}5${NC}) stop-all  - Stop all servers"
        echo "  ${CYAN}6${NC}) logs      - View master server logs"
        echo "  ${CYAN}7${NC}) cli       - Connect with Redis CLI"
        echo "  ${CYAN}8${NC}) test      - Test connectivity"
        echo "  ${CYAN}9${NC}) dev       - Development mode"
        echo "  ${CYAN}c${NC}) compile   - Compile project"
        echo "  ${CYAN}h${NC}) help      - Show detailed help"
        echo "  ${CYAN}q${NC}) quit      - Exit"
        echo
        
        read -p "${PURPLE}redis>${NC} " choice
        
        case $choice in
            1|start)
                start_server "$DEFAULT_PORT"
                ;;
            2|replica)
                start_server 6380 --replicaof "127.0.0.1 $DEFAULT_PORT"
                ;;
            3|cluster)
                start_cluster "$DEFAULT_PORT" 2 6380
                ;;
            4|status)
                status
                ;;
            5|stop-all)
                stop_all
                ;;
            6|logs)
                logs "$DEFAULT_PORT"
                ;;
            7|cli)
                start_cli "$DEFAULT_PORT"
                ;;
            8|test)
                test_server "$DEFAULT_PORT"
                ;;
            9|dev)
                dev_mode "$DEFAULT_PORT"
                ;;
            c|compile)
                compile
                ;;
            h|help)
                show_help
                ;;
            q|quit|exit)
                log_info "Goodbye!"
                exit 0
                ;;
            "")
                # Empty input, just continue
                ;;
            *)
                echo -e "${RED}Invalid option: $choice${NC}"
                ;;
        esac
    done
}

# Show help
show_help() {
    cat << EOF
${CYAN}Mini Redis Server Management Script${NC}

${YELLOW}Usage:${NC} $0 [COMMAND] [OPTIONS]

${YELLOW}Build Commands:${NC}
  compile                    Compile the project only
  package                    Build JAR with dependencies
  clean                      Clean old logs, PIDs, and build artifacts

${YELLOW}Server Commands:${NC}
  start [PORT] [ARGS...]     Start Redis server (default port: $DEFAULT_PORT)
  stop [PORT]                Stop Redis server on specific port
  stop-all                   Stop all running Redis servers
  restart [PORT] [ARGS...]   Restart Redis server
  status                     Show status of all Redis servers
  logs [PORT]                Follow logs for server on port
  cluster [MASTER] [REPLICAS] [BASE_PORT]  Start master-replica cluster

${YELLOW}Interactive Commands:${NC}
  cli [PORT]                 Start Redis CLI session (requires redis-cli)
  interactive                Interactive server management menu
  dev [PORT] [ARGS...]       Development mode with manual restart controls
  test [PORT]                Test server connectivity and basic operations

${YELLOW}Examples:${NC}
  $0 start                              # Start master on port $DEFAULT_PORT
  $0 start 6380 --replicaof "127.0.0.1 6379"  # Start replica
  $0 cluster 6379 2 6380               # Start cluster: master + 2 replicas
  $0 cli 6379                          # Connect to server with Redis CLI
  $0 interactive                       # Interactive management menu
  $0 dev                               # Development mode
  $0 test                              # Test connectivity

${YELLOW}Supported Configuration Arguments:${NC}
  --port PORT                   Server port (default: $DEFAULT_PORT)
  --replicaof "HOST PORT"       Configure as replica of master
  --dir PATH                    Data directory (default: /var/lib/redis)
  --dbfilename NAME             Database filename (default: dump.rdb)
  --appendonly                  Enable AOF persistence
  --bind ADDRESS                Bind address (default: 127.0.0.1)

${YELLOW}Development Tips:${NC}
  • Use 'redis-cli -p PORT' to connect and test commands
  • Install redis-tools: sudo apt-get install redis-tools (Ubuntu/Debian)
  • Monitor logs: tail -f logs/redis-server-PORT.log
  • Test replication: set key on master, get key on replica

EOF
}

# Main script logic
main() {
    case "${1:-interactive}" in
        "start")
            local port=${2:-$DEFAULT_PORT}
            start_server "$port" "${@:3}"
            ;;
        "stop")
            local port=${2:-$DEFAULT_PORT}
            stop_server "$port"
            ;;
        "stop-all")
            stop_all
            ;;
        "restart")
            local port=${2:-$DEFAULT_PORT}
            stop_server "$port"
            sleep 2
            start_server "$port" "${@:3}"
            ;;
        "status")
            status
            ;;
        "logs")
            local port=${2:-$DEFAULT_PORT}
            logs "$port"
            ;;
        "cluster")
            start_cluster "${2:-$DEFAULT_PORT}" "${3:-2}" "${4:-6380}"
            ;;
        "cli")
            local port=${2:-$DEFAULT_PORT}
            start_cli "$port"
            ;;
        "test")
            local port=${2:-$DEFAULT_PORT}
            test_server "$port"
            ;;
        "dev")
            local port=${2:-$DEFAULT_PORT}
            dev_mode "$port" "${@:3}"
            ;;
        "interactive")
            interactive_mode
            ;;
        "compile")
            compile
            ;;
        "package")
            package
            ;;
        "clean")
            clean
            ;;
        "help"|"-h"|"--help")
            show_help
            ;;
        *)
            if [ $# -eq 0 ]; then
                interactive_mode
            else
                log_error "Unknown command: $1"
                echo
                echo "Use '$0 help' for usage information or '$0' for interactive mode"
                exit 1
            fi
            ;;
    esac
}

# Run main function with all arguments
main "$@"