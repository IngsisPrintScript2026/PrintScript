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
```

- **`common`** → No depende de ningún otro módulo. Es la base de todo el sistema.
- **`charstream`** → Depende de `common` (usa `SafeIterator`, `MetaCharacter`, `Position`, `Result`).
- **`lexer`** → Depende de `common` y `charstream` (consume `SafeIterator<MetaCharacter>`).
- **`parser`** → Depende de `common` (usa `Token`, `TokenType`, `SafeIterator`, `Result`, nodos AST).

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

- **Lenguaje**: Java
- **Build System**: Gradle (multi-módulo)
- **Testing**: JUnit 6.0.0 (Jupiter)

---

## Ejecución de Tests

```bash
./gradlew test
```

Salida esperada:
```
> Task :com.ingsis.common:test PASSED
> Task :com.ingsis.charstream:test PASSED
> Task :com.ingsis.lexer:test PASSED
> Task :com.ingsis.parser:test PASSED

BUILD SUCCESSFUL
```
