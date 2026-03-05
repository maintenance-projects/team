#!/bin/bash

APP_NAME="SpaceNX"
PID_FILE=".spacenx.pid"
LOG_FILE="spacenx.log"
PORT=8081
DB_CONTAINER="gitnx-db"
DB_PORT=5432

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

print_status() { echo -e "${GREEN}[${APP_NAME}]${NC} $1"; }
print_warn() { echo -e "${YELLOW}[${APP_NAME}]${NC} $1"; }
print_error() { echo -e "${RED}[${APP_NAME}]${NC} $1"; }

check_db() {
    if docker ps --format '{{.Names}}' | grep -q "^${DB_CONTAINER}$"; then
        print_status "Database container '${DB_CONTAINER}' is running."
        return 0
    fi

    if docker ps -a --format '{{.Names}}' | grep -q "^${DB_CONTAINER}$"; then
        print_warn "Database container exists but is stopped. Starting..."
        docker start ${DB_CONTAINER}
    else
        print_warn "Database container not found. Starting with docker-compose..."
        docker compose up -d
    fi

    print_status "Waiting for database to be ready..."
    for i in $(seq 1 30); do
        if docker exec ${DB_CONTAINER} pg_isready -U root -d gitnx > /dev/null 2>&1; then
            print_status "Database is ready."
            return 0
        fi
        sleep 1
    done
    print_error "Database failed to start within 30 seconds."
    return 1
}

check_port() {
    if lsof -i :${PORT} -sTCP:LISTEN > /dev/null 2>&1; then
        print_error "Port ${PORT} is already in use."
        return 1
    fi
    return 0
}

build() {
    print_status "Building application..."
    ./gradlew build -x test --quiet
    if [ $? -ne 0 ]; then
        print_error "Build failed!"
        return 1
    fi
    print_status "Build successful."
    return 0
}

start() {
    if [ -f "${PID_FILE}" ]; then
        PID=$(cat ${PID_FILE})
        if kill -0 ${PID} 2>/dev/null; then
            print_warn "Application is already running (PID: ${PID})."
            return 0
        else
            rm -f ${PID_FILE}
        fi
    fi

    check_db || return 1
    check_port || return 1
    build || return 1

    print_status "Starting application on port ${PORT}..."
    JAR=$(ls build/libs/*.jar 2>/dev/null | grep -v plain | head -1)
    if [ -z "$JAR" ]; then
        print_error "No JAR file found in build/libs/"
        return 1
    fi

    nohup java -jar ${JAR} > ${LOG_FILE} 2>&1 &
    echo $! > ${PID_FILE}
    print_status "Application started (PID: $(cat ${PID_FILE}))"
    print_status "Log file: ${LOG_FILE}"
    print_status "Access: http://localhost:${PORT}"
}

stop() {
    if [ ! -f "${PID_FILE}" ]; then
        print_warn "PID file not found. Application may not be running."
        return 0
    fi

    PID=$(cat ${PID_FILE})
    if kill -0 ${PID} 2>/dev/null; then
        print_status "Stopping application (PID: ${PID})..."
        kill ${PID}
        sleep 3
        if kill -0 ${PID} 2>/dev/null; then
            print_warn "Force killing..."
            kill -9 ${PID}
        fi
        print_status "Application stopped."
    else
        print_warn "Process ${PID} is not running."
    fi
    rm -f ${PID_FILE}
}

restart() {
    stop
    sleep 2
    start
}

status() {
    if [ -f "${PID_FILE}" ]; then
        PID=$(cat ${PID_FILE})
        if kill -0 ${PID} 2>/dev/null; then
            print_status "Application is running (PID: ${PID})"
            return 0
        fi
    fi
    print_warn "Application is not running."
    return 1
}

logs() {
    if [ -f "${LOG_FILE}" ]; then
        tail -f ${LOG_FILE}
    else
        print_error "Log file not found."
    fi
}

case "$1" in
    start)   start ;;
    stop)    stop ;;
    restart) restart ;;
    status)  status ;;
    logs)    logs ;;
    build)   build ;;
    clean)   ./gradlew clean; print_status "Clean complete." ;;
    db-check) check_db ;;
    *)
        echo "Usage: $0 {start|stop|restart|status|logs|build|clean|db-check}"
        exit 1
        ;;
esac
