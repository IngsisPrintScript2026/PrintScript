# 📦 Módulo `com.ingsis.parser`

## Descripción General

El módulo `com.ingsis.parser` es el **analizador sintáctico** del lenguaje PrintScript. Su responsabilidad es consumir un flujo de `Token` (producido por el módulo `lexer`) y construir un **Árbol de Sintaxis Abstracta (AST)** que representa la estructura jerárquica del programa fuente.

Implementa un parser de **descenso recursivo** combinado con **Pratt Parsing** (Top-Down Operator Precedence) para resolver la precedencia de operadores en expresiones.

---

## Dependencias

| Módulo | Relación |
|--------|----------|
| `com.ingsis.common` | **Depende de** — Usa `SafeIterator`, `IterationStep`, `Result`, `Token`, `TokenType`, nodos AST (`Node`, `ProgramNode`, etc.), `Position`, `PrintScriptVersion`. |

---

## Responsabilidades Clave

1. **Navegación Inmutable del Flujo de Tokens**: Provee la abstracción `TokenStream` para consumir, verificar y retroceder tokens sin mutación destructiva.
2. **Parametrización por Versiones**: Adapta las reglas sintácticas según la versión del lenguaje (`PrintScriptVersion` 1.0 vs 1.1) mediante `GrammarRules`.
3. **Análisis Sintáctico Recursivo**: Descompone sentencias (declaraciones, expresiones, condicionales, funciones).
4. **Precedencia de Operadores**: Resuelve la precedencia matemática usando el algoritmo Pratt.
5. **Manejo Funcional de Errores**: Retorna `Result<T>` evitando excepciones no controladas.

---

## Estructura de Archivos

```
com.ingsis.parser/
├── build.gradle
└── src/
    ├── main/java/
    │   ├── Parser.java                         (interfaz base)
    │   ├── syntactic/
    │   │   ├── Parser.java                     (interfaz genérica)
    │   │   ├── SyntacticParser.java            (orquestador principal)
    │   │   ├── ParserFactory.java              (fábrica de parsers)
    │   │   ├── DeclarationParser.java          (declaraciones let/const)
    │   │   ├── LineExpressionParser.java       (expresiones en línea)
    │   │   ├── ConditionalParser.java          (if/else)
    │   │   ├── FunctionParser.java             (llamadas a funciones)
    │   │   ├── PrattParser.java                (precedencia de operadores)
    │   │   ├── IdentifierParser.java           (identificadores)
    │   │   ├── NumberLiteralParser.java         (literales numéricos)
    │   │   ├── StringLiteralParser.java         (literales de cadena)
    │   │   └── BooleanLiteralParser.java        (literales booleanos)
    │   └── tokenstream/
    │       ├── TokenStream.java                 (interfaz de flujo)
    │       ├── TokenStreamAdapter.java          (implementación inmutable)
    │       ├── rules/
    │       │   └── TokenMatchers.java           (predicados de matching)
    │       └── version/
    │           └── GrammarRules.java            (reglas por versión)
    └── test/java/syntactic/
        ├── ExpressionParserTest.java
        └── SyntacticParserTest.java
```

---

## Paquete `tokenstream` — Flujo de Tokens

### `TokenStream` — Interfaz

Extiende `SafeIterator<Token>` con métodos especializados para el consumo condicional de tokens.

| Método | Firma | Descripción |
|--------|-------|-------------|
| `consume()` | `Result<IterationStep<Token>> consume()` | Consume el siguiente token. |
| `consume(type)` | `Result<IterationStep<Token>> consume(TokenType expectedType)` | Consume solo si el tipo coincide. |
| `consume(matcher)` | `Result<IterationStep<Token>> consume(Predicate<Token> matcher)` | Consume solo si satisface el predicado. |
| `peek()` | `Result<Token> peek(int offset)` | Inspecciona un token en posición relativa sin avanzar. |
| `isEmpty()` | `boolean isEmpty()` | Indica si se ha alcanzado el final. |
| `pointer()` | `int pointer()` | Retorna el índice actual del puntero. |

### `TokenStreamAdapter` — Clase Final

Implementación **inmutable** de `TokenStream`. Contiene una lista inmutable de tokens (`List.copyOf`) y un puntero de posición. Cada operación de consumo produce una **nueva instancia** con `pointer + 1`.

