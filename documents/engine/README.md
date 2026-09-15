# Modulo com.ingsis.engine

## Descripcion General

El modulo `com.ingsis.engine` es el orquestador principal y punto de entrada ejecutable (*driver / CLI application*) del lenguaje PrintScript. Su funcion es integrar todos los modulos del compilador/interprete en un pipeline unificado, proporcionando tanto una API programatica de alto nivel a traves de la interfaz [`Engine`](file:///home/elchurro274/Faculty/ingsis/PrintScript/com.ingsis.engine/src/main/java/engine/Engine.java) como una aplicacion de linea de comandos completa mediante [`CliEngine`](file:///home/elchurro274/Faculty/ingsis/PrintScript/com.ingsis.engine/src/main/java/engine/CliEngine.java) basada en **Picocli**.

---

## Dependencias del Modulo

Definidas en [build.gradle](file:///home/elchurro274/Faculty/ingsis/PrintScript/com.ingsis.engine/build.gradle):

| Modulo | Tipo | Proposito |
| :--- | :--- | :--- |
| `com.ingsis.common` | `implementation` | Nodos del AST, tipos, monada `Result<T>`, iteradores y abstracciones base. |
| `com.ingsis.charstream` | `implementation` | Streaming de caracteres y buffers de entrada. |
| `com.ingsis.lexer` | `implementation` | Tokenizador Maximal Munch. |
| `com.ingsis.parser` | `implementation` | Parser sintactico descendente/Pratt y chequeador semantico. |
| `com.ingsis.interpreter` | `implementation` | Evaluador en tiempo de ejecucion y entornos de memoria. |
| `com.ingsis.formatter` | `implementation` | Formateador por AST y stream de tokens. |
| `com.ingsis.sca` | `implementation` | Analizador estatico de codigo (Linter). |
| `info.picocli:picocli` | `implementation` | Framework para CLI, opciones, flags y parsing de argumentos de consola. |

---

## Arquitectura del Modulo

```
com.ingsis.engine/
├── src/main/java/
│   ├── engine/
│   │   ├── Engine.java               # Interfaz raiz del motor
│   │   ├── CliEngine.java            # Aplicacion CLI ejecutable (Picocli) y REPL interactivo
│   │   ├── InputSupplier.java        # Abstraccion funcional para lectura de inputs (readInput)
│   │   └── OutputEmitter.java        # Abstraccion funcional para emision de salidas (println)
│   └── service/
│       ├── ExecuteService.java       # Servicio de orquestacion y ejecucion en streaming
│       ├── ValidationService.java    # Servicio de validacion sintactica y semantica
│       ├── FormatService.java        # Servicio de formateo de codigo fuente
│       └── LintService.java          # Servicio de analisis estatico con reglas SCA
```

---

## Servicios Especializados

El modulo organiza cada responsabilidad en un servicio independiente que orquesta los modulos correspondientes:

### 1. `ValidationService` (Validacion)
* **Objetivo:** Valida la correccion sintactica y semantica del codigo fuente sin ejecutarlo.
* **Pipeline:** `InputStream` $\rightarrow$ `CharStream` $\rightarrow$ `Lexer` $\rightarrow$ `LazyTokenStream` $\rightarrow$ `Parser.parse()` $\rightarrow$ `SemanticChecker.checkNode()`.
* **Manejo de Errores:** En caso de fallas, formatea la salida reportando las coordenadas exactas de rango: `[Line X, Column Y to Line W, Column Z]`.

### 2. `ExecuteService` (Ejecucion / Interpretacion)
* **Objetivo:** Ejecuta el programa en modo streaming linea a linea.
* **Pipeline:** Lee sentencias de forma perezosa, valida sintaxis y tipos con `SemanticChecker`, e interpreta de inmediato el nodo resultante utilizando `DefaultInterpreter` sobre el entorno de ejecucion (`Environment`).
* **I/O Desacoplado:** Recibe [`InputSupplier`](file:///home/elchurro274/Faculty/ingsis/PrintScript/com.ingsis.engine/src/main/java/engine/InputSupplier.java) y [`OutputEmitter`](file:///home/elchurro274/Faculty/ingsis/PrintScript/com.ingsis.engine/src/main/java/engine/OutputEmitter.java), permitiendo conectar consolas, buffers de prueba, archivos o sockets de red sin acoplarse a `System.in`/`System.out`.

### 3. `FormatService` (Formateo)
* **Objetivo:** Aplica las politicas de espaciado, saltos de linea y sangria al codigo fuente.
* **Pipeline:** Carga la configuracion YAML mediante `YamlFormatRulesLoader` y delega a `TokenStreamFormatter` o `ASTFormatter`.

### 4. `LintService` (Analisis Estatico)
* **Objetivo:** Audita el codigo fuente segun las reglas de estilo y nomenclatura configuradas en un archivo YAML.
* **Pipeline:** Parsea el programa completo a un `ProgramNode`, inicializa el `ASTSca` con las reglas de `YamlScaRulesLoader` y emite la lista consolidada de violaciones detectadas.

---

## Interfaz de Linea de Comandos (`CliEngine`)

La aplicacion CLI se ejecuta a traves de Gradle:

```bash
./gradlew :com.ingsis.engine:run --args="<operacion> [opciones]"
```

### Operaciones Disponibles:

1. **Validacion:**
   ```bash
   ./gradlew :com.ingsis.engine:run --args="validation -i archivo.ps -v 1.1"
   ```

2. **Ejecucion:**
   ```bash
   ./gradlew :com.ingsis.engine:run --args="execution -i archivo.ps -v 1.1"
   ```

3. **Formateo:**
   ```bash
   ./gradlew :com.ingsis.engine:run --args="formatting -i entrada.ps -c format-rules.yaml -o formateado.ps -v 1.1"
   ```

4. **Analisis Estatico (Linting):**
   ```bash
   ./gradlew :com.ingsis.engine:run --args="analyzing -i archivo.ps -c sca-rules.yaml -v 1.1"
   ```

5. **Modo REPL Interactivo:**
   Si se omite la operacion o el archivo de entrada, el CLI inicia automaticamente un REPL interactivo para evaluar sentencias linea a linea preservando el entorno entre comandos.

---

## Patrones de Diseno Aplicados

| Patron | Implementacion y Proposito |
| :--- | :--- |
| **Facade** | `CliEngine` y `ExecuteService` actuan como fachadas que ocultan la complejidad de instanciacion y coordinacion entre Lexer, Parser, Interpreter, Formatter y SCA. |
| **Service Layer** | Los servicios (`ValidationService`, `ExecuteService`, `FormatService`, `LintService`) encapsulan casos de uso especificos. |
| **Adapter / Inversion of Control** | `InputSupplier` y `OutputEmitter` desacoplan la logica de ejecucion de las implementaciones concretas de Entrada/Salida. |
| **Command (Picocli)** | `CliEngine` modela la interaccion de usuario mediante comandos y banderas parseadas de forma declarativa. |
| **Monad Result** | Todas las operaciones retornan `Result<T>` (`CorrectResult` o `IncorrectResult`), garantizando un control de flujo determinista sin excepciones no controladas. |
