# PrintScript - Documentación Detallada de Arquitectura y Métodos

Este repositorio contiene la arquitectura completa para el analizador léxico (**Lexer**) del lenguaje **PrintScript**, diseñada con un enfoque modular en Gradle, estricto cumplimiento de principios **SOLID**, patrones de diseño GoF y restricciones de código limpio (métodos $\le 10$ líneas, firmas $\le 3$ parámetros, cero `var`/`yield`).

---

## 🏛️ 1. Estructura de Módulos

* **`:com.ingsis.common`**: Modelos de dominio (`Token`, `Position`, `MetaCharacter`), mónadas de resultado (`Result<T>`) y la cadena de patrones para la clasificación de tipos de token (`TokenTypeMatcher`).
* **`:com.ingsis.charstream`**: Adaptadores I/O para streaming de caracteres con pushback (`CharStream`, `StreamCharReader`, `PositionTracker`).
* **`:com.ingsis.lexer`**: Orquestador léxico (`Lexer`) e intérprete de estados de acumulación (`PrintScriptTokenizer`).

---

## 📐 2. Decisiones de Diseño y Patrones Aplicados

### A. Evaluador Perezoso / Streaming (`SafeIterator<T>`)
* **Decisión:** Para cumplir la restricción del enunciado de procesar archivos extensos que no entran en memoria, no se genera una lista global de tokens.
* **Mecanismo:** El `Lexer` implementa `SafeIterator<Token>`, emitiendo cada `Token` empaquetado en un `IterationStep(Token, nextLexer)` solo cuando el consumidor invoca el método `next()`.

### B. Algoritmo Maximal Munch con Lookahead Pushback
* **Decisión:** Selección del token válido más largo posible.
* **Mecanismo:** Al acumular caracteres, el `Lexer` lee hasta encontrar un caracter que rompe la regla sintáctica (`TokenizeResult.Invalid`). En ese momento, detiene el bucle y devuelve (`unread`) todos los caracteres leídos en lookahead que pertenecían al siguiente token.

### C. Chain of Responsibility (Cadena de Responsabilidad)
* **Decisión:** Clasificación desacoplada de `TokenType`.
* **Mecanismo:** Cada regla léxica se aísla en su propio matcher. La evaluación fluye ordenadamente:
  $$\text{LexemeMatcher} \longrightarrow \text{NumberMatcher} \longrightarrow \text{BooleanMatcher} \longrightarrow \text{StringMatcher} \longrightarrow \text{IdentifierMatcher}$$

### D. Mónada de Resultado (`Result<T>`) sin Null Object ni Excepciones
* **Decisión:** Eliminar `NullToken` y excepciones no controladas.
* **Mecanismo:** Uso de tipos algebraicos sellados `Result<T>` (`CorrectResult<T>` e `IncorrectResult<T>`).

---

## 🛠️ 3. Explicación Detallada de Métodos por Módulo

---

### 📦 Módulo `:com.ingsis.common`

#### Package `iterator`
* **`SafeIterator<T>` (Interfaz)**
  * `Result<IterationStep<T>> next()`: Devuelve el resultado del siguiente paso de iteración perezosa.
  * `default void unread(T item)`: Permite devolver elementos al flujo de entrada para operaciones de lookahead.
* **`IterationStep<T>` (Record)**
  * `T value()`: Retorna el elemento parseado en el paso actual.
  * `SafeIterator<T> next()`: Retorna el iterador avanzado hacia el siguiente estado.

#### Package `result`
* **`Result<T>` (Sealed Interface)**
  * `boolean isCorrect()`: Indica si el resultado representa un éxito o un fallo.
* **`CorrectResult<T>` (Record)**
  * `T value()`: Contiene el valor resultante de la operación exitosa.
* **`IncorrectResult<T>` (Record)**
  * `String error()`: Contiene el mensaje detallado de la razón del fallo.

#### Package `position`
* **`Position` (Record)**
  * Represeta la ubicación inmutable `(int line, int column)` de un caracter o token en el archivo fuente.

#### Package `metaChar`
* **`MetaCharacter` (Record)**
  * Encapsula un caracter leído `(Character character, Position position)`.
* **`MetaCharStringBuilder` (Clase)**
  * `void append(MetaCharacter mc)`: Acumula un caracter y registra la posición del primer caracter añadido.
  * `String buildString()`: Construye el `String` final acumulado.
  * `Position getStartPosition()`: Retorna la posición inicial exacta donde comenzó a formarse la cadena.

#### Package `token`
* **`TokenType` (Enum)**: Definición de todos los lexemas, keywords, literales y delimitadores soportados.
* **`TokenInterface` (Interfaz)**: Define la API para representar tokens (`type()`, `value()`, `startPosition()`, `endPosition()`, `line()`, `column()`).
* **`Token` (Record)**: Implementación inmutable de `TokenInterface`.

#### Package `token.tokenize`
* **`TokenizeResult` (Sealed Interface)**
  * `Complete(Token token)`: Representa la formación completa de un token válido.
  * `Prefix()`: Indica que el acumulador es un prefijo en construcción que puede ser válido al leer más caracteres.
  * `Invalid(String reason)`: Indica que el acumulador rompe la sintaxis y no puede ser un token válido.

#### Package `token.tokenizer`
* **`Tokenizer` (Interfaz)**
  * `TokenizeResult tokenize(MetaCharStringBuilder sb)`: Procesa un acumulador de caracteres y retorna su estado sintáctico.

#### Package `token.matcher`
* **`TokenTypeMatcher` (Interfaz)**
  * `Result<TokenType> match(String input)`: Clasifica un texto en un `TokenType`.
