# Paquete: charstream

Contiene los adaptadores de I/O para streaming de caracteres con pushback lookahead.

## 📄 Clases y Métodos

### `CharReader` (Interfaz)
* `int readNextChar()`: Lee el código del siguiente caracter.
* `void unread(int c)`: Devuelve un caracter al buffer de lectura.
* `void close()`: Cierra el lector subyacente.

### `StreamCharReader` (Clase)
* Implementa `CharReader` utilizando un `PushbackReader` de 1024 caracteres de buffer.

### `PositionTracker` (Clase Inmutable)
* `PositionTracker advance(char currentChar)`: Incrementa línea en `\n`/`\r` o columna en caracteres normales.
* `int getLine()`, `int getColumn()`: Coordenadas actuales.

### `CharStream` (Clase)
* Implementa `SafeIterator<MetaCharacter>`.
* `Result<IterationStep<MetaCharacter>> next()`: Lee el siguiente caracter y calcula su `Position`.
* `void unread(MetaCharacter item)`: Devuelve el caracter al `CharReader` para permitir lookahead pushback de forma polimórfica.
