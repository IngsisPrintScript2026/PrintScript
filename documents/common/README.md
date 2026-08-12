# 📦 Módulo `com.ingsis.common`

## Descripción General

El módulo `com.ingsis.common` es el **módulo fundacional** de PrintScript. Define los modelos de dominio, abstracciones y utilidades compartidas que son utilizados por todos los demás módulos (`charstream`, `lexer`, `parser`).

Actúa como la **capa de tipos y contratos** del sistema, proporcionando:

- Los tipos de dato centrales (`Token`, `Position`, `MetaCharacter`)
- La mónada de resultado (`Result<T>`) para manejo de errores sin excepciones
- El iterador perezoso (`SafeIterator<T>`) para streaming eficiente
- La cadena de responsabilidad para clasificación de tipos de token (`TokenTypeMatcher`)
- La jerarquía completa del **Árbol de Sintaxis Abstracta (AST)**
- El versionado del lenguaje (`PrintScriptVersion`)
- La fábrica de nodos del AST (`NodeFactory`)

---

## Dependencias

> **Este módulo no depende de ningún otro módulo del proyecto.** Es la base de todo el sistema.

Es dependencia directa de:
- `com.ingsis.charstream`
- `com.ingsis.lexer`
- `com.ingsis.parser`

---

## Estructura de Paquetes

```
com.ingsis.common/src/main/java/
├── iterator/
│   ├── SafeIterator.java
│   └── IterationStep.java
├── result/
│   ├── Result.java
│   ├── CorrectResult.java
│   └── IncorrectResult.java
├── position/
│   └── Position.java
├── metaChar/
│   ├── MetaCharacter.java
│   ├── MetaCharacterStringBuilder.java     (sealed interface)
│   └── MetaCharStringBuilder.java          (implementación)
├── state/
│   └── State.java
├── token/
│   ├── TokenType.java
│   ├── TokenInterface.java
│   ├── Token.java
│   ├── tokenize/
│   │   └── TokenizeResult.java
│   ├── tokenizer/
│   │   └── Tokenizer.java
│   └── matcher/
│       ├── TokenTypeMatcher.java
│       ├── AbstractTokenTypeMatcher.java
│       ├── LexemeMatcher.java
│       ├── NumberMatcher.java
│       ├── BooleanMatcher.java
│       ├── StringMatcher.java
│       ├── IdentifierMatcher.java
│       └── chain/
│           ├── ChainTokenTypeMatcher.java
│           └── TokenMatcher.java
├── node/
│   ├── Node.java                           (interfaz base AST)
│   ├── ProgramNode.java                    (nodo raíz)
│   ├── expression/
│   │   ├── ExpressionNode.java
│   │   ├── LiteralNode.java
│   │   ├── Identifier/
│   │   │   └── IdentifierNode.java
│   │   ├── literal/
│   │   │   ├── NumberLiteralNode.java
│   │   │   ├── StringLiteralNode.java
│   │   │   └── BooleanLiteralNode.java
│   │   ├── function/
│   │   │   └── CallFunctionNode.java
│   │   ├── nullObject/
│   │   │   └── NilExpressionNode.java
│   │   └── operator/
│   │       ├── OperatorNode.java
│   │       └── OperatorType.java
│   ├── factory/
│   │   └── NodeFactory.java
│   └── keyword/
│       ├── AssignNode.java
│       ├── DeclarationKeywordNode.java
│       ├── IfKeywordNode.java
│       └── declaration/
│           └── DeclarationType.java
└── version/
    └── printscript/
        └── PrintScriptVersion.java
```

---

## Detalle de Paquetes y Clases

---

### 📁 Package `iterator`

#### `SafeIterator<T>` — Interfaz

Iterador perezoso y seguro que emite elementos envueltos en `Result`. Diseñado para procesamiento en streaming de archivos extensos sin cargar todo en memoria.

