# 📦 Módulo `com.ingsis.charstream`

## Descripción General

El módulo `com.ingsis.charstream` es el **componente de entrada de infraestructura** para el analizador léxico de PrintScript. Su función principal es transformar un flujo crudo de bytes de un `java.io.Reader` en un flujo de `MetaCharacter`, que son objetos que encapsulan un carácter junto con su posición exacta (línea y columna) en el archivo fuente.

Este módulo actúa como un **adaptador** entre la lectura de bajo nivel de caracteres y el procesamiento de alto nivel del lexer.

---

## Dependencias

| Módulo | Relación |
|--------|----------|
| `com.ingsis.common` | **Depende de** — Usa `SafeIterator`, `IterationStep`, `MetaCharacter`, `Position`, `Result`. |

---

## Responsabilidades Clave

1. **Abstracción de Lectura Física de I/O**: Lee texto fuente carácter por carácter desde cualquier `java.io.Reader`.
2. **Seguimiento de Posición Espacial**: Calcula y mantiene las coordenadas de línea y columna, gestionando saltos de línea Unix (`\n`), Mac pre-OSX (`\r`) y Windows (`\r\n`).
3. **Enriquecimiento de Datos**: Asocia cada carácter con su posición exacta, empaquetándolos en objetos `MetaCharacter`.
4. **Soporte de Lookahead/Pushback**: Permite "desleer" caracteres devolviéndolos al buffer para resolver ambigüedades en la tokenización.
5. **Iteración Funcional e Inmutable**: Proporciona una interfaz `SafeIterator` donde el avance produce un nuevo estado en lugar de mutar el existente.

---

## Estructura de Archivos

```
com.ingsis.charstream/
├── build.gradle
├── README.md
└── src/
    ├── main/java/charstream/
    │   ├── CharReader.java
    │   ├── StreamCharReader.java
    │   ├── PositionTracker.java
    │   └── CharStream.java
    └── test/java/
        (vacío actualmente — los tests de integración están en el módulo lexer)
```

---

## Detalle de Clases e Interfaces

### `CharReader` — Interfaz

Define el contrato para la lectura de caracteres de un flujo. Abstrae la fuente de entrada, permitiendo diferentes implementaciones. Extiende `AutoCloseable`.

| Método | Firma | Descripción |
|--------|-------|-------------|
| `readNextChar()` | `int readNextChar() throws IOException` | Lee el siguiente carácter y lo devuelve como entero. Retorna `-1` al alcanzar EOF. |
| `unread()` | `default void unread(int c) throws IOException` | Devuelve un carácter al buffer de lectura. Implementación default vacía (no-op). |
| `close()` | `void close() throws Exception` | Cierra el lector y libera recursos (heredado de `AutoCloseable`). |

---

### `StreamCharReader` — Clase

Implementación concreta de `CharReader` que envuelve un `java.io.Reader` estándar con capacidades de buffering y pushback.

**Decoración interna:**
- `BufferedReader` con buffer de **8192 bytes** para lectura eficiente.
- `PushbackReader` con buffer de **1024 caracteres** para soporte de backtracking.

| Método | Firma | Descripción |
|--------|-------|-------------|
| *constructor* | `StreamCharReader(Reader reader)` | Recibe un `Reader` genérico y lo envuelve con `BufferedReader` + `PushbackReader`. |
| `readNextChar()` | `int readNextChar() throws IOException` | Delega a `reader.read()` del `PushbackReader` interno. |
| `unread()` | `void unread(int c) throws IOException` | Si `c != -1`, invoca `reader.unread(c)` para devolver el carácter al buffer. |
| `close()` | `void close() throws Exception` | Cierra el `PushbackReader` subyacente. |

---

### `PositionTracker` — Clase Inmutable

Rastrea la posición (línea y columna) del carácter actual en el archivo fuente de forma **inmutable**. Cada avance crea una nueva instancia.

Las coordenadas comienzan en `(1, 1)`.

