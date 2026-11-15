#!/usr/bin/env bash
# Unattended frame-generation pacing test.
#
# Builds Super Resolution for one version, deploys the Fabric jar into the Wisteria dev run
# (Wisteria/sr_mod), launches straight into a world with SR_AUTO_TRACE so the game records,
# exports and quits by itself, then summarizes the new trace with analyze_frame_pacing.py.
#
# Usage: script/fg_autotest.sh [--no-build] [--warmup SECONDS] [--duration SECONDS] [--world NAME]
#                              [--size WIDTHxHEIGHT]
# Environment: WISTERIA_DIR, SR_VERSION_CONFIG (default 26.1.x), SR_ARTIFACT_MC (default 26.1),
#              TIMEOUT (seconds for the whole game run, default 600), LOG_DIR.
set -euo pipefail

SR_DIR=$(cd "$(dirname "$0")/.." && pwd)
WISTERIA_DIR=${WISTERIA_DIR:-$(cd "$SR_DIR/../Wisteria" && pwd)}
VERSION_CONFIG=${SR_VERSION_CONFIG:-26.1.x}
ARTIFACT_MC=${SR_ARTIFACT_MC:-26.1}
TIMEOUT=${TIMEOUT:-600}
LOG_DIR=${LOG_DIR:-/tmp/sr-fg-autotest}
WORLD="New World"
WARMUP=20
DURATION=10
BUILD=1
GAME_ARGS=

while [ $# -gt 0 ]; do
    case "$1" in
        --no-build) BUILD=0 ;;
        --warmup) WARMUP=$2; shift ;;
        --duration) DURATION=$2; shift ;;
        --world) WORLD=$2; shift ;;
        --size) GAME_ARGS="--width ${2%x*} --height ${2#*x}"; shift ;;
        *) echo "unknown argument: $1" >&2; exit 2 ;;
    esac
    shift
done

REPORTS="$WISTERIA_DIR/fabric/run/config/super_resolution/reports"
# Loom passes the client's JVM arguments through this file, so it identifies a dev-run game.
CLIENT_PATTERN="$WISTERIA_DIR/fabric/build/loom-cache/argFiles/runClient"
mkdir -p "$LOG_DIR"

if pgrep -f "$CLIENT_PATTERN" > /dev/null; then
    echo "[fg-autotest] a Wisteria dev client is already running; close it first" >&2
    exit 1
fi

if [ "$BUILD" = 1 ]; then
    echo "[fg-autotest] building $VERSION_CONFIG"
    if ! (cd "$SR_DIR" && ./gradlew buildOneVersion -Psr.version="$VERSION_CONFIG") > "$LOG_DIR/build.log" 2>&1; then
        tail -40 "$LOG_DIR/build.log"
        echo "[fg-autotest] build failed, full log: $LOG_DIR/build.log" >&2
        exit 1
    fi
fi

shopt -s nullglob
jars=("$SR_DIR"/build_jars/super_resolution-fabric-"$ARTIFACT_MC"-*.opengl.jar)
if [ ${#jars[@]} -ne 1 ]; then
    echo "[fg-autotest] expected one fabric $ARTIFACT_MC jar in build_jars, found ${#jars[@]}" >&2
    exit 1
fi
rm -f "$WISTERIA_DIR"/sr_mod/super_resolution-fabric-"$ARTIFACT_MC"-*.opengl.jar
cp -p "${jars[0]}" "$WISTERIA_DIR/sr_mod/"
echo "[fg-autotest] deployed $(basename "${jars[0]}")"

marker="$LOG_DIR/.run-start"
touch "$marker"
echo "[fg-autotest] launching '$WORLD' (warmup ${WARMUP}s, record ${DURATION}s)"
status=0
SR_AUTO_TRACE="warmup=$WARMUP,duration=$DURATION,exit=true" \
    timeout --kill-after=30 "$TIMEOUT" \
    "$WISTERIA_DIR/gradlew" -p "$WISTERIA_DIR" :fabric:runClient \
    --args="--quickPlaySingleplayer \"$WORLD\" $GAME_ARGS" > "$LOG_DIR/run.log" 2>&1 || status=$?

# The game JVM is a child of the Gradle daemon, so a timed-out client can leave it running.
if [ "$status" = 124 ] || [ "$status" = 137 ]; then
    pkill -f "$CLIENT_PATTERN" || true
    echo "[fg-autotest] run timed out after ${TIMEOUT}s" >&2
fi

grep -E "Auto frame-pacing trace|Created Vulkan presentation swapchain|presenter stopped|Exception in|FATAL" \
    "$LOG_DIR/run.log" | tail -20 || true

trace=$(find "$REPORTS" -name 'frame_pacing_*.csv' -newer "$marker" | sort | tail -1)
if [ -z "$trace" ]; then
    echo "[fg-autotest] no trace was produced (exit status $status), run log: $LOG_DIR/run.log" >&2
    exit 1
fi
echo
python3 -I "$SR_DIR/script/analyze_frame_pacing.py" "$trace"
