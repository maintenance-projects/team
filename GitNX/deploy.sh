#!/bin/bash
# GitNX 서버 배포 스크립트
# 사용법: ./deploy.sh <서버IP> <SSH유저> [SSH포트]
#
# 예시: ./deploy.sh 192.168.1.100 ubuntu
#       ./deploy.sh 192.168.1.100 root 2222

set -e

SERVER_IP="${1:?서버 IP를 입력하세요. 사용법: ./deploy.sh <IP> <USER> [PORT]}"
SSH_USER="${2:?SSH 유저를 입력하세요. 사용법: ./deploy.sh <IP> <USER> [PORT]}"
SSH_PORT="${3:-22}"
REMOTE_DIR="/opt/gitnx"

SSH_CMD="ssh -p $SSH_PORT $SSH_USER@$SERVER_IP"
SCP_CMD="scp -P $SSH_PORT"

echo "=============================="
echo " GitNX 배포 시작"
echo " 서버: $SSH_USER@$SERVER_IP:$SSH_PORT"
echo "=============================="

# 1. Docker 설치 (없으면)
echo ""
echo "[1/4] Docker 설치 확인..."
$SSH_CMD << 'INSTALL_DOCKER'
if command -v docker &> /dev/null; then
    echo "Docker 이미 설치됨: $(docker --version)"
else
    echo "Docker 설치 중 (CentOS 7)..."
    sudo yum install -y yum-utils
    sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
    sudo yum install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
    sudo systemctl start docker
    sudo systemctl enable docker
    sudo usermod -aG docker $USER
    echo "Docker 설치 완료: $(docker --version)"
fi
INSTALL_DOCKER

# 2. 서버에 디렉토리 생성
echo ""
echo "[2/4] 서버 디렉토리 준비..."
$SSH_CMD "sudo mkdir -p $REMOTE_DIR && sudo chown $SSH_USER:$SSH_USER $REMOTE_DIR"

# 3. 필요한 파일 전송
echo ""
echo "[3/4] 파일 전송 중..."
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# 전송할 파일 목록
$SCP_CMD "$SCRIPT_DIR/Dockerfile" "$SSH_USER@$SERVER_IP:$REMOTE_DIR/"
$SCP_CMD "$SCRIPT_DIR/docker-compose.yml" "$SSH_USER@$SERVER_IP:$REMOTE_DIR/"
$SCP_CMD "$SCRIPT_DIR/build.gradle" "$SSH_USER@$SERVER_IP:$REMOTE_DIR/"
$SCP_CMD "$SCRIPT_DIR/settings.gradle" "$SSH_USER@$SERVER_IP:$REMOTE_DIR/"
$SCP_CMD "$SCRIPT_DIR/gradlew" "$SSH_USER@$SERVER_IP:$REMOTE_DIR/"
$SCP_CMD -r "$SCRIPT_DIR/gradle" "$SSH_USER@$SERVER_IP:$REMOTE_DIR/"
$SCP_CMD -r "$SCRIPT_DIR/src" "$SSH_USER@$SERVER_IP:$REMOTE_DIR/"

echo "파일 전송 완료"

# 4. Docker Compose 빌드 & 실행
echo ""
echo "[4/4] Docker Compose 빌드 및 실행..."
$SSH_CMD << REMOTE_RUN
cd $REMOTE_DIR
sudo docker compose down 2>/dev/null || true
sudo docker compose up --build -d
echo ""
echo "컨테이너 상태:"
sudo docker compose ps
REMOTE_RUN

echo ""
echo "=============================="
echo " 배포 완료!"
echo " 접속: http://$SERVER_IP:9090"
echo "=============================="
