# 📦 Módulo `com.ingsis.lexer`

## Descripción General

El módulo `com.ingsis.lexer` es el **orquestador léxico principal** de PrintScript. Su responsabilidad es consumir un flujo de `MetaCharacter` (producido por el módulo `charstream`) y transformarlo en un flujo de `Token` mediante el **algoritmo Maximal Munch**.

Este módulo implementa la lógica de decisión de cuándo un acumulado de caracteres forma un token válido, cuándo es un prefijo que necesita más caracteres, y cuándo es inválido.

---

## Dependencias

| Módulo | Relación |
|--------|----------|
| `com.ingsis.common` | **Depende de** — Usa `SafeIterator`, `IterationStep`, `Result`, `MetaCharacter`, `MetaCharStringBuilder`, `Token`, `TokenType`, `TokenizeResult`, `Tokenizer`, `TokenMatcher`, `Position`. |
| `com.ingsis.charstream` | **Depende de** — Consume el `SafeIterator<MetaCharacter>` producido por `CharStream`. Usa `StreamCharReader` en tests. |

---

## Responsabilidades Clave

1. **Filtrado de espacios en blanco**: Omite caracteres whitespace antes de procesar un nuevo token.
2. **Algoritmo Maximal Munch**: Acumula la mayor cantidad de caracteres posible que formen un token válido.
3. **Evaluación funcional**: Usa `Result<T>` en lugar de excepciones para control de flujo.
4. **Gestión de Lookahead**: Guarda caracteres leídos en exceso y los reinserta en el stream.
5. **Integración con evaluadores de tokens**: Delega el reconocimiento de tipos a `Tokenizer` y `TokenTypeMatcher`.

---

## Estructura de Archivos

```
com.ingsis.lexer/
├── build.gradle
├── README.md
└── src/
    ├── main/java/lexer/
    │   ├── PrintScriptTokenizer.java
    │   ├── Lexer.java
    │   └── MunchState.java
    └── test/java/lexer/
        ├── LexerTest.java
        └── PrintScriptTokenizerTest.java
```

---

## Detalle de Clases

### `PrintScriptTokenizer` — Clase

Implementa la interfaz `Tokenizer`. Se encarga de evaluar si un acumulado de caracteres constituye un token válido, un prefijo en progreso, o una secuencia inválida.

**Atributos:**
- `TokenTypeMatcher tokenMatcher` — Cadena de evaluadores para determinar el tipo de token.

**Constructores:**

| Constructor | Descripción |
|-------------|-------------|
| `PrintScriptTokenizer(TokenTypeMatcher tokenMatcher)` | Inicializa con un matcher específico. |
| `PrintScriptTokenizer()` | Usa `ChainTokenTypeMatcher.defaultChain()` por defecto. |

**Métodos públicos:**

| Método | Firma | Descripción |
|--------|-------|-------------|
| `tokenize()` | `TokenizeResult tokenize(MetaCharStringBuilder sb)` | Extrae el texto del builder, consulta al matcher, y evalúa el resultado. |

**Métodos privados:**

| Método | Descripción |
|--------|-------------|
| `evaluateMatch(Result<TokenType>, String, MetaCharStringBuilder)` | Si match exitoso → `TokenizeResult.Complete` con nuevo `Token`. Si falla → delega a `evaluateFailure`. |
| `evaluateFailure(String, String)` | Si el texto es un prefijo válido → `TokenizeResult.Prefix`. Si no → `TokenizeResult.Invalid`. |
| `isPrefix(String)` | Evalúa si la cadena es un string no cerrado o un decimal incompleto (termina en `.`). |
| `isUnfinishedString(String)` | Verifica si un string con comillas aún no ha sido cerrado. |

---

### `MunchState` — Clase Interna / Record

Encapsula el estado mutable del bucle Maximal Munch durante la acumulación de caracteres.

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `lastValidToken` | `Token` | Último token completo y válido encontrado (puede ser `null` inicialmente). |
| `nextStreamAfterToken` | `SafeIterator<MetaCharacter>` | Referencia al sub-stream posterior al último token válido. |
| `lookaheadBuffer` | `List<MetaCharacter>` | Caracteres leídos que no pertenecen al token actual y deben ser devueltos al stream. |

| Método | Descripción |
|--------|-------------|
| `updateLastValid(Token)` | Actualiza `lastValidToken` y limpia el `lookaheadBuffer`. |
| `addToLookahead(MetaCharacter)` | Añade un metacarácter al buffer de lookahead. |
| `stopLoop()` | Marca el flag para detener el bucle de acumulación. |

---

### `Lexer` — Clase Principal

Implementa `SafeIterator<Token>`. Es el punto de entrada del módulo y el orquestador del algoritmo Maximal Munch. Emite tokens perezosamente.

**Constructores:**

| Constructor | Descripción |
|-------------|-------------|
| `Lexer(SafeIterator<MetaCharacter> stream, Tokenizer tokenizer)` | Inicializa con el stream y un tokenizer específico. |
| `Lexer(SafeIterator<MetaCharacter> stream)` | Sobrecarga que usa `PrintScriptTokenizer` por defecto. |

**Métodos públicos:**

| Método | Firma | Descripción |
|--------|-------|-------------|
| `next()` | `Result<IterationStep<Token>> next()` | Punto de entrada público. Avanza el análisis léxico para obtener el siguiente token. |

**Métodos privados:**

