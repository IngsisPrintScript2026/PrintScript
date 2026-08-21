#!/usr/bin/env bash

# PrintScript Execution Script
# Usage:
#   ./run.sh 1.x            (Interactive REPL for version 1.x)
#   ./run.sh 1.x text.ps    (Execute PrintScript file with version 1.x)

VERSION="${1:-1.0}"
FILE="$2"

if [ -n "$FILE" ]; then
    ./gradlew :com.ingsis.engine:run --args="-v $VERSION -i $FILE" --console=plain
else
    ./gradlew :com.ingsis.engine:run --args="-v $VERSION" --console=plain
fi
