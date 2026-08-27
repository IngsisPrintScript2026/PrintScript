#!/usr/bin/env bash

# PrintScript Runner & CLI Manager Script
# Supports: Manual/Help, Tab-Completion Guide, Bash Tab-Completion & Pipelines.

set -e

# ==============================================================================
# GUÍA DE COMANDOS Y FORMA DE EJECUTARLOS (help / tab / man)
# ==============================================================================
show_commands_guide() {
    cat << "EOF"
================================================================================
           PRINTSCRIPT CLI - COMANDOS Y FORMA DE EJECUTARLOS
================================================================================

1. VALIDATION (Validación sintáctica y semántica)
   Forma de ejecutar:
       ./run.sh Validation <version> <archivo.ps>
   Ejemplo:
       ./run.sh Validation 1.0 samples/valid_v10.ps
       ./run.sh Validation 1.1 samples/valid_v11.ps

2. EXECUTION (Ejecución del programa o modo REPL)
   Forma de ejecutar:
       ./run.sh Execution <version> <archivo.ps>
       ./run.sh Execution <version>                  (Inicia el REPL interactivo)
   Ejemplo:
       ./run.sh Execution 1.0 samples/valid_v10.ps
       ./run.sh Execution 1.1 samples/valid_v11.ps
       ./run.sh Execution 1.1

3. FORMATTING (Formateo de código fuente aplicando reglas YAML)
   Forma de ejecutar:
       ./run.sh Formatting <version> <archivo.ps> <reglas.yaml> [archivo_salida.ps]
   Ejemplo:
       ./run.sh Formatting 1.0 samples/unformatted.ps samples/format_rules.yaml samples/formatted_output.ps

4. ANALYZING (Análisis estático de código / Linter / SCA)
   Forma de ejecutar:
       ./run.sh Analyzing <version> <archivo.ps> <reglas_sca.yaml>
   Ejemplo:
       ./run.sh Analyzing 1.1 samples/sca_test.ps samples/sca_rules.yaml

================================================================================
VERSIONES DEL LENGUAJE:
   - 1.0 : Variables 'let', tipos number y string, operadores binarios, println.
   - 1.1 : Agrega 'const', boolean, bloques condicionales if-else, readInput, readEnv.

ACTIVAR AUTO-COMPLETADO CON LA TECLA TAB EN BASH:
   Ejecutá en tu terminal:
       source <(./run.sh completion)
================================================================================
EOF
}

# ==============================================================================
# SCRIPT DE AUTO-COMPLETADO DINÁMICO PARA LA TECLA TAB EN BASH
# ==============================================================================
show_completion() {
    cat << "EOF"
# PrintScript Bash Auto-Completion Script con Guía en Vivo
_printscript_run_completion() {
    local cur prev opts
    COMPREPLY=()
    cur="${COMP_WORDS[COMP_CWORD]}"
    prev="${COMP_WORDS[COMP_CWORD-1]}"

    # Nivel 1: Mostrar lista de operaciones y cómo ejecutarlas al presionar TAB
    if [ $COMP_CWORD -eq 1 ]; then
        if [ -z "$cur" ]; then
            echo -e "\n\033[1;36m=== Comandos de PrintScript y forma de ejecutarlos ===\033[0m" >&2
            echo -e "  \033[1;32mValidation\033[0m : ./run.sh Validation <version> <source.ps>" >&2
            echo -e "  \033[1;32mExecution\033[0m  : ./run.sh Execution <version> <source.ps>" >&2
            echo -e "  \033[1;32mFormatting\033[0m : ./run.sh Formatting <version> <source.ps> <rules.yaml> [out.ps]" >&2
            echo -e "  \033[1;32mAnalyzing\033[0m  : ./run.sh Analyzing <version> <source.ps> <rules.yaml>" >&2
            echo -e "  \033[1;33mhelp / tab\033[0m : Muestra la guía detallada de comandos" >&2
            echo -e "\033[1;36m====================================================\033[0m" >&2
        fi
        opts="Validation Execution Formatting Analyzing help man tab completion"
        COMPREPLY=( $(compgen -W "${opts}" -- "${cur}") )
        return 0
    fi

    # Nivel 2: Mostrar versiones disponibles
    if [ $COMP_CWORD -eq 2 ]; then
        if [ -z "$cur" ]; then
            echo -e "\n\033[1;36m=== Versiones disponibles ===\033[0m" >&2
            echo -e "  \033[1;32m1.0\033[0m : let, number, string, println" >&2
            echo -e "  \033[1;32m1.1\033[0m : const, boolean, if/else, readInput, readEnv" >&2
            echo -e "\033[1;36m==============================\033[0m" >&2
        fi
        opts="1.0 1.1"
        COMPREPLY=( $(compgen -W "${opts}" -- "${cur}") )
        return 0
    fi

    # Nivel 3: Archivo fuente (.ps)
    if [ $COMP_CWORD -eq 3 ]; then
        if [ -z "$cur" ]; then
            echo -e "\n\033[1;36m=== Seleccione archivo fuente (.ps) ===\033[0m" >&2
        fi
        COMPREPLY=( $(compgen -f -X '!*.ps' -- "${cur}") $(compgen -d -- "${cur}") )
        return 0
    fi

    # Nivel 4: Archivo de configuración (.yaml / .yml)
    if [ $COMP_CWORD -eq 4 ]; then
        if [ -z "$cur" ]; then
            echo -e "\n\033[1;36m=== Seleccione archivo de configuración (.yaml/.yml) ===\033[0m" >&2
        fi
        COMPREPLY=( $(compgen -f -X '!*.y*ml' -- "${cur}") $(compgen -d -- "${cur}") )
        return 0
    fi

    # Nivel 5: Archivo de salida (.ps)
    if [ $COMP_CWORD -eq 5 ]; then
        if [ -z "$cur" ]; then
            echo -e "\n\033[1;36m=== Ingrese nombre de archivo de salida (ej: formateado.ps) ===\033[0m" >&2
        fi
        COMPREPLY=( $(compgen -f -- "${cur}") )
        return 0
    fi
}

complete -F _printscript_run_completion ./run.sh
EOF
}

# ==============================================================================
# PARSEO DE COMANDOS ESPECIALES (help, tab, man, completion, /t, -t)
# ==============================================================================
case "$1" in
    help|man|-h|--help|tab|-t|--tab|/t|/tab)
        show_commands_guide
        exit 0
        ;;
    completion|--completion)
        show_completion
        exit 0
        ;;
esac

# Si se ejecuta sin argumentos, mostrar guía de comandos
if [ $# -eq 0 ]; then
    show_commands_guide
    exit 0
fi

OPERATION="Execution"
VERSION="1.0"
FILE=""
CONFIG=""
OUTPUT=""

# Check si el primer argumento es una operación
case "$1" in
    Validation|validation|validate)
        OPERATION="Validation"
        shift
        ;;
    Execution|execution|exec|interpret|run)
        OPERATION="Execution"
        shift
        ;;
    Formatting|formatting|format|fmt)
        OPERATION="Formatting"
        shift
        ;;
    Analyzing|analyzing|sca|analyze|lint)
        OPERATION="Analyzing"
        shift
        ;;
esac

# Si el siguiente argumento parece una versión (1.0 o 1.1)
if [[ "$1" =~ ^[0-9]+\.[0-9]+$ ]]; then
    VERSION="$1"
    shift
fi

# Argumentos posicionales restantes: FILE, CONFIG, OUTPUT
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

# Construir argumentos para Gradle con rutas absolutas
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