| Método | Descripción |
|--------|-------------|
| `maximalMunchOf(SafeIterator<MetaCharacter>)` | Salta whitespace y lanza el bucle de acumulación. |
| `skipWhitespace(SafeIterator<MetaCharacter>, Result<...>)` | Bucle que salta ` `, `\t`, `\n`, `\r`. |
| `runMaximalMunchLoop(SafeIterator<MetaCharacter>, Result<...>)` | Ejecuta el bucle Maximal Munch, devuelve lookahead, construye resultado. |
| `executeMunch(Result<...>)` | Bucle principal: acumula caracteres y evalúa tokenización. |
| `processTokenizeStep(TokenizeResult, IterationStep<MetaCharacter>, MunchState)` | Procesa resultado de tokenización: `Complete` → actualiza token, `Prefix` → buffer, `Invalid` → stop. |
| `unreadLookaheadBuffer(SafeIterator<MetaCharacter>, List<MetaCharacter>)` | Devuelve caracteres del buffer en **orden inverso** al stream. |
| `buildResult(Token, SafeIterator<MetaCharacter>)` | Construye `CorrectResult<IterationStep<Token>>` con el token y un **nuevo `Lexer`**. |

---

## Algoritmo Maximal Munch — Explicación Detallada

El algoritmo garantiza que siempre se selecciona el **token válido más largo posible**:

```
┌─────────────────────────────────────────────────────┐
│                  Lexer.next()                       │
├─────────────────────────────────────────────────────┤
│                                                     │
│  1. SALTAR WHITESPACE                               │
│     └── Consume ` `, `\t`, `\n`, `\r` del stream   │
│                                                     │
│  2. BUCLE MAXIMAL MUNCH (executeMunch)              │
│     ├── Leer siguiente MetaCharacter                │
│     ├── Acumular en MetaCharStringBuilder            │
│     ├── tokenizer.tokenize(sb) → TokenizeResult     │
│     │   ├── Complete → Guardar como lastValidToken  │
│     │   │              Limpiar lookaheadBuffer       │
│     │   │              Guardar nextStream            │
│     │   │              CONTINUAR (buscar más largo)  │
│     │   ├── Prefix  → Añadir a lookaheadBuffer      │
│     │   │              CONTINUAR                     │
│     │   └── Invalid → Añadir a lookaheadBuffer      │
│     │                  DETENER bucle                 │
│     └── Repetir mientras haya caracteres             │
│                                                     │
│  3. RESTAURAR STREAM (unreadLookaheadBuffer)        │
│     └── Devolver lookaheadBuffer en ORDEN INVERSO   │
│                                                     │
│  4. EMITIR RESULTADO                                │
│     ├── lastValidToken != null                      │
│     │   └── CorrectResult(IterationStep(token,      │
│     │                     new Lexer(nextStream)))    │
│     └── lastValidToken == null                      │
│         └── IncorrectResult("No token found")       │
└─────────────────────────────────────────────────────┘
```

### Ejemplo paso a paso

Para la entrada `let`:

| Paso | Acumulador | TokenizeResult | lastValidToken | lookaheadBuffer |
|------|-----------|----------------|----------------|-----------------|
| 1 | `l` | Prefix | `null` | `[l]` |
| 2 | `le` | Prefix | `null` | `[l, e]` |
| 3 | `let` | Complete(LET) | `Token(LET, "let")` | `[]` |
| 4 | `let ` | Invalid | `Token(LET, "let")` | `[' ']` |

→ Se devuelve `' '` al stream y se emite `Token(LET, "let")`.

---

## Patrones de Diseño

| Patrón | Aplicación |
|--------|-----------|
| **Maximal Munch** | Algoritmo central que selecciona el token más largo posible. |
| **Lazy Iterator** | `Lexer` implementa `SafeIterator<Token>`, emitiendo tokens solo al invocar `next()`. |
| **Strategy** | `Tokenizer` es inyectado en `Lexer`, permitiendo cambiar la lógica de tokenización sin modificar el Lexer. |
| **Chain of Responsibility** | `PrintScriptTokenizer` delega a `TokenTypeMatcher` (cadena de matchers del módulo `common`). |
| **State Object** | `MunchState` encapsula el estado mutable temporal del bucle. |

---

## Posición en el Pipeline

```
Reader → StreamCharReader → CharStream → [ LEXER ] → Parser
                                           ▲
                                    Este módulo
```

El `Lexer` es el **segundo procesador** en el pipeline. Recibe `MetaCharacter`s del `CharStream` y produce `Token`s que serán consumidos por el `Parser`.

---

## Tests

### `LexerTest`

Tests de integración que construyen el pipeline completo (`StringReader → StreamCharReader → CharStream → Lexer`):

| Test | Descripción |
|------|-------------|
| Tokenización de programa completo | Procesa declaraciones `let`, tipos `number`, operadores `/`, instrucciones `println` y verifica 30 tokens con tipos y valores correctos. |
| Posiciones correctas | Verifica que cada token tenga la línea y columna correctas. |
| Manejo de EOF | Verifica `IncorrectResult` cuando no quedan tokens. |

### `PrintScriptTokenizerTest`

| Test | Descripción |
|------|-------------|
| Keywords | Reconoce `let`, `println`. |
| Operadores/delimitadores | Reconoce `+`, `-`, `*`, `/`, `=`, `;`, `:`, `(`, `)`. |
| Literales numéricos | Reconoce enteros y decimales. |
| Prefijos | Detecta strings no cerrados, decimales incompletos. |
| Entradas inválidas | Detecta secuencias no válidas. |
