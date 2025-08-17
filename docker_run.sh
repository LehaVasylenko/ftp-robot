#!/usr/bin/env bash
set -euo pipefail

# === Настройки с разумными дефолтами (можно переопределять через ENV) ===
IMAGE="${IMAGE:-ftp-robot:latest}"        # имя образа (или, например, lehavasylenko/ftp-robot:latest)
CONTAINER="${CONTAINER:-ftp-robot}"       # имя контейнера
HOST_FTP_DIR="${HOST_FTP_DIR:-/srv/ftp}"  # где лежат файлы на хосте
HOST_LOG_DIR="${HOST_LOG_DIR:-/var/log/ftp-robot}"  # куда писать логи на хосте

# === Проверки ===
if ! command -v docker >/dev/null 2>&1; then
  echo "[-] Docker не найден. Установи docker и попробуй снова." >&2
  exit 1
fi

if [ ! -d "$HOST_FTP_DIR" ]; then
  echo "[-] Каталог $HOST_FTP_DIR не существует. Создай его и выдай права." >&2
  exit 1
fi

# === Определяем UID:GID владельца каталога /srv/ftp, чтобы контейнер писал без проблем ===
UID_FTP="$(stat -c '%u' "$HOST_FTP_DIR")"
GID_FTP="$(stat -c '%g' "$HOST_FTP_DIR")"
APP_UID="${APP_UID:-$UID_FTP}"
APP_GID="${APP_GID:-$GID_FTP}"

echo "[*] Используем UID:GID = ${APP_UID}:${APP_GID}"

# === Собираем образ, если в текущей папке есть Dockerfile ===
if [ -f Dockerfile ]; then
  echo "[*] Собираю образ ${IMAGE} из Dockerfile..."
  docker build -t "$IMAGE" .
else
  echo "[*] Dockerfile не найден — предполагаю, что образ ${IMAGE} уже есть локально или в реестре."
fi

# === Готовим каталог логов на хосте ===
mkdir -p "$HOST_LOG_DIR"
# На всякий случай отдадим владение тем же uid/gid, что и у /srv/ftp
chown "$APP_UID:$APP_GID" "$HOST_LOG_DIR" || true

# === Останавливаем и удаляем старый контейнер, если он есть ===
if docker ps -a --format '{{.Names}}' | grep -wq "$CONTAINER"; then
  echo "[*] Останавливаю старый контейнер ${CONTAINER}..."
  docker rm -f "$CONTAINER" >/dev/null 2>&1 || true
fi

# === Запускаем ===
echo "[*] Запускаю контейнер ${CONTAINER}..."
docker run -d --name "$CONTAINER" \
  --restart unless-stopped \
  --user "${APP_UID}:${APP_GID}" \
  -v "$HOST_FTP_DIR:/srv/ftp:rw" \
  -v "$HOST_LOG_DIR:/app/log:rw" \
  "$IMAGE"

echo "[+] Готово!"
docker ps --filter "name=$CONTAINER"
echo
echo "Логи в реальном времени:  docker logs -f $CONTAINER"
echo "Переопределить параметры можно так:"
echo "  IMAGE=lehavasylenko/ftp-robot:latest HOST_FTP_DIR=/srv/ftp HOST_LOG_DIR=/var/log/ftp-robot ./run-ftp-robot.sh"