* **`AbstractTokenTypeMatcher` (Clase Abstracta)**
  * `AbstractTokenTypeMatcher linkWith(AbstractTokenTypeMatcher next)`: Enlaza el siguiente matcher en la cadena.
  * `Result<TokenType> match(String input)`: Método plantilla que ejecuta `doMatch` y delega a `passToNext` en caso de no coincidencia.
  * `Result<TokenType> passToNext(String input, String reason)`: Reenvía la consulta al siguiente enlace de la cadena.
  * `abstract Result<TokenType> doMatch(String input)`: Contrato a implementar por cada regla léxica concreta.
* **Matchers Concretos (`LexemeMatcher`, `NumberMatcher`, `BooleanMatcher`, `StringMatcher`, `IdentifierMatcher`)**
  * `Result<TokenType> doMatch(String input)`: Valida la cadena contra su diccionario estático o expresión regular correspondiente.
* **`ChainTokenTypeMatcher` (Clase Utilidad)**
  * `static TokenTypeMatcher defaultChain()`: Ensambla y retorna la cadena de responsabilidad por defecto.
* **`TokenMatcher` (Clase Facade)**
  * `static Result<TokenType> match(String input)`: Facade estático que delega la clasificación a la cadena por defecto.

---

### 📦 Módulo `:com.ingsis.charstream`

#### Package `charstream`
* **`CharReader` (Interfaz)**
  * `int readNextChar()`: Lee el código entero del siguiente caracter del stream.
  * `void unread(int c)`: Devuelve un código de caracter al buffer de lectura.
  * `void close()`: Libera los recursos del lector.
* **`StreamCharReader` (Clase)**
  * Implementa `CharReader` envolviendo el `Reader` de entrada en un `PushbackReader` de 1024 caracteres de buffer.
  * `int readNextChar()`: Invoca `reader.read()`.
  * `void unread(int c)`: Invoca `reader.unread(c)` para permitir backtracking.
* **`PositionTracker` (Clase Inmutable)**
  * `PositionTracker advance(char currentChar)`: Calcula y devuelve un nuevo `PositionTracker` incrementando línea en `\n`/`\r` o columna en caracteres normales.
  * `int getLine()`, `int getColumn()`: Acceso a las coordenadas actuales.
* **`CharStream` (Clase)**
  * Implementa `SafeIterator<MetaCharacter>`.
  * `Result<IterationStep<MetaCharacter>> next()`: Lee el siguiente caracter de `CharReader` y calcula su `Position` empaquetándolo en un `MetaCharacter`.
  * `void unread(MetaCharacter item)`: Delega a `reader.unread(item.character())` permitiendo devolver caracteres al stream sin acoplarnos a la implementación concreta.

---

### 📦 Módulo `:com.ingsis.lexer`

#### Package `lexer`
* **`PrintScriptTokenizer` (Clase)**
  * Implementa `Tokenizer`.
  * `TokenizeResult tokenize(MetaCharStringBuilder sb)`: Extrae el texto del acumulador e invoca `evaluateMatch`.
  * `TokenizeResult evaluateMatch(Result<TokenType> matchResult, String text, MetaCharStringBuilder sb)`: Traduce una coincidencia exitosa en un `TokenizeResult.Complete`.
  * `TokenizeResult evaluateFailure(String text, String error)`: Determina si una no-coincidencia es un prefijo en progreso o un error definitivo.
  * `boolean isPrefix(String text)`: Evalúa si la cadena corresponde a un prefijo de string o número decimal incompleto.
  * `boolean isUnfinishedString(String text)`: Verifica si un string rodeado por comillas está pendiente de cierre o de escape.

* **`Lexer` (Clase Principal)**
  * Implementa `SafeIterator<Token>`.
  * `Result<IterationStep<Token>> next()`: Punto de entrada público que invoca `maximalMunchOf`.
  * `Result<IterationStep<Token>> maximalMunchOf(SafeIterator<MetaCharacter> stream)`: Salta espacios en blanco iniciales de forma segura y lanza el bucle de acumulación.
  * `Result<IterationStep<Token>> runMaximalMunchLoop(SafeIterator<MetaCharacter> curr, Result<IterationStep<MetaCharacter>> initialResult)`: Ejecuta el bucle de Maximal Munch, devuelve el buffer de lookahead no consumido y emite el resultado.
  * `MunchState executeMunch(Result<IterationStep<MetaCharacter>> initialResult)`: Bucle que acumula caracteres uno a uno mientras la tokenización no sea `Invalid`.
  * `boolean processTokenizeStep(TokenizeResult tr, IterationStep<MetaCharacter> step, MunchState state)`: Actualiza la variable `lastValidToken` cuando un token es `Complete` o añade el caracter al buffer de lookahead.
  * `void unreadLookaheadBuffer(SafeIterator<MetaCharacter> stream, List<MetaCharacter> buffer)`: Recorre el buffer de lookahead en orden inverso invocando `stream.unread(...)`.
  * `Result<IterationStep<Token>> buildResult(Token token, SafeIterator<MetaCharacter> nextStream)`: Construye la tupla de resultado final `Result.success(IterationStep(token, nextLexer))`.

---

## 🧪 4. Ejecución de Pruebas Unitarias

Para compilar y verificar el comportamiento del proyecto:

```bash
./gradlew test
```

Salida esperada:
```text
> Task :com.ingsis.common:test PASSED
> Task :com.ingsis.charstream:test PASSED
> Task :com.ingsis.lexer:test PASSED

BUILD SUCCESSFUL in 1s
```