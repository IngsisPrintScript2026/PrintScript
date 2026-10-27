# Samples para probar PrintScript

Todos los comandos se corren desde la raíz del repo con `run.sh`, que recompila la CLI solo si cambió algo.
Para correr todo de una vez: `./samples/probar_todo.sh`.

Orden de argumentos de `run.sh`:

```
./run.sh exec     <versión> <programa.ps>
./run.sh validate <versión> <programa.ps>
./run.sh format   <versión> <programa.ps> <reglas.yaml> [salida.ps]   # sin salida, imprime en pantalla
./run.sh sca      <versión> <programa.ps> <reglas.yaml>
```

> Ojo con `format`: el **tercer** argumento es el YAML y el **cuarto** es el archivo de salida. Si los invertís, el resultado pisa el YAML.

## 1. Ejecución (`samples/exec`)

| Comando | Qué prueba | Salida esperada |
| --- | --- | --- |
| `./run.sh exec 1.0 samples/exec/v10_ejemplo1.ps` | Ejemplo 1 de la consigna | `Joe Doe` |
| `./run.sh exec 1.0 samples/exec/v10_ejemplo2.ps` | Ejemplo 2: división en otra variable | `Result: 3` |
| `./run.sh exec 1.0 samples/exec/v10_ejemplo3.ps` | Ejemplo 3: reasignación | `Result: 3` |
| `./run.sh exec 1.0 samples/exec/v10_aritmetica.ps` | Precedencia, asociatividad, decimales, división no exacta, concatenación, comillas simples, declarar sin valor | `11` `3` `2` `3` `0.3` `2.5` `3.333333333333333` `Total: 12` `3 manzanas` `comillas simples` `42` (una por línea) |
| `./run.sh exec 1.1 samples/exec/v11_const_if.ps` | `const`, `boolean`, `if`/`else`, scope de bloque, `if` sin `else` | `PrintScript 1.1 está activo` · `Intentos restantes: 2` · `Fin` |
| `printf 'Ana\n30\ntrue\n' \| ./run.sh exec 1.1 samples/exec/v11_readinput.ps` | `readInput` con string, number y boolean (una línea de stdin por llamada) | los 3 prompts y luego `Hola Ana` · `El año que viene tenés 31` · `Tenés descuento` |
| `NOMBRE=Ana VECES=21 ./run.sh exec 1.1 samples/exec/v11_readenv.ps` | `readEnv` con string y number | `Hola Ana` · `42` |

Probá también `./run.sh exec 1.1 samples/exec/v11_readinput.ps` sin el `printf` y respondé a mano.

## 2. Errores (`samples/errors`)

Todos terminan con código de salida 1 y muestran tipo de error y posición de inicio y fin.

| Comando | Error esperado |
| --- | --- |
| `./run.sh validate 1.0 samples/errors/lexical.ps` | `Lexical error: unrecognized character '@' at line 2, column 19` |
| `./run.sh validate 1.0 samples/errors/syntax_missing_colon.ps` | `Syntactic error: Unexpected token 'number' from line 2, column 7 to line 2, column 12` |
| `./run.sh validate 1.0 samples/errors/syntax_missing_semicolon.ps` | `Syntactic error: Unexpected token 'println' from line 2, column 1 …` |
| `./run.sh validate 1.0 samples/errors/semantic_type_mismatch.ps` | `Semantic error: Cannot assign type STRING to variable 'a' of type NUMBER` |
| `./run.sh validate 1.0 samples/errors/semantic_undeclared.ps` | `Semantic error: Variable 'total' is not declared` |
| `./run.sh validate 1.1 samples/errors/semantic_const_reassign.ps` | `Semantic error: Cannot reassign constant variable 'limite'` |
| `./run.sh validate 1.1 samples/errors/semantic_if_not_boolean.ps` | `Semantic error: Condition of 'if' at line 2 must be boolean` |
| `./run.sh exec 1.0 samples/errors/runtime_division_by_zero.ps` | imprime `antes del error` y luego `Runtime error: Division by zero` |
| `echo quizas \| ./run.sh exec 1.1 samples/errors/runtime_bad_input.ps` | `Runtime error: … Cannot parse input 'quizas' as boolean` |
| `echo Ana \| ./run.sh exec 1.0 samples/errors/v10_uses_v11_features.ps` | `Function 'readInput' is not supported in PrintScript 1.0` (con `1.1` funciona) |
| `./run.sh validate 1.0 samples/exec/v11_const_if.ps` | `Unexpected token 'const' … (not valid in PrintScript 1.0)` |

## 3. Formatter (`samples/format`)

| Comando | Qué cambia |
| --- | --- |
| `./run.sh format 1.0 samples/format/feo_v10.ps samples/format/reglas_espaciado.yaml` | `nombre: string = "Ana"`, una sentencia por línea, un espacio entre tokens y alrededor de operadores, una línea vacía antes de cada `println` |
| `./run.sh format 1.0 samples/format/feo_v10.ps samples/format/reglas_compacto.yaml` | `nombre : string="Ana"`, sin líneas vacías antes de `println` |
| `./run.sh format 1.1 samples/format/feo_v11.ps samples/format/reglas_if.yaml` | bloques `if` indentados con 2 espacios, `{` en la línea del `if`, `} else {` |
| `./run.sh format 1.1 samples/format/feo_v11.ps samples/format/reglas_if_llave_abajo.yaml` | indentación de 4 y la `{` en la línea siguiente |
| `./run.sh format 1.0 samples/format/feo_v10.ps samples/format/reglas_espaciado.yaml /tmp/salida.ps` | lo mismo, pero escrito en `/tmp/salida.ps` |

Salida esperada del primero:

```
let nombre: string = "Ana";
let edad: number = 30;
let total: number = edad * 2 + 10 / 5;

println(nombre + " tiene " + edad);

println(total);
```

## 4. Analizador estático (`samples/sca`)

| Comando | Resultado esperado |
| --- | --- |
| `./run.sh sca 1.0 samples/sca/con_violaciones.ps samples/sca/reglas_camel.yaml` | 2 violaciones: `'mi_nombre'` no es camel case (línea 1, columna 5) y `println` con una expresión (línea 3) |
| `./run.sh sca 1.0 samples/sca/limpio_camel.ps samples/sca/reglas_camel.yaml` | `SCA analysis passed with 0 violations` |
| `./run.sh sca 1.0 samples/sca/limpio_snake.ps samples/sca/reglas_snake.yaml` | `SCA analysis passed with 0 violations` |
| `./run.sh sca 1.0 samples/sca/limpio_snake.ps samples/sca/reglas_camel.yaml` | 2 violaciones de camel case (`mi_nombre`, `edad_actual`) |
| `./run.sh sca 1.1 samples/sca/readinput_v11.ps samples/sca/reglas_readinput.yaml` | 1 violación: `readInput` con una expresión (línea 2); la línea 3 con un literal pasa |
