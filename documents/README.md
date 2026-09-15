# 📚 PrintScript - Documentación del Sistema

## Descripción General

**PrintScript** es un analizador léxico y sintáctico modular para un lenguaje de programación educativo, diseñado con un enfoque en principios **SOLID**, patrones de diseño **GoF** y restricciones de código limpio.

El sistema implementa un **pipeline de compilación** completo que transforma código fuente en un Árbol de Sintaxis Abstracta (AST):

```
Código Fuente (.ps)
       │
       ▼
┌─────────────────────┐
│  com.ingsis.common   │  Modelos de dominio, abstracciones y utilidades compartidas
└──────────┬──────────┘
           │
           ▼
┌──────────────────────────┐
│  com.ingsis.charstream   │  Lectura de caracteres con posición y pushback
└──────────┬───────────────┘
           │ SafeIterator<MetaCharacter>
           ▼
┌─────────────────────┐
│  com.ingsis.lexer    │  Tokenización con algoritmo Maximal Munch
└──────────┬──────────┘
           │ SafeIterator<Token>
           ▼
┌─────────────────────┐
│  com.ingsis.parser   │  Análisis sintáctico con Pratt Parsing → AST
└──────────┬──────────┘
           │ ProgramNode (AST)
           ▼
    [ Intérprete / Validador ]
```

---

## Índice de Módulos

| Módulo | Descripción | Documentación |
|--------|-------------|---------------|
| `com.ingsis.common` | Modelos de dominio, Result monad, SafeIterator, TokenType matchers | [📄 Ver documentación](./common/README.md) |
| `com.ingsis.charstream` | Streaming de caracteres con posición y soporte de pushback | [📄 Ver documentación](./charstream/README.md) |
| `com.ingsis.lexer` | Analizador léxico con algoritmo Maximal Munch | [📄 Ver documentación](./lexer/README.md) |
| `com.ingsis.parser` | Analizador sintáctico con Pratt Parsing y construcción de AST | [📄 Ver documentación](./parser/README.md) |
| `com.ingsis.interpreter` | Motor de ejecución, evaluación de expresiones, scopes y built-ins | [📄 Ver documentación](./interpreter/README.md) |
| `com.ingsis.formatter` | Formateador de código fuente por AST y flujo de tokens | [📄 Ver documentación](./formatter/README.md) |
| `com.ingsis.sca` | Analizador estático de código (linter de convenciones y buenas prácticas) | [📄 Ver documentación](./sca/README.md) |
| `com.ingsis.engine` | CLI (Picocli) y orquestador de servicios (ejecución, validación, linting, formateo, REPL) | [📄 Ver documentación](./engine/README.md) |
| `buildSrc` | Plugins de convención Gradle y suite de calidad (Spotless, Checkstyle, PMD, SpotBugs, JaCoCo) | [📄 Ver documentación](../buildSrc/README.md) |
| `samples` | Programas de muestra (`.ps`) y configuraciones de reglas YAML (`format`, `sca`) | [📄 Ver documentación](../samples/README.md) |

### 📖 Guías de Arquitectura y Runtimes
- [🌳 Construcción de Árboles Sintácticos (AST), Nodos Cabeza y Runtimes (readEnv, readInput, if)](file:///home/elchurro274/Faculty/ingsis/PrintScript/documents/CONSTRUCCION_ARBOLES_Y_RUNTIMES.md)
- [⚙️ Arquitectura y Funcionamiento de Interpreter y Formatter](file:///home/elchurro274/Faculty/ingsis/PrintScript/documents/INTERPRETER_AND_FORMATTER.md)
- [🔍 Handlers del Formatter y Funcionamiento del Linter SCA](file:///home/elchurro274/Faculty/ingsis/PrintScript/documents/FORMATTER_HANDLERS_AND_SCA.md)
- [📦 Sistema y Decisiones de Diseño de com.ingsis.common](file:///home/elchurro274/Faculty/ingsis/PrintScript/documents/common/SISTEMA_COMMON.md)

---

## Diagrama de Dependencias entre Módulos

```
com.ingsis.common (Base)
       │
       ├──────────────────────────┐
       │                          │
       ▼                          ▼
com.ingsis.charstream      com.ingsis.parser
       │                          │
       └────────┐                 │
                ▼                 │
         com.ingsis.lexer ◄───────┘
                │
                ├─────────────────────────────────────┐
                ▼                                     ▼
        com.ingsis.interpreter               com.ingsis.formatter
                ▲                                     ▲
                │                                     │
                └───────────────┬─────────────────────┘
                                │
                        com.ingsis.sca
                                │
                                ▼
                        com.ingsis.engine (CLI / Orquestador)
```

- **`common`** → No depende de ningún otro módulo. Es la base de todo el sistema.
- **`charstream`** → Depende de `common` (usa `SafeIterator`, `MetaCharacter`, `Position`, `Result`).
- **`lexer`** → Depende de `common` y `charstream` (consume `SafeIterator<MetaCharacter>`).
- **`parser`** → Depende de `common` (usa `Token`, `TokenType`, `SafeIterator`, `Result`, nodos AST).
- **`interpreter`** → Depende de `common` y `parser` (ejecuta el AST y gestiona entornos).
- **`formatter`** → Depende de `common`, `charstream`, `lexer` y `parser` (reconstruye código formateado).
- **`sca`** → Depende de `common`, `parser` e `interpreter` (analiza AST y reglas sintácticas/semánticas).
- **`engine`** → Fachada que integra todos los módulos y expone la interfaz de línea de comandos (CLI) y servicios públicos.

---

## Decisiones de Diseño Globales

### 1. Evaluación Perezosa (Lazy Streaming)
El sistema procesa archivos extensos sin cargarlos completamente en memoria. Cada componente implementa `SafeIterator<T>`, emitiendo elementos solo cuando el consumidor invoca `next()`.

### 2. Manejo de Errores sin Excepciones
Se utiliza la mónada `Result<T>` (tipo algebraico sellado) en lugar de excepciones para control de flujo, forzando al consumidor a manejar ambos casos (éxito/fallo).

### 3. Inmutabilidad
Uso extensivo de Records de Java para garantizar inmutabilidad en modelos de datos (`Token`, `Position`, `MetaCharacter`, `IterationStep`).

### 4. Principios SOLID
- **S**: Cada clase tiene una única responsabilidad.
- **O**: Los matchers y parsers son extensibles sin modificar código existente.
- **L**: Las implementaciones respetan los contratos de sus interfaces.
- **I**: Interfaces pequeñas y cohesivas (`CharReader`, `Tokenizer`, `Parser<T>`).
- **D**: Dependencia de abstracciones, no de implementaciones concretas.

---

## Tecnologías

- **Lenguaje**: Java 21 (Records, Sealed Types, Pattern Matching)
- **Build System**: Gradle (multi-módulo con convención Kotlin DSL en `buildSrc`)
- **CLI**: Picocli
- **Testing**: JUnit 6.0.0 (Jupiter)

---

## Ejecución de Tests y Control de Calidad

```bash
# Compilación y ejecución de tests
./gradlew test

# Verificación completa de calidad (Spotless, Checkstyle, SpotBugs, PMD, JaCoCo)
./gradlew check
```

Salida esperada de tests:
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

BUILD SUCCESSFUL
```