### `TokenMatchers` — Clase de Utilidad

Métodos fábrica estáticos para construir predicados de matching:

| Método | Firma | Descripción |
|--------|-------|-------------|
| `isType()` | `static Predicate<Token> isType(TokenType type)` | Verifica si el token tiene el tipo dado. |
| `isOneOf()` | `static Predicate<Token> isOneOf(TokenType... types)` | Verifica si el tipo está en el conjunto. |
| `isTypeAndValue()` | `static Predicate<Token> isTypeAndValue(TokenType type, String value)` | Valida tipo y contenido textual. |

### `GrammarRules` — Record

Contenedor inmutable de predicados de tokens que define las reglas gramaticales por versión.

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `declarationKeywords` | `Predicate<Token>` | Keywords válidos para declaraciones. |
| `supportedDataTypes` | `Predicate<Token>` | Tipos de dato soportados. |
| `binaryOperators` | `Predicate<Token>` | Operadores binarios válidos. |

| Método | Firma | Descripción |
|--------|-------|-------------|
| `fromVersion()` | `static GrammarRules fromVersion(PrintScriptVersion version)` | Construye reglas según la versión (1.0 o 1.1). |

**Diferencias por versión:**

| Característica | V_1_0 | V_1_1 |
|----------------|-------|-------|
| Keywords de declaración | `let` | `let`, `const` |
| Tipos de dato | `number`, `string` | `number`, `string`, `boolean` |
| Operadores binarios | `+`, `-`, `*`, `/` | `+`, `-`, `*`, `/` |

---

## Paquete `syntactic` — Parsers Sintácticos

### `Parser<T extends Node>` — Interfaz Genérica

Contrato parametrizado para sub-analizadores sintácticos.

| Método | Firma | Descripción |
|--------|-------|-------------|
| `parse()` | `Result<IterationStep<T>> parse(TokenStream stream)` | Analiza el flujo y retorna un nodo del AST con el nuevo stream. |

### `SyntacticParser` — Clase Final (Orquestador)

Punto de entrada del analizador sintáctico. Ejecuta un bucle sobre el `TokenStream` usando una cadena de parsers para construir progresivamente un `ProgramNode`.

| Constructor | Descripción |
|-------------|-------------|
| `SyntacticParser(PrintScriptVersion version)` | Inicializa a través de `ParserFactory`. |
| `SyntacticParser(Parser<Node> chainParser)` | Inyección directa de la cadena de parsers. |

| Método | Firma | Descripción |
|--------|-------|-------------|
| `parse()` | `Result<IterationStep<ProgramNode>> parse(TokenStream stream)` | Recorre el flujo de tokens construyendo la lista de sentencias para el `ProgramNode`. |

### `ParserFactory` — Clase Final

Fábrica estática que ensambla los sub-parsers según la versión del lenguaje.

| Método | Firma | Descripción |
|--------|-------|-------------|
| `createParser()` | `static Parser<Node> createParser(PrintScriptVersion version)` | Compone la jerarquía de parsers según la versión. |

---

### Parsers de Sentencias

#### `DeclarationParser`

Analiza declaraciones de variables: `let x: number = 5;` o `const y: string;`

**Secuencia de consumo:**
1. Consume keyword de declaración (`let` / `const`)
2. Consume `IDENTIFIER` (nombre)
3. Consume `COLON`
4. Consume tipo de dato (`number`, `string`, `boolean`)
5. (Opcional) Consume `ASSIGN` + expresión
6. Consume `SEMICOLON`

#### `LineExpressionParser`

Reconoce una expresión válida en una sola línea seguida de `;`.

#### `ConditionalParser`

Reconoce estructuras `if (condicion) { ... } else { ... }`.

**Secuencia de consumo:**
1. Consume `if`
2. Consume `LPAREN`
3. Parsea expresión de condición
4. Consume `RPAREN`
5. Consume `LBRACE`
6. Parsea sentencias del bloque `then`
7. Consume `RBRACE`
8. (Opcional) Consume `else` + bloque

#### `FunctionParser`

Analiza invocaciones a funciones: `id(arg1, arg2)`.

> ⚠️ **Nota**: Presenta una inconsistencia en el código — el constructor se definió como `CallFunctionParser` en lugar de `FunctionParser`.

---

