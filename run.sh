#!/usr/bin/env bash
# Atajo para correr la CLI de PrintScript.
#
# Uso:
#   ./run.sh exec     <versión> <archivo.ps>
#   ./run.sh validate <versión> <archivo.ps>
#   ./run.sh format   <versión> <archivo.ps> <reglas.yaml> [salida.ps]
#   ./run.sh sca      <versión> <archivo.ps> <reglas.yaml>     (alias: lint)
#   ./run.sh repl     <versión>
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BIN="$ROOT/com.ingsis.engine/build/install/com.ingsis.engine/bin/com.ingsis.engine"
STAMP="$ROOT/com.ingsis.engine/build/install/com.ingsis.engine/lib"

usage() {
    sed -n '4,9p' "$0" | sed 's/^# \{0,1\}//'
    exit 2
}

# Recompila la distribución solo si falta o si cambió algún fuente/build desde la última vez.
build_if_needed() {
    if [[ ! -x "$BIN" ]] || [[ -n "$(find "$ROOT" -path '*/build' -prune -o -path '*/.gradle' -prune -o \
        \( -name '*.java' -o -name '*.gradle' -o -name '*.groovy' \) -newer "$STAMP" -print -quit)" ]]; then
        echo "[run.sh] Compilando la CLI (installDist)..." >&2
        (cd "$ROOT" && ./gradlew :com.ingsis.engine:installDist -q)
        touch "$STAMP"
    fi
}

require_file() {
    [[ -f "$1" ]] || { echo "[run.sh] No existe el archivo: $1" >&2; exit 2; }
}

[[ $# -ge 2 ]] || usage
OP="$1"
VERSION="$2"
shift 2

case "$OP" in
    exec | validate)
        [[ $# -eq 1 ]] || usage
        require_file "$1"
        ARGS=("$OP" -v "$VERSION" -i "$1" --no-progress)
        ;;
    format)
        [[ $# -eq 2 || $# -eq 3 ]] || usage
        require_file "$1"
        require_file "$2"
        ARGS=(format -v "$VERSION" -i "$1" -c "$2" --no-progress)
        [[ $# -eq 3 ]] && ARGS+=(-o "$3")
        ;;
    sca | lint)
        [[ $# -eq 2 ]] || usage
        require_file "$1"
        require_file "$2"
        ARGS=(lint -v "$VERSION" -i "$1" -c "$2" --no-progress)
        ;;
    repl)
        [[ $# -eq 0 ]] || usage
        ARGS=(-v "$VERSION")
        ;;
    *)
        usage
        ;;
esac

build_if_needed
exec "$BIN" "${ARGS[@]}"
