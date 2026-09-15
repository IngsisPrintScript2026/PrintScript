# 📦 Módulo `com.ingsis.interpreter`

## 📖 Descripción General

El módulo `com.ingsis.interpreter` es el motor de ejecución de **PrintScript**. Su objetivo es interpretar y ejecutar las instrucciones del programa garantizando el aislamiento de ámbitos léxicos (scopes), la inmutabilidad de variables constantes, la evaluación aritmética y lógica precisa, y la interacción desacoplada con el entorno de I/O (consola, entrada de usuario y variables de entorno del sistema).

---

## 🔗 Dependencias del Módulo

Definidas en [`build.gradle`](./build.gradle):

| Módulo | Tipo de Dependencia | Uso e Insumos Consumidos |
| :--- | :--- | :--- |
| **`com.ingsis.common`** | `implementation` | - **Nodos AST**: `Node`, `ProgramNode`, `DeclarationKeywordNode`, `AssignNode`, `IfKeywordNode`, `CallFunctionNode`, `OperatorNode`, `IdentifierNode`, `NumberLiteralNode`, `StringLiteralNode`, `BooleanLiteralNode`, `NilExpressionNode`.<br>- **Enums**: `DataType`, `OperatorType`, `DeclarationType`.<br>- **Flujo y Mónadas**: `TokenStream`, `IterationStep`, `Result`, `CorrectResult`, `IncorrectResult`. |
| **`com.ingsis.parser`** | `implementation` | - **Parser Sintáctico**: `syntactic.Parser<Node>` (ej. provisto por `ParserFactory`).<br>- **Análisis Semántico**: `SemanticChecker`, `SemanticEnvironment`. |

---

## 📥 Insumos Necesarios para Operar

El intérprete implementa la interfaz [`Interpreter`](./src/main/java/interpreter/Interpreter.java) a través de [`DefaultInterpreter`](./src/main/java/interpreter/DefaultInterpreter.java). Admite dos modos de ejecución según los insumos provistos:

### 1. Modo Streaming / Evaluación Perezosa ($O(1)$ en memoria)
```java
Result<SemanticEnvironment> interpret(
    TokenStream tokenStream,
    SemanticEnvironment semanticEnv,
    Environment runtimeEnv
)
```
**Insumos requeridos:**
1. **`TokenStream`**: Flujo de tokens continuo y perezoso (Lazy).
2. **`syntactic.Parser<Node>`**: Parser inyectado encargado de parsear una sentencia por vez (`Parser.parse(stream)`).
3. **`SemanticChecker`**: Chequeador semántico para validar cada sentencia de forma incremental antes de ejecutarla.
4. **`SemanticEnvironment`**: Tabla de símbolos semánticos para validar tipos estáticos y existencia de variables.
5. **`Environment`**: Tabla de símbolos de runtime para instanciar las variables y almacenar sus valores en memoria.

> **Flujo de Ejecución:**
> En cada iteración: `Parse Statement` ➔ `Semantic Check` ➔ `Execute Statement`. Si ocurre algún fallo sintáctico, semántico o de runtime, la ejecución se interrumpe de inmediato retornando el `Result.failure(...)` correspondiente.

### 2. Modo AST Completo
```java
Result<Void> interpret(ProgramNode program, Environment globalEnv)
```
**Insumos requeridos:**
1. **`ProgramNode`**: Árbol de sintaxis abstracta previamente construido y validado, que expone la lista de sentencias vía `program.statements()`.
2. **`Environment`**: Entorno en el cual se persistirán las declaraciones y modificaciones del programa.

---

## 🔌 Servicios Externos e I/O (Inversión de Control)

Para evitar acoplamiento rígido con el sistema operativo y permitir testing aislado, el intérprete requiere las siguientes abstracciones configurables:

| Abstracción | Firma / Interfaz | Propósito | Default |
| :--- | :--- | :--- | :--- |
| **Emisor de Salida** | `Consumer<String> outputEmitter` | Captura las cadenas emitidas por `println` y los prompts de `readInput`. | `System.out::println` |
| **Proveedor de Entrada** | `InputProvider` (`prompt -> String`) | Provee la lectura de texto ingresado por el usuario para `readInput`. | `prompt -> ""` |
| **Proveedor de Entorno** | `EnvProvider` (`name -> String`) | Provee la resolución de variables del sistema para `readEnv`. | `System::getenv` |
| **Registro de Funciones** | `FunctionRegistry` | Diccionario de funciones nativas invocables (`BuiltInFunction`). | Instancia de `DefaultFunctionRegistry` |

---

## 🏛️ Arquitectura y Paquetes

```mermaid
flowchart TD
    TS[TokenStream] --> DI[DefaultInterpreter]
    SP[syntactic.Parser] --> DI
    SC[SemanticChecker] --> DI

    DI -->|Statement por statement| SE[DefaultStatementExecutor]
    SE -->|Evalúa expresiones| EE[DefaultExpressionEvaluator]
    SE -->|Guarda / actualiza valores| ENV[Environment (Ámbitos léxicos)]

    EE -->|Consulta identificadores| ENV
    EE -->|Evalúa llamadas a built-ins| FR[DefaultFunctionRegistry]
    SE -->|Ejecuta llamadas de sentencia| FR

    FR --> PF[PrintlnFunction -> Consumer&lt;String&gt;]
    FR --> RIF[ReadInputFunction -> InputProvider]
    FR --> REF[ReadEnvFunction -> EnvProvider]
```

### Estructura de Clases:

