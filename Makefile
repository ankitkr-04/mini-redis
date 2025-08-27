# Makefile for Mini Redis Server
# Provides convenient shortcuts for common development tasks

.PHONY: help start stop status logs test cli interactive dev clean build

# Default target
help:
	@echo "Mini Redis Server - Available Commands:"
	@echo ""
	@echo "  make start       - Start Redis server"
	@echo "  make stop        - Stop all servers"
	@echo "  make status      - Show server status"
	@echo "  make logs        - Show server logs"
	@echo "  make cli         - Connect with Redis CLI"
	@echo "  make test        - Test server connectivity"
	@echo "  make interactive - Interactive management menu"
	@echo "  make dev         - Development mode"
	@echo "  make build       - Build the project"
	@echo "  make clean       - Clean build artifacts"
	@echo ""
	@echo "Use './run.sh help' for detailed options"

# Server commands
start:
	@./run.sh start

stop:
	@./run.sh stop-all

status:
	@./run.sh status

logs:
	@./run.sh logs

cli:
	@./run.sh cli

test:
	@./run.sh test

interactive:
	@./run.sh interactive

dev:
	@./run.sh dev

# Build commands
build:
	@./run.sh package

clean:
	@./run.sh clean
