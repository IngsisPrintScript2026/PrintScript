#!/usr/bin/env bash
set -e

# Configura git para usar el directorio de hooks versionados
git config core.hooksPath .githooks

# Asegura permisos de ejecución
chmod +x .githooks/*

echo "✅ Hooks de Git instalados correctamente desde .githooks (core.hooksPath = .githooks)"
