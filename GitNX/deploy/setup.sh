#!/usr/bin/env bash
set -euo pipefail

APP_NAME="gitnx"
BASE_DIR="/opt/${APP_NAME}"
SOURCE_DIR="${BASE_DIR}/source"
GITNX_DIR="${SOURCE_DIR}/GitNX"
ENV_FILE="${BASE_DIR}/${APP_NAME}.env"
SERVICE_FILE="/etc/systemd/system/${APP_NAME}.service"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# --- Colors ---
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

info()  { echo -e "${GREEN}[INFO]${NC} $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $*"; }
error() { echo -e "${RED}[ERROR]${NC} $*"; exit 1; }

# --- Root check ---
if [[ $EUID -ne 0 ]]; then
    error "This script must be run as root (sudo bash $0)"
fi

# --- Git repo URL argument ---
REPO_URL="${1:-}"
if [[ -z "$REPO_URL" ]]; then
    error "Usage: sudo bash $0 <git-repo-url>"
fi

# --- Java 21 check ---
if command -v java &>/dev/null; then
    JAVA_VER=$(java -version 2>&1 | head -1 | awk -F '"' '{print $2}' | cut -d. -f1)
    if [[ "$JAVA_VER" -lt 21 ]]; then
        error "Java 21+ required (found Java $JAVA_VER). Install: sudo apt install openjdk-21-jdk-headless"
    fi
    info "Java $(java -version 2>&1 | head -1 | awk -F '"' '{print $2}') detected"
else
    error "Java not found. Install: sudo apt install openjdk-21-jdk-headless"
fi

# --- Git check ---
if ! command -v git &>/dev/null; then
    error "Git not found. Install: sudo apt install git"
fi

# --- Create system user ---
if id "$APP_NAME" &>/dev/null; then
    info "User '$APP_NAME' already exists"
else
    useradd --system --no-create-home --shell /usr/sbin/nologin "$APP_NAME"
    info "Created system user '$APP_NAME'"
fi

# --- Create directory structure ---
mkdir -p "${BASE_DIR}/repos"
info "Directory structure ready: ${BASE_DIR}"

# --- Clone or pull ---
if [[ -d "${SOURCE_DIR}/.git" ]]; then
    info "Source already exists, pulling latest..."
    cd "$SOURCE_DIR"
    git pull
else
    info "Cloning repository..."
    git clone "$REPO_URL" "$SOURCE_DIR"
fi

# --- Build ---
info "Building GitNX..."
cd "$GITNX_DIR"
chmod +x gradlew
./gradlew build -x test --quiet
JAR_FILE=$(ls "${GITNX_DIR}/build/libs"/*.jar 2>/dev/null | grep -v plain | head -1)
if [[ -z "$JAR_FILE" ]]; then
    error "Build failed: JAR not found in build/libs/"
fi
info "Build successful: $(basename "$JAR_FILE")"

# --- Create env file (only if not exists) ---
if [[ ! -f "$ENV_FILE" ]]; then
    cat > "$ENV_FILE" <<'EOF'
# GitNX Environment Configuration
# Edit this file with your actual database credentials, then run:
#   sudo systemctl restart gitnx

DB_URL=jdbc:postgresql://localhost:5432/gitnx
DB_USERNAME=root
DB_PASSWORD=CHANGE_ME

# Git repository storage path
# GITNX_REPOS_PATH=/opt/gitnx/repos

# Public clone base URL (change to your server's domain/IP)
# GITNX_CLONE_URL=http://your-server:8080/repo
EOF
    chmod 640 "$ENV_FILE"
    info "Environment file created: $ENV_FILE"
    warn ">>> Edit $ENV_FILE with your DB credentials before starting! <<<"
else
    info "Environment file already exists: $ENV_FILE (skipped)"
fi

# --- Set ownership ---
chown -R "${APP_NAME}:${APP_NAME}" "$BASE_DIR"

# --- Install systemd service ---
cp "${SCRIPT_DIR}/gitnx.service" "$SERVICE_FILE"
systemctl daemon-reload
systemctl enable "$APP_NAME"
info "systemd service installed and enabled"

# --- Start service ---
systemctl start "$APP_NAME"
info "GitNX started"

# --- Summary ---
echo ""
echo "============================================"
info "GitNX deployment complete!"
echo "============================================"
echo ""
echo "  Source dir     : $GITNX_DIR"
echo "  Config file    : $ENV_FILE"
echo "  Repos dir      : ${BASE_DIR}/repos"
echo "  Service        : systemctl {start|stop|restart|status} $APP_NAME"
echo "  Logs           : journalctl -u $APP_NAME -f"
echo ""
echo "  Next steps:"
echo "    1. Edit $ENV_FILE with your DB credentials"
echo "    2. Ensure PostgreSQL is running and the 'gitnx' database exists"
echo "    3. sudo systemctl restart $APP_NAME"
echo "    4. Access http://<your-server>:8080"
echo ""
echo "  To update later:"
echo "    cd ${SOURCE_DIR} && sudo git pull"
echo "    cd ${GITNX_DIR} && sudo ./gradlew build -x test"
echo "    sudo systemctl restart $APP_NAME"
echo ""
