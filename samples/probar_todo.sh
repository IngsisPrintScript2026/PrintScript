#!/usr/bin/env bash
# Corre todos los samples de samples/PRUEBAS.md y muestra la salida de cada uno.
# Uso: ./samples/probar_todo.sh   (desde la raíz del repo)
set -uo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
R=./run.sh
S=samples

run() {
    local stdin="$1"; shift
    echo
    echo "=== $*"
    printf '%b' "$stdin" | "$@"
    echo "[exit $?]"
}

echo "##### 1. Ejecución"
run "" $R exec 1.0 $S/exec/v10_ejemplo1.ps
run "" $R exec 1.0 $S/exec/v10_ejemplo2.ps
run "" $R exec 1.0 $S/exec/v10_ejemplo3.ps
run "" $R exec 1.0 $S/exec/v10_aritmetica.ps
run "" $R exec 1.1 $S/exec/v11_const_if.ps
run 'Ana\n30\ntrue\n' $R exec 1.1 $S/exec/v11_readinput.ps
run "" env NOMBRE=Ana VECES=21 $R exec 1.1 $S/exec/v11_readenv.ps

echo; echo "##### 2. Errores (todos deben terminar con exit 1)"
for f in lexical syntax_missing_colon syntax_missing_semicolon semantic_type_mismatch semantic_undeclared; do
    run "" $R validate 1.0 $S/errors/$f.ps
done
run "" $R validate 1.1 $S/errors/semantic_const_reassign.ps
run "" $R validate 1.1 $S/errors/semantic_if_not_boolean.ps
run "" $R exec 1.0 $S/errors/runtime_division_by_zero.ps
run 'quizas\n' $R exec 1.1 $S/errors/runtime_bad_input.ps
run 'Ana\n' $R exec 1.0 $S/errors/v10_uses_v11_features.ps
run "" $R validate 1.0 $S/exec/v11_const_if.ps

echo; echo "##### 3. Formatter"
run "" $R format 1.0 $S/format/feo_v10.ps $S/format/reglas_espaciado.yaml
run "" $R format 1.0 $S/format/feo_v10.ps $S/format/reglas_compacto.yaml
run "" $R format 1.1 $S/format/feo_v11.ps $S/format/reglas_if.yaml
run "" $R format 1.1 $S/format/feo_v11.ps $S/format/reglas_if_llave_abajo.yaml

echo; echo "##### 4. Analizador estático"
run "" $R sca 1.0 $S/sca/con_violaciones.ps $S/sca/reglas_camel.yaml
run "" $R sca 1.0 $S/sca/limpio_camel.ps $S/sca/reglas_camel.yaml
run "" $R sca 1.0 $S/sca/limpio_snake.ps $S/sca/reglas_snake.yaml
run "" $R sca 1.0 $S/sca/limpio_snake.ps $S/sca/reglas_camel.yaml
run "" $R sca 1.1 $S/sca/readinput_v11.ps $S/sca/reglas_readinput.yaml
