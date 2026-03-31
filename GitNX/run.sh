#!/usr/bin/env bash
set -euo pipefail

# ─────────────────────────────────────────────
# GitNX 실행 스크립트
# ─────────────────────────────────────────────
APP_NAME="GitNX"
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
DB_CONTAINER="gitnx-db"
DB_NAME="gitnx"
DB_USER="root"
SERVER_PORT=9090

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

log()  { echo -e "${CYAN}[${APP_NAME}]${NC} $1"; }
ok()   { echo -e "${GREEN}[✓]${NC} $1"; }
warn() { echo -e "${YELLOW}[!]${NC} $1"; }
err()  { echo -e "${RED}[✗]${NC} $1"; }

# ── 사용법 ──
usage() {
    echo ""
    echo -e "${CYAN}Usage:${NC} ./run.sh [command]"
    echo ""
    echo "  start       앱 실행 (기본값)"
    echo "  stop        실행 중인 앱 종료"
    echo "  restart     재시작"
    echo "  status      상태 확인"
    echo "  build       빌드만 수행"
    echo "  clean       빌드 정리 후 재빌드"
    echo "  logs        최근 로그 확인"
    echo "  db-check    DB 연결 확인"
    echo "  help        도움말"
    echo ""
}

# ── Docker / PostgreSQL 확인 ──
check_db() {
    log "PostgreSQL 상태 확인 중..."

    if ! command -v docker &>/dev/null; then
        err "Docker가 설치되어 있지 않습니다."
        exit 1
    fi

    if ! docker ps --format '{{.Names}}' | grep -q "^${DB_CONTAINER}$"; then
        warn "컨테이너 '${DB_CONTAINER}'가 실행 중이 아닙니다. 시작합니다..."
        docker start "${DB_CONTAINER}" 2>/dev/null || {
            err "컨테이너를 시작할 수 없습니다. 'docker start ${DB_CONTAINER}' 를 수동 실행하세요."
            exit 1
        }
        sleep 3
    fi
    ok "PostgreSQL 컨테이너 실행 중"

    # DB 존재 확인
    if ! docker exec "${DB_CONTAINER}" psql -U "${DB_USER}" -d postgres -tAc \
        "SELECT 1 FROM pg_database WHERE datname='${DB_NAME}'" 2>/dev/null | grep -q 1; then
        warn "데이터베이스 '${DB_NAME}'가 없습니다. 생성합니다..."
        docker exec "${DB_CONTAINER}" psql -U "${DB_USER}" -d postgres -c "CREATE DATABASE ${DB_NAME};"
        ok "데이터베이스 '${DB_NAME}' 생성 완료"
    else
        ok "데이터베이스 '${DB_NAME}' 확인 완료"
    fi
}

# ── 포트 사용 확인 ──
check_port() {
    if lsof -i ":${SERVER_PORT}" -sTCP:LISTEN &>/dev/null; then
        local pid
        pid=$(lsof -ti ":${SERVER_PORT}" -sTCP:LISTEN 2>/dev/null | head -1)
        warn "포트 ${SERVER_PORT}이 이미 사용 중입니다 (PID: ${pid})"
        return 1
    fi
    return 0
}

# ── 빌드 ──
do_build() {
    log "프로젝트 빌드 중..."
    cd "${PROJECT_DIR}"
    ./gradlew build -x test --quiet 2>&1
    if [ $? -eq 0 ]; then
        ok "빌드 성공"
    else
        err "빌드 실패"
        exit 1
    fi
}

# ── 시작 ──
do_start() {
    log "${APP_NAME} 시작 중..."

    check_db

    if ! check_port; then
        read -rp "기존 프로세스를 종료하고 시작할까요? (y/N): " answer
        if [[ "${answer}" =~ ^[Yy]$ ]]; then
            do_stop
            sleep 2
        else
            err "종료합니다."
            exit 1
        fi
    fi

    do_build

    cd "${PROJECT_DIR}"
    log "서버 시작 중 (port: ${SERVER_PORT})..."
    nohup ./gradlew bootRun --quiet > "${PROJECT_DIR}/gitnx.log" 2>&1 &
    local pid=$!
    echo "${pid}" > "${PROJECT_DIR}/.gitnx.pid"

    # 서버 기동 대기
    log "서버 기동 대기 중..."
    for i in $(seq 1 30); do
        if curl -s -o /dev/null -w "" "http://localhost:${SERVER_PORT}/login" 2>/dev/null; then
            echo ""
            ok "${APP_NAME} 서버가 시작되었습니다!"
            echo ""
            echo -e "  ${CYAN}URL:${NC}  http://localhost:${SERVER_PORT}"
            echo -e "  ${CYAN}PID:${NC}  ${pid}"
            echo -e "  ${CYAN}Log:${NC}  ${PROJECT_DIR}/gitnx.log"
            echo ""
            return 0
        fi
        printf "."
        sleep 1
    done

    echo ""
    err "서버 시작 타임아웃 (30초). 로그를 확인하세요: ${PROJECT_DIR}/gitnx.log"
    exit 1
}

