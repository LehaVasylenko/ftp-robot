#!/usr/bin/env bash
set -Eeuo pipefail

# === Где лежат исходники (из каталога с этим скриптом) ===
BASE_DIR="$(cd "$(dirname "$0")" && pwd)"
SRC_JAR="${BASE_DIR}/target/ftp-robot-1.0.jar"
SRC_ENV="${BASE_DIR}/systemd/ftp-robot.env"
SRC_SERVICE="${BASE_DIR}/systemd/ftp-robot.service"

# === Куда ставим ===
DEST_JAR_DIR="/opt/ftp-robot"
DEST_JAR="${DEST_JAR_DIR}/app.jar"
DEST_ENV="/etc/default/ftp-robot"
DEST_SERVICE="/etc/systemd/system/ftp-robot.service"
LOG_DIR="/var/log/ftp-robot"

SERVICE_NAME="ftp-robot"
SERVICE_USER="ftprobot"
SERVICE_HOME="/var/lib/ftp-robot"

### Проверки
[[ -f "$SRC_JAR" ]]      || { echo "❌ Не найден JAR: $SRC_JAR"; exit 1; }
[[ -f "$SRC_ENV" ]]      || { echo "❌ Не найден env: $SRC_ENV"; exit 1; }
[[ -f "$SRC_SERVICE" ]]  || { echo "❌ Не найден service: $SRC_SERVICE"; exit 1; }

echo "==> Устанавливаем $SERVICE_NAME"
echo "    JAR: $SRC_JAR"
echo "    ENV: $SRC_ENV"
echo "    SVC: $SRC_SERVICE"
echo

# 1) Системный пользователь (если нет)
if ! id -u "$SERVICE_USER" >/dev/null 2>&1; then
  echo "==> Создаю системного пользователя $SERVICE_USER"
  sudo useradd -r -s /usr/sbin/nologin -d "$SERVICE_HOME" -M "$SERVICE_USER"
fi

# 2) Останавливаем, если запущен
if systemctl is-active --quiet "$SERVICE_NAME"; then
  echo "==> Останавливаю сервис $SERVICE_NAME"
  sudo systemctl stop "$SERVICE_NAME"
fi

# 3) Копируем файлы и права
echo "==> Копирую JAR в ${DEST_JAR}"
sudo install -d -o "$SERVICE_USER" -g "$SERVICE_USER" -m 755 "$DEST_JAR_DIR"
sudo install -o "$SERVICE_USER" -g "$SERVICE_USER" -m 640 "$SRC_JAR" "$DEST_JAR"

echo "==> Копирую ENV в ${DEST_ENV}"
sudo install -o root -g root -m 640 "$SRC_ENV" "$DEST_ENV"

echo "==> Копирую unit в ${DEST_SERVICE}"
sudo install -o root -g root -m 644 "$SRC_SERVICE" "$DEST_SERVICE"

echo "==> Готовлю каталог логов ${LOG_DIR}"
sudo install -d -o "$SERVICE_USER" -g "$SERVICE_USER" -m 755 "$LOG_DIR"

# 4) Перечитать юниты и запустить
echo "==> systemctl daemon-reload"
sudo systemctl daemon-reload

echo "==> Включаю автозапуск и стартую сервис"
sudo systemctl enable "$SERVICE_NAME"
sudo systemctl start "$SERVICE_NAME"

echo
echo "==> Статус:"
systemctl --no-pager --full status "$SERVICE_NAME" || true
echo
echo "==> Последние логи:"
journalctl -u "$SERVICE_NAME" -n 50 --no-pager || true

echo
echo "✅ Готово. Для логов в реальном времени:  journalctl -u $SERVICE_NAME -f"