### Parsers de Expresiones

#### `PrattParser` — Algoritmo de Precedencia

Implementa **Pratt Parsing** (Top-Down Operator Precedence) para resolver la precedencia de operadores en expresiones binarias.

| Precedencia | Operadores |
|-------------|-----------|
| Baja | `+`, `-` |
| Alta | `*`, `/` |

**Funcionamiento:**
1. Parsea la expresión atómica izquierda (leaf parser).
2. Mientras el siguiente token sea un operador con `binding power ≥ minBP`:
   - Consume el operador.
   - Parsea recursivamente el lado derecho con `minBP` incrementado.
   - Construye un `BinaryExpressionNode`.
3. Retorna la expresión resultante.

#### Parsers Atómicos (Leaf Parsers)

| Parser | Token que consume | Nodo que produce |
|--------|-------------------|------------------|
| `NumberLiteralParser` | `NUMBER_LITERAL` | `NumberLiteralNode` (con `BigDecimal`) |
| `StringLiteralParser` | `STRING_LITERAL` | `StringLiteralNode` (removiendo comillas) |
| `BooleanLiteralParser` | `BOOLEAN_LITERAL` | `BooleanLiteralNode` |
| `IdentifierParser` | `IDENTIFIER` | `IdentifierNode` |

---

## Gramática Soportada

```bnf
Program         → Statement*
Statement       → Declaration | LineExpression | Conditional
Declaration     → DeclKeyword IDENTIFIER ':' Type ('=' Expression)? ';'
DeclKeyword     → 'let' | 'const'           (V_1_1)
Type            → 'number' | 'string' | 'boolean'  (V_1_1)
LineExpression  → Expression ';'
Conditional     → 'if' '(' Expression ')' '{' Statement* '}' ('else' '{' Statement* '}')?
Expression      → Term (('+' | '-') Term)*
Term            → Primary (('*' | '/') Primary)*
Primary         → NUMBER_LITERAL | STRING_LITERAL | BOOLEAN_LITERAL
                | IDENTIFIER | FunctionCall | '(' Expression ')'
FunctionCall    → IDENTIFIER '(' (Expression (',' Expression)*)? ')'
```

---

## Patrones de Diseño

| Patrón | Aplicación |
|--------|-----------|
| **Factory Method** | `ParserFactory.createParser()` y `GrammarRules.fromVersion()` centralizan instanciación por versión. |
| **Pratt Precedence Parsing** | `PrattParser` maneja operadores con distintas prioridades de vinculación. |
| **Adapter & Immutable Iterator** | `TokenStreamAdapter` adapta lista de tokens a `SafeIterator<Token>` inmutable. |
| **Chain of Responsibility / Recursive Descent** | Los sub-parsers intentan emparejar gramáticas y delegan recursivamente. |
| **Result Monad** | Uso intensivo de `Result<T>` para control de flujo determinista. |
| **Composite** | Los nodos del AST forman una jerarquía compuesta (`ProgramNode` → `Statement` → `Expression`). |
| **Immutable Record** | `GrammarRules` agrupa predicados de configuración como record inmutable. |

---

## Posición en el Pipeline

```
Código Fuente → CharStream → Lexer → [ PARSER ] → AST (ProgramNode)
                                       ▲
                                  Este módulo
```

El Parser es el **tercer y último procesador** en el pipeline de compilación:

1. **Entrada**: Recibe tokens del Lexer, envueltos en un `TokenStreamAdapter`.
2. **Transformación**: `SyntacticParser` evalúa la secuencia según la gramática de la versión configurada.
3. **Salida**: `Result<IterationStep<ProgramNode>>` con la raíz del AST.
4. **Consumo posterior**: El `ProgramNode` es entregado a módulos subsiguientes (intérprete, validador de tipos o formateador).

---

## Tests

### `ExpressionParserTest`

| Test | Descripción |
|------|-------------|
| Precedencia de operadores | Verifica que `5 + 3 * 2` agrupe correctamente (`*` como hijo de `+`). |
| Agrupamiento de nodos | Verifica estructura correcta del AST para expresiones complejas. |

### `SyntacticParserTest`

| Test | Descripción |
|------|-------------|
| Declaración simple | Verifica parsing de `let x: number = 42;`. |
| Programa completo | Verifica parsing de múltiples sentencias. |