| Método | Firma | Descripción |
|--------|-------|-------------|
| `next()` | `Result<IterationStep<T>> next()` | Devuelve el siguiente paso de iteración envuelto en un `Result`. |
| `unread()` | `default void unread(T item)` | Método default (no-op). Permite pushback/lookahead. Las implementaciones concretas lo sobreescriben. |

#### `IterationStep<T>` — Record

Encapsula un paso de iteración: el valor actual y el iterador para el siguiente estado.

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `value` | `T` | El elemento parseado en el paso actual. |
| `next` | `SafeIterator<?>` | El iterador avanzado hacia el siguiente estado. |

| Método | Firma | Descripción |
|--------|-------|-------------|
| `nextStream()` | `<S extends SafeIterator<?>> S nextStream()` | Método genérico para castear el siguiente iterador a su tipo concreto. |

---

### 📁 Package `result`

Implementa el patrón **Mónada de Resultado** como tipo algebraico sellado.

#### `Result<T>` — Sealed Interface

| Método | Firma | Descripción |
|--------|-------|-------------|
| `isCorrect()` | `boolean isCorrect()` | Indica si el resultado es un éxito (`true`) o un fallo (`false`). |
| `success()` | `static <T> Result<T> success(T value)` | Método fábrica para crear un `CorrectResult`. |
| `failure()` | `static <T> Result<T> failure(String error)` | Método fábrica para crear un `IncorrectResult`. |

#### `CorrectResult<T>` — Record

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `value` | `T` | El valor resultante de la operación exitosa. |

#### `IncorrectResult<T>` — Record

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `error` | `String` | Mensaje detallado de la razón del fallo. |

---

### 📁 Package `position`

#### `Position` — Record

Representa la ubicación inmutable de un carácter o token en el archivo fuente.

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `line` | `Integer` | Número de línea (1-indexed). |
| `column` | `Integer` | Número de columna (1-indexed). |

| Método | Descripción |
|--------|-------------|
| `toString()` | Devuelve formato visual `"[line:column]"`. |

---

### 📁 Package `metaChar`

#### `MetaCharacter` — Record

Encapsula un carácter leído junto con su posición en el archivo fuente.

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `character` | `Character` | El carácter leído. |
| `position` | `Position` | La posición del carácter. |

#### `MetaCharacterStringBuilder` — Sealed Interface

Interfaz sellada para construcciones de cadenas a partir de `MetaCharacter`.

| Método | Firma | Descripción |
|--------|-------|-------------|
| `append()` | `MetaCharacterStringBuilder append(MetaCharacter mc)` | Agrega un metacarácter al buffer. |
| `buildString()` | `String buildString()` | Retorna la cadena acumulada. |
| `isEmpty()` | `boolean isEmpty()` | Indica si el buffer está vacío. |

#### `MetaCharStringBuilder` — Final Class

Implementación concreta usando `StringBuilder` interno. Registra la posición del primer carácter insertado.

| Método | Firma | Descripción |
|--------|-------|-------------|
| `append()` | `MetaCharacterStringBuilder append(MetaCharacter mc)` | Acumula el carácter y guarda posición del primero. |
| `buildString()` | `String buildString()` | Retorna el texto acumulado. |
| `getStartPosition()` | `Position getStartPosition()` | Posición del primer metacarácter (o `Position(-1,-1)` si vacío). |
| `isEmpty()` | `boolean isEmpty()` | Evalúa si no hay caracteres acumulados. |

---

### 📁 Package `state`

#### `State` — Enum

Estados de emparejamiento incremental en tokenizadores.

| Valor | Descripción |
|-------|-------------|
| `INVALID` | La secuencia acumulada no forma un token válido. |
| `PREFIX` | La secuencia es un prefijo válido que necesita más caracteres. |
| `COMPLETE` | La secuencia forma un token completo. |

---

### 📁 Package `version.printscript`

#### `PrintScriptVersion` — Enum

Versiones soportadas de la sintaxis del lenguaje PrintScript.

| Valor | Descripción |
|-------|-------------|
| `V_1_0` | Soporte básico: `let`, tipos `number`/`string`, operadores `+`, `-`, `*`, `/`. |
| `V_1_1` | Soporte extendido: `const`, `if/else`, tipo `boolean`, literales booleanos. |