| Método | Firma | Descripción |
|--------|-------|-------------|
| *constructor* | `PositionTracker()` | Inicializa en línea 1, columna 1, último carácter `'\0'`. |
| `advance()` | `PositionTracker advance(char currentChar)` | Calcula y devuelve un **nuevo** `PositionTracker` con las coordenadas actualizadas. |
| `getLine()` | `int getLine()` | Retorna el número de línea actual. |
| `getColumn()` | `int getColumn()` | Retorna el número de columna actual. |

**Lógica de avance:**

| Carácter | Acción |
|----------|--------|
| `\n` (después de `\r`) | Secuencia CRLF: incrementa línea, resetea columna a 1. |
| `\n` (sin `\r` previo) | Incrementa línea +1, resetea columna a 1. |
| `\r` | Incrementa línea +1, resetea columna a 1. |
| Cualquier otro | Mantiene línea, incrementa columna +1. |

---

### `CharStream` — Clase Final

Clase principal del módulo. Implementa `SafeIterator<MetaCharacter>` y combina la lectura de caracteres con el rastreo de posición.

| Método | Firma | Descripción |
|--------|-------|-------------|
| *constructor* | `CharStream(CharReader reader)` | Inicializa el stream con un `CharReader` y un `PositionTracker` en `(1,1)`. |
| `next()` | `Result<IterationStep<MetaCharacter>> next()` | Lee el siguiente carácter, calcula su `Position`, lo empaqueta en `MetaCharacter` y retorna un nuevo `CharStream` avanzado. Retorna `IncorrectResult` en EOF o error I/O. |
| `unread()` | `void unread(MetaCharacter item)` | Delega a `reader.unread(item.character())` para devolver el carácter al stream. |

**Flujo del método `next()`:**

```
1. reader.readNextChar()
   │
   ├── (-1) → Result.failure("EOF")
   │
   ├── IOException → Result.failure("I/O Error: ...")
   │
   └── (char c) → 
       ├── Calcula Position desde PositionTracker
       ├── Crea MetaCharacter(c, position)
       ├── Avanza PositionTracker: tracker.advance(c)
       ├── Crea nuevo CharStream(reader, newTracker)
       └── Result.success(IterationStep(metaChar, nextStream))
```

---

## Patrones de Diseño

| Patrón | Aplicación |
|--------|-----------|
| **Adapter** | `StreamCharReader` adapta `java.io.Reader` a la interfaz `CharReader` del dominio. |
| **Decorator** | `StreamCharReader` decora el `Reader` con `BufferedReader` + `PushbackReader`. |
| **Immutable Iterator** | `CharStream` implementa `SafeIterator<MetaCharacter>`. No muta estado; `next()` produce una nueva tupla. |
| **Immutable Value Object** | `PositionTracker` es inmutable. Cada `advance()` retorna una nueva instancia. |
| **Result Monad** | Manejo funcional de EOF y errores I/O mediante `Result<T>`. |

---

## Integración en el Pipeline

```
[ Fuente de Entrada: Archivo / String ]
              │
              ▼
  [ StreamCharReader ]  ←  java.io.Reader + PushbackReader (1024 chars)
              │
              ▼
     [ CharStream ]  ←  PositionTracker rastrea línea/columna
              │
              ▼  Produce Result<IterationStep<MetaCharacter>>
        [ Lexer ]
              │
              ▼  Combina MetaCharacters y emite Tokens
         [ Parser ]
```

1. **Entrada**: El código fuente se entrega envuelto en un `StreamCharReader`.
2. **Procesamiento**: `CharStream` consume la entrada carácter a carácter y adjunta posición exacta.
3. **Consumo por el Lexer**: El Lexer itera sobre `CharStream`, acumulando caracteres para formar tokens.
4. **Lookahead y Retroceso**: Si el Lexer lee de más, invoca `unread(metaChar)` para restituir al stream.
5. **Reporte de Errores**: Cada carácter transporta su `Position`, permitiendo reportar errores con línea y columna exactas.
