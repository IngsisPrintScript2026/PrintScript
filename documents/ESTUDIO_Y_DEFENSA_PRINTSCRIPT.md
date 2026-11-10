# PrintScript — Guía de estudio y defensa (de punta a punta)

> Documento para estudiar y defender el proyecto. Está escrito a partir del **código real** del repo
> (no de los READMEs, que en algunos puntos están desactualizados; ver §20).
> Estado verificado al 10/11/2026: `./gradlew test` → **703 tests, 0 fallas** (los `containerTest`
> con Docker no entran en esa cuenta).

---

## Índice

1. [Qué es PrintScript y qué pide la consigna](#1-qué-es-printscript-y-qué-pide-la-consigna)
2. [Mapa del repositorio (árbol de directorios)](#2-mapa-del-repositorio)
3. [Arquitectura general: pipeline, componentes y dependencias](#3-arquitectura-general)
4. [Módulo por módulo](#4-módulo-por-módulo)
5. [El AST: nodos, árboles de ejemplo y qué hace cada herramienta con cada nodo](#5-el-ast)
6. [Versiones 1.0 vs 1.1: dónde se decide cada cosa](#6-versiones-10-vs-11)
7. [Manejo de errores y posiciones](#7-manejo-de-errores-y-posiciones)
8. [Memoria y streaming (el requisito más importante)](#8-memoria-y-streaming)
9. [CLI, comandos y scripts](#9-cli-comandos-y-scripts)
10. [Gradle a fondo](#10-gradle-a-fondo)
11. [Calidad de código: Spotless, Checkstyle, PMD, SpotBugs, JaCoCo](#11-calidad-de-código)
12. [Reportes](#12-reportes)
13. [Tests](#13-tests)
14. [CI/CD con GitHub Actions y publicación de paquetes](#14-cicd)
15. [El repo `workflows-and-actions`](#15-el-repo-workflows-and-actions)
16. [Por qué los pasos viven en las Actions y no en una gran task de Gradle](#16-por-qué-steps-en-actions-y-no-una-task-de-gradle)
17. [El `printscript--tck`](#17-el-printscript--tck)
18. [Patrones de diseño y SOLID](#18-patrones-de-diseño-y-solid)
19. [Decisiones de diseño (estilo ADR)](#19-decisiones-de-diseño)
20. [Limitaciones conocidas e inconsistencias (lo que te pueden marcar)](#20-limitaciones-conocidas-e-inconsistencias)
21. [Preguntas probables de defensa, con respuesta](#21-preguntas-probables-de-defensa)
22. [Glosario](#22-glosario)
23. [Chuleta de comandos](#23-chuleta-de-comandos)

---

## 1. Qué es PrintScript y qué pide la consigna

PrintScript es un lenguaje chico, subconjunto de TypeScript, orientado a imprimir texto y números.
El TP pide **tres herramientas** más una **CLI**:

| Herramienta | Qué hace | Módulo |
|---|---|---|
| **Interpreter** | Lee, analiza y ejecuta un programa | `com.ingsis.interpreter` |
| **Formatter** | Reescribe el código con un estilo configurable (YAML/JSON) | `com.ingsis.formatter` |
| **Static Code Analyzer (SCA / linter)** | Reporta violaciones de reglas configurables con su posición | `com.ingsis.sca` |
| **CLI** | Operaciones `Validation`, `Execution`, `Formatting`, `Analyzing` | `com.ingsis.engine` |

### Requisitos "duros" de la consigna (y dónde se cumplen)

| Requisito | Cómo se cumple |
|---|---|
| Lexer → Parser → AST, con una interfaz para operar sobre el AST | `lexer`, `parser` (`syntactic`) y los nodos en `common/node` |
| El lenguaje va a crecer: extensible "con el menor esfuerzo" | `VersionStrategy` + registry, handlers por nodo, cadena de matchers, reglas de formato en lista |
| **Fuentes que no entran en memoria**: no se puede lexear todo y después parsear | Pipeline *pull* y *lazy*: `CharStream` → `Lexer` → `LazyTokenStream` → parser **sentencia por sentencia** (§8) |
| Errores con **fila y columna de inicio y fin** | `Position`, `SourceRange`, mensajes etiquetados (§7) |
| Mostrar el **progreso del parsing** en la CLI | `ProgressCharReader` (Decorator) + `ConsoleProgressReporter` (Observer) |
| Sistema separado en módulos que dependen entre sí | 8 subproyectos Gradle (§3, §10) |
| Tests automáticos y escalables | ~700 tests: unit, integration, e2e y container (§13) |
| Criterios de Diseño de Sistemas (SOLID, patrones, cohesión/acoplamiento) | §18 |

### PrintScript 1.0
- `let` con tipo obligatorio (`let x: number;`), una variable por sentencia, declarar y asignar juntos.
- Asignación `x = 5;`. Tipos `number` (enteros y decimales) y `string` (comillas simples o dobles).
- Toda sentencia termina en `;`.
- Operadores `+ - * /`; `+` concatena si algún operando es string.
- `println(expr)`.

### PrintScript 1.1 (segunda parte)
- `const`, tipo `boolean`, `if (cond) { } else { }` (sin `else if`, llaves obligatorias, condición boolean).
- `readInput("mensaje")` y `readEnv("VAR")`: el tipo de retorno **se adapta a la variable destino**
  (o es `string` si van dentro de `println`); si el valor no se puede convertir, la ejecución falla.
- Formatter: indentación configurable dentro de `if` y llave `{` en la misma línea.
- SCA: `readInput` solo con identificador o literal (regla prendible).
- La CLI permite elegir la versión y si usás algo de 1.1 en 1.0 tiene que fallar.

---

## 2. Mapa del repositorio

```text
PrintScript/
├── settings.gradle                  # registra el root y los 8 subproyectos
├── build.gradle                     # root: versión/grupo, JaCoCo agregado, task installGitHooks
├── gradlew / gradlew.bat / gradle/  # Gradle Wrapper (Gradle 9.3.0)
├── buildSrc/                        # lógica de build compartida (convention plugins)
│   ├── build.gradle                 # trae los plugins de Spotless, SpotBugs y Shadow
│   └── src/main/
│       ├── groovy/
│       │   ├── buildlogic.java-common-conventions.gradle       # calidad + Java 21 + JaCoCo
│       │   ├── buildlogic.java-library-conventions.gradle      # common + java-library
│       │   ├── buildlogic.java-application-conventions.gradle  # common + application + shadow + picocli
│       │   ├── com.ingsis.java-conventions.gradle              # legado, no lo usa nadie
│       │   └── PublishingConventionPlugin.groovy               # maven-publish → GitHub Packages
│       └── resources/META-INF/gradle-plugins/publishing-convention.properties
├── .checkstyle.xml                  # reglas Checkstyle (20 líneas/método, 4 params, etc.)
├── config/
│   ├── checkstyle/suppressions.xml  # excepciones (tests y FormatContext)
│   └── spotbugs/exclude.xml         # filtro vacío
├── .editorconfig                    # utf-8, LF, 4 espacios, 120 cols en Java
├── .githooks/{pre-commit,pre-push}  # hooks versionados
├── install-hooks.sh                 # activa los hooks (core.hooksPath)
├── run.sh                           # atajo para la CLI (recompila solo si hace falta)
├── samples/                         # programas de ejemplo + probar_todo.sh + PRUEBAS.md
├── .github/workflows/{ci.yml,publish.yml}
├── documents/                       # documentación (este archivo incluido) + consigna
├── com.ingsis.common/        # modelos y contratos compartidos
├── com.ingsis.charstream/    # lectura de caracteres con posición y pushback
├── com.ingsis.lexer/         # tokenización (maximal munch)
├── com.ingsis.parser/        # token stream lazy, parser (Pratt), semántica y pipeline
├── com.ingsis.interpreter/   # ejecución
├── com.ingsis.formatter/     # formateo
├── com.ingsis.sca/           # análisis estático
└── com.ingsis.engine/        # fachada + CLI (picocli) + servicios + progreso
```

Cada módulo sigue el layout estándar de Maven/Gradle: `src/main/java`, `src/test/java`
(y el engine además `src/containerTest/java`).

Repos hermanos que participan del proyecto (en `~/Faculty/ingsis/`):
- `printscript--tck/` → el kit de compatibilidad de la cátedra, con nuestro adapter (§17).
- `workflows-and-actions/` → workflows reutilizables de la organización (§15).

---

## 3. Arquitectura general

### 3.1 El pipeline

```text
 archivo / stdin
      │  InputStream
      ▼
 StreamCharReader (Reader → PushbackReader)        charstream
      │  [opcional: ProgressCharReader decorando]   engine/progress
      ▼
 CharStream  ──►  MetaCharacter(char, Position)     charstream
      │  SafeIterator<MetaCharacter>
      ▼
 Lexer (maximal munch + cadena de matchers)         lexer
      │  SafeIterator<Token>
      ▼
 LazyTokenStream (lista enlazada memoizada)         parser/tokenstream
      │  TokenStream (peek(k), consume, persistente)
      ▼
 Parser de sentencias (ParserFactory, Pratt)        parser/syntactic
      │  UNA sentencia → Node (sub-árbol AST)
      ▼
 SemanticChecker (Visitor + handlers)               parser/semantic
      │  SemanticEnvironment actualizado
      ▼
 ┌───────────────┬──────────────────┬────────────────────┐
 │ Interpreter   │ ASTSca (linter)  │ (nada: Validation) │
 │ ejecuta       │ junta violaciones│                    │
 └───────────────┴──────────────────┴────────────────────┘
```

El **Formatter** que usa la CLI toma un atajo: se cuelga directamente del `Lexer` (token por token)
sin parsear (§4.7 explica por qué).

La clave: **es un pipeline "pull"**. Nadie empuja datos; el que está al final pide la próxima
sentencia, el parser pide tokens, el lexer pide caracteres. Solo existe en memoria **la sentencia
actual** (más un buffer chico de lookahead). Eso es lo que permite procesar archivos de cualquier
tamaño (§8).

### 3.2 Diagrama de componentes / dependencias entre módulos

Dependencias declaradas en cada `build.gradle` (todas `implementation`):

```text
                       ┌──────────────────┐
                       │ com.ingsis.common│  (no depende de nadie)
                       └────────▲─────────┘
                                │
                     ┌──────────┴─────────┐
                     │ com.ingsis.charstream│
                     └──────────▲─────────┘
                                │
                       ┌────────┴────────┐
                       │ com.ingsis.lexer │
                       └────────▲────────┘
                                │
                       ┌────────┴────────┐
                       │ com.ingsis.parser│  (tokenstream + syntactic + semantic + pipeline)
                       └──▲─────▲─────▲──┘
             ┌────────────┘     │     └─────────────┐
   ┌─────────┴──────────┐ ┌─────┴────────────┐ ┌────┴────────────┐
   │com.ingsis.interpreter│ │com.ingsis.formatter│ │ com.ingsis.sca │
   └─────────▲──────────┘ └─────▲────────────┘ └────▲────────────┘
             └──────────────┬───┴──────────────────┘
                     ┌──────┴───────────┐
                     │ com.ingsis.engine │  (depende de los 7) + picocli
                     └──────────────────┘
```

Detalle exacto:

| Módulo | Depende de (main) | Extra |
|---|---|---|
| common | — | — |
| charstream | common | — |
| lexer | common, charstream | — |
| parser | common, charstream, lexer | — |
| interpreter | common, parser | tests: charstream, lexer |
| formatter | common, charstream, lexer, parser | snakeyaml 2.2 |
| sca | common, charstream, lexer, parser | snakeyaml 2.2 |
| engine | los 7 anteriores | picocli 4.7.7, Testcontainers (solo containerTest) |

Propiedades para defender:
- **Es un DAG** (sin ciclos). Gradle además fallaría si hubiera un ciclo de proyecto.
- **Dependencias hacia abajo/estables**: todos dependen de `common` (abstracciones y modelos), que
  no depende de nada → *Stable Dependencies Principle* y DIP a nivel módulo.
- Las tres herramientas **no se conocen entre sí** (interpreter, formatter y sca son hermanos).
  Solo el `engine` las compone → bajo acoplamiento.
- **`implementation` y no `api`**: lo que un módulo usa no "se filtra" al classpath de compilación
  de quien lo consume. Consecuencia práctica: el TCK tiene que declarar **los 8 artefactos** porque
  usa clases de `common` (como `Result` o `Version`) directamente (§17).

### 3.3 Paquetes por módulo

Los paquetes no usan dominio invertido (`com.ingsis...`), se llaman por concepto:

| Módulo | Paquetes |
|---|---|
| common | `environment` (Scope), `iterator` (SafeIterator, IterationStep), `metaChar`, `node`, `node.expression.*`, `node.keyword.*`, `node.factory`, `node.visitor`, `parser` (Parser funcional), `position`, `result`, `state`, `token`, `token.matcher(.chain)`, `token.tokenizer`, `token.tokenize`, `tokenstream` (interfaz), `version` |
| charstream | `charstream` |
| lexer | `lexer` |
| parser | `tokenstream` (LazyTokenStream, TokenStreamAdapter, rules, version), `syntactic` (+ `parser.literal`, `parser.operator`, `parser.root`, `strategy`, `util`, `version`), `semantic` (+ `environment`, `evaluator`, `handler`), `pipeline` |
| interpreter | `interpreter`, `executor`, `evaluator`, `environment` (Environment), `builtin` (+ `provider`), `value` |
| formatter | `formatter`, `formatter.config`, `formatter.handler`, `formatter.rule` |
| sca | `sca`, `sca.config`, `sca.handler` |
| engine | `engine`, `service`, `progress` |

Ojo (pregunta posible): hay **paquetes partidos entre jars** (*split packages*): `environment`
existe en common y en interpreter, `tokenstream` en common y en parser. En el classpath clásico
funciona; con JPMS (`module-info.java`) no se permitiría. También el paquete `interpreter` existe en
nuestro jar y en el TCK.

---

## 4. Módulo por módulo

### 4.1 `com.ingsis.common` — el vocabulario compartido

**Responsabilidad:** definir los tipos y contratos que usan todos los demás. No tiene lógica de
negocio pesada: es el "lenguaje común" del sistema.

Piezas clave:

- **`result.Result<T>`** — `sealed interface` con dos `record`: `CorrectResult<T>(T value)` e
  `IncorrectResult<T>(String error)`. Reemplaza excepciones para errores **esperables** (errores del
  programa del usuario). Al ser `sealed`, el `switch` con pattern matching es exhaustivo sin
  `default`:
  ```java
  return switch (result) {
      case CorrectResult<Token>(Token t) -> ...;
      case IncorrectResult<Token>(String err) -> ...;
  };
  ```
- **`iterator.SafeIterator<T>`** — `Result<IterationStep<T>> next()` + `unread(T)` opcional.
  Un iterador "seguro": nunca tira excepción; termina con `IncorrectResult("EOF")`.
- **`iterator.IterationStep<T>(T value, SafeIterator<?> next)`** — el valor y **el iterador que
  sigue**. Esto es lo que hace a los iteradores *persistentes/inmutables*: en vez de mutar un cursor,
  cada paso devuelve un nuevo iterador. Permite backtracking gratis (§4.4).
- **`position.Position(line, column)`** y **`position.SourceRange(start, end)`** — ubicación de
  todo; `SourceRange.toString()` da `"from line 1, column 5 to line 1, column 9"`.
- **`metaChar.MetaCharacter(char, Position)`** — un carácter con su posición.
  `MetaCharStringBuilder` acumula MetaCharacters recordando posición de inicio y fin (usa
  `Position(-1,-1)` como "vacío" para no usar `null`).
- **`token.*`** — `Token(type, value, startPosition, endPosition)` (record), `TokenType` (enum con
  todos los tipos), `SymbolType` (símbolos `= : ; ( ) { } ,`), `TokenizeResult` (sealed:
  `Complete`, `Prefix`, `Invalid`), la interfaz `Tokenizer`.
- **`token.matcher.*`** — la **Chain of Responsibility** que decide el tipo de un lexema:
  `LexemeMatcher` (palabras reservadas y símbolos fijos) → `NumberMatcher` → `BooleanMatcher` →
  `StringMatcher` → `IdentifierMatcher`. `ChainTokenTypeMatcher.defaultChain()` la arma. El orden
  importa: las keywords van antes que identificador, así `let` es `LET` y no `IDENTIFIER`.
- **`node.*`** — los nodos del AST (§5), todos `record` (inmutables), más `NodeFactory`
  (Factory) y `NodeVisitor<R, C>` (Visitor genérico: R = resultado, C = contexto).
- **`tokenstream.TokenStream`** — contrato del stream de tokens: `consume()`, `consume(TokenType)`,
  `consume(Predicate<Token>)`, `peek(offset)`, `isEmpty()`, `pointer()`.
- **`parser.Parser<T>`** — interfaz funcional `Result<IterationStep<T>> parse(TokenStream)`: un
  parser es una función "stream → (nodo, stream restante)". Esto es el estilo de **parser
  combinators**.
- **`version.Version`** — enum `V_1_0`, `V_1_1` con `fromString("1.0")`.
- **`node.expression.literal.DataType`** — `BOOLEAN`, `STRING`, `NUMBER`.
- **`node.keyword.declaration.DeclarationType`** — `LET` (mutable) y `CONST` (inmutable).

Código sin uso en producción (bueno saberlo): `environment.Scope`, `state.State`,
`MetaCharacterStringBuilder` (interfaz sellada con una sola implementación).

### 4.2 `com.ingsis.charstream` — leer caracteres sabiendo dónde estoy

- **`CharReader`** — interfaz mínima (`readNextChar()`, `unread(int)`), `AutoCloseable`.
- **`StreamCharReader`** — adapta cualquier `Reader` (Adapter): lo envuelve en `BufferedReader`
  (8 KB) y `PushbackReader` (buffer de 1024) para poder **devolver caracteres** al stream.
- **`PositionTracker`** — inmutable. `advance(c)` devuelve un tracker nuevo: suma columna, o salta de
  línea con `\n`, `\r` y `\r\n` (este último cuenta una sola línea).
- **`CharStream implements SafeIterator<MetaCharacter>`** — cada `next()` lee un char, le pone la
  posición actual y devuelve `IterationStep(metaChar, new CharStream(reader, tracker.advance(c)))`.
  `unread` delega al `PushbackReader`.

Sutileza para la defensa: `CharStream` es inmutable **en la posición**, pero el `Reader` de abajo es
mutable y compartido. O sea, no se puede "volver atrás" a un `CharStream` viejo: el retroceso a nivel
caracter se hace explícitamente con `unread` (lo usa el lexer para devolver el lookahead). El
backtracking "de verdad" se hace a nivel **tokens** (LazyTokenStream, §4.4).

### 4.3 `com.ingsis.lexer` — tokenización con *maximal munch*

**`Lexer implements SafeIterator<Token>`**: cada `next()` produce **un** token y un `Lexer` nuevo
parado después de ese token.

Algoritmo (`maximalMunchOf`):
1. Saltea whitespace (`Character.isWhitespace`). **El lexer descarta los espacios**; las posiciones
   quedan en los tokens (el formatter reconstruye los espacios a partir de ellas).
2. Va agregando caracteres a un `MetaCharStringBuilder` y en cada paso le pregunta al
   **`PrintScriptTokenizer`** qué es el texto acumulado:
   - `Complete(token)` → es un token válido: lo guarda como "último válido" y **sigue** (puede haber
     uno más largo).
   - `Prefix` → todavía no es válido pero puede llegar a serlo (string sin cerrar `"Hol`, número
     `12.`): sigue, guardando los chars en el *lookahead buffer*.
   - `Invalid` → corta.
3. Devuelve el **último token completo** (el más largo = *maximal munch*) y hace `unread` de los
   caracteres de lookahead que leyó de más.
4. Si nunca hubo un `Complete` → `"Lexical error: unrecognized character '@' at line 2, column 19"`.

Ejemplos:
- `letter` → `l`(id) `le`(id) `let`(LET) `lett`(id) … `letter`(IDENTIFIER). Gana el más largo, por
  eso `letter` no se parte en `let` + `ter`.
- `12.5;` → `1`,`12` (NUMBER) → `12.` (Prefix) → `12.5` (NUMBER) → `12.5;` (Invalid) → token `12.5`,
  se devuelve el `;`.
- `x=5` → `x` (id) → `x=` (Invalid) → token `x`, se devuelve `=`.

`PrintScriptTokenizer` usa la cadena de matchers (§4.1) y solo agrega la noción de **prefijo**
(`isPrefix`: string sin cerrar o `^\d+\.$`).

Notas:
- `readInput`/`readEnv` se lexean como `IDENTIFIER` (no son keywords); `println` sí es keyword
  (`PRINTLN`). `true`/`false` son `BOOLEAN_LITERAL` en **ambas** versiones: el lexer no conoce
  versiones; el que restringe es el parser (§6).
- `MunchState` (campos públicos mutables) es el estado temporal de una pasada de munch.
- No hay comentarios en el lenguaje, así que el lexer no los maneja.

### 4.4 `com.ingsis.parser` — tokens lazy, sintaxis, semántica y pipeline

Es el módulo más grande. Tiene cuatro partes.

#### a) `tokenstream` — el puente lexer → parser

**`LazyTokenStream implements TokenStream`**: una **lista enlazada memoizada y perezosa** de tokens.
- Cada `Node` interno guarda un token y una referencia al lexer que produce el siguiente.
- `nextNode()` (synchronized) calcula el siguiente **una sola vez**, lo cachea en `next` y **suelta
  el lexer** (`lexer = null`).
- `consume()` devuelve `IterationStep(token, new LazyTokenStream(nodoSiguiente, pointer+1))`.
- `peek(k)` camina k nodos (forzando el lexeo de esos k tokens). Así el parser tiene lookahead
  arbitrario (LL(k)).
- Errores léxicos se guardan como un nodo de error **que no es EOF**: `isEmpty()` devuelve `false`
  para que el error llegue al parser y se reporte con contexto.

¿Por qué así? Porque da lo mejor de dos mundos:
1. **Persistencia/backtracking**: una referencia vieja al stream sigue siendo válida (el nodo ya
   está memoizado), así que probar un parser, fallar y probar otro es gratis.
2. **Memoria constante**: los nodos *detrás* de la posición actual quedan inalcanzables apenas nadie
   los referencia → el GC los libera (§8).

**`TokenStreamAdapter`**: implementación sobre una `List<Token>` (Adapter). Se usa en tests para
armar streams a mano.

`tokenstream.rules.TokenMatchers`: predicados reutilizables (`isType`, `isOneOf`, `isTypeAndValue`).
`tokenstream.version.GrammarRules`: no se usa en producción (quedó de una versión anterior; hoy eso
lo hace `VersionStrategy`).

#### b) `syntactic` — el parser sintáctico

Todo parser implementa `syntactic.Parser<T extends Node>` (que extiende el `parser.Parser<T>`
funcional de common). El grafo de parsers lo arma **`ParserFactory.createParser(version)`**:

```text
createParser(version)
 ├─ strategy = VersionStrategyRegistry.getStrategy(version)
 ├─ identifierParser  = IdentifierParser (IDENTIFIER o PRINTLN)
 ├─ prattParser       = OperatorParser(() -> primaryParser)       ← expresiones
 │     primaryParser  = prueba en orden strategy.primaryParsers(...)
 │                       1.0: number, string, function, identifier
 │                       1.1: number, string, boolean, function, identifier
 ├─ statement parsers = strategy.statementParsers(...)
 │                       1.0: Declaration, Assign, LineExpression
 │                       1.1: Declaration, Assign, Conditional, LineExpression
 └─ composite: el primero cuyo canStart(stream) da true se adueña de la sentencia
```

Detalles importantes:

- **Recursión mutua con `AtomicReference`**: el `ConditionalParser` necesita el parser de sentencias
  (para el cuerpo del `if`), pero el parser de sentencias contiene al `ConditionalParser`. Se rompe
  el huevo-y-la-gallina con un `AtomicReference<Parser<Node>>` que se completa después de construir
  todo (lo mismo para expresión ↔ argumentos de función).
- **Despacho predictivo (`StatementParser.canStart`)**: cada parser de sentencia mira adelante sin
  consumir:
  - `DeclarationParser`: el token 0 es keyword de declaración de la versión (`let`, o `let`/`const`).
  - `AssignParser`: token 0 `IDENTIFIER` **y** token 1 `=` (lookahead 2).
  - `ConditionalParser`: token 0 `if`.
  - `LineExpressionParser`: siempre `true` → es el *fallback* (por eso va último).

  Ventaja: el parser que se "compromete" reporta **su** error específico (por ejemplo "Unexpected
  token 'number'… " si falta el `:`), en lugar de un genérico "no matcheó nada".
- **`OperatorParser` = Pratt parser** (precedence climbing). Cada `OperatorType` tiene *binding
  powers* izquierdo y derecho:

  | Operador | lbp | rbp |
  |---|---|---|
  | `+` `-` | 11 | 12 |
  | `*` `/` | 21 | 22 |
  | `=` (ASSIGNATION) | 2 | 1 |

  Algoritmo: parsear un primario; mientras el próximo token sea un operador con `lbp > rbp_actual`,
  consumirlo y parsear el lado derecho con `rbp` del operador. **`lbp < rbp` ⇒ asociatividad a
  izquierda.**

  Traza de `10 - 4 - 3` (rbp inicial 0):
  ```text
  primario 10 ; ve '-' (11 > 0) → derecho con rbp 12: primario 4 ; ve '-' (11 > 12? no) → devuelve 4
  arma (10 - 4) ; ve '-' (11 > 0) → derecho con rbp 12: 3 → arma ((10 - 4) - 3) = 3   ✔
  ```
  Traza de `a + b * 2`:
  ```text
  primario a ; '+' (11>0) → derecho con rbp 12: primario b ; '*' (21>12) → derecho rbp 22: 2 → (b*2)
  arma a + (b * 2)   ✔ precedencia
  ```
- **Primarios con backtracking**: `parseFirstMatching` prueba number, string, (boolean), función,
  identificador. `FunctionParser` intenta identificador + `(`args`)`; si no hay `(`, falla y se prueba
  `IdentifierParser` **sobre el mismo stream** (gracias a la persistencia).
- **Strategy en declaraciones**: después de `let x: number`, se mira el próximo símbolo y se busca en
  un `Map<SymbolType, DeclarationSymbolStrategy>`: `=` → `AssignmentSymbolStrategy`
  (`let x: number = expr;`) o `;` → `EmptyDeclarationSymbolStrategy` (`let x: number;`, con
  `NilExpressionNode` como valor = **Null Object**).
- **Strategy en el `else`**: `ConditionalParser` arma un `ConditionalBlock(ifToken, condición,
  then)` y elige `WithElseStrategy` o `WithoutElseStrategy` según si el próximo token es `else`.
- **Utilidades**: `BlockParserUtils.parseBlock` (`{ sentencias }`) y
  `ArgumentsParserUtils.parseSeparatedList` (`( a, b, c )`) — recursivas (para respetar el límite
  de 20 líneas por método).
- **`SyntacticParser`**: arma un `ProgramNode` completo (todas las sentencias). **No lo usa la
  CLI** (rompería el streaming); se usa en tests.

`syntactic.version`: `VersionStrategy` (interfaz), `Version10Strategy`, `Version11Strategy`,
`VersionStrategyRegistry` (Map versión → estrategia) y `LiteralParsers` (record que agrupa los tres
parsers de literales, para no pasar más de 4 parámetros).

#### c) `semantic` — chequeo de tipos y scopes

- **`SemanticEnvironment`**: tabla de símbolos **inmutable/persistente**. `define(...)` devuelve un
  entorno nuevo (copia el mapa y agrega). `lookup` busca en el actual y después en el padre. Guarda
  `VariableSymbol(type, isMutable, isInitialized)`.
- **`SemanticChecker implements NodeVisitor<Result<SemanticEnvironment>, SemanticEnvironment>`**:
  Visitor que despacha a un **handler por tipo de nodo** registrado en un
  `Map<Class<? extends Node>, SemanticNodeHandler<?>>`:
  - `DeclarationNodeSemanticHandler`: no redeclarar (busca también en scopes padre), el tipo de la
    expresión coincide con el declarado. Sin valor → símbolo no inicializado.
  - `AssignNodeSemanticHandler`: la variable existe, no es `const` ya inicializada, el tipo coincide.
  - `IfNodeSemanticHandler`: la condición es `BOOLEAN`. El `SemanticChecker` además chequea then y
    else en **un scope hijo** (lo declarado adentro no se ve afuera).
  - `CallFunctionNodeSemanticHandler`: la función existe en la versión y los argumentos tipan.
- **`ExpressionTypeInference`**: infiere el tipo de una expresión:
  - literales → su tipo; identificador → lo busca (si no existe: "Variable 'x' is not declared").
  - `+`: si algún lado es STRING → STRING; si ambos NUMBER → NUMBER; si no, error.
  - `- * /`: solo NUMBER.
  - llamadas: la función tiene que estar en `strategy.builtInFunctions()` (1.0: `println`; 1.1:
    `println`, `readInput`, `readEnv`). `readInput`/`readEnv` devuelven tipo `null` = **"se adapta
    al destino"**, que los handlers interpretan como compatible con cualquier tipo.

#### d) `pipeline` — el loop compartido

**`StatementPipeline.run(stream, env, action)`**: mientras el stream no esté vacío:
parsear **una** sentencia → chequearla semánticamente → aplicarle una `StatementAction` → seguir.
Etiqueta los errores (`Lexical`/`Syntactic`/`Semantic`/`Runtime`) y les agrega la posición. Un error
que empieza con `EOF` se toma como fin normal.

`StatementAction` es una interfaz funcional; `StatementAction.NONE` no hace nada (eso **es** la
validación). Lo usan `ValidationService` (acción NONE) y `LintService` (acción = juntar violaciones).
El intérprete tiene su propio loop equivalente por un tema de memoria (§8.3).

> El archivo `com.ingsis.parser/src/main/java/Parser.java` (paquete por defecto) y
> `semantic.SemanticStep` son restos sin uso.

### 4.5 `com.ingsis.interpreter` — ejecución

- **`Interpreter`** / **`DefaultInterpreter`**: `interpret(TokenStream, SemanticEnvironment,
  Environment)` hace el loop parse → semántica → ejecutar, sentencia por sentencia. También tiene
  `interpret(ProgramNode)` para ejecutar un AST ya armado (tests). Muchos constructores
  "telescópicos" con valores por defecto: es **inyección de dependencias por constructor** sin
  framework (DIP).
- **`DefaultStatementExecutor`**: `switch` con pattern matching sobre el tipo de nodo:
  - `DeclarationKeywordNode` → evalúa la expresión **con el tipo declarado como tipo destino** y
    `env.declare(...)`.
  - `AssignNode` → busca el tipo de la variable, evalúa con ese tipo destino, `env.assign`.
  - `IfKeywordNode` → evalúa condición (tiene que ser `Boolean`), ejecuta la rama en un
    `new Environment(env)` (scope de bloque).
  - `CallFunctionNode` → busca la función en el `FunctionRegistry` y la ejecuta (ej. `println`).
- **`DefaultExpressionEvaluator`**: literales, identificadores, operadores y llamadas a
  `readInput`/`readEnv` como expresión.
  - Números en **`BigDecimal`** (exactitud decimal: `0.1 + 0.2 = 0.3`, no `0.30000000000000004`).
  - División con `MathContext.DECIMAL64` (16 dígitos): `10 / 3 = 3.333333333333333`.
  - **División por cero → error de runtime** (TypeScript daría `Infinity`; decisión explícita).
  - `PrintScriptValues.normalize/display`: saca ceros insignificantes (`1.5 * 2` → `3`, como TS).
  - `+` con un string → concatena usando `display`.
- **`Environment`**: entorno de runtime **mutable** con padre (cadena de scopes). `declare`,
  `assign` (sube por los padres; falla si es const), `get`, `find`.
- **`builtin`**: `BuiltInFunction` (interfaz), `PrintlnFunction`, `ReadInputFunction`,
  `ReadEnvFunction`, `FunctionRegistry` / `DefaultFunctionRegistry` (Registry, case-insensitive).
  - `readInput(prompt)`: emite el prompt por el emitter, pide la línea al `InputProvider` y la
    **convierte al tipo destino** (`"30"` → `30` si la variable es number; `"quizas"` como boolean →
    `Cannot parse input 'quizas' as boolean`).
  - `readEnv(nombre)`: igual pero lee del `EnvProvider` (`System::getenv` por defecto); si no existe
    → error.
  - `InputProvider` y `EnvProvider` son interfaces funcionales: la CLI las conecta a stdin y al
    entorno; el TCK y los tests inyectan fakes. Esto cumple "en el uso programático debe existir un
    mecanismo para proveer valores".

¿Por qué dos entornos (`SemanticEnvironment` y `Environment`)? Porque son dos fases distintas: uno
sabe **tipos** (para validar sin ejecutar), el otro guarda **valores**. Así `Validation` no ejecuta
nada y el intérprete igual valida antes de ejecutar cada sentencia.

### 4.6 `com.ingsis.engine` — fachada, CLI y servicios

- **`Engine`** (interfaz, **Facade**): `validate`, `interpret`, `format`, `analyze`. Es la API
  programática del sistema (la que usaría otro servicio, ej. el snippet service).
- **`ExecuteService implements Engine`**: arma el intérprete para la versión pedida y delega
  validación/formato/lint en los servicios específicos.
- **`ValidationService`**: `StatementPipeline` con `StatementAction.NONE` → "Validation successful:
  Syntax and semantics are valid."
- **`FormatService`**: carga el YAML/JSON → `FormatContext` → `TokenStreamFormatter` escribiendo en
  un `Writer` mientras lee.
- **`LintService`**: `StatementPipeline` con una acción que llama a `ASTSca.analyzeNode` por
  sentencia y **solo guarda las violaciones** (nunca el AST entero). 0 violaciones → éxito; si hay →
  `Result.failure` con todas, una por línea (exit code 1).
- **`SourceStreams`**: arma `InputStream → StreamCharReader → [ProgressCharReader] → CharStream →
  Lexer → LazyTokenStream`.
- **`progress`**:
  - `ProgressListener` (**Observer**, con `NONE` como Null Object).
  - `ProgressCharReader` (**Decorator** de `CharReader`): cuenta los caracteres que **el lexer
    realmente consume**. Como los tokens se piden perezosamente desde el parser, el porcentaje sigue
    el avance del **parsing**, no de la lectura del disco (eso pide la consigna).
  - `ConsoleProgressReporter`: dibuja `Parsing: NN%` en **stderr** reescribiendo la línea con `\r`
    (no ensucia stdout); `clear()` lo borra antes de cada `println` del programa.
- **`CliEngine`** (picocli): ver §9.
- `InputSupplier` y `OutputEmitter`: interfaces funcionales propias del engine (para no exponer las
  de interpreter hacia afuera).

### 4.7 `com.ingsis.formatter` — dos formatters

Hay **dos implementaciones** de `Formatter`:

1. **`TokenStreamFormatter`** ← la que usa la CLI y el TCK.
   - Lee tokens del `Lexer` y los escribe **apenas los lee** en el `Writer` (streaming puro, no arma
     AST).
   - Como el lexer tiró los espacios, **reconstruye el separador original** entre dos tokens a partir
     de sus posiciones (`separatorBetween`: si cambió la línea → `\n` × diferencia + la columna
     como espacios; si no → tantos espacios como columnas de diferencia).
   - Después pasa ese separador por una **lista ordenada de `FormattingRule`**; cada regla que
     `applies(prev, current, ctx)` lo reescribe (`formatSeparator`). El orden importa: la primera
     normaliza, las siguientes pisan casos particulares:

     | # | Regla | Configurable | Qué hace |
     |---|---|---|---|
     | 1 | `SingleSpaceSeparationRule` | parcialmente (`mandatory-single-space-separation`) | como máximo 1 espacio entre tokens; nada antes de `;` |
     | 2 | `SpaceBeforeColonRule` | sí | espacio antes de `:` |
     | 3 | `SpaceAfterColonRule` | sí | espacio después de `:` |
     | 4 | `SpaceAroundEqualsRule` | sí | espacio alrededor de `=` |
     | 5 | `SpaceAroundOperatorsRule` | no (obligatoria) | un espacio alrededor de `+ - * /` |
     | 6 | `SpaceAfterCommaRule` | sí | espacio después de `,` |
     | 7 | `LineBreakAfterStatementRule` | no (obligatoria) | salto después de `;`, contenido de bloques en su línea |
     | 8 | `LinesBeforePrintlnRule` | sí (0,1,2) | líneas vacías antes de `println` |
     | 9 | `BracePositionRule` | sí | `{` en la línea del `if` (default) o abajo |
     | — | `IndentationRule` | sí (`indent-inside-if`) | se aplica al final, con estado (profundidad de llaves) |
     | — | líneas después de `println` | sí | lo maneja el estado (`justFinishedPrintln`) |

   - Estado (`FormattingState`): token previo, profundidad de `{}`, si la sentencia actual tiene
     `println`.
2. **`ASTFormatter`** (Visitor + `FormatNodeHandler` por nodo, como la semántica y el SCA):
   regenera el código desde el AST. Queda como implementación alternativa "sobre el AST" (lo que
   menciona la consigna) y está testeada, pero **la CLI no la usa**.

¿Por qué la CLI usa el de tokens y no el del AST? (pregunta casi segura)
- **Memoria**: formatea sin armar ni siquiera una sentencia completa.
- **Fidelidad**: una regla *no configurada* deja el código como estaba (ej. si no configurás la
  indentación, se respeta la del autor; el TCK lo exige — está comentado en `IndentationRule`).
  Regenerar desde el AST pierde el layout original (saltos de línea, espacios no regulados).
- Contra: no valida la sintaxis (un programa sintácticamente inválido se formatea igual mientras sea
  léxicamente válido).

**Configuración** (`YamlFormatRulesLoader` + `FormatContext`):
- Se parsea con **SnakeYAML**; como JSON es (prácticamente) un subconjunto de YAML, el mismo loader
  lee el `config.json` del TCK y los `.yaml` nuestros.
- Acepta **varios alias por regla** (kebab-case, camelCase y los nombres del TCK), ej.
  `enforce-spacing-before-colon-in-declaration` / `space-before-colon` / `spaceBeforeColon`.
- `FormatContext` es un `record` con **`Boolean`/`Integer` nullables a propósito**: `null` significa
  "regla no configurada → no tocar". Es un *tri-estado* (sí / no / no me importa).
- Si el YAML está roto, se usa el contexto por defecto en silencio (ver §20).

Claves soportadas (las principales):

| Clave (TCK) | Efecto |
|---|---|
| `enforce-spacing-before-colon-in-declaration` | espacio antes de `:` |
| `enforce-spacing-after-colon-in-declaration` | espacio después de `:` |
| `enforce-spacing-around-equals` / `enforce-no-spacing-around-equals` | espacio alrededor de `=` |
| `line-breaks-after-println` | líneas después de `println` |
| `line-breaks-before-println` | líneas antes de `println` |
| `mandatory-single-space-separation` | exactamente un espacio entre tokens |
| `mandatory-space-surrounding-operations` | espacio alrededor de operadores |
| `mandatory-line-break-after-statement` | salto después de `;` |
| `indent-inside-if` (alias `indent-size`, `indentSpaces`…) | n espacios por nivel de bloque |
| `if-brace-same-line` / `if-brace-below-line` | posición de la `{` |
| `enforce-spacing-after-comma-in-arguments` | espacio después de `,` |

### 4.8 `com.ingsis.sca` — analizador estático

- **`ASTSca implements NodeVisitor<List<String>, ScaContext>, Sca`**: Visitor + un
  `ScaNodeHandler` por nodo:
  - `DeclarationScaHandler`: nombre del identificador declarado según `identifier_format`
    (`camel case` → `^[a-z]+(?:[A-Z][a-z0-9]*)*$`; `snake case` → `^[a-z]+(?:_[a-z0-9]+)*$`) y analiza
    la expresión asignada.
  - `CallFunctionScaHandler`: si la regla está prendida, los argumentos de `println` / `readInput`
    tienen que ser `LiteralNode` o `IdentifierNode` (no una expresión).
  - `AssignScaHandler` (analiza la expresión, por ej. `x = readInput("a" + "b")`), `IfScaHandler`
    (recorre then/else), `ProgramScaHandler`.
  - Cada violación lleva línea y columna: `Identifier 'mi_nombre' does not respect camel case
    naming convention at line 1, column 5`.
- **`ScaContext`** (record): `identifierFormat`, `mandatoryLiteralOrIdentifierInPrintln`,
  `mandatoryLiteralOrIdentifierInReadInput`. Lo arma `YamlScaRulesLoader` (también acepta varios
  alias de clave).
- Corre **después** de la semántica (vía `StatementPipeline` en `LintService`), así que el linter
  también reporta errores sintácticos y semánticos, como pide la consigna ("detectar errores de
  sintaxis, uso incorrecto de variables…").
- **`TokenStreamSca`**: alternativa que analiza a nivel tokens. No está conectada a la CLI y además
  junta todos los tokens en una lista (rompería el streaming). Queda como experimento con tests.

---

## 5. El AST

### 5.1 Jerarquía de nodos (todo en `com.ingsis.common/node`)

```text
Node (interfaz: line(), column(), symbol(), children(), accept(visitor, ctx))
├── ProgramNode(statements)                         raíz; "PROGRAM"
├── DeclarationKeywordNode(declarationType, identifierNode, expressionNode, declaredType)
├── AssignNode(identifierNode, expressionNode)
├── IfKeywordNode(condition, thenBody, elseBody)
└── ExpressionNode (interfaz; children() devuelve List<ExpressionNode>)
    ├── IdentifierNode(name)
    ├── CallFunctionNode(identifierNode, argumentNodes)    println / readInput / readEnv
    ├── OperatorNode(operatorType, left, right)            binario
    ├── NilExpressionNode()                                Null Object: "sin valor"
    └── LiteralNode<T> (value(): Result<T>)
        ├── NumberLiteralNode(BigDecimal)
        ├── StringLiteralNode(String)                       ya sin comillas
        └── BooleanLiteralNode(Boolean)
```

- Todos son **`record`** → inmutables, `equals/hashCode/toString` gratis, ideales como datos.
  Las listas se copian con `List.copyOf` (copia defensiva, inmutable).
- Es un **Composite**: `children()` permite recorrer el árbol de manera uniforme.
- **Doble dispatch del Visitor**: `ProgramNode`, `DeclarationKeywordNode`, `AssignNode`,
  `IfKeywordNode` y `CallFunctionNode` sobrescriben `accept` para llamar a su `visit(...)`; el resto
  usa el `accept` por defecto de `Node`, que llama a `visitDefault`. O sea: el Visitor es para
  **sentencias**; las expresiones se recorren con `switch` + pattern matching.
- Cada nodo guarda línea y columna (del token que lo inicia) → los errores y violaciones apuntan al
  lugar exacto.

### 5.2 Árboles de ejemplo

`let total: number = a + b * 2;`
```text
DeclarationKeywordNode (LET, declaredType=NUMBER) [1:1]
├── IdentifierNode "total" [1:5]
└── OperatorNode PLUS [1:23]
    ├── IdentifierNode "a"
    └── OperatorNode STAR
        ├── IdentifierNode "b"
        └── NumberLiteralNode 2
```

`let nombre: string;` → `DeclarationKeywordNode(LET, "nombre", NilExpressionNode, STRING)`.

`x = readInput("Edad: ");`
```text
AssignNode
├── IdentifierNode "x"
└── CallFunctionNode "readInput"
    ├── IdentifierNode "readInput"
    └── StringLiteralNode "Edad: "
```

`if (activo) { println("si"); } else { println("no"); }`
```text
IfKeywordNode
├── condition: IdentifierNode "activo"
├── thenBody: [ CallFunctionNode println( StringLiteral "si" ) ]
└── elseBody: [ CallFunctionNode println( StringLiteral "no" ) ]
```

`println("Total: " + 10 - 4 - 3);` → `CallFunctionNode(println, [ ((("Total: " + 10) - 4) - 3) ])`
(y la semántica lo rechaza: `string - number`).

### 5.3 Qué hace cada herramienta con cada nodo

| Nodo | Semántica | Intérprete | SCA | ASTFormatter |
|---|---|---|---|---|
| Declaration | no redeclarar, tipos | `env.declare` (valor convertido al tipo) | naming convention + analiza expr | `let x: tipo = expr;` |
| Assign | existe, no const, tipos | `env.assign` | analiza expr | `x = expr;` |
| If | condición boolean, scope hijo | ejecuta rama en scope hijo | recorre ramas | `if (c) {` + bloque indentado |
| CallFunction | función existe en la versión | ejecuta builtin | args literal/identificador | `f(a, b);` |
| Operator (expr) | inferencia de tipos | aritmética / concatenación | (es la "expresión" prohibida) | `a + b` |
| Program | recorre | recorre (modo AST) | recorre | une con `\n` |

### 5.4 "Posibilidades" del árbol: cómo se extiende

Si mañana se agrega `while (cond) { ... }`:
1. `common`: `WhileNode` (record) + `visit(WhileNode)` en `NodeVisitor` + `TokenType.WHILE` y la
   entrada `"while"` en `LexemeMatcher`.
2. `parser`: `WhileParser implements StatementParser` con `canStart` = token `while`, y agregarlo en
   una nueva `Version12Strategy` registrada en `VersionStrategyRegistry`. 1.0 y 1.1 **no cambian**.
3. `semantic`: `WhileNodeSemanticHandler` (registrar en la lista).
4. `interpreter`: un `case WhileNode` en `DefaultStatementExecutor`.
5. `sca`/`formatter`: handler nuevo si hace falta; el `TokenStreamFormatter` ya indenta todo lo que
   esté entre llaves.

Agregar una función builtin (ej. `readFile`): una clase `BuiltInFunction`, registrarla en
`DefaultFunctionRegistry` y sumarla a `builtInFunctions()` de la versión correspondiente.

Agregar una regla de formato: una clase `FormattingRule` en la lista + su clave en el loader.
Agregar una regla de lint: un handler o ampliar el existente + su campo en `ScaContext`.

Punto débil a reconocer: agregar un nodo obliga a tocar la interfaz `NodeVisitor` y, por lo tanto,
todos los visitors (el clásico trade-off del Visitor: fácil agregar operaciones, caro agregar tipos).

---

## 6. Versiones 1.0 vs 1.1

| Funcionalidad | Qué la bloquea en 1.0 | Mensaje en 1.0 |
|---|---|---|
| `const` | `Version10Strategy.declarationKeywords()` solo `LET` → ningún parser la acepta | `Unexpected token 'const' … (not valid in PrintScript 1.0)` |
| tipo `boolean` | `supportedDataTypes()` = `STRING`, `NUMBER` | `Unexpected token 'boolean' …` |
| literales `true`/`false` | `primaryParsers()` sin parser de boolean | `Unexpected token 'true' … (not valid in PrintScript 1.0)` |
| `if` / `else` | `statementParsers()` sin `ConditionalParser` | `Unexpected token 'if' …` |
| `readInput` / `readEnv` | **semántica**: `builtInFunctions()` = `{println}` | `Function 'readInput' is not supported in PrintScript 1.0` |

- El **lexer no conoce versiones**: siempre produce los mismos tokens. La versión se decide en
  `ParserFactory` (sintaxis) y `ExpressionTypeInference` (funciones disponibles).
- Todo lo que cambia entre versiones vive en **una sola clase por versión**
  (`VersionXXStrategy`) → **Open/Closed**: agregar 1.2 es agregar una clase y registrarla.
- La CLI toma la versión con `-v 1.0|1.1` (por defecto `1.0`); `Version.fromString` falla con
  cualquier otra cosa.

---

## 7. Manejo de errores y posiciones

### 7.1 `Result` en vez de excepciones

- Errores del **programa del usuario** (léxicos, sintácticos, semánticos, de runtime) viajan como
  `IncorrectResult`. No se usan excepciones para control de flujo.
- Ventajas: el tipo de retorno documenta que algo puede fallar; el compilador obliga a manejarlo
  (`switch` exhaustivo sobre la `sealed interface`); la composición es explícita (estilo *railway*).
- Las excepciones quedan para lo excepcional (I/O, bugs) y se atrapan en los bordes
  (`ExecuteService`, servicios, adapter del TCK) para convertirlas en `Result`.
- Costo: más verborragia (los `((CorrectResult<...>) res).value()` que se ven en el código).

### 7.2 Formato de los mensajes

El pipeline/intérprete agrega **tipo + mensaje + ubicación**. Salida real de la CLI con los archivos
de `samples/errors/`:

```text
Error: Lexical error: unrecognized character '@' at line 2, column 19 [in statement starting at line 2, column 1]
Error: Syntactic error: Unexpected token 'number' from line 2, column 7 to line 2, column 12 [in statement starting at line 2, column 1]
Error: Semantic error: Cannot assign type STRING to variable 'a' of type NUMBER [from line 3, column 1 to line 3, column 6]
Error: Runtime error: Division by zero [from line 3, column 1 to line 3, column 26]
```

- **Sintácticos**: el rango del token problemático (`token.range()`) y dónde empezaba la sentencia.
- **Semánticos y runtime**: el rango **de toda la sentencia** (`statementRange`: desde el primer
  token hasta el último consumido, usando `pointer()` del stream). Así se cumple "fila y columna de
  inicio y fin del problema".
- **SCA**: cada violación con su línea y columna.
- La CLI imprime `Error: ...` y sale con **exit code 1**.

---

## 8. Memoria y streaming

### 8.1 El requisito
"Los códigos fuente pueden ser tan extensos que no es posible contenerlos de forma completa en
memoria… no se puede primero hacer todo el análisis léxico para luego pasar al análisis semántico."

### 8.2 Cómo se cumple

1. **Lectura**: `StreamCharReader` con buffers fijos (8 KB + 1 KB de pushback).
2. **Lexer perezoso**: produce un token solo cuando se lo piden.
3. **`LazyTokenStream`**: lista enlazada que crece hacia adelante a demanda; lo que queda atrás se
   libera si nadie lo referencia.
4. **Parser por sentencia**: `StatementPipeline` y `DefaultInterpreter` parsean, chequean y procesan
   **una sentencia**, y pasan a la siguiente soltando la anterior.
5. **El entorno** guarda solo variables (no código).
6. **Formatter**: escribe cada token apenas lo lee.
7. **Linter**: guarda solo las violaciones.

### 8.3 El caso real: el test de memoria del TCK

`InterpreterLargeFileTest` corre con **heap de 4–6 MB** (`maxHeapSize = "6m"` en el TCK) un programa
de **32.768 líneas** `println("This is a text");`:
- `testWithCounter`: solo cuenta los prints → **no debe haber errores**. Exige streaming de verdad.
- `testWithCollector`: guarda todos los mensajes en una lista → **debe** quedarse sin memoria y
  reportar exactamente `"Java heap space"` (por eso el adapter del TCK atrapa `OutOfMemoryError` y
  normaliza el mensaje).

El problema que hubo (commit `de9a71d`, "prevent token retention in TCK"): aunque el loop avanzaba,
**alguien seguía apuntando a la cabeza de la lista de tokens** (la variable/parámetro con el stream
inicial, viva en el frame del método). Mientras la cabeza es alcanzable, **toda** la lista memoizada
también → el heap explota igual que si se hubiera lexeado todo.

La solución en `DefaultInterpreter.interpret`:
```java
TokenStream[] source = new TokenStream[] {tokenStream};
tokenStream = null;                 // suelto el parámetro
return executeLoop(source, ...);    // y adentro: current = source[0]; source[0] = null;
```
Con eso, la única referencia viva es `current`, que avanza. Por eso el intérprete tiene su propio
loop en vez de reusar `StatementPipeline` (la lambda/los frames intermedios retenían la cabeza).

### 8.4 Dónde **no** hay streaming (y está bien saberlo)
- `SyntacticParser.parseProgram` y `ASTFormatter.format(ProgramNode)` arman el programa entero
  (usados en tests, no en la CLI).
- `TokenStreamSca` junta todos los tokens en una lista (no se usa en la CLI).
- El REPL acumula el bloque tipeado hasta la línea vacía (es interactivo, no aplica).
- Un bloque `if` gigante se mantiene entero en memoria (es una sola sentencia) y el parseo de bloques
  es recursivo (un bloque con muchísimas sentencias podría desbordar el stack).

---

## 9. CLI, comandos y scripts

### 9.1 `CliEngine` (picocli)

```text
Usage: cli-engine [--no-progress] [-c=<configFile>] [-i=<inputFile>]
                  [-o=<outputFile>] [-v=<versionString>] [<operation>]
```

| Parámetro | Significado | Default |
|---|---|---|
| `<operation>` (posicional) | `Validation`/`validate`, `Execution`/`interpret`/`exec`, `Formatting`/`format`/`fmt`, `Analyzing`/`analyze`/`lint` (case-insensitive) | `Execution` si se omite |
| `-i, --input` | archivo fuente | stdin |
| `-c, --config` | YAML/JSON de reglas (format/lint) | reglas por defecto |
| `-o, --output` | archivo de salida | stdout |
| `-v, --version` | `1.0` o `1.1` | `1.0` |
| `--no-progress` | no mostrar `Parsing: NN%` | se muestra si hay `-i` |

Comportamiento:
- **Exit code**: `0` éxito, `1` error (picocli lo devuelve desde `call()` y `main` hace
  `System.exit`).
- **REPL**: si no hay `-i` y stdin está vacío (`System.in.available() == 0`), entra a un REPL:
  acumulás líneas, **línea vacía ejecuta**, `exit` sale. Mantiene `SemanticEnvironment` y
  `Environment` entre bloques (las variables sobreviven).
- **readInput en la CLI**: lee **una línea de stdin** por llamada, con **un único `BufferedReader`**
  para toda la corrida (si se creara uno por llamada se perderían líneas que ya quedaron en el buffer
  del anterior — bug real evitado).
- **stdout seguro**: si no hay `-o`, el `Writer` envuelve `System.out` en un `FilterWriter` cuyo
  `close()` solo hace `flush()` (el try-with-resources no cierra stdout).
- **Progreso**: solo con `-i` (necesita el tamaño del archivo para calcular el %), en stderr.
- **Salida del formatter**: va al `Writer` (stdout o `-o`); el CLI no agrega mensaje de éxito.
- En `exec`, después de la salida del programa se imprime `Program executed successfully`.

### 9.2 Formas de correrla

```bash
# 1) Vía Gradle (desarrollo)
./gradlew :com.ingsis.engine:run --args="Execution -v 1.0 -i samples/exec/v10_ejemplo1.ps"

# 2) Distribución instalada (lo que usaría un usuario)
./gradlew :com.ingsis.engine:installDist
./com.ingsis.engine/build/install/com.ingsis.engine/bin/com.ingsis.engine exec -v 1.1 -i prog.ps

# 3) Atajo run.sh
./run.sh exec 1.0 samples/exec/v10_ejemplo1.ps
```

`installDist` (plugin `application`) genera `bin/` (scripts de arranque sh y bat con el classpath
armado) y `lib/` (todos los jars). La task `run` está configurada con `standardInput = System.in` y
`workingDir = rootProject.projectDir` para que funcionen stdin y las rutas relativas.

### 9.3 `run.sh`

```bash
./run.sh exec     <versión> <archivo.ps>
./run.sh validate <versión> <archivo.ps>
./run.sh format   <versión> <archivo.ps> <reglas.yaml> [salida.ps]
./run.sh sca      <versión> <archivo.ps> <reglas.yaml>     # alias: lint
./run.sh repl     <versión>
```
- **Recompila solo si hace falta**: compara con `find -newer` si algún `.java`, `.gradle` o
  `.groovy` es más nuevo que la carpeta `lib/` de la distribución; si sí, corre
  `./gradlew :com.ingsis.engine:installDist -q` y toca el "stamp".
- Valida que existan los archivos, arma los argumentos (siempre con `--no-progress`) y hace `exec`
  del binario (reemplaza el proceso, así el exit code es el de la CLI).
- `set -euo pipefail`: corta ante cualquier error.

### 9.4 `samples/` y `probar_todo.sh`
- `samples/exec`: ejemplos de la consigna, aritmética, 1.1 (const/if, readInput, readEnv).
- `samples/errors`: un archivo por tipo de error (léxico, sintáctico, semántico, runtime, uso de 1.1
  en 1.0).
- `samples/format` y `samples/sca`: programas "feos" + YAMLs de reglas.
- `samples/PRUEBAS.md`: tabla comando → salida esperada.
- `samples/probar_todo.sh`: corre todo lo anterior (con stdin y variables de entorno cuando hace
  falta) e imprime el exit code de cada uno. Sirve como **demo para la defensa**.

### 9.5 Git hooks
- `.githooks/pre-commit`: `spotlessApply`, `git update-index --again` (re-stagea lo formateado) y
  `check -x test` (lint/análisis sin tests, para que el commit sea rápido).
- `.githooks/pre-push`: `./gradlew test`.
- Se activan con `./install-hooks.sh` o `./gradlew installGitHooks` (ambos hacen
  `git config core.hooksPath .githooks`). Están **versionados** en el repo: todo el equipo usa los
  mismos (los hooks en `.git/hooks` no se versionan).

---

## 10. Gradle a fondo

### 10.1 Piezas
- **Wrapper** (`gradlew`, `gradle/wrapper/gradle-wrapper.properties` → Gradle **9.3.0**): todos
  (y el CI) usan la misma versión sin instalar nada.
- **`settings.gradle`**: nombre del root (`PrintScript`) e `include` de los 8 subproyectos →
  **multi-project build**.
- **`build.gradle` (root)**: no tiene código. Hace tres cosas:
  1. `version` desde `-Pversion` → `RELEASE_VERSION` → `1.0.0`; `group = 'org.example'`.
  2. **Reporte JaCoCo agregado**: en `gradle.projectsEvaluated` configura `jacocoTestReport` del root
     para juntar los `.exec`, clases y fuentes de **todos** los subproyectos (alias:
     `jacocoRootReport`). El `test` del root depende del `test` de todos.
  3. Task `installGitHooks` (tipo `Exec`).
- **`buildSrc/`**: Gradle lo compila automáticamente antes del build y pone sus plugins en el
  classpath de los scripts. Contiene **convention plugins**:
  - *Precompiled script plugins* en Groovy: el **nombre del archivo es el id** del plugin
    (`buildlogic.java-common-conventions.gradle` → `id 'buildlogic.java-common-conventions'`).
  - Un **plugin binario** (`PublishingConventionPlugin.groovy`, clase que implementa
    `Plugin<Project>`) registrado con `META-INF/gradle-plugins/publishing-convention.properties` →
    `id 'publishing-convention'`.

### 10.2 Jerarquía de convention plugins

```text
buildlogic.java-common-conventions
  ├─ plugins: checkstyle, spotless, spotbugs, java, jacoco, pmd, publishing-convention
  ├─ Java 21 (toolchain), JUnit Jupiter 5.10.2, spotbugs-annotations
  ├─ test → finalizedBy jacocoTestReport → finalizedBy jacocoTestCoverageVerification (80%)
  ├─ check → spotlessCheck, spotbugsMain, checkstyleMain, pmdMain, jacocoTestCoverageVerification
  ├─ build → spotlessApply
  └─ reportes HTML en build/custom-reports/...

buildlogic.java-library-conventions      = common + java-library        → common, charstream, lexer,
                                                                            parser, interpreter,
                                                                            formatter, sca
buildlogic.java-application-conventions  = common + application + shadow + picocli → engine
```

Por qué convention plugins (y no `subprojects { }` en el root ni copiar y pegar):
- **DRY**: la configuración de calidad está en un solo lugar.
- **Opt-in explícito**: cada módulo declara qué *es* (librería o aplicación) con una línea.
- **Composición** (library y application reutilizan common) en vez de herencia por configuración
  global.
- `subprojects {}`/`allprojects {}` es el patrón viejo: acopla todo al root y Gradle lo desaconseja.

### 10.3 `PublishingConventionPlugin`
- Aplica `java` y `maven-publish`.
- Resuelve la versión (`-Pversion` → `RELEASE_VERSION` → versión del root) y **nunca publica
  SNAPSHOT** (si termina en `-SNAPSHOT` o no está especificada → `1.0.0`).
- Publicación `gpr` (`from components.java`, `artifactId` = nombre del proyecto en minúscula) al repo
  Maven `https://maven.pkg.github.com/<GITHUB_REPOSITORY>` con credenciales de
  `USERNAME`/`GITHUB_ACTOR`/`gprUser` y `TOKEN`/`GITHUB_TOKEN`/`gprToken`.
- Como lo aplica el common convention, **los 8 módulos se publican** como artefactos separados
  (`org.example:com.ingsis.<módulo>:<versión>`).

### 10.4 El `build.gradle` del engine
- `application { mainClass = 'engine.CliEngine' }`.
- **Source set extra `containerTest`** (`src/containerTest/java`) con sus propias configuraciones que
  extienden las de test, más Testcontainers 2.0.5.
- Task `containerTest` (tipo `Test`): depende de `installDist`, le pasa la ruta de la distribución
  por system property (`printscript.dist`), corre después de `test`, y tiene **JaCoCo deshabilitado**
  (la CLI corre en otra JVM dentro del contenedor; medir cobertura ahí solo mediría el harness).
  `check` depende de `containerTest`.

### 10.5 Árbol de tasks (lo que pasa cuando corrés…)

```text
./gradlew build
├── assemble ── jar (por módulo), distZip/distTar (engine), shadowJar (engine)
├── check
│   ├── test ──finalizedBy──► jacocoTestReport ──finalizedBy──► jacocoTestCoverageVerification
│   ├── spotlessCheck
│   ├── checkstyleMain / checkstyleTest
│   ├── pmdMain / pmdTest
│   ├── spotbugsMain / spotbugsTest
│   ├── jacocoTestCoverageVerification
│   └── containerTest (solo engine; requiere installDist; se saltea sin Docker)
└── spotlessApply   (build depende de él: ¡build reformatea el código!)
```

Fases de Gradle (pregunta clásica): **inicialización** (lee `settings.gradle`, decide qué proyectos
hay) → **configuración** (evalúa todos los `build.gradle`, arma el grafo de tasks — acá corre
`projectsEvaluated`) → **ejecución** (corre solo las tasks pedidas y sus dependencias, saltando las
*up-to-date* por inputs/outputs).

### 10.6 `implementation` vs `api` vs `testImplementation`
- `implementation`: en el classpath de compilación y runtime del módulo, **no** expuesto en
  compilación a quien lo consume (en el POM queda con scope `runtime`).
- `api` (requiere `java-library`): se expone transitivamente. No se usa.
- `testImplementation`: solo para tests (ej. interpreter usa lexer/charstream solo en tests).

### 10.7 Versiones de herramientas

| Herramienta | Versión |
|---|---|
| Gradle | 9.3.0 |
| Java (toolchain) | 21 |
| JUnit Jupiter (módulos) | 5.10.2 |
| Spotless plugin / google-java-format | 6.25.0 / 1.22.0 (AOSP) |
| Checkstyle | 10.15.0 |
| PMD | la que trae Gradle por defecto |
| SpotBugs plugin / SpotBugs | 6.0.7 / 4.8.3 |
| JaCoCo | 0.8.11 |
| Shadow | 8.3.5 (`com.gradleup.shadow`, aplicado con el id viejo) |
| picocli | 4.7.7 |
| SnakeYAML | 2.2 |
| Testcontainers | 2.0.5 |

---

## 11. Calidad de código

| Herramienta | Qué analiza | ¿Rompe el build? | Config |
|---|---|---|---|
| **Spotless** | formato del código fuente | `spotlessCheck` sí | google-java-format AOSP (4 espacios), reflow de strings largos, imports sin usar, orden de imports, header de licencia, newline final; `.md`/`.gitignore` sin espacios finales |
| **Checkstyle** | reglas de estilo/diseño sobre el código fuente | **sí** (`maxWarnings = 0`, `ignoreFailures = false`) | `.checkstyle.xml` |
| **PMD** | code smells sobre el código fuente | no (`ignoreFailures = true`) | reglas por defecto del plugin |
| **SpotBugs** | bugs probables sobre el **bytecode** | no (`ignoreFailures = true`), esfuerzo MAX, umbral LOW | `config/spotbugs/exclude.xml` (vacío) |
| **JaCoCo** | cobertura de tests | **sí**, < 80% falla | global 80% + por clase 80% de líneas (excluye `*.Main`, `*.config.*`) |

### Reglas de Checkstyle (y su impacto en el diseño)
1. `OuterTypeFilename`: el archivo se llama como la clase.
2. **`MethodLength` máx. 20 líneas** (sin contar vacías) → explica los muchos métodos privados chicos
   y por qué algunos recorridos son recursivos.
3. `OneStatementPerLine`.
4. **`ParameterNumber` máx. 4** (ignora `@Override`) → explica los *parameter objects*:
   `DeclarationInfo`, `ConditionalBlock`, `LiteralParsers`, `Delimiters`, `CliStreams`,
   `SpacingRules`, `FormattingRulesData`.
5. **`NestedIfDepth` máx. 2** → *early returns* y `switch` con pattern matching.

Supresiones (`config/checkstyle/suppressions.xml`): `MethodLength` en `*Test.java` y `ParameterNumber`
en `FormatContext.java` (el record tiene 13 componentes; un record canónico no se puede partir).

¿Por qué PMD y SpotBugs no rompen? Reportan *posibles* problemas con falsos positivos (SpotBugs en
nivel LOW es muy ruidoso, ej. "expone representación interna" en records). Se dejaron como
**informativos** (los reportes se suben en CI) y se hicieron obligatorios los que tienen criterio
objetivo: formato, reglas de estilo de la cátedra y cobertura.

---

## 12. Reportes

| Reporte | Dónde queda (por módulo salvo indicación) | Cómo se genera |
|---|---|---|
| Tests JUnit (HTML) | `<módulo>/build/reports/tests/test/index.html` | `./gradlew test` |
| Resultados JUnit (XML) | `<módulo>/build/test-results/test/*.xml` | `./gradlew test` |
| JaCoCo por módulo | `<módulo>/build/custom-reports/jacoco/html/index.html` (+ `jacoco.xml`), copiado a `build/reports/jacoco/` | automático después de `test` |
| JaCoCo agregado | `build/custom-reports/jacoco-aggregate/html/index.html` y `build/reports/jacoco-aggregate/` (root) | `./gradlew jacocoRootReport` |
| Checkstyle | `<módulo>/build/custom-reports/checkstyle/checkstyleMain.html` | `checkstyleMain` |
| PMD | `<módulo>/build/custom-reports/pmd/pmdMain.html` | `pmdMain` |
| SpotBugs | `<módulo>/build/custom-reports/spotbugs/spotbugsMain.html` | `spotbugsMain` |
| Container tests | `com.ingsis.engine/build/reports/tests/containerTest/` | `containerTest` |

- En CI, `ci.yml` sube **todo** (`**/build/reports/` y `**/build/custom-reports/`) como artefacto
  `reports` con `if: always()` → se puede bajar el reporte **aunque el pipeline haya fallado**
  (justamente cuando más se necesita).
- Se fuerzan HTML (para humanos) y XML de JaCoCo (para herramientas como Codecov/Sonar); se apaga CSV.

---

## 13. Tests

### 13.1 Números

| Módulo | Tests (`@Test`/`@ParameterizedTest` en el código) |
|---|---|
| common | 31 |
| charstream | 15 |
| lexer | 29 |
| parser | 127 |
| interpreter | 178 |
| formatter | 137 |
| sca | 111 |
| engine | 70 (unit + integration + e2e + container) |

Última corrida: **703 tests ejecutados, 0 fallas, 0 salteados** (los parametrizados suman casos).

### 13.2 Estructura (igual en todos los módulos)

```text
src/test/java/<módulo>/
├── unit/          una clase aislada, colaboradores reemplazados por dobles
├── integration/   varias piezas reales juntas (ej. lexer+parser+semántica con código fuente real)
├── fake/          dobles de prueba hechos a mano (Fake, Stub, Spy)
├── support/       builders y fixtures (ej. FormatContextBuilder, Sources)
└── e2e/           (engine) la CLI completa en el mismo proceso (CliRunner captura stdout/stderr)
src/containerTest/java/engine/container/   (engine) la distribución instalada corriendo en Docker
```

### 13.3 Convenciones
- **Given / When / Then** en tres lugares: el nombre (`cli_whenExecutingAssignmentExample_thenPrints…`),
  el `@DisplayName("Given: … | When: … | Then: …")` y comentarios `// Given // When // Then`.
- **Dobles de prueba a mano, sin Mockito**: `FakeInputProvider`, `FakeEnvProvider`, `SpyOutput`,
  `StubExpressionEvaluator`, `SpyProgressListener`, `FailingWriter`, etc. Funciona porque todo se
  inyecta por constructor y las interfaces son chicas (ISP). Ventajas: tests legibles, sin "magia",
  sin sobre-especificar interacciones, dobles reutilizables.
  - *Fake*: implementación simplificada que funciona (ej. registry en memoria).
  - *Stub*: devuelve respuestas fijas.
  - *Spy*: registra lo que le pasó para verificarlo después.

### 13.4 Pirámide
```text
          ▲  container (pocos, lentos: Docker + JRE limpio + launcher real)
         ▲▲  e2e (CLI completa in-process)
       ▲▲▲▲  integration (pipeline real con fuentes PrintScript)
   ▲▲▲▲▲▲▲▲  unit (la mayoría, rápidos)
```

### 13.5 Container tests (Testcontainers)
- Levantan `eclipse-temurin:21-jre`, copian la distribución de `installDist` a `/app` y ejecutan
  `sh /app/bin/com.ingsis.engine …` con un archivo como stdin y variables de entorno
  (`GREETING`, `RETRIES`) para probar `readEnv`.
- Es **caja negra**: prueba lo que realmente se entrega (scripts de arranque, classpath, encoding),
  aislado de la máquina del dev (sin IDE, sin Gradle, sin variables locales).
- `@Testcontainers(disabledWithoutDocker = true)`: sin Docker se saltean en vez de fallar.
- `PrintScriptCli` es un helper (*Page Object* para CLI): `withProgram`, `withConfig`, `run`,
  `readFile`.

### 13.6 Más allá: el TCK
Los tests propios se complementan con el **TCK** de la cátedra (§17), que valida compatibilidad con
golden files y el test de memoria.

---

## 14. CI/CD

### 14.1 `.github/workflows/ci.yml` — "CI Pipeline"
- **Disparadores**: push y PR a `main` y `dev`, y manual (`workflow_dispatch`).
- Un job en `ubuntu-latest`:
  1. `actions/checkout@v4`
  2. `actions/setup-java@v4` (Temurin 21, `cache: gradle`)
  3. `chmod +x gradlew`
  4. `spotlessCheck` — formato
  5. `checkstyleMain checkstyleTest pmdMain pmdTest spotbugsMain spotbugsTest` — análisis
  6. `assemble` — compila y empaqueta
  7. `test jacocoTestReport` — tests + cobertura
  8. `jacocoTestCoverageVerification` — falla si < 80%
  9. `containerTest` — Docker (en los runners de GitHub hay Docker disponible)
  10. `upload-artifact` de reportes con `if: always()`

### 14.2 `.github/workflows/publish.yml` — "CI & Publish Packages"
- **Disparadores**: push a `main`, push de tags (`v*`, `*.*`, `[0-9]*`), release publicada y manual
  con input `version`.
- **Job `ci`**: spotlessCheck → `check -x test` → `test`.
- **Job `publish`** (`needs: ci`, permisos `contents: read`, `packages: write`):
  1. **Determina una versión inmutable**:
     - manual con input → ese valor; release → `tag_name`; tag → nombre del tag; push a main →
       `1.0.<run_number>`.
     - saca la `v` inicial (`v1.2.0` → `1.2.0`), **rechaza SNAPSHOT**, avisa si no es semver.
  2. `./gradlew publish -Pversion=<v>` con `GITHUB_TOKEN` (token automático de la Action, no hace
     falta un PAT) → los 8 módulos a **GitHub Packages**.
- **Inmutabilidad**: GitHub Packages no deja re-publicar la misma versión (409 Conflict). Por eso:
  nunca SNAPSHOT, y si algo salió mal se publica la siguiente versión de parche.
- **Doble disparo conocido**: si pusheás el tag y después creás la Release, corren dos publicaciones
  de la misma versión; la segunda falla con 409 (pasó con `v1.2.0`). Los paquetes quedan bien
  (los publicó la primera). La solución sugerida es sacar el trigger `release` (ver README_DEV §6.4.6).
- Tag vs Release: el **tag** es un puntero inmutable de git a un commit; la **Release** es una
  entidad de GitHub encima del tag (notas, binarios adjuntos, "Latest").

### 14.3 Flujo de una release
```bash
VERSION=1.3.0
git tag -a "v$VERSION" -m "Release $VERSION"
git push origin "v$VERSION"                      # dispara publish.yml → GitHub Packages
gh release create "v$VERSION" --verify-tag --generate-notes   # (dispara de nuevo → 409 esperable)
```

### 14.4 Cómo se consume lo publicado
```groovy
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/IngsisPrintScript2026/PrintScript")
        credentials { username = ...; password = ... }   // GitHub Packages exige auth incluso para leer
    }
}
dependencies { implementation 'org.example:com.ingsis.engine:1.2.0' }
```

---

## 15. El repo `workflows-and-actions`

Repo de la organización `IngsisPrintScript2026` que centraliza **workflows reutilizables**
(`on: workflow_call`), hooks, config de Checkstyle y JaCoCo para todos los proyectos (PrintScript y
los microservicios del Snippet Searcher).

| Archivo | Para qué |
|---|---|
| `.github/workflows/ci.yml` | CI genérico: credenciales de Gradle para GitHub Packages, `spotlessApply`, `clean build`, `jacocoTestCoverageVerification` |
| `.github/workflows/printscript-ci.yaml` | CI pensado para PrintScript con **jobs en paralelo** (`setup` → `format`, `analysis`, `test`) y cache de Gradle por hash de los `.gradle` |
| `.github/workflows/deploy.yaml` | publicar módulos Java + build/push de imagen Docker a GHCR + prender la VM de Azure + deploy por SSH con `docker compose` |
| `.github/workflows/build-ui.yaml` | build de la UI (Vite + Auth0) y deploy |
| `config/jacoco.gradle`, `config/checkstyle/checkstyle.xml` | estándares compartidos (para aplicar con `apply from:`) |
| `.githooks/`, `scripts/install-hooks.sh` | hooks comunes |

Un repo los usa así:
```yaml
jobs:
  ci:
    uses: IngsisPrintScript2026/workflows-and-actions/.github/workflows/ci.yml@main
    secrets: inherit
```

Relación con PrintScript: **el repo PrintScript tiene sus propios workflows autocontenidos** (no
llama a los reutilizables). Motivo defendible: PrintScript tiene necesidades propias (container
tests, la publicación de 8 artefactos con versionado inmutable, reglas de la cátedra en su propio
`.checkstyle.xml`) y es la **librería** que consumen los demás; los reutilizables sirven sobre todo a
los microservicios, que comparten exactamente el mismo flujo (build → imagen → deploy).

---

## 16. Por qué steps en Actions y no una task de Gradle

La pregunta: ¿por qué el CI llama a varias tasks de Gradle como **steps separados**
(`spotlessCheck`, luego checkstyle/pmd/spotbugs, luego `assemble`, `test`, cobertura,
`containerTest`), y la resolución de versión está en un step de bash, en vez de definir una única
task de Gradle tipo `./gradlew ciPipeline` (o solo `./gradlew check publish`)?

**Principio**: Gradle define **qué** es construir y verificar (las tasks, que son las mismas en local
y en CI). El workflow define **cuándo, dónde, en qué orden, con qué permisos y con qué secretos**.
Cada uno hace lo suyo y no se duplica lógica: los steps solo **invocan** tasks que ya existen.

Razones concretas:
1. **Visibilidad y diagnóstico**: cada step aparece en la UI de Actions con su log, duración y
   ✅/❌. Se ve de un vistazo si falló el formato, el lint, la compilación, los tests o la cobertura,
   sin bucear en un log único de una task gigante.
2. **Fail-fast ordenado por costo**: lo barato primero (Spotless tarda segundos) y lo caro al final
   (tests, Docker). Si el formato está mal, no se gastan minutos de runner.
3. **Cosas que Gradle no puede/debe hacer**:
   - `if: always()` para subir reportes aunque haya fallado algo.
   - `needs:` entre jobs (publicar **solo** si el CI pasó) y **permisos por job** (`packages: write`
     solo en `publish`, principio de mínimo privilegio).
   - Acceso al **contexto del evento** (tag, release, `run_number`, inputs del dispatch): la versión
     depende de *por qué* corrió el pipeline, información que solo existe en Actions. Por eso la
     resolución de versión es un step y no código en Gradle (Gradle solo recibe el resultado por
     `-Pversion`).
   - Manejo de **secrets** (`GITHUB_TOKEN`) acotado al step que los necesita.
   - Paralelismo entre jobs y cache (`printscript-ci.yaml` corre format/analysis/test en paralelo).
4. **Local y CI quieren cosas distintas**: en local conviene una sola puerta (`./gradlew check`, el
   hook pre-commit con `check -x test`); en CI conviene granularidad. Con tasks estándar sirven ambos
   usos sin tasks "especiales de CI".
5. **Reutilización entre repos**: un workflow reutilizable (`workflow_call`) se comparte entre
   proyectos de la organización; una task custom de Gradle habría que copiarla en cada build.

Lo que **sí** quedó en Gradle (y por qué): todo lo que es *lógica de build* y debe poder correrse
igual en cualquier máquina: formato, linters, cobertura mínima, containerTest, publicación
(`maven-publish`), hasta `installGitHooks`. Regla mental: *si lo tiene que poder correr un dev en su
máquina, es una task de Gradle; si depende de GitHub (eventos, permisos, secretos, artefactos), es un
step del workflow.*

Costos (para no quedar como que no los ves):
- Varias invocaciones de Gradle = algo de overhead de arranque (mitigado con el daemon y
  `cache: gradle` de `setup-java`).
- Riesgo de **desalineación** entre lo que corre `check` en local y lo que lista el yml (hay que
  mantener ambos).

---

## 17. El `printscript--tck`

### 17.1 Qué es
**TCK = Technology/Test Compatibility Kit**: un repo de la cátedra (fork en
`~/Faculty/ingsis/printscript--tck`) con casos de prueba (programas `.ps`, salidas esperadas, golden
files de formato y configs) que cualquier implementación de PrintScript tiene que pasar. Valida
**compatibilidad con la especificación**, independientemente de cómo esté implementado.

### 17.2 Cómo se conecta con nuestro proyecto

```text
PrintScript (este repo) ──publish.yml──► GitHub Packages (org.example:com.ingsis.*:<versión>)
                                                 │
printscript--tck/build.gradle ───────────────────┘  dependencias "latest.release" (o -PprintscriptVersion=X)
        │
        └─ CustomImplementationFactory (ADAPTER) ─► ExecuteService / FormatService / LintService
```

- El TCK define interfaces propias: `PrintScriptInterpreter.execute(src, version, emitter, handler,
  provider)`, `PrintScriptFormatter.format(src, version, config, writer)`,
  `PrintScriptLinter.lint(src, version, config, handler)`, y una `PrintScriptFactory`.
- Nosotros completamos `CustomImplementationFactory` = **patrón Adapter**: traduce sus interfaces a
  nuestro `Engine`/servicios (`PrintEmitter::print` → `OutputEmitter`, `InputProvider::input` →
  `InputSupplier`, `IncorrectResult` → `ErrorHandler.reportError`).
- Atrapa `OutOfMemoryError` y reporta exactamente `"Java heap space"` (lo exige el test de memoria);
  cualquier otro `Throwable` se reporta como error.
- Depende de **los 8 artefactos** porque usa clases de varios módulos (`Version` y `Result` de common,
  servicios de engine) y nuestras dependencias son `implementation` (no transitivas en compilación).
- Versión: `latest.release` (versión dinámica de Gradle: la release más alta publicada) o fija con
  `-PprintscriptVersion=1.2.0`. Antes usaba `1.0-SNAPSHOT` desde `mavenLocal()`; se pasó a releases
  publicadas para que el TCK pruebe **lo mismo que se entrega**.
- Credenciales: GitHub Packages exige auth incluso para leer → `gprUser/gprToken` o
  `USERNAME/TOKEN`/`GITHUB_TOKEN`.

### 17.3 Suites
| Suite | Qué hace |
|---|---|
| `print-statement/1.0`, `1.1` | ejecuta `main.ps` (con `input.txt` si hay) y compara los prints con `output.txt` (aritmética, decimales, concatenación, if/else, readInput, readEnv) |
| `validation/1.0`, `1.1` | archivos `valid-*.ps` no deben reportar errores, `invalid-*.ps` sí |
| `formatter/1.0`, `1.1` | `main.ps` + `config.json` → la salida debe ser **idéntica** a `golden.ps` (los de 1.0 también se corren en 1.1) |
| `linter/1.0`, `1.1` | errores reportados según config (camel/snake, println/readInput con expresión) |
| `InterpreterLargeFileTest` | 32K líneas con 6 MB de heap (§8.3) |

- El test de `read-env` usa la variable `BEST_FOOTBALL_CLUB=San Lorenzo` que setea el `build.gradle`
  del TCK en la task `test`.
- El TCK usa **JUnit 4** con `Parameterized` y Hamcrest (es independiente de nuestro JUnit 5).
- Se corre con `./gradlew build` (o `test`) dentro del TCK.

### 17.4 Cambios hechos en nuestro código "por el TCK" (historial)
- `e294e04 Passing TCK`: aparece el `TokenStreamFormatter` con reglas, el `ValidationService`, ajustes
  de declaraciones y del parser.
- `ca6c53e fix(formatter): align output with the PrintScript TCK`: respetar la indentación original
  si no se configura, ajustes de espacios.
- `de9a71d fix(interpreter): … prevent token retention in TCK`: el truco del `TokenStream[]` (§8.3).

---

## 18. Patrones de diseño y SOLID

| Patrón | Dónde | Para qué |
|---|---|---|
| **Chain of Responsibility** | `AbstractTokenTypeMatcher` + `LexemeMatcher → Number → Boolean → String → Identifier` | clasificar un lexema; agregar un tipo de token = un eslabón |
| **Iterator (persistente/inmutable)** | `SafeIterator` + `IterationStep` (CharStream, Lexer, TokenStream) | streaming pull y backtracking |
| **Strategy** | `VersionStrategy`, `DeclarationSymbolStrategy`, `ConditionalElseStrategy`, `FormattingRule` | variar comportamiento sin `if`s gigantes |
| **Registry** | `VersionStrategyRegistry`, `DefaultFunctionRegistry`, mapas de handlers por clase | buscar la implementación por clave |
| **Visitor** (doble dispatch) | `NodeVisitor` en `SemanticChecker`, `ASTFormatter`, `ASTSca` | agregar operaciones sobre el AST sin tocar los nodos |
| **Handler por nodo** (Visitor + Registry) | `SemanticNodeHandler`, `FormatNodeHandler`, `ScaNodeHandler` | una clase chica por regla/nodo (SRP, OCP) |
| **Composite** | `Node.children()` | tratar el árbol uniformemente |
| **Factory** | `NodeFactory`, `ParserFactory`, `ASTFormatter.fromYamlConfig` | centralizar la construcción |
| **Null Object** | `NilExpressionNode`, `ProgressListener.NONE`, `StatementAction.NONE` | evitar `null` y `if (x != null)` |
| **Decorator** | `ProgressCharReader` sobre `CharReader` | agregar conteo de progreso sin tocar la lectura |
| **Observer** | `ProgressListener` / `ConsoleProgressReporter` | notificar avance |
| **Adapter** | `StreamCharReader` (Reader→CharReader), `TokenStreamAdapter` (List→TokenStream), `CustomImplementationFactory` (TCK) | compatibilizar interfaces |
| **Facade** | `Engine` / `ExecuteService` | una API simple sobre todo el sistema |
| **Pratt parser** (algoritmo) | `OperatorParser` | precedencia y asociatividad con datos (binding powers) |
| **Parser combinators** (estilo) | `Parser<T>`: stream → `Result<(nodo, stream)>` | componer parsers chicos |
| **Command** (vía picocli) | `CliEngine implements Callable<Integer>` | encapsular la ejecución de la CLI |
| **Result / Either** | `Result<T>` sealed | errores como valores |

SOLID, con ejemplo concreto:
- **S**: cada handler valida/formatea/analiza un tipo de nodo; cada `FormattingRule` es una regla;
  `PositionTracker` solo sabe de posiciones.
- **O**: nueva versión = nueva `VersionStrategy`; nueva regla = nueva clase en la lista; nueva
  función = nueva `BuiltInFunction`. No se modifica lo existente.
- **L**: cualquier `TokenStream` (Lazy o Adapter) o `CharReader` (Stream o Progress) es
  intercambiable; los tests lo aprovechan.
- **I**: interfaces mínimas (`CharReader` 2 métodos, `InputProvider` 1, `StatementAction` 1,
  `ProgressListener` 1 + defaults).
- **D**: el intérprete depende de `StatementExecutor`, `ExpressionEvaluator`, `FunctionRegistry`,
  `InputProvider` (abstracciones) inyectadas por constructor; el engine las concreta.

Cohesión y acoplamiento a nivel módulo: cada módulo tiene una sola razón de cambio (lexer = reglas
léxicas, formatter = estilo…); las herramientas no se conocen entre sí; todo lo compartido baja a
`common`.

Java moderno que se aprovecha (Java 21): `record`, `sealed interface`, pattern matching en `switch`
y `instanceof`, **record patterns** (`case CorrectResult<Token>(Token t)`), text blocks en tests,
`List.copyOf`.

---

## 19. Decisiones de diseño

Formato: **Decisión** — alternativa descartada — por qué — costo.

1. **Pipeline lazy pull, sentencia por sentencia** — lexear todo y armar el AST completo — la consigna
   prohíbe cargar todo en memoria y el TCK lo mide con 6 MB — más complejidad (iteradores
   persistentes, cuidado con referencias retenidas).
2. **Iteradores inmutables (`IterationStep` con el "siguiente iterador")** — iterador clásico con
   cursor mutable — backtracking gratis en el parser y razonamiento más simple — muchos objetos
   chicos (baratos para el GC moderno).
3. **`LazyTokenStream` memoizado** — buffer circular de k tokens — lookahead arbitrario + persistencia
   — riesgo de retener la cabeza (resuelto, §8.3).
4. **`Result` sellado en vez de excepciones** — excepciones checked/unchecked — errores del usuario
   son esperables, se vuelven parte del tipo, switch exhaustivo — verborragia de casts.
5. **Maximal munch + cadena de matchers** — lexer escrito a mano con un `switch` gigante, o
   generadores (ANTLR) — extensible, cada matcher es simple — reprocesa el prefijo en cada paso
   (irrelevante para tokens cortos).
6. **Pratt parser para expresiones** — una función por nivel de precedencia (descenso recursivo
   clásico) — precedencia como **datos** (binding powers), agregar un operador = una línea en el enum
   — algoritmo menos conocido (hay que saber explicarlo).
7. **Despacho predictivo con `canStart`** — probar parsers en orden y quedarse con el primero que no
   falle — mensajes de error específicos y sin trabajo desperdiciado — cada parser tiene que saber
   reconocerse.
8. **Versionado con Strategy** — `if (version == 1.1)` desparramados — OCP, todo lo de una versión
   en una clase — dos mecanismos (sintáctico y semántico) a mantener alineados.
9. **Semántica separada del parser y antes de ejecutar cada sentencia** — chequear tipos durante la
   ejecución — permite `Validation` sin ejecutar y que el SCA reuse el chequeo — dos entornos
   (tipos y valores).
10. **`SemanticEnvironment` inmutable** — mutable como el de runtime — fácil volver atrás (REPL,
    scopes de bloque) — `define` copia el mapa (O(n) por declaración).
11. **Visitor + handlers registrados por clase** — un `switch` por herramienta — agregar reglas sin
    tocar el visitor central; el intérprete usa `switch` con pattern matching porque ahí la operación
    es una sola y es más directo.
12. **Formatter por tokens (CLI) y por AST (alternativa)** — solo AST — memoria y fidelidad al
    original (TCK) — no valida sintaxis; dos implementaciones.
13. **`FormatContext` con `Boolean` nullable** — `boolean` con defaults — distinguir "no configurado"
    de "apagado" — más checks de null (encapsulados en getters).
14. **BigDecimal + DECIMAL64** — `double` — exactitud decimal y salida "linda" como TS (`3` y no
    `3.0`) — más lento (irrelevante acá).
15. **División por cero = error de runtime** — devolver `Infinity` como TS — más útil para el usuario
    del lenguaje — difiere de TypeScript.
16. **CLI con picocli** — parsear `args[]` a mano — parsing, validación, usage y conversión de tipos
    gratis — una dependencia más.
17. **Multi-módulo Gradle + convention plugins en `buildSrc`** — un solo módulo / `subprojects {}` —
    módulos con responsabilidad clara, build DRY — más archivos de build.
18. **Publicar los 8 módulos a GitHub Packages con versiones inmutables** — distribuir un fat jar /
    SNAPSHOTs — el TCK y otros servicios consumen artefactos versionados y reproducibles — cada
    corrección es una versión nueva.
19. **Dobles de test a mano** — Mockito — legibilidad y diseño para la testabilidad — hay que
    escribirlos.
20. **Container tests sobre la distribución instalada** — solo tests in-process — prueban lo que se
    entrega (scripts, classpath) en un JRE limpio — más lentos y requieren Docker.

---

## 20. Limitaciones conocidas e inconsistencias

Mejor conocerlas vos antes de que te las marquen. Para cada una, una respuesta corta.

**Del lenguaje / implementación**
- **No hay paréntesis en expresiones**: `(1 + 2) * 3` da `Syntactic error: Unexpected token '('`.
  Los primarios no incluyen agrupación. *La consigna no lo pide; agregarlo es un primary parser más
  (`(` expr `)`).*
- **No hay menos unario** (`-5`).
- `println` como expresión (ej. `let x: string = println("a");`) pasa la semántica (se infiere STRING)
  y falla recién en runtime ("Unsupported function call").
- `=` está en `OperatorType` (ASSIGNATION); dentro de una expresión `a = 3` se parsea como operador.
  A nivel sentencia no molesta porque `AssignParser` lo captura antes.
- Redeclarar en un bloque una variable de afuera es error ("already declared"): no hay *shadowing*.
- En el REPL, `readInput` devuelve `""` (no lee del teclado).
- Los bloques se parsean recursivamente (muchísimas sentencias en un `if` → riesgo de stack overflow).
- El formatter de la CLI no valida sintaxis.
- Si el YAML/JSON de configuración está mal, se usan defaults **en silencio** (podría reportarse).
- `ASTSca` guarda `currentEnv` en un campo: no es thread-safe (no importa en la CLI).

**De la CLI**
- `mixinStandardHelpOptions = true`, pero `--help` responde `Unknown option` (el `-v/--version` propio
  choca con el `--version` estándar de picocli). El usage se ve igual ante cualquier opción inválida.
- En `exec` se imprime `Program executed successfully` al final de la salida del programa.

**Código muerto / restos**
- `com.ingsis.parser/src/main/java/Parser.java` (paquete por defecto), `semantic.SemanticStep`,
  `tokenstream.version.GrammarRules`, `environment.Scope`, `state.State`, `LinesAfterPrintlnRule`
  (fuera de la lista por defecto), `TokenStreamSca` y `ASTFormatter` (no conectados a la CLI),
  `buildSrc/.../com.ingsis.java-conventions.gradle`.

**Del build**
- `group = 'org.example'` y header de licencia `My Project`: restos del template de Gradle.
- El root aplica JUnit BOM **6.0.0** y los módulos usan Jupiter **5.10.2** (el root no tiene código,
  pero es inconsistente).
- `build` depende de `spotlessApply`: correr `build` **modifica** archivos.
- PMD y SpotBugs con `ignoreFailures = true` (informativos).
- Hay un cambio sin commitear en `com.ingsis.engine/build.gradle` (JaCoCo deshabilitado en
  `containerTest`).

**De la documentación (READMEs desactualizados)**
- `README.md` dice que `run.sh` acepta `interpret`, `fmt`, `analyze` y que si el primer argumento es
  una versión asume `exec`: **`run.sh` no hace eso** (acepta `exec`, `validate`, `format`, `sca`,
  `lint`, `repl`). Los alias sí existen en la CLI Java.
- `README.md` muestra tasks `com.ingsis.syntactic`/`com.ingsis.semantic` (módulos que ya no existen:
  se unificaron en `com.ingsis.parser`).
- `buildSrc/README.md` dice `MethodLength` 15, `MatchXpath` y PMD `errorprone`: lo real es 20, sin
  `MatchXpath`, y PMD con reglas por defecto.

---

## 21. Preguntas probables de defensa

**¿Por qué tantos módulos? ¿No es sobre-ingeniería?**
La consigna pide módulos que dependan entre sí y un ecosistema que crezca. Cada módulo tiene una
responsabilidad y una razón de cambio; las tres herramientas se pueden usar por separado (el TCK y
otros servicios consumen artefactos individuales). El grafo es un DAG con `common` como base estable.

**¿Cómo garantizan que no se carga todo el archivo en memoria?**
Pipeline pull: el parser pide tokens, el lexer pide chars; se procesa una sentencia por vez y lo
anterior queda sin referencias. Lo prueba el test del TCK con 6 MB de heap y 32K líneas. Contar el
bug de la cabeza retenida y el arreglo (§8.3) suma muchísimo.

**¿Qué es maximal munch?**
Tomar siempre el token más largo posible: seguir leyendo mientras el texto sea token válido o prefijo
de uno, y quedarse con el último válido, devolviendo el lookahead sobrante. Por eso `letter` es un
identificador y no `let` + `ter`.

**¿Cómo resuelven precedencia y asociatividad?**
Pratt parser con binding powers: `*`/`/` (21,22) ligan más que `+`/`-` (11,12); `lbp < rbp` da
asociatividad a izquierda. Hacer la traza de `10 - 4 - 3`.

**¿Por qué el lexer no conoce versiones?**
Porque los tokens son los mismos; lo que cambia es qué combinaciones son válidas. Así el lexer no
cambia entre versiones y la diferencia vive en `VersionStrategy` (sintaxis) y en las builtins por
versión (semántica).

**¿Cómo agregarían PrintScript 1.2?**
Una `Version12Strategy` registrada en el registry (+ los parsers/handlers/nodos nuevos que haga
falta). 1.0 y 1.1 no se tocan (§5.4).

**¿Por qué `Result` y no excepciones?**
Los errores del programa del usuario son esperables; modelarlos como valores los hace explícitos en
la firma y el `switch` exhaustivo sobre la interfaz sellada obliga a manejarlos.

**¿Cómo funciona `readInput` con tipos?**
La semántica lo tipa como "se adapta" (null); en runtime se evalúa con el tipo destino (el declarado o
el de la variable asignada; `string` dentro de `println`) y se convierte; si no se puede → error de
runtime. La entrada viene de un `InputProvider` inyectado (stdin en la CLI, fake en tests, el
provider del TCK).

**¿Cómo se muestra el progreso y por qué es del parsing?**
Un Decorator sobre el lector de caracteres cuenta lo que consume el lexer; como el lexer solo avanza
cuando el parser pide tokens, el porcentaje refleja el avance del parsing. Se dibuja en stderr con
`\r` y se borra antes de cada `println`.

**¿Por qué el formatter de la CLI no usa el AST?**
Memoria y fidelidad (§4.7). Existe `ASTFormatter` como alternativa sobre el AST.

**¿Qué rompe el build y qué no?**
Rompen: Spotless (check), Checkstyle, cobertura < 80% y tests. No rompen: PMD y SpotBugs (se reportan).

**¿Por qué 80% de cobertura y por clase?**
El global solo puede esconder clases sin tests; la regla por clase obliga a que cada clase esté
cubierta. Se excluyen `Main`/config.

**¿Qué diferencia hay entre unit, integration, e2e y container test acá?**
Unit: una clase con dobles. Integration: piezas reales juntas con código PrintScript. E2E: la CLI
entera en el proceso. Container: la distribución instalada, en un JRE limpio en Docker, caja negra.

**¿Por qué no Mockito?**
Inyección por constructor + interfaces chicas hacen que los dobles a mano sean triviales, legibles y
reutilizables; evita tests acoplados a interacciones internas.

**¿Qué hace `buildSrc` y qué es un convention plugin?**
§10.1–10.2. Es la forma recomendada por Gradle de compartir configuración entre subproyectos.

**¿Qué diferencia hay entre `implementation` y `api`?** §10.6 (y por qué el TCK declara los 8).

**¿Por qué no SNAPSHOT? ¿Qué pasa si publican dos veces la misma versión?**
GitHub Packages es inmutable: la segunda da 409. Releases inmutables = builds reproducibles; si hay
un error, se publica la siguiente versión de parche.

**¿Por qué pasos separados en Actions y no una task de Gradle?** §16.

**¿Qué es el TCK y cómo lo conectaron?** §17 (Adapter + paquetes publicados + latest.release).

**¿Qué patrón usaron para X?** Tabla §18.

**¿Qué mejorarían?** Paréntesis y menos unario; reportar YAML inválido; arreglar `--help`; sacar
código muerto; alinear READMEs; volver obligatorio PMD con un ruleset curado; evitar el doble
disparo de `publish.yml`; quitar la recursión del parseo de bloques; `sealed` en la jerarquía de
nodos para switches exhaustivos.

---

## 22. Glosario

| Término | Significado |
|---|---|
| **Lexema / token** | texto reconocido (`let`) / ese texto clasificado con tipo y posición (`LET [1:1]`) |
| **Lexer** | convierte caracteres en tokens |
| **Parser** | convierte tokens en un árbol según la gramática |
| **AST** | árbol de sintaxis abstracta: estructura del programa sin detalles de puntuación |
| **Análisis semántico** | chequeos de significado: tipos, variables declaradas, constantes |
| **Maximal munch** | tomar el token más largo posible |
| **Pratt parser** | parser de expresiones guiado por *binding powers* |
| **Binding power** | qué tan fuerte "agarra" un operador a sus operandos |
| **Lookahead / LL(k)** | mirar k tokens adelante sin consumirlos para decidir |
| **Backtracking** | probar una alternativa y volver atrás si falla |
| **Streaming / pull** | procesar a medida que se consume; el consumidor pide los datos |
| **Memoización** | calcular una vez y guardar el resultado |
| **Persistente (estructura)** | las versiones viejas siguen válidas después de "modificarla" |
| **Scope** | región donde una variable es visible |
| **Golden file** | salida esperada guardada contra la que se compara |
| **TCK** | kit de pruebas de compatibilidad con una especificación |
| **Convention plugin** | plugin de Gradle con configuración compartida que aplican los subproyectos |
| **Toolchain** | versión de JDK que Gradle usa para compilar/correr, independiente del JDK instalado |
| **GitHub Packages** | registro de paquetes (Maven) de GitHub |
| **Reusable workflow** | workflow de Actions invocable desde otros repos (`workflow_call`) |
| **Testcontainers** | librería que levanta contenedores Docker desde los tests |
| **Test double** | Fake / Stub / Spy / Mock: reemplazos de colaboradores en tests |

---

## 23. Chuleta de comandos

```bash
# Build y calidad
./gradlew build                      # compila, tests, check (y reformatea con spotlessApply)
./gradlew test                       # tests de todos los módulos (+ JaCoCo por módulo)
./gradlew check                      # spotless, checkstyle, pmd, spotbugs, cobertura, containerTest
./gradlew spotlessApply              # formatear
./gradlew spotlessCheck              # verificar formato
./gradlew checkstyleMain pmdMain spotbugsMain
./gradlew jacocoRootReport           # cobertura agregada → build/reports/jacoco-aggregate/html/index.html
./gradlew :com.ingsis.engine:containerTest   # requiere Docker
./gradlew :com.ingsis.lexer:test --tests '*LexerUnitTest'   # un test puntual

# CLI
./gradlew :com.ingsis.engine:installDist
B=./com.ingsis.engine/build/install/com.ingsis.engine/bin/com.ingsis.engine
$B Execution  -v 1.0 -i prog.ps
$B Validation -v 1.1 -i prog.ps
$B Formatting -v 1.1 -i prog.ps -c reglas.yaml [-o salida.ps]
$B Analyzing  -v 1.0 -i prog.ps -c reglas_sca.yaml
$B -v 1.1                            # REPL (sin -i y sin stdin)
echo 'println("hola");' | $B exec -v 1.0      # desde stdin

# Atajos
./run.sh exec 1.1 samples/exec/v11_const_if.ps
./samples/probar_todo.sh             # demo completa

# Hooks
./install-hooks.sh                   # o ./gradlew installGitHooks

# TCK (en ~/Faculty/ingsis/printscript--tck)
./gradlew test -PgprUser=<usuario> -PgprToken=<token>        # última release
./gradlew test -PprintscriptVersion=1.2.0 ...                 # versión fija

# Release
git tag -a v1.3.0 -m "Release 1.3.0" && git push origin v1.3.0
```
