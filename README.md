## Uso de la Interfaz de Línea de Comandos (`run.sh`)

El script `./run.sh` permite ejecutar de manera sencilla las distintas operaciones de PrintScript (intérprete/ejecución, formateador de código y analizador estático SCA).

### Sintaxis General
```bash
./run.sh [OPERACIÓN] [VERSIÓN] [ARCHIVO_FUENTE] [CONFIG_YAML] [ARCHIVO_SALIDA]
```

### Operaciones Disponibles

#### 1. `exec` / `interpret` / `run` (Ejecución del Código)
Ejecuta el código fuente de PrintScript a través de la tubería completa (Sintáctico $\rightarrow$ Semántico $\rightarrow$ Intérprete). Si no se provee un archivo fuente, inicia el modo REPL interactivo.
```bash
./run.sh exec 1.0 mi_programa.ps
```

#### 2. `format` / `fmt` (Formateador de Código)
Parsea la sintaxis del programa y reconstruye el código fuente aplicando las reglas de formateo configurables mediante un archivo YAML.
```bash
# Mostrar el resultado formateado por consola (STDOUT):
./run.sh format 1.0 mi_programa.ps reglas_formateo.yaml

# Guardar el resultado formateado en un nuevo archivo:
./run.sh format 1.0 mi_programa.ps reglas_formateo.yaml programa_formateado.ps
```

#### 3. `sca` / `analyze` / `lint` (Analizador Estático de Código)
Analiza el AST y el entorno semántico del código fuente contra reglas de convenciones de nombrado (`camelCase` / `snake_case`) y restricciones de parámetros de funciones (`println`, `readInput`).
```bash
./run.sh sca 1.0 mi_programa.ps reglas_sca.yaml
```

### Compatibilidad y Valores por Defecto
Si el primer argumento enviado es directamente un número de versión (ej: `1.0` o `1.1`), la operación se asume por defecto como `exec`:
```bash
./run.sh 1.0 mi_programa.ps
```

---

## 1. Estructura de Módulos

* **`:com.ingsis.common`**: Modelos de dominio (`Token`, `Position`, `MetaCharacter`, `Node`), mónadas de resultado (`Result<T>`) e interfaces base.
* **`:com.ingsis.charstream`**: Adaptadores I/O para streaming de caracteres con pushback (`CharStream`, `StreamCharReader`, `PositionTracker`).
* **`:com.ingsis.lexer`**: Orquestador léxico (`Lexer`) e intérprete de estados de acumulación (`PrintScriptTokenizer`).
* **`:com.ingsis.parser`**: Parser sintáctico del lenguaje que construye el AST (`ProgramNode`) y validador semántico/de tipos (`SemanticChecker`).
* **`:com.ingsis.formatter`**: Formateador de código fuente basado en AST mediante Visitor con Dispatch y Handlers configurables via YAML.
* **`:com.ingsis.sca`**: Analizador estático de código basado en AST y entorno semántico mediante Visitor con Dispatch y Handlers configurables via YAML.
* **`:com.ingsis.interpreter`**: Intérprete y evaluador de sentencias/expresiones sobre el AST.
* **`:com.ingsis.engine`**: Fachada de alto nivel ([`Engine`](file:///home/elchurro274/Faculty/ingsis/PrintScript/com.ingsis.engine/src/main/java/engine/Engine.java), [`CliEngine`](file:///home/elchurro274/Faculty/ingsis/PrintScript/com.ingsis.engine/src/main/java/engine/CliEngine.java)) que orquesta los servicios ([`ExecuteService`](file:///home/elchurro274/Faculty/ingsis/PrintScript/com.ingsis.engine/src/main/java/service/ExecuteService.java), [`FormatService`](file:///home/elchurro274/Faculty/ingsis/PrintScript/com.ingsis.engine/src/main/java/service/FormatService.java), [`LintService`](file:///home/elchurro274/Faculty/ingsis/PrintScript/com.ingsis.engine/src/main/java/service/LintService.java)).

---

## 2. Ejecución de Pruebas Unitarias

Para compilar y verificar el comportamiento de todos los módulos del proyecto:

```bash
./gradlew test
```

Salida esperada:
```text
> Task :com.ingsis.common:test PASSED
> Task :com.ingsis.charstream:test PASSED
> Task :com.ingsis.lexer:test PASSED
> Task :com.ingsis.syntactic:test PASSED
> Task :com.ingsis.semantic:test PASSED
> Task :com.ingsis.formatter:test PASSED
> Task :com.ingsis.sca:test PASSED
> Task :com.ingsis.interpreter:test PASSED
> Task :com.ingsis.engine:test PASSED

BUILD SUCCESSFUL in 1s
```