---

### 📁 Package `token`

#### `TokenType` — Enum

Catálogo completo de tipos de tokens en PrintScript:

| Categoría | Valores |
|-----------|---------|
| **Especiales** | `NONE`, `NULL` |
| **Keywords** | `LET`, `CONST`, `IF`, `ELSE`, `PRINTLN` |
| **Tipos de dato** | `NUMBER`, `STRING`, `BOOLEAN` |
| **Literales** | `NUMBER_LITERAL`, `STRING_LITERAL`, `BOOLEAN_LITERAL` |
| **Identificadores** | `IDENTIFIER` |
| **Operadores** | `PLUS`, `MINUS`, `STAR`, `SLASH`, `EQUAL` |
| **Delimitadores** | `LPAREN`, `RPAREN`, `LBRACE`, `RBRACE`, `COLON`, `SEMICOLON`, `COMMA` |

#### `TokenInterface` — Interfaz

| Método | Firma | Descripción |
|--------|-------|-------------|
| `type()` | `TokenType type()` | Tipo del token. |
| `value()` | `String value()` | Valor textual. |
| `startPosition()` | `Position startPosition()` | Posición de inicio. |
| `endPosition()` | `Position endPosition()` | Posición de fin. |
| `line()` | `default Integer line()` | Línea de inicio (o `-1` si nulo). |
| `column()` | `default Integer column()` | Columna de inicio (o `-1` si nulo). |
| `isNull()` | `default boolean isNull()` | Si es token nulo (default `false`). |

#### `Token` — Record

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `type` | `TokenType` | Tipo del token. |
| `value` | `String` | Valor textual. |
| `startPosition` | `Position` | Posición de inicio. |
| `endPosition` | `Position` | Posición de fin (puede ser nula). |

Constructores: completo (4 campos) y sobrecargado (3 campos, `endPosition` nula).

---

### 📁 Package `token.tokenize`

#### `TokenizeResult` — Sealed Interface

| Variante | Campos | Descripción |
|----------|--------|-------------|
| `Complete` | `Token token` | Token completado exitosamente. |
| `Prefix` | — | Prefijo válido pero incompleto. |
| `Invalid` | `String reason` | Secuencia inválida. |

---

### 📁 Package `token.tokenizer`

#### `Tokenizer` — Interfaz

| Método | Firma | Descripción |
|--------|-------|-------------|
| `tokenize()` | `TokenizeResult tokenize(MetaCharStringBuilder sb)` | Procesa un buffer y retorna `TokenizeResult`. |

---

### 📁 Package `token.matcher`

Implementa el patrón **Chain of Responsibility** para clasificación de `TokenType`.

#### Flujo de la Cadena

```
LexemeMatcher → NumberMatcher → BooleanMatcher → StringMatcher → IdentifierMatcher
```

#### `TokenTypeMatcher` — Interfaz

| Método | Firma | Descripción |
|--------|-------|-------------|
| `match()` | `Result<TokenType> match(String input)` | Clasifica un texto en un `TokenType`. |

#### `AbstractTokenTypeMatcher` — Clase Abstracta

| Método | Firma | Descripción |
|--------|-------|-------------|
| `linkWith()` | `AbstractTokenTypeMatcher linkWith(AbstractTokenTypeMatcher next)` | Enlaza el siguiente matcher. |
| `match()` | `Result<TokenType> match(String input)` | Método plantilla: ejecuta `doMatch`, delega si falla. |
| `doMatch()` | `abstract Result<TokenType> doMatch(String input)` | Punto de extensión para reglas concretas. |

#### Matchers Concretos

