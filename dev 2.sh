#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FRONTEND_DIR="$ROOT_DIR/ordering-frontend"
BACKEND_DIR="$ROOT_DIR/ordering-system"

usage() {
  cat <<'EOF'
Uso:
  ./dev.sh            Avvia Backend (Spring) + Frontend (Angular)
  ./dev.sh --install  Esegue npm ci e poi avvia
  ./dev.sh --mail     Avvia anche un server SMTP locale (Mailpit) per vedere le email
  ./dev.sh -h|--help  Mostra questo aiuto

Note:
- Backend: ./ordering-system/mvnw spring-boot:run (profilo: local)
- Frontend: npm start (ng serve)

Mail (solo con --mail):
- SMTP: localhost:1025
- Web UI: http://localhost:8025
EOF
}

INSTALL=false
MAIL=false
for arg in "$@"; do
  case "$arg" in
    --install) INSTALL=true ;;
    --mail) MAIL=true ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Argomento non riconosciuto: $arg"; echo; usage; exit 2 ;;
  esac
done

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Comando mancante: $1" >&2
    exit 1
  fi
}

# Termina un processo e tutti i suoi figli (ricorsivo)
kill_tree() {
  local pid="$1"
  if ! kill -0 "$pid" >/dev/null 2>&1; then
    return 0
  fi

  local child
  while IFS= read -r child; do
    [ -n "$child" ] || continue
    kill_tree "$child"
  done < <(pgrep -P "$pid" 2>/dev/null || true)

  kill -TERM "$pid" >/dev/null 2>&1 || true
}

require_cmd npm
require_cmd java
require_cmd pgrep

find_container_cli() {
  if command -v docker >/dev/null 2>&1; then
    echo "docker"
    return 0
  fi
  if command -v podman >/dev/null 2>&1; then
    echo "podman"
    return 0
  fi
  return 1
}

MAIL_CONTAINER_NAME="orderapp-mail"
CONTAINER_CLI=""
if [ "$MAIL" = true ]; then
  if ! CONTAINER_CLI="$(find_container_cli)"; then
    echo "Opzione --mail richiesta ma non trovo né 'docker' né 'podman' in PATH." >&2
    echo "In alternativa avvia tu un SMTP dev su localhost:1025 (es. MailHog/Mailpit)." >&2
    exit 1
  fi

  # Verifica che il daemon sia attivo (Docker Desktop / Podman service)
  set +e
  "$CONTAINER_CLI" info >/dev/null 2>&1
  INFO_OK=$?
  set -e
  if [ "$INFO_OK" -ne 0 ]; then
    echo "Opzione --mail richiesta, ma '$CONTAINER_CLI' non risponde (daemon non avviato)." >&2
    echo "- Se usi Docker: avvia Docker Desktop e riprova." >&2
    echo "- In alternativa: avvia manualmente un SMTP dev su localhost:1025." >&2
    exit 1
  fi

  echo "[MAIL] Avvio Mailpit (SMTP 1025, UI 8025)…"
  # Stop eventuale container precedente
  "$CONTAINER_CLI" rm -f "$MAIL_CONTAINER_NAME" >/dev/null 2>&1 || true
  "$CONTAINER_CLI" run -d --rm \
    --name "$MAIL_CONTAINER_NAME" \
    -p 1025:1025 \
    -p 8025:8025 \
    axllent/mailpit >/dev/null
  echo "[MAIL] UI: http://localhost:8025"
fi

if [ ! -d "$FRONTEND_DIR" ]; then
  echo "Cartella frontend non trovata: $FRONTEND_DIR" >&2
  exit 1
fi
if [ ! -d "$BACKEND_DIR" ]; then
  echo "Cartella backend non trovata: $BACKEND_DIR" >&2
  exit 1
fi

if [ ! -f "$BACKEND_DIR/mvnw" ]; then
  echo "Maven wrapper non trovato: $BACKEND_DIR/mvnw" >&2
  exit 1
fi

# Prova a rendere eseguibile mvnw (utile su macOS dopo unzip/clone)
chmod +x "$BACKEND_DIR/mvnw" >/dev/null 2>&1 || true

if [ "$INSTALL" = true ]; then
  if [ -f "$FRONTEND_DIR/package-lock.json" ]; then
    echo "[FE] npm ci…"
    (cd "$FRONTEND_DIR" && npm ci)
  else
    echo "[FE] package-lock.json non trovato: uso npm install…"
    (cd "$FRONTEND_DIR" && npm install)
  fi
fi

echo "[BE] Avvio Spring Boot…"
(
  cd "$BACKEND_DIR"
  exec ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
) &
BE_PID=$!

echo "[FE] Avvio Angular…"
(
  cd "$FRONTEND_DIR"
  exec npm start
) &
FE_PID=$!

cleanup() {
  echo
  echo "Stop in corso…"

  if [ "$MAIL" = true ] && [ -n "$CONTAINER_CLI" ]; then
    "$CONTAINER_CLI" stop "$MAIL_CONTAINER_NAME" >/dev/null 2>&1 || true
  fi

  # Uccide prima FE/BE e i figli, poi attende
  kill_tree "$FE_PID"
  kill_tree "$BE_PID"

  wait "$FE_PID" 2>/dev/null || true
  wait "$BE_PID" 2>/dev/null || true
}

trap cleanup INT TERM

# Se uno dei due termina, fermiamo anche l'altro.
# Nota: macOS spesso usa Bash 3.2, quindi evitiamo `wait -n`.
(
  wait "$BE_PID" 2>/dev/null
  echo
  echo "[BE] terminato. Arresto anche FE…"
  cleanup
) &
WATCH_BE_PID=$!

(
  wait "$FE_PID" 2>/dev/null
  echo
  echo "[FE] terminato. Arresto anche BE…"
  cleanup
) &
WATCH_FE_PID=$!

# Attende i processi principali (i watcher faranno cleanup se uno termina prima)
wait "$BE_PID" 2>/dev/null || true
wait "$FE_PID" 2>/dev/null || true

# Chiude i watcher se sono ancora vivi
kill_tree "$WATCH_BE_PID" 2>/dev/null || true
kill_tree "$WATCH_FE_PID" 2>/dev/null || true