# ── 종료 ──
do_stop() {
    log "${APP_NAME} 종료 중..."

    # PID 파일로 종료
    if [ -f "${PROJECT_DIR}/.gitnx.pid" ]; then
        local pid
        pid=$(cat "${PROJECT_DIR}/.gitnx.pid")
        if kill -0 "${pid}" 2>/dev/null; then
            kill "${pid}" 2>/dev/null
            sleep 2
            if kill -0 "${pid}" 2>/dev/null; then
                kill -9 "${pid}" 2>/dev/null
            fi
            ok "프로세스 종료 (PID: ${pid})"
        fi
        rm -f "${PROJECT_DIR}/.gitnx.pid"
    fi

    # 포트를 점유한 프로세스 직접 종료
    local port_pid
    port_pid=$(lsof -ti ":${SERVER_PORT}" -sTCP:LISTEN 2>/dev/null || true)
    if [[ -n "$port_pid" ]]; then
        kill "$port_pid" 2>/dev/null || true
        sleep 2
        if kill -0 "$port_pid" 2>/dev/null; then
            kill -9 "$port_pid" 2>/dev/null || true
        fi
        ok "포트 ${SERVER_PORT} 점유 프로세스 종료 (PID: ${port_pid})"
    fi

    # bootRun 프로세스도 정리
    pkill -f "gitnx.*bootRun" 2>/dev/null || true
    pkill -f "com.gitnx.GitNxApplication" 2>/dev/null || true

    ok "${APP_NAME} 종료 완료"
}

# ── 상태 ──
do_status() {
    echo ""
    echo -e "${CYAN}═══ ${APP_NAME} Status ═══${NC}"
    echo ""

    # App
    if [ -f "${PROJECT_DIR}/.gitnx.pid" ] && kill -0 "$(cat "${PROJECT_DIR}/.gitnx.pid")" 2>/dev/null; then
        ok "앱: 실행 중 (PID: $(cat "${PROJECT_DIR}/.gitnx.pid"))"
    elif lsof -i ":${SERVER_PORT}" -sTCP:LISTEN &>/dev/null; then
        ok "앱: 포트 ${SERVER_PORT} 활성"
    else
        warn "앱: 중지됨"
    fi

    # DB
    if docker ps --format '{{.Names}}' 2>/dev/null | grep -q "^${DB_CONTAINER}$"; then
        ok "DB: ${DB_CONTAINER} 실행 중"
    else
        warn "DB: ${DB_CONTAINER} 중지됨"
    fi

    # HTTP check
    local http_code
    http_code=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:${SERVER_PORT}/login" 2>/dev/null || echo "000")
    if [ "${http_code}" = "200" ]; then
        ok "HTTP: http://localhost:${SERVER_PORT} 응답 정상 (${http_code})"
    else
        warn "HTTP: 응답 없음 (${http_code})"
    fi

    echo ""
}

# ── 클린 빌드 ──
do_clean() {
    log "클린 빌드..."
    cd "${PROJECT_DIR}"
    ./gradlew clean build -x test --quiet
    ok "클린 빌드 완료"
}

# ── 로그 ──
do_logs() {
    if [ -f "${PROJECT_DIR}/gitnx.log" ]; then
        tail -100 "${PROJECT_DIR}/gitnx.log"
    else
        warn "로그 파일이 없습니다."
    fi
}

# ── 메인 ──
cd "${PROJECT_DIR}"

case "${1:-start}" in
    start)    do_start ;;
    stop)     do_stop ;;
    restart)  do_stop; sleep 2; do_start ;;
    status)   do_status ;;
    build)    do_build ;;
    clean)    do_clean ;;
    logs)     do_logs ;;
    db-check) check_db ;;
    help|-h)  usage ;;
    *)
        err "알 수 없는 명령: $1"
        usage
        exit 1
        ;;
esac