| Matcher | Criterio |
|---------|----------|
| `LexemeMatcher` | Mapa hash estático de keywords, operadores y delimitadores (`let`, `const`, `if`, `else`, `println`, `+`, `-`, `*`, `/`, `=`, `;`, `:`, `(`, `)`, `{`, `}`, `,`, `number`, `string`, `boolean`). |
| `NumberMatcher` | Regex `^\d+(\.\d+)?$`. |
| `BooleanMatcher` | Valores `"true"` y `"false"` (case insensitive). |
| `StringMatcher` | Regex para strings entre comillas con soporte de secuencias de escape: `^"(?:\\\\.|[^\\\\"])*"\|'(?:\\\\.\|[^\\\\'])*'$`. |
| `IdentifierMatcher` | Regex `^[a-zA-Z_][a-zA-Z0-9_]*$`. |

#### `ChainTokenTypeMatcher` — Clase Utilidad

| Método | Firma | Descripción |
|--------|-------|-------------|
| `defaultChain()` | `static TokenTypeMatcher defaultChain()` | Ensambla la cadena por defecto. |

#### `TokenMatcher` — Clase Facade

| Método | Firma | Descripción |
|--------|-------|-------------|
| `match()` | `static Result<TokenType> match(String input)` | Facade estático que delega a la cadena por defecto. |

---

### 📁 Package `node` — Árbol de Sintaxis Abstracta (AST)

#### `Node` — Interfaz (Base del AST)

Interfaz Composite para todos los nodos del AST.

| Método | Firma | Descripción |
|--------|-------|-------------|
| `line()` | `Integer line()` | Línea en el código fuente. |
| `column()` | `Integer column()` | Columna en el código fuente. |
| `symbol()` | `String symbol()` | Representación simbólica del nodo. |
| `children()` | `List<? extends Node> children()` | Hijos sintácticos. |

#### `ProgramNode` — Record (Nodo Raíz)

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `statements` | `List<Node>` | Lista inmutable de sentencias del programa. |

| Método | Retorna | Descripción |
|--------|---------|-------------|
| `symbol()` | `"PROGRAM"` | — |
| `children()` | `statements` | — |

---

### 📁 Package `node.expression`

#### `ExpressionNode` — Interfaz

Especialización de `Node` para expresiones que producen un valor.

#### `LiteralNode<T>` — Interfaz Genérica

| Método | Firma | Descripción |
|--------|-------|-------------|
| `value()` | `Result<T> value()` | Retorna el valor encapsulado en un `Result`. |

#### `IdentifierNode` — Record

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `name` | `String` | Nombre del identificador. |

#### Nodos Literales

| Nodo | Tipo de Valor | Descripción |
|------|---------------|-------------|
| `NumberLiteralNode` | `BigDecimal` | Literal numérico con precisión arbitraria. |
| `StringLiteralNode` | `String` | Literal de cadena de texto. |
| `BooleanLiteralNode` | `Boolean` | Literal booleano (`true`/`false`). |

#### `CallFunctionNode` — Record

Invocación a función (ej: `println(...)`).

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `identifierNode` | `IdentifierNode` | Identificador de la función. |
| `argumentNodes` | `List<ExpressionNode>` | Lista inmutable de argumentos. |

#### `NilExpressionNode` — Record

Patrón **Null Object** para ausencia de expresión en declaraciones sin inicialización (`let x: number;`).

| Método | Retorna | Descripción |
|--------|---------|-------------|
| `symbol()` | `"NIL"` | — |
| `line()` | `-1` | — |
| `column()` | `-1` | — |

---

### 📁 Package `node.expression.operator`

#### `OperatorType` — Enum

Operadores binarios con precedencias de ligadura (Pratt Parsing).

| Valor | Símbolo | lBindingPower | rBindingPower |
|-------|---------|--------------|---------------|
| `ASSIGNATION` | `=` | 2 | 1 |
| `PLUS` | `+` | 12 | 11 |
| `MINUS` | `-` | 12 | 11 |
| `STAR` | `*` | 22 | 21 |
| `SLASH` | `/` | 22 | 21 |

| Método | Firma | Descripción |
|--------|-------|-------------|
| `isOperator()` | `static boolean isOperator(String symbol)` | Si el string es un operador válido. |
| `fromSymbol()` | `static Result<OperatorType> fromSymbol(String symbol)` | Obtiene `OperatorType` desde símbolo. |

