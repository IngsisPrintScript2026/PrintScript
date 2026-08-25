#!/usr/bin/env bash

# PrintScript Runner Script
# Usage:
#   ./run.sh [exec|format|sca] [version (1.0|1.1)] [file.ps] [config.yaml] [output.ps]
#
# Examples:
#   ./run.sh exec 1.0 program.ps
#   ./run.sh format 1.0 program.ps rules.yaml formatted.ps
#   ./run.sh sca 1.0 program.ps rules.yaml
#   ./run.sh 1.0 program.ps                      (Defaults to exec/interpret)

set -e

OPERATION="interpret"
VERSION="1.0"
FILE=""
CONFIG=""
OUTPUT=""

# Check if first arg is an operation name
case "$1" in
    exec|interpret|run)
        OPERATION="interpret"
        shift
        ;;
    format|fmt)
        OPERATION="format"
        shift
        ;;
    sca|analyze|lint)
        OPERATION="analyze"
        shift
        ;;
esac

# If next arg looks like a version (e.g., 1.0, 1.1)
if [[ "$1" =~ ^[0-9]+\.[0-9]+$ ]]; then
    VERSION="$1"
    shift
fi

# Remaining positional arguments: FILE, CONFIG, OUTPUT
if [ -n "$1" ]; then
    FILE="$1"
    shift
fi

if [ -n "$1" ]; then
    CONFIG="$1"
    shift
fi

if [ -n "$1" ]; then
    OUTPUT="$1"
    shift
fi

# Build Gradle --args string with absolute file paths
ARGS="$OPERATION -v $VERSION"

if [ -n "$FILE" ]; then
    ABS_FILE=$(realpath "$FILE")
    ARGS="$ARGS -i $ABS_FILE"
fi

if [ -n "$CONFIG" ]; then
    ABS_CONFIG=$(realpath "$CONFIG")
    ARGS="$ARGS -c $ABS_CONFIG"
fi

if [ -n "$OUTPUT" ]; then
    ABS_OUTPUT=$(realpath -m "$OUTPUT")
    ARGS="$ARGS -o $ABS_OUTPUT"
fi

./gradlew :com.ingsis.engine:run --args="$ARGS" --console=plain