```
com.ingsis.interpreter/src/main/java/
├── interpreter/
│   ├── Interpreter.java                 # Contrato del intérprete
│   └── DefaultInterpreter.java          # Orquestador del ciclo parse-check-execute
├── environment/
│   └── Environment.java                 # Ámbitos léxicos anidados y tabla de símbolos
├── executor/
│   ├── StatementExecutor.java           # Contrato para ejecutar nodos sentencia
│   └── DefaultStatementExecutor.java    # Pattern matching y ejecución de sentencias
├── evaluator/
│   ├── ExpressionEvaluator.java         # Contrato de evaluación de expresiones
│   └── DefaultExpressionEvaluator.java  # Evaluación de literales, operadores y llamadas
└── builtin/
    ├── BuiltInFunction.java             # Interfaz para funciones nativas
    ├── FunctionRegistry.java            # Contrato de registro de funciones
    ├── DefaultFunctionRegistry.java     # Implementación con println, readInput, readEnv
    ├── PrintlnFunction.java             # Función nativa println
    ├── ReadInputFunction.java           # Función nativa readInput (con coerción de tipos)
    ├── ReadEnvFunction.java             # Función nativa readEnv (con coerción de tipos)
    └── provider/
        ├── InputProvider.java           # Interfaz funcional (prompt -> String)
        └── EnvProvider.java             # Interfaz funcional (varName -> String)
```

---

## 🧩 Detalle de Componentes Clave

### 1. `Environment` (Gestión de Memoria y Scopes)
Modela los ámbitos léxicos mediante una jerarquía enlazada hacia su padre (`private final Environment parent`).
- **Registro de variables**: Almacena tuplas `VariableInfo(Object value, DataType type, boolean isMutable)`.
- **Reglas de negocio**:
  - `declare(...)`: Registra en el ámbito actual. Falla con runtime error si la variable ya fue declarada en el mismo scope.
  - `assign(...)`: Busca en el scope actual; si es inmutable (`const`), falla impidiendo reasignación. Si no existe localmente, delega recursivamente en el `parent`. Falla si alcanza la raíz sin encontrarla.
  - `get(...)` y `find(...)`: Resuelven el identificador navegando la cadena de ancestros (soporta *variable shadowing* en bloques internos).

### 2. `DefaultStatementExecutor` (Ejecución de Sentencias)
Ejecuta los nodos que representan instrucciones mediante pattern matching:
- **`DeclarationKeywordNode`**: Evalúa opcionalmente la expresión inicial y la declara en el `Environment` respetando su mutabilidad.
- **`AssignNode`**: Evalúa la expresión y actualiza el valor en el `Environment`.
- **`IfKeywordNode`**: Evalúa la condición (exige que sea `Boolean`), crea un entorno anidado `new Environment(env)` y ejecuta secuencialmente el cuerpo `thenBody` o `elseBody`.
- **`CallFunctionNode`**: Busca la función en el `FunctionRegistry`, evalúa argumentos y ejecuta `function.execute(...)`.

### 3. `DefaultExpressionEvaluator` (Evaluación de Expresiones)
Resuelve el valor computado de cualquier expresión:
- **Literales**: Extrae el valor directo de `NumberLiteralNode` (`BigDecimal`), `StringLiteralNode` (`String`), `BooleanLiteralNode` (`Boolean`) o `NilExpressionNode` (`null`).
- **Identificadores (`IdentifierNode`)**: Obtiene el valor asociado en el `Environment`.
- **Operadores Binarios (`OperatorNode`)**:
  - `+`: Concatena si algún operando es `String`; de lo contrario, suma exacta con `BigDecimal.add()`.
  - `-`, `*`, `/`: Operaciones aritméticas estrictas con `BigDecimal` (`subtract`, `multiply`, `divide`).
  - `=`: Retorna el valor evaluado a la derecha.
- **Llamadas a Función (`CallFunctionNode`)**:
  - Evalúa llamadas a `readInput` o `readEnv`, aplicando coerción de tipos al `DataType targetType` requerido (`STRING`, `NUMBER`, `BOOLEAN`).

### 4. Funciones Nativas (`builtin`)
- **`println`**: Consume el argumento y lo despacha al `outputEmitter`.
- **`readInput`**: Emite el prompt al `outputEmitter`, lee del `InputProvider` y transforma la cadena a `BigDecimal`, `Boolean` o `String`.
- **`readEnv`**: Consulta la variable en `EnvProvider` y la transforma al tipo solicitado. Arroja error de runtime si la variable no existe en el sistema.

---

## 🚨 Manejo de Errores y Formato de Salida

El intérprete no propaga excepciones no controladas; encapsula todas las respuestas en [`Result<T>`](file:///home/elchurro274/Faculty/ingsis/PrintScript/com.ingsis.common/src/main/java/result/Result.java) estandarizando prefijos diagnósticos:

- **Error Sintáctico**: `"Syntactic error: <detalle>"`
- **Error Semántico**: `"Semantic error: <detalle>"`
- **Error de Runtime**: `"Runtime error: <detalle>"` (variable no declarada, constante reasignada, tipo incompatible, variable de entorno ausente, etc.).

---

## 🛠️ Extensibilidad: ¿Qué se necesita para ampliar el intérprete?

1. **Nuevos Operadores (comparadores `<`, `>`, `==`, lógicos `&&`, `||`)**:
   - Extender el enum `OperatorType` en `common`.
   - Incorporar los casos en `DefaultExpressionEvaluator.evaluateOperator`.
2. **Nuevas Funciones Built-in**:
   - Implementar `BuiltInFunction`.
   - Registrar la instancia en `DefaultFunctionRegistry`.
3. **Nuevas Estructuras de Control (ej. `while`, `for`)**:
   - Agregar el nodo correspondiente al `switch` de `DefaultStatementExecutor.execute`.