#### `OperatorNode` — Record

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `operatorType` | `OperatorType` | Tipo de operador. |
| `left` | `ExpressionNode` | Expresión izquierda. |
| `right` | `ExpressionNode` | Expresión derecha. |

---

### 📁 Package `node.keyword`

#### `DeclarationType` — Enum

| Valor | Keyword | `isMutable()` |
|-------|---------|---------------|
| `LET` | `"let"` | `true` |
| `CONST` | `"const"` | `false` |

#### `DeclarationKeywordNode` — Record

Declaración de variable (`let x: number = 10;` o `const y: string;`).

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `declarationType` | `DeclarationType` | Tipo de declaración (`LET`/`CONST`). |
| `identifierNode` | `IdentifierNode` | Identificador. |
| `expressionNode` | `ExpressionNode` | Expresión asignada o `NilExpressionNode`. |

#### `AssignNode` — Record

Sentencia de asignación (`x = 5`).

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `identifierNode` | `IdentifierNode` | Identificador destino. |
| `expressionNode` | `ExpressionNode` | Expresión asignada. |

#### `IfKeywordNode` — Record

Sentencia condicional (`if (cond) { ... } else { ... }`).

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `condition` | `ExpressionNode` | Condición del bloque. |
| `thenBody` | `List<Node>` | Sentencias del bloque *then*. |
| `elseBody` | `List<Node>` | Sentencias del bloque *else*. |

---

### 📁 Package `node.factory`

#### `NodeFactory` — Final Class

Fábrica estática para instanciar nodos del AST con posiciones extraídas de tokens.

| Método | Firma |
|--------|-------|
| `createIdentifier` | `static IdentifierNode createIdentifier(Token token)` |
| `createDeclaration` | `static DeclarationKeywordNode createDeclaration(DeclarationType, IdentifierNode, ExpressionNode, Token)` |
| `createAssign` | `static AssignNode createAssign(IdentifierNode, ExpressionNode, Token)` |
| `createCall` | `static CallFunctionNode createCall(String name, List<ExpressionNode> args, Token)` |
| `createIf` | `static IfKeywordNode createIf(ExpressionNode cond, List<Node> then, List<Node> else, Token)` |
| `createOperator` | `static OperatorNode createOperator(OperatorType, ExpressionNode left, ExpressionNode right, Token)` |
| `createProgram` | `static ProgramNode createProgram(List<Node> statements)` |

---

## Patrones de Diseño

| Patrón | Aplicación |
|--------|-----------|
| **Result Monad / Either** | `Result<T>` sellado fuerza manejo explícito de éxito/fallo. |
| **Chain of Responsibility** | Clasificación de `TokenType` mediante cadena de matchers. |
| **Composite** | Jerarquía del AST (`Node` → `ExpressionNode` → literales/operadores). |
| **Factory Method** | `NodeFactory`, `ChainTokenTypeMatcher.defaultChain()`, `Result.success()`/`failure()`. |
| **Template Method** | `AbstractTokenTypeMatcher.match()` define algoritmo; `doMatch()` extensible. |
| **Facade** | `TokenMatcher` oculta la cadena de responsabilidad. |
| **Null Object** | `NilExpressionNode` para declaraciones sin inicialización. |
| **Immutable Value Objects** | Records extensivos (`Token`, `Position`, `MetaCharacter`, nodos AST). |
| **Pratt Binding Powers** | `OperatorType` con `lBindingPower`/`rBindingPower` para el parser de precedencia. |

---

## Tests

| Test | Descripción |
|------|-------------|
| `TokenMatcherTest` | Valida la cadena de responsabilidad: keywords (`let`, `const`, `println`), operadores (`+`, `:`, `;`), literales numéricos (`123`, `3.14`), strings (`"hello"`, `'world'`), booleanos (`true`, `false`), identificadores (`myVariable`), y cadenas inválidas. |
